#if os(iOS)
    import ClaudeSpyCommon
    import ClaudeSpyNetworking
    import PhotosUI
    import SwiftUI

    /// Selects one photo and forwards it to the current remote tmux pane.
    ///
    /// The selected pane is captured before Photos data is loaded. A later
    /// focus change therefore cannot redirect an in-flight image to another
    /// pane.
    struct ImageUploadToolbarButton: View {
        let paneId: String?
        let relayClient: ViewerRelayClient

        @State private var selectedPhoto: PhotosPickerItem?
        @State private var uploadTask: Task<Void, Never>?
        @State private var uploadID: UUID?
        @State private var errorMessage: String?

        var body: some View {
            PhotosPicker(
                selection: $selectedPhoto,
                matching: .images,
                preferredItemEncoding: .automatic
            ) {
                if uploadID != nil {
                    ProgressView()
                        .accessibilityLabel("Sending Image")
                } else {
                    Label("Send Image", symbol: .photoBadgePlus)
                }
            }
            .disabled(!canSendImage)
            .accessibilityIdentifier("terminal-send-image")
            .onChange(of: selectedPhoto) { _, newValue in
                guard let newValue else { return }
                selectedPhoto = nil
                startUpload(newValue)
            }
            .onDisappear(perform: cancelUpload)
            .alert("Image Upload Failed", isPresented: .init(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK") { errorMessage = nil }
            } message: {
                if let errorMessage {
                    Text(errorMessage)
                }
            }
        }

        private var canSendImage: Bool {
            relayClient.isHostConnected && paneId != nil && uploadID == nil
        }

        private func startUpload(_ item: PhotosPickerItem) {
            guard
                uploadID == nil,
                relayClient.isHostConnected,
                let targetPaneId = paneId
            else { return }

            let currentUploadID = UUID()
            uploadID = currentUploadID
            errorMessage = nil

            uploadTask = Task { @MainActor in
                do {
                    guard let data = try await item.loadTransferable(type: Data.self) else {
                        throw ImageUploadError.unreadable
                    }
                    try Task.checkCancellation()

                    let prepared = await Task.detached(priority: .userInitiated) {
                        RelayImagePreparer.prepare(
                            data,
                            maxBytes: SendDroppedFiles.maxRawBytes
                        )
                    }.value
                    try Task.checkCancellation()

                    guard let prepared else {
                        throw ImageUploadError.cannotFitRelayLimit
                    }
                    guard relayClient.isHostConnected else {
                        throw ImageUploadError.disconnected
                    }

                    let filename = "pasted-image-\(UUID().uuidString).\(prepared.format.fileExtension)"
                    let result = await relayClient.sendCommand(
                        SendDroppedFiles(files: [
                            DroppedFile(name: filename, data: prepared.data),
                        ]),
                        paneId: targetPaneId,
                        timeout: 30
                    )
                    try Task.checkCancellation()

                    if case let .failure(error) = result {
                        throw error
                    }
                } catch is CancellationError {
                    // Navigating away cancels silently.
                } catch {
                    guard uploadID == currentUploadID else { return }
                    errorMessage = error.localizedDescription
                }

                guard uploadID == currentUploadID else { return }
                uploadTask = nil
                uploadID = nil
            }
        }

        private func cancelUpload() {
            uploadTask?.cancel()
            uploadTask = nil
            uploadID = nil
        }
    }

    private enum ImageUploadError: LocalizedError {
        case unreadable
        case cannotFitRelayLimit
        case disconnected

        var errorDescription: String? {
            switch self {
            case .unreadable:
                "The selected photo could not be read."
            case .cannotFitRelayLimit:
                "The selected photo could not be compressed to the relay's \(SendDroppedFiles.maxRawBytes / 1_024) KB limit."
            case .disconnected:
                "The remote Mac disconnected before the image was sent."
            }
        }
    }
#endif
