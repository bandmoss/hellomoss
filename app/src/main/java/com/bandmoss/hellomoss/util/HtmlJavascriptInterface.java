package com.bandmoss.hellomoss.util;

import android.webkit.JavascriptInterface;

/**
 * Created by rok on 2015. 3. 18..
 */
public interface HtmlJavascriptInterface {
    @JavascriptInterface
    public void processHtml(String html);
}
