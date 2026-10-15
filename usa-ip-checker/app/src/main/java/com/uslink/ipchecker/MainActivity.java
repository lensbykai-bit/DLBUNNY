package com.uslink.ipchecker;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
  private WebView webView;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final String HOME="file:///android_asset/index.html";
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(12,13,29));
    getWindow().setNavigationBarColor(Color.rgb(12,13,29));
    webView=new WebView(this);
    webView.setBackgroundColor(Color.rgb(12,13,29));
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setAllowFileAccessFromFileURLs(false);
    webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
    webView.addJavascriptInterface(new Bridge(),"Native");
    webView.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view,android.webkit.WebResourceRequest r){
        return !HOME.equals(r.getUrl().toString());
      }
    });
    setContentView(webView);
    webView.loadUrl(HOME);
  }
  private void send(String event,JSONObject data) {
    runOnUiThread(()->{if(webView!=null) webView.evaluateJavascript("window."+event+"("+data.toString()+")",null);});
  }
  public class Bridge {
    @JavascriptInterface public void refreshIp() {
      executor.execute(()->{
        JSONObject out=new JSONObject();
        HttpURLConnection c=null;
        try {
          c=(HttpURLConnection)new URL("https://ipwho.is/").openConnection();
          c.setRequestMethod("GET"); c.setConnectTimeout(10000); c.setReadTimeout(10000);
          c.setRequestProperty("Accept","application/json");
          if(c.getResponseCode()!=200) throw new Exception("HTTP "+c.getResponseCode());
          try(InputStream in=c.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int len;
            while((len=in.read(buffer))!=-1)bytes.write(buffer,0,len);
            JSONObject json=new JSONObject(bytes.toString("UTF-8"));
            if(!json.optBoolean("success",false)) throw new Exception("IP unavailable");
            JSONObject conn=json.optJSONObject("connection");
            out.put("ok",true);
            out.put("ip",json.optString("ip","—"));
            out.put("country",json.optString("country","Unknown"));
            out.put("countryCode",json.optString("country_code",""));
            out.put("city",json.optString("city","Unknown"));
            out.put("isp",conn==null?"Unknown":conn.optString("isp","Unknown"));
          }
        }catch(Exception e){
          try{out.put("ok",false);out.put("error","Connection failed. Check internet and retry.");}catch(Exception ignored){}
        }finally{if(c!=null)c.disconnect();}
        send("receiveLookup",out);
      });
    }
    @JavascriptInterface public void openVpnSettings(){
      runOnUiThread(()->{
        try{startActivity(new Intent(Settings.ACTION_VPN_SETTINGS));}
        catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
      });
    }
    @JavascriptInterface public void openTikTok(){
      runOnUiThread(()->{
        Intent i=getPackageManager().getLaunchIntentForPackage("com.zhiliaoapp.musically");
        if(i==null)i=getPackageManager().getLaunchIntentForPackage("com.ss.android.ugc.trill");
        if(i!=null)startActivity(i);
        else Toast.makeText(MainActivity.this,"TikTok is not installed",Toast.LENGTH_LONG).show();
      });
    }
    @JavascriptInterface public void copyIp(String value){
      runOnUiThread(()->{
        if(value==null||value.isEmpty()||"—".equals(value))return;
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Public IP",value));
        Toast.makeText(MainActivity.this,"IP copied",Toast.LENGTH_SHORT).show();
      });
    }
  }
  @Override protected void onDestroy(){
    executor.shutdownNow();
    if(webView!=null){webView.removeJavascriptInterface("Native");webView.destroy();webView=null;}
    super.onDestroy();
  }
}
