package id.sdn6sibetan.absen;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.content.Intent;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> picker;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#1f5fbf"));
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (picker != null) picker.onReceiveValue(null);
                picker = cb;
                try { startActivityForResult(p.createIntent(), 7); } catch (Exception e) { picker = null; return false; }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == 7 && picker != null) {
            picker.onReceiveValue(res == RESULT_OK && data != null && data.getData() != null ? new Uri[]{data.getData()} : null);
            picker = null;
        }
    }

    @Override public void onBackPressed() { moveTaskToBack(true); }

    class Bridge {
        @JavascriptInterface public void saveFile(String name, String mime, String text) {
            write(name, mime, text.getBytes(StandardCharsets.UTF_8));
        }
        @JavascriptInterface public void saveBase64(String name, String mime, String b64) {
            write(name, mime, android.util.Base64.decode(b64, android.util.Base64.DEFAULT));
        }
        @JavascriptInterface public void printHtml(final String html) {
            runOnUiThread(() -> {
                WebView w = new WebView(MainActivity.this);
                w.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView v, String url) {
                        PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        pm.print("Rekap Absensi", v.createPrintDocumentAdapter("Rekap_Absensi"),
                            new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape()).build());
                    }
                });
                w.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            });
        }
    }
    private void write(String name, String mime, byte[] data) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                try (OutputStream o = getContentResolver().openOutputStream(u)) { o.write(data); }
                toast("Tersimpan di folder Download: " + name);
            } else {
                java.io.File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                dir.mkdirs();
                java.io.File f = new java.io.File(dir, name);
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) { o.write(data); }
                toast("Tersimpan: " + f.getAbsolutePath());
            }
        } catch (Exception e) { toast("Gagal menyimpan: " + e.getMessage()); }
    }
    private void toast(final String t) { runOnUiThread(() -> Toast.makeText(this, t, Toast.LENGTH_LONG).show()); }
}
