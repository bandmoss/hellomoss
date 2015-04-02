package com.bandmoss.hellomoss;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bandmoss.hellomoss.model.UserInfo;
import com.bandmoss.hellomoss.util.Callback;
import com.bandmoss.hellomoss.util.Util;
import com.bandmoss.hellomoss.widget.ScrollObservableWebview;
import com.bumptech.glide.Glide;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Tagable;

import java.lang.reflect.Field;

public class MainActivity extends ActionBarActivity implements Drawer.OnDrawerItemClickListener, WebViewFragment.FragmentCallback {

    private static final String TAG = "MainActivity";

    private WebViewFragment mWebViewFragment = null;

    private Toolbar mToolbar;
    private View mUpperSurface;
    private View mLogoutDrawerView;
    private Drawer.Result mNavigationDrawer;
    private ScrollObservableWebview.OnScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Toolbar
        initToolbar();

        // Init fragment
        initFragment(savedInstanceState);

        // Init Drawer
        initNavigationDrawer(savedInstanceState);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // init marquee animation to toolbar title
        try {
            Field f = mToolbar.getClass().getDeclaredField("mTitleTextView");
            f.setAccessible(true);

            TextView titleTextView = null;
            titleTextView = (TextView) f.get(mToolbar);
            titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            titleTextView.setFocusable(true);
            titleTextView.setFocusableInTouchMode(true);
            titleTextView.requestFocus();
            titleTextView.setSingleLine(true);
            titleTextView.setSelected(true);
            titleTextView.setMarqueeRepeatLimit(-1);

        } catch (NoSuchFieldException | IllegalAccessException ignored) {

        }

        // set parallax effect to toolbar
        mUpperSurface = findViewById(R.id.upper_surface);
        mScrollListener = new ScrollObservableWebview.OnScrollListener() {

            private int minUpperSurfaceY;

            {
                TypedValue tv = new TypedValue();
                if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                    minUpperSurfaceY = -TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics()) - (int) (getResources().getDimension(R.dimen.tool_bar_top_padding) / getResources().getDisplayMetrics().density) - 100;
                } else {
                    minUpperSurfaceY = 0;
                }
            }

            @Override
            public void onScroll(int l, int t, int oldl, int oldt) {
                final int offset = (int) ((t - oldt) * .66f);
                final float futureAppbarPosY = mUpperSurface.getY() - offset;

                if (futureAppbarPosY <= minUpperSurfaceY) {
                    mUpperSurface.setY(minUpperSurfaceY);
                } else if (futureAppbarPosY >= 0) {
                    mUpperSurface.setY(0);
                } else {
                    mUpperSurface.setY(futureAppbarPosY);
                }

            }
        };
    }

    private void initFragment(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("WebViewFragment");
            if (fragment != null && fragment instanceof WebViewFragment) {
                mWebViewFragment = (WebViewFragment) fragment;
            }
        }

        if (mWebViewFragment == null) {
            mWebViewFragment = WebViewFragment.newInstance(AppConstants.BASE_URL);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_container, mWebViewFragment, "WebViewFragment")
                    .commit();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            View shadowBeforeLollipop = findViewById(R.id.tab_shadow);
            shadowBeforeLollipop.setVisibility(View.VISIBLE);
        }
    }

    private void initNavigationDrawer(Bundle savedInstanceState) {
        if (mNavigationDrawer == null) {
            LinearLayout footer = new LinearLayout(this);
            footer.setOrientation(LinearLayout.VERTICAL);
            footer.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);

            mLogoutDrawerView = AppConstants.DRAWER_ITEM_LOGOUT.convertView(getLayoutInflater(), null, footer);
            mLogoutDrawerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClick(null, view, -1, 0, AppConstants.DRAWER_ITEM_LOGOUT);
                    mNavigationDrawer.closeDrawer();
                }
            });
            //footer.addView(AppConstants.DRAWER_ITEM_SETTING.convertView(getLayoutInflater(), null, footer));

            footer.addView(mLogoutDrawerView);
            mNavigationDrawer = new Drawer()
                    .withActivity(this)
                    .withToolbar(mToolbar)
                    .withHeader(R.layout.header)
                    .withActionBarDrawerToggle(true)
                    .addDrawerItems(
                            //AppConstants.DRAWER_ITEM_HOME,
                            AppConstants.DRAWER_ITEM_NOTICE,
                            AppConstants.DRAWER_ITEM_FREEBOARD,
                            AppConstants.DRAWER_ITEM_MEMBERBOARD,
                            AppConstants.DRAWER_ITEM_GALLERY,
                            AppConstants.DRAWER_ITEM_VIDEO,
                            AppConstants.DRAWER_ITEM_TIMETABLE)
                    .withStickyFooter(footer)
                    .withFooterClickable(true)
                    .withSelectedItem(0)
                    .withOnDrawerItemClickListener(this)
                    .withSavedInstance(savedInstanceState)
                    .build();
        }

        View logo = mNavigationDrawer.getHeader().findViewById(R.id.logo);
        View profileIcon = mNavigationDrawer.getHeader().findViewById(R.id.profile_image);

        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mWebViewFragment != null) {
                    mWebViewFragment.navigateTo(AppConstants.HOME_URL);
                }
                if(mNavigationDrawer.isDrawerOpen()) {
                    mNavigationDrawer.closeDrawer();
                }
            }
        });
        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mWebViewFragment != null) {
                    mWebViewFragment.navigateTo(AppConstants.MEMBERINFO_URL);
                }
                if(mNavigationDrawer.isDrawerOpen()) {
                    mNavigationDrawer.closeDrawer();
                }
            }
        });
        onLoginStateChanged(false);
    }

    @Override
    public void onBackPressed() {
        // Close navigation drawer if opened
        if (mNavigationDrawer != null && mNavigationDrawer.isDrawerOpen()) {
            mNavigationDrawer.closeDrawer();

            // Navigate back if history stack available
        } else if (mWebViewFragment == null || !mWebViewFragment.triggerGoBack()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Do not create any options menu
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Toggle navigation drawer
        if (mNavigationDrawer != null) {
            if (mNavigationDrawer.isDrawerOpen()) {
                mNavigationDrawer.closeDrawer();
            } else {
                mNavigationDrawer.openDrawer();
            }
        }

        // Ignore conventional context menu
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
        if (drawerItem != null) {
            mNavigationDrawer.setSelection(drawerItem, false);

            if (drawerItem instanceof Tagable && drawerItem.getTag() != null) {
                if (drawerItem.getTag() instanceof String) {
                    String tag = (String) drawerItem.getTag();
                    mWebViewFragment.navigateTo(tag);
                }
            }

        }
    }

    @Override
    public void onTitleChanged(String title) {
        if (title == null) {
            title = getString(R.string.app_name);
        }
        getSupportActionBar().setTitle(title);
        Util.setTaskDescription(this, title);
    }

    @Override
    public void onLoginStateChanged(final boolean isLoggedIn) {
        if (isLoggedIn) {
            Util.requestUserInfo(new Callback<UserInfo>() {
                @Override
                public void callback(final UserInfo result) {
                    if (result != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                View header = mNavigationDrawer.getHeader();
                                ImageView profileIcon = (ImageView) header.findViewById(R.id.profile_image);
                                TextView username = (TextView) header.findViewById(R.id.profile_name_text);
                                TextView email = (TextView) header.findViewById(R.id.profile_email_text);

                                Glide.with(MainActivity.this).load(result.getImageUrl()).crossFade().into(profileIcon);
                                username.setText(result.getNickname());
                                email.setText("");

                                mLogoutDrawerView.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        onLoginStateChanged(false);
                    }
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View header = mNavigationDrawer.getHeader();
                    ImageView profileIcon = (ImageView) header.findViewById(R.id.profile_image);
                    TextView username = (TextView) header.findViewById(R.id.profile_name_text);
                    TextView email = (TextView) header.findViewById(R.id.profile_email_text);

                    profileIcon.setImageResource(R.drawable.person_image_empty);
                    username.setText(getString(R.string.not_signed_in));
                    email.setText("");

                    mLogoutDrawerView.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onLoadingCompleted(String url) {
        if (url != null) {
            if (url.contains("freeboard")) {
                mNavigationDrawer.setSelection(AppConstants.DRAWER_ITEM_FREEBOARD, false);
            } else if (url.contains("memberboard")) {
                mNavigationDrawer.setSelection(AppConstants.DRAWER_ITEM_MEMBERBOARD, false);
            } else if (url.contains("gallery")) {
                mNavigationDrawer.setSelection(AppConstants.DRAWER_ITEM_GALLERY, false);
            } else if (url.contains("mossvideo")) {
                mNavigationDrawer.setSelection(AppConstants.DRAWER_ITEM_VIDEO, false);
            } else if (url.contains("board_PKGm60")) {
                mNavigationDrawer.setSelection(AppConstants.DRAWER_ITEM_NOTICE, false);
            }

            if (url.contains("/attach/")) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                mNavigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
                mNavigationDrawer.getActionBarDrawerToggle().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
                mNavigationDrawer.getActionBarDrawerToggle().setToolbarNavigationClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mWebViewFragment != null) {
                            mWebViewFragment.triggerGoBack();
                        }
                    }
                });
            } else {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                mNavigationDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
                mNavigationDrawer.getActionBarDrawerToggle().setToolbarNavigationClickListener(null);
            }
        }
    }

    @Override
    public ScrollObservableWebview.OnScrollListener getOnScrollListener() {
        return mScrollListener;
    }

}
