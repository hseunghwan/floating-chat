package spike.floatask;

// SPIKE / THROWAWAY. 실현 가능성 확인용. 제품 코드로 쓰지 말 것.

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.net.URLEncoder;

public class AskActivity extends Activity {
    static final String TAG = "FA";
    static final String[][] SERVICES = {
            // name, ask url prefix, home
            {"GPT", "https://chatgpt.com/?q=", "https://chatgpt.com/"},
            {"Claude", "https://claude.ai/new?q=", "https://claude.ai/new"},
            {"Gemini", "https://gemini.google.com/app?q=", "https://gemini.google.com/app"},
    };

    WebView web;
    TextView logView;
    SharedPreferences prefs;
    String question; // null = 로그인용 홈 열기

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("spike", MODE_PRIVATE);
        int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.8);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, h);
        getWindow().setGravity(Gravity.BOTTOM);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(this);
        for (int i = 0; i < SERVICES.length; i++) {
            final int idx = i;
            bar.addView(button(SERVICES[i][0], v -> { prefs.edit().putInt("svc", idx).apply(); load(); }));
        }
        bar.addView(button("UA", v -> { prefs.edit().putBoolean("stripWv", !stripWv()).apply(); applyUa(); load(); }));
        bar.addView(button("X", v -> finish()));
        root.addView(bar);

        logView = new TextView(this);
        logView.setTextSize(10);
        logView.setMaxLines(5);
        root.addView(logView);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        applyUa();
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                if (m.message().startsWith("FA ")) log(m.message().substring(3));
                return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                log("loaded " + url);
                if (question != null) v.evaluateJavascript(autoSendJs(question), null);
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        handle(i);
    }

    void handle(Intent i) {
        CharSequence sel = i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        question = sel == null ? null : sel + suffixOf(i.getComponent());
        log("q=" + question + " ua.stripWv=" + stripWv());
        load();
    }

    void load() {
        String[] svc = SERVICES[prefs.getInt("svc", 0)];
        try {
            web.loadUrl(question == null ? svc[2] : svc[1] + URLEncoder.encode(question, "UTF-8"));
        } catch (Exception e) {
            log("load fail " + e);
        }
    }

    String suffixOf(ComponentName c) {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(c, PackageManager.GET_META_DATA);
            return ai.metaData == null ? "" : ai.metaData.getString("suffix", "");
        } catch (Exception e) {
            return "";
        }
    }

    boolean stripWv() { return prefs.getBoolean("stripWv", false); }

    void applyUa() {
        String ua = WebSettings.getDefaultUserAgent(this);
        web.getSettings().setUserAgentString(stripWv() ? ua.replace("; wv", "") : ua);
    }

    // 4초 동안 ?q= 자체 동작 관찰 → 안 보냈으면 JS로 입력 + 전송 버튼 클릭
    static String autoSendJs(String q) {
        return "(function(q){\n" +
                "if(window.__fa)return;window.__fa=1;\n" +
                "const log=m=>console.log('FA '+m), t0=Date.now(), path0=location.pathname;\n" +
                "const IN=['#prompt-textarea','div.ProseMirror[contenteditable=true]','rich-textarea .ql-editor','div[contenteditable=true]','textarea'];\n" +
                "const SEND=['button[data-testid=send-button]','button[aria-label*=\"Send\"]','button[aria-label*=\"send\"]','button[aria-label*=\"보내\"]','button[aria-label*=\"전송\"]','button.send-button'];\n" +
                "const find=a=>{for(const s of a){const e=document.querySelector(s);if(e)return e;}return null};\n" +
                "const txt=e=>e?(e.value!==undefined?e.value:e.innerText).trim():'';\n" +
                "let typed=false;\n" +
                "const iv=setInterval(()=>{const dt=Date.now()-t0, inp=find(IN);\n" +
                " if(dt<4000)return;\n" +
                " if(!typed){log('after4s path='+location.pathname+' input='+(inp?inp.tagName+':'+JSON.stringify(txt(inp).slice(0,30)):'none'));\n" +
                "  if(location.pathname!==path0&&location.pathname.length>1){log('RESULT: URL param auto-sent');clearInterval(iv);return;}\n" +
                "  if(!inp)return;\n" +
                "  if(!txt(inp)){inp.focus();if(inp.tagName==='TEXTAREA'){Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,'value').set.call(inp,q);inp.dispatchEvent(new Event('input',{bubbles:true}));}else{document.execCommand('selectAll');document.execCommand('insertText',false,q);}log('typed by JS');}\n" +
                "  else log('prefilled by URL param');\n" +
                "  typed=true;return;}\n" +
                " const b=find(SEND);\n" +
                " if(b&&!b.disabled){b.click();log('RESULT: sent by JS click');clearInterval(iv);setTimeout(()=>log('path now '+location.pathname),3000);return;}\n" +
                " if(dt>15000){log('RESULT: FAIL send button '+(b?'disabled':'not found'));clearInterval(iv);}\n" +
                "},500);\n" +
                "})(" + JSONObject.quote(q) + ");";
    }

    Button button(String t, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    void log(String m) {
        Log.i(TAG, m);
        runOnUiThread(() -> logView.append(m + "\n"));
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }
}
