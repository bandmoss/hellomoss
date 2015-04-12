package com.bandmoss.hellomoss;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bandmoss.hellomoss.util.HtmlJavascriptInterface;
import com.bandmoss.hellomoss.util.Util;
import com.bandmoss.hellomoss.widget.ScrollObservableWebview;
import com.bandmoss.hellomoss.widget.TextDrawable;
import com.melnykov.fab.FloatingActionButton;
import com.mikepenz.iconics.typeface.FontAwesome;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = WebViewFragment.class.getSimpleName();

    public static final int FILECHOOSER_LOLLIPOP_REQUEST_CODE = 1;
    public static final int FILECHOOSER_BEFORE_KITKAT_REQUEST_CODE = 2;

    private static final String DEFAULT_URL = AppConstants.BASE_URL;

    // field for file handling
    private ValueCallback<Uri[]> mFilePathCallback;
    private ValueCallback<Uri> mFilePathCallbackForCompat;
    private String mCameraPhotoPath;

    // widgets
    private ScrollObservableWebview mWebView;
    private FloatingActionButton mFab;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // fragment listener
    private FragmentCallback mFragmentCallback;

    // etc
    private String mCurrentUrl;
    private boolean isFabHideOnScrollEnabled = true;
    private boolean isFabShowing = false;
    private boolean isFabHiding = false;
    private long downloadQueueId;
    private boolean isWriting;

    public WebViewFragment() {

    }

    public static WebViewFragment newInstance(String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            String url = args.getString("url", DEFAULT_URL);
            navigateTo(url);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        mWebView = (ScrollObservableWebview) rootView.findViewById(R.id.webView);
        mFab = (FloatingActionButton) rootView.findViewById(R.id.fab);

        initWebView();
        initFab();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mWebView.reload();
            }
        });
        mSwipeRefreshLayout.setProgressViewOffset(false, (int) getResources().getDimension(R.dimen.tool_bar_top_padding), (int) getResources().getDimension(R.dimen.appbar_x3));
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            int mLastHeightDifferece = 0;

            @Override
            public void onGlobalLayout() {
                // get screen frame rectangle
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                // get screen height
//                int screenHeight = getWindow().getDecorView().getHeight();
                int screenHeight = rootView.getRootView().getHeight();

                // calculate the height difference
                int heightDifference = screenHeight - (r.bottom - r.top);

                // if height difference is different then the last height difference and
                // is bigger then a third of the screen we can assume the keyboard is open
                if (heightDifference > screenHeight / 3 && heightDifference != mLastHeightDifferece) {
                    // keyboard visible
                    // get root view layout params
                    ViewGroup.LayoutParams lp = rootView.getLayoutParams();
                    // set the root view height to screen height minus the height difference
                    lp.height = screenHeight - heightDifference;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        lp.height += getResources().getDimension(R.dimen.tool_bar_top_padding);
                    }
                    // call request layout so the changes will take affect
                    rootView.requestLayout();
                    // save the height difference so we will run this code only when a change occurs.
                    mLastHeightDifferece = heightDifference;

                } else if (heightDifference != mLastHeightDifferece) {
                    // keyboard hidden
                    // get root view layout params and reset all the changes we have made when the keyboard opened.
                    ViewGroup.LayoutParams lp = rootView.getLayoutParams();
                    lp.height = screenHeight;
                    // call request layout so the changes will take affect
                    rootView.requestLayout();
                    // save the height difference so we will run this code only when a change occurs.
                    mLastHeightDifferece = heightDifference;
                }

            }
        });

        if (getArguments() != null) {
            String url = getArguments().getString("url", DEFAULT_URL);
            navigateTo(url);
        }

        return rootView;
    }

    private void initFab() {
        mFab.setAlpha(0.f);
        mFab.setScaleX(0.f);
        mFab.setScaleY(0.f);
        mFab.setImageDrawable(new TextDrawable(getActivity(), FontAwesome.Icon.faw_pencil, android.R.color.white));
        mFab.setOnClickListener(this);
        if (mWebView != null) {
            mWebView.setOnScrollChangedCallback(new ScrollObservableWebview.OnScrollListener() {
                private int mLastScrollY = -1;
                private int mScrollThreshold = getResources().getDimensionPixelOffset(com.melnykov.fab.R.dimen.fab_scroll_threshold);

                @Override
                public void onScroll(int l, int t, int oldl, int oldt) {
                    if (mLastScrollY < 0) mLastScrollY = t;
                    if (isFabHideOnScrollEnabled && mFab.getTag() != null) {
                        boolean isSignificantDelta = Math.abs(t - mLastScrollY) > mScrollThreshold;
                        if (isSignificantDelta) {
                            if (t > mLastScrollY) {
                                showFab();
                            } else {
                                hideFab();
                            }
                        }
                    }

                    if(mFragmentCallback != null && mFragmentCallback.getOnScrollListener() != null) {
                        mFragmentCallback.getOnScrollListener().onScroll(l, t, oldl, oldt);
                    }

                    mLastScrollY = t;
                }
            });
        }

        if(Util.hasNavigationBar(getActivity())) {
            int navbarHeight = Util.getNavigationBarHeight(getActivity());
            if (navbarHeight > 0) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFab.getLayoutParams();
                layoutParams.bottomMargin += navbarHeight;
                mFab.requestLayout();
            }
        }
    }

    public void showFab() {
        if (mFab != null && !isFabShowing) {
            isFabShowing = true;
            isFabHiding = false;
            mFab.post(new Runnable() {
                @Override
                public void run() {
                    mFab.setScaleX(mFab.getScaleX());
                    mFab.setScaleY(mFab.getScaleY());
                    mFab.setAlpha(mFab.getAlpha());
                    mFab.animate().scaleX(1.f).scaleY(1.f).alpha(1.f)
                            .setDuration(300)
                            .setInterpolator(new OvershootInterpolator())
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isFabShowing = false;
                                    mFab.setOnClickListener(WebViewFragment.this);
                                }
                            })
                            .start();
                }
            });
        }
    }

    public void hideFab() {
        if (mFab != null && !isFabHiding) {
            isFabShowing = false;
            isFabHiding = true;
            mFab.post(new Runnable() {
                @Override
                public void run() {
                    mFab.animate().scaleX(0.f).scaleY(0.f).alpha(0.f)
                            .setDuration(300)
                            .setInterpolator(new AnticipateOvershootInterpolator())
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isFabHiding = false;
                                    mFab.setOnClickListener(null);
                                }
                            })
                            .start();
                }
            });
        }
    }

    public void toggleFab() {
        if(mFab != null) {
            if(isFabHiding || isFabShowing) return;
            if(mFab.getAlpha() == 0.0f) {
                showFab();
            } else {
                hideFab();
            }
        }
    }

    public void setFabIcon(final FontAwesome.Icon icon) {
        mFab.post(new Runnable() {
            @Override
            public void run() {
                mFab.setImageDrawable(new TextDrawable(getActivity(), icon, android.R.color.white));
            }
        });
    }

    public void navigateTo(final String url) {
        if (mWebView != null) {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                    mWebView.loadUrl(url);
                }
            });
        }
    }

    public boolean triggerGoBack() {
        if (mWebView != null && mWebView.canGoBack()) {
            if(isWriting) {
                new MaterialDialog.Builder(getActivity())
                        .title(mWebView.getTitle())
                        .content(R.string.action_exit_confirm)
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.no)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                isWriting = false;
                                mWebView.goBack();
                                mWebView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(mFragmentCallback != null) {
                                            mFragmentCallback.onTitleChanged(mWebView.getTitle());
                                            mFragmentCallback.onLoadingCompleted(mWebView.getUrl());
                                        }
                                    }
                                }, 100);
                            }
                        })
                        .show();
            } else {
                mWebView.goBack();
                mWebView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mFragmentCallback != null) {
                            mFragmentCallback.onTitleChanged(mWebView.getTitle());
                            mFragmentCallback.onLoadingCompleted(mWebView.getUrl());
                        }
                    }
                }, 100);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mFab)) {
            if (mFab.getTag() != null && mFab.getTag() instanceof String) {
                final String tag = (String) mFab.getTag();
                if (tag.contains("/attach/")) {
                    if (Util.isDownloadManagerAvailable(getActivity())) {
                        new MaterialDialog.Builder(getActivity())
                                .title(R.string.action_download)
                                .content(R.string.action_download_confirm)
                                .positiveText(android.R.string.ok)
                                .negativeText(android.R.string.no)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        downloadQueueId = Util.requestDownload(getActivity(), tag);
                                    }
                                })
                                .show();
                    }
                    return;
                }

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    if (tag.contains("gallery")) {
                        new MaterialDialog.Builder(getActivity())
                                .title(R.string.action_write)
                                .content(R.string.action_write_alert_kitkat)
                                .cancelable(true)
                                .positiveText(android.R.string.ok)
                                .negativeText(android.R.string.no)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tag));
                                        startActivity(intent);
                                    }

                                    @Override
                                    public void onNegative(MaterialDialog dialog) {
                                        navigateTo(tag);
                                    }
                                })
                                .show();
                        return;
                    }
                }

                if (tag.startsWith("http://")) {
                    navigateTo(tag);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isFabHideOnScrollEnabled", isFabHideOnScrollEnabled);
        outState.putBoolean("isFabShowing", isFabShowing);
        outState.putBoolean("isFabHiding", isFabHiding);
        outState.putLong("downloadQueueId", downloadQueueId);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            isFabHideOnScrollEnabled = savedInstanceState.getBoolean("isFabHideOnScrollEnabled");
            isFabShowing = savedInstanceState.getBoolean("isFabShowing");
            isFabHiding = savedInstanceState.getBoolean("isFabHiding");
            downloadQueueId = savedInstanceState.getLong("downloadQueueId");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILECHOOSER_LOLLIPOP_REQUEST_CODE && mFilePathCallback != null) {
            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } else if (requestCode == FILECHOOSER_BEFORE_KITKAT_REQUEST_CODE && mFilePathCallbackForCompat != null) {
            Uri result = data == null || resultCode != Activity.RESULT_OK ? null : data.getData();

            Log.d(TAG, "mKitkatFileCompat:" + (result != null ? result.toString() : ""));
            mFilePathCallbackForCompat.onReceiveValue(result);
            mFilePathCallbackForCompat = null;

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("JavascriptInterface")
    private void initWebView() {
        WebSettings webSettings = mWebView.getSettings();

        // Enable Javascript
        webSettings.setJavaScriptEnabled(true);

        // Use WideViewport and Zoom out if there is no viewport defined
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // Enable pinch to zoom without the zoom buttons
        webSettings.setBuiltInZoomControls(true);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            webSettings.setDisplayZoomControls(false);
        }

        // enable webview cookie
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // AppRTC requires third party cookies to work
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }

        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);

        WebViewClient webViewClient = new XEWebViewClient();
        mWebView.setWebViewClient(webViewClient);
        mWebView.addJavascriptInterface(webViewClient, "HTMLOUT");
        mWebView.setWebChromeClient(new WebChromeClient() {

            private final String TAG = "WebChromeClient";

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if(mFragmentCallback != null && !view.getUrl().contains("/attach/")) {
                    mFragmentCallback.onTitleChanged(title);
                }
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return true;
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                // Set up the take picture intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = Util.createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                // Set up the intent to get an existing image
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                // Set up the intents for the Intent chooser
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQUEST_CODE);

                return true;

            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mFilePathCallbackForCompat = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_BEFORE_KITKAT_REQUEST_CODE);

            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mFilePathCallbackForCompat = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_BEFORE_KITKAT_REQUEST_CODE);
            }

            // For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mFilePathCallbackForCompat = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_BEFORE_KITKAT_REQUEST_CODE);
            }

        });
        mWebView.setOnTouchListener(new View.OnTouchListener() {

            GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent motionEvent) {
                    String url = mWebView.getUrl();
                    if (url != null && url.contains("/attach/")) {
                        Activity activity = getActivity();
                        if (activity != null && activity instanceof MainActivity) {
                            ((MainActivity) activity).toggleToolbar();
                        }
                    }
                    return false;
                }
            });

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });
    }

    private class XEWebViewClient extends WebViewClient implements HtmlJavascriptInterface {

        boolean isLoggedIn = false;
        int topPadding = -1;
        int bottomPadding = -1;

        @Override
        @JavascriptInterface
        public void processHtml(String html) {

            //check logged in
            if (!isLoggedIn && html.contains("dispMemberLogout")) {
                isLoggedIn = true;
                if (mFragmentCallback != null) {
                    mFragmentCallback.onLoginStateChanged(true);
                }
            } else if(isLoggedIn && (html.contains("procMemberLogin") || html.contains("dispMemberLoginForm"))) {
                isLoggedIn = false;
                if(mFragmentCallback != null) {
                    mFragmentCallback.onLoginStateChanged(false);
                }
            }

            //check write board provided
            Pattern pattern = Pattern.compile("\"/xe/.*dispBoardWrite\"");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                String postingUrl = "http://bandmoss.com" + matcher.group().replaceAll("\"", "").replace("&amp;", "&");
                setFabIcon(FontAwesome.Icon.faw_pencil);
                isFabHideOnScrollEnabled = true;
                mFab.setTag(postingUrl);
                showFab();
            } else {
                mFab.setTag(null);
                hideFab();
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if(mFragmentCallback != null) {
                mFragmentCallback.onLoading(url);
            }
            mSwipeRefreshLayout.setRefreshing(true);
            hideFab();
        }

        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            if (!url.contains("bandmoss")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            if(!isAdded()) return;

            mCurrentUrl = url;
            mSwipeRefreshLayout.setRefreshing(false);

            //viewing attachment image
            if(url.contains("/attach/")) {
                setFabIcon(FontAwesome.Icon.faw_download);
                isFabHideOnScrollEnabled = false;
                mSwipeRefreshLayout.setEnabled(false);
                mFab.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFab.setTag(url);
                        showFab();
                    }
                }, 200);

            } else {
                //hide appstore button
                view.loadUrl("javascript:jQuery(\"#app_store\").hide()");

                //hide header
                view.loadUrl("javascript:jQuery(\"#hd\").hide();");
                view.loadUrl("javascript:jQuery(\".lo_head\").hide();");
                view.loadUrl("javascript:jQuery(\".head_hr\").hide();");
                view.loadUrl("javascript:jQuery(\".bd_hd\").hide();");

                //hide footer
                view.loadUrl("javascript:jQuery(\"#ft\").hide();");
                view.loadUrl("javascript:jQuery(\".lo_foot\").hide();");
                view.loadUrl("javascript:jQuery(\"#fakeM0\").remove();");
                view.loadUrl("javascript:jQuery(\"#fakeM1\").remove();");

                //force remember (at login)
                view.loadUrl("javascript:jQuery(\"input:checkbox[id='keepid']\").attr(\"checked\", true).parent().hide();");
                view.loadUrl("javascript:jQuery(\"input:checkbox[id='keepid_opt']\").attr(\"checked\", true).parent().hide();");

                //force hide unnecessary buttons
                //view.loadUrl("javascript:{var edit = jQuery(\".rd_nav\")[0]; if(edit !== null) edit.remove()}");
                view.loadUrl("javascript:jQuery(\".write\").parent().hide()");

                //clear existing textarea input
                view.loadUrl("javascript:jQuery(\".autogrow-textarea-mirror\").remove()");
                view.loadUrl("javascript:jQuery(\"textarea\").val('')");

                //add padding (status bar height)
                if (topPadding < 0) {
                    topPadding = (int) ((getResources().getDimension(R.dimen.appbar) + getResources().getDimension(R.dimen.tool_bar_top_padding)) / getResources().getDisplayMetrics().density);
                }
                if (bottomPadding < 0) {
                    if(Util.hasNavigationBar(getActivity())) {
                        bottomPadding = (int) (Util.getNavigationBarHeight(getActivity()) / getResources().getDisplayMetrics().density);
                    } else {
                        bottomPadding = 0;
                    }
                }
                view.loadUrl("javascript:jQuery(\"body\").css(\"paddingTop\", \"" + topPadding + "px\")");
                view.loadUrl("javascript:jQuery(\"body\").css(\"paddingBottom\", \"" + bottomPadding + "px\")");

                if (url.contains("dispBoardWrite")) {
                    //hide file upload form on kitkat
                    if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                        view.loadUrl("javascript:jQuery(\"#mUpload\").hide();");
                    }
                    //hide temporary save button
                    view.loadUrl("javascript:jQuery(\".bd_btn.temp\").hide()");

                    mSwipeRefreshLayout.setEnabled(false);
                    isWriting = true;
                    mFab.setTag(null);
                    hideFab();

                } else {
                    //get html of page
                    view.loadUrl("javascript:window.HTMLOUT.processHtml('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    mSwipeRefreshLayout.setEnabled(true);
                    isWriting = false;
                }
            }

            if (mFragmentCallback != null) {
                mFragmentCallback.onLoadingCompleted(url);
                if(!url.contains("/attach/")) {
                    mFragmentCallback.onTitleChanged(view.getTitle());
                }
            }
        }
    }

    public void setFragmentCallback(FragmentCallback fragmentCallback) {
        this.mFragmentCallback = fragmentCallback;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof FragmentCallback) {
            setFragmentCallback((FragmentCallback) activity);
        }
        activity.registerReceiver(downloadCompleteReceiver,  new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onDetach() {
        getActivity().unregisterReceiver(downloadCompleteReceiver);
        super.onDetach();
    }

    public interface FragmentCallback {
        public void onTitleChanged(String title);
        public void onLoading(String url);
        public void onLoadingCompleted(String url);
        public void onLoginStateChanged(boolean isLoggedIn);
        public ScrollObservableWebview.OnScrollListener getOnScrollListener();
    }

    BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();

                query.setFilterById(downloadQueueId);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}
