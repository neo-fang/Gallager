package app.gallager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import app.gallager.android.ui.GallagerApp
import app.gallager.android.ui.GallagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GallagerTheme {
                val viewModel: GallagerViewModel = viewModel(
                    factory = GallagerViewModel.Factory(application as GallagerApplication),
                )
                GallagerApp(viewModel)
            }
        }
    }
}
