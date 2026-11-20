package uz.evg.energo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onBack && window.onBack()", v -> {
            if (!"true".equals(v)) MainActivity.super.onBackPressed();
        });
    }

    class Bridge {
        /** Скачивает страницу в фоне и возвращает текст в JS-колбэк. */
        @JavascriptInterface
        public void fetch(final String url, final String cb) {
            new Thread(() -> {
                String result;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(12000);
                    c.setReadTimeout(15000);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) Energo/1.0");
                    c.setInstanceFollowRedirects(true);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append('\n');
                    r.close();
                    result = sb.toString();
                } catch (Exception e) {
                    result = "ERROR:" + e.getMessage();
                }
                final String js = cb + "(" + org.json.JSONObject.quote(result) + ")";
                runOnUiThread(() -> web.evaluateJavascript(js, null));
            }).start();
        }

        @JavascriptInterface
        public void share(final String text) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(i, "Отправить расчёт"));
            });
        }
    }
}
