package com.bandmoss.hellomoss.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by rok on 2015. 3. 17..
 */
public class ScrollObservableWebview extends WebView {
    private OnScrollListener mOnScrollListener;

    public ScrollObservableWebview(final Context context) {
        super(context);
    }

    public ScrollObservableWebview(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollObservableWebview(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollListener != null) mOnScrollListener.onScroll(l, t, oldl, oldt);
    }

    public OnScrollListener getOnScrollChangedCallback() {
        return mOnScrollListener;
    }

    public void setOnScrollChangedCallback(final OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    public static interface OnScrollListener {
        public void onScroll(int l, int t, int oldl, int oldt);
    }

}
