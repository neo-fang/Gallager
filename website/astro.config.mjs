import { defineConfig } from "astro/config";
import sitemap from "@astrojs/sitemap";

export default defineConfig({
  // Production deployments must set their own public origin. Do not silently
  // publish canonical links for infrastructure owned by the upstream project.
  site: process.env.CTRLX_SITE_URL ?? "http://localhost:4321",
  integrations: [sitemap()],
});
