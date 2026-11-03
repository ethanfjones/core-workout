package io.github.ethanfjones.coreworkout;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

/** Full-screen WebView around the workout page, with the TV's own text-to-speech for voice cues. */
public class MainActivity extends Activity {
    private static final String HOST = "core-workout.gonzoscreen.com";
    private static final String HOME = "https://" + HOST + "/";
    private static final String OFFLINE = "file:///android_asset/index.html";

    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0E1116);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        // Default chrome client so confirm() dialogs show.
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                return !isOurs(req.getUrl());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame() && !OFFLINE.equals(req.getUrl().toString())) view.loadUrl(OFFLINE);
            }
        });
        web.addJavascriptInterface(new Speech(), "AndroidTTS");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                ttsReady = true;
            }
        });

        web.loadUrl(HOME);
        web.requestFocus();
    }

    private static boolean isOurs(Uri u) {
        if (u.toString().startsWith("file:///android_asset/")) return true;
        return "https".equals(u.getScheme()) && HOST.equals(u.getHost());
    }

    /** Back asks the page first (it ends a running workout with a confirm); otherwise the app closes. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("!!(window.onTvBack && window.onTvBack())", handled -> {
            if (!"true".equals(handled)) finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        web.destroy();
        super.onDestroy();
    }

    private class Speech {
        @JavascriptInterface
        public void speak(String text) {
            if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue");
        }

        @JavascriptInterface
        public void stop() {
            if (ttsReady) tts.stop();
        }
    }
}
