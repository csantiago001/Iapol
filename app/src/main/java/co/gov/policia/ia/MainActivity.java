package co.gov.policia.ia;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView webView;

    private static final String URL =
            "https://app.ia.policia.gov.co/login";

    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Safari/537.36";

    // Ancho virtual de escritorio (px)
    private static final int DESKTOP_WIDTH = 1280;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        // Contenedor que respeta la barra de estado y la de navegación.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#1B2F6B"));
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(root);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Simula Chrome en un computador.
        settings.setUserAgentString(DESKTOP_USER_AGENT);

        // Escala inicial. Si se corta a los lados prueba 40; si se ve chico, 50.
        webView.setInitialScale(45);

        // Cookies necesarias para iniciar sesión.
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView v, String url, Bitmap favicon) {
                v.evaluateJavascript(
                    "try{" +
                    "Object.defineProperty(screen,'width',{get:function(){return " + DESKTOP_WIDTH + ";}});" +
                    "Object.defineProperty(screen,'height',{get:function(){return 800;}});" +
                    "}catch(e){}", null);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                v.evaluateJavascript(
                    "var m=document.querySelector('meta[name=viewport]');" +
                    "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}" +
                    "m.setAttribute('content','width=" + DESKTOP_WIDTH + ", user-scalable=yes');", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {

                    if (android.os.Build.VERSION.SDK_INT >= 23) {

                        boolean cameraGranted =
                                checkSelfPermission(Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED;

                        boolean audioGranted =
                                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED;

                        if (!cameraGranted || !audioGranted) {
                            requestPermissions(
                                    new String[]{
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                    },
                                    1001
                            );
                            return;
                        }
                    }

                    request.grant(request.getResources());
                });
            }
        });

        // Solicita permisos al iniciar.
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    },
                    1001
            );
        }

        webView.loadUrl(URL);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
