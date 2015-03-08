package org.no_ip.zhouzian.mynas;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.no_ip.zhouzian.mynas.infrastructure.CifsDownloadManager;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //Remove title bar. We'll use Bootstrap nav to replace it.
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); //Enable javascript
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);   //Enable Chrome remote debug on Kitkat or later
        }
        webView.setWebViewClient(new NASWebViewClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/view/index.html");      //Load single page AngularJS app
        initializeJSInterface();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            webView.loadUrl("javascript:Global.onMenuClicked()");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        CifsDownloadManager.Sync();
        super.onStop();
    }

    @Override
    public void onBackPressed(){
        webView.loadUrl("javascript:Global.onBackClicked()");
    }

    /* Initialize the interface between javascript and back-end java code */
    private void initializeJSInterface() {
        webView.addJavascriptInterface(new WebAppInterface(this, webView), "webAppInterface");
    }

    /* Used to override open link behavior. By default, new links are opened in browser, but we want them to
     * be opened in the application webview */
    private class NASWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading( WebView view, String url ) {
            // open all links in the webView
            return false;
        }
    }
}
