package pro.dbro.bart;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;

public class RouteViewActivity extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		WebView webview = new WebView(this);
		webview.getSettings().setBuiltInZoomControls(true);
		setContentView(webview);
		webview.loadUrl("file:///android_asset/BART_cc_map.png");
	}
} 