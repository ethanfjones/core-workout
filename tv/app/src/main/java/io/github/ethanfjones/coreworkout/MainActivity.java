package io.github.ethanfjones.coreworkout;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
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
                Voice best = bestVoice();
                if (best != null) tts.setVoice(best);
                ttsReady = true;
            }
        });

        web.loadUrl(HOME);
        web.requestFocus();
    }

    /** Highest-quality installed voice for the TV's language, preferring on-device voices on a tie. */
    private Voice bestVoice() {
        try {
            String lang = Locale.getDefault().getLanguage();
            Voice best = null;
            for (Voice v : tts.getVoices()) {
                if (!lang.equals(v.getLocale().getLanguage())) continue;
                if (v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                if (best == null || score(v) > score(best)) best = v;
            }
            return best;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int score(Voice v) {
        int s = v.getQuality() * 10 - v.getLatency();
        if (!v.isNetworkConnectionRequired()) s += 5;
        if (v.getLocale().equals(Locale.getDefault())) s += 20;
        return s;
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
        web.resumeTimers();
        web.onResume();
    }

    /** Home or another app: pause the workout and stop its timers, so nothing keeps running unseen. */
    @Override
    protected void onPause() {
        web.evaluateJavascript("window.onTvPause && window.onTvPause()", null);
        if (ttsReady) tts.stop();
        web.onPause();
        web.pauseTimers();
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
        public void speakAt(String text, float rate) {
            if (!ttsReady) return;
            tts.setSpeechRate(rate);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue");
        }

        @JavascriptInterface
        public void stop() {
            if (ttsReady) tts.stop();
        }
    }
}
