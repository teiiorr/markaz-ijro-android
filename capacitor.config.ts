import type { CapacitorConfig } from "@capacitor/cli";

/**
 * Capacitor config — native shell points at the live web app. Whatever ships
 * to https://markaz-ijro.uz is what the user sees in the APK; we never have
 * to re-bundle the JS to push a UI change. Set responsive design = handles
 * every screen size automatically (the web already does).
 */
const config: CapacitorConfig = {
  appId: "uz.bkrm.markazijro",
  appName: "Markaz Ijro",
  // We don't ship a webDir — the app loads the remote URL below directly.
  // Capacitor still requires the field to be set; point it at the marker dir.
  webDir: "www",
  server: {
    // Production web app — same backend, same auth, same data.
    url: "https://markaz-ijro.uz",
    cleartext: false,
    // Allowlist: the WebView only navigates to the production host. External
    // links (mailto:, http external sites) open in the system browser via
    // the @capacitor/browser plugin.
    allowNavigation: ["markaz-ijro.uz", "www.markaz-ijro.uz"],
  },
  android: {
    // Match the brand colour from the web's dark logo chip.
    backgroundColor: "#0E1330",
    // Allow file downloads through the WebView (PDF / XLSX exports).
    allowMixedContent: false,
    captureInput: true,
    webContentsDebuggingEnabled: false,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1200,
      backgroundColor: "#0E1330",
      androidSplashResourceName: "splash",
      androidScaleType: "CENTER_CROP",
      showSpinner: false,
      splashFullScreen: true,
      splashImmersive: true,
    },
    StatusBar: {
      style: "DARK",
      backgroundColor: "#0E1330",
      overlaysWebView: false,
    },
  },
};

export default config;
