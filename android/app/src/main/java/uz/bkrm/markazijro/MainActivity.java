package uz.bkrm.markazijro;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

/**
 * MainActivity hooks three things into the Capacitor WebView:
 *
 * 1) A DownloadListener — server-generated files (XLSX exports, PDF reports,
 *    attachment downloads) get handed off to Android's system DownloadManager
 *    with the WebView's auth cookies forwarded. Without this, downloads would
 *    silently no-op, which is the WebView default.
 *
 * 2) A BridgeWebViewClient subclass — when the main-frame load fails because
 *    the device is offline, the bundled assets/public/offline.html page is
 *    shown instead of the bare Chrome error screen. The page reads
 *    navigator.onLine and auto-redirects to the live URL when connectivity
 *    is restored.
 *
 * 3) Aggressive cookie persistence — NextAuth sets a 30-day Expires on the
 *    session token, but WebView's CookieManager only flushes to disk on its
 *    own schedule. If the OS kills the app before that flush happens, the
 *    user wakes up logged out. We enable cookies explicitly, then flush on
 *    every onPause / onDestroy so the session survives a cold restart.
 */
public class MainActivity extends BridgeActivity {

    private static final int STORAGE_REQ = 1001;
    private static final String OFFLINE_URL = "file:///android_asset/public/offline.html";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Legacy storage permission is only needed for API < 29; on newer
        // versions DownloadManager writes to the user-visible Downloads
        // directory via scoped storage without an explicit prompt.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            int granted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQ);
            }
        }

        if (bridge != null && bridge.getWebView() != null) {
            WebView webView = bridge.getWebView();

            // Persistent cookies. The WebView speaks to a single first-party
            // origin (uzsiac-journal.uz / markaz-ijro.uz), so accept-cookie +
            // accept-third-party-cookie are both safe and required for the
            // auth callback (NextAuth) cookie chain.
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);

            // Extend Capacitor's bridge client so the bridge JS interface
            // keeps working — we only add error-handling on top.
            webView.setWebViewClient(new BridgeWebViewClient(bridge) {
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            && request != null
                            && request.isForMainFrame()
                            && !isOfflinePage(view.getUrl())) {
                        view.post(() -> view.loadUrl(OFFLINE_URL));
                    }
                }
            });

            // If the very first load fires before there's a network at all,
            // skip the broken Chrome error and go straight to offline.html.
            if (!hasNetwork()) {
                webView.post(() -> webView.loadUrl(OFFLINE_URL));
            }

            webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);

                    // Carry the WebView's cookies so authenticated routes
                    // like /api/export/* return the real file, not a 401.
                    String cookies = CookieManager.getInstance().getCookie(url);
                    if (cookies != null) {
                        request.addRequestHeader("Cookie", cookies);
                    }

                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.allowScanningByMediaScanner();
                    request.setTitle(fileName);
                    request.setDescription("Markaz Ijro");

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(this, fileName + " — yuklanmoqda…", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Yuklab olishda xatolik", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Force WebView to write its in-memory cookies to disk so the session
     * survives an OS-initiated kill or a swipe from Recents. Without this,
     * the user logs in and is asked to log in again on the next cold start
     * because the persistent Expires cookie never made it past RAM.
     */
    @Override
    public void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        CookieManager.getInstance().flush();
        super.onDestroy();
    }

    private static boolean isOfflinePage(String url) {
        return url != null && url.contains("offline.html");
    }

    private boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
