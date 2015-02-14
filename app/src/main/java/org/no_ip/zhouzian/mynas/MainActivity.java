package org.no_ip.zhouzian.mynas;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //Remove title bar. We'll use Bootstrap nav to replace it.
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);                //Enable javascript
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);   //Enable Chrome remote debug on Kitkat or later
        }
        webView.setWebViewClient(new NASWebViewClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/view/home/main_page.html");      //Load single page AngularJS app
        initializeJSInterface(webView);
    }
    /* Initialize the interface between javascript and back-end java code */
    private void initializeJSInterface(WebView webView) {
        webView.addJavascriptInterface(new WebAppInterface(this), "webAppInterface");
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
