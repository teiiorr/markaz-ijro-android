package uz.bkrm.markazijro;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

/**
 * MainActivity attaches a DownloadListener to the Capacitor WebView so that
 * server-generated files (XLSX exports, PDF reports, attachment downloads,
 * etc.) hand off to Android's system DownloadManager instead of doing
 * nothing — which is the WebView default.
 *
 * Cookies and the User-Agent are forwarded so the download is authenticated
 * the same way the page itself is. Files land in the public Downloads/
 * folder and trigger the system "download complete" notification.
 */
public class MainActivity extends BridgeActivity {

    private static final int STORAGE_REQ = 1001;

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
            bridge.getWebView().setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
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
}
