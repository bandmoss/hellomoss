package com.bandmoss.hellomoss;

import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;

/**
 * Created by rok on 2015. 4. 1..
 */
public class AppConstants {

    public final static String BASE_URL = "http://bandmoss.com";
    public final static String HOME_URL = "http://bandmoss.com/xe";
    public final static String NOTICE_URL = "http://bandmoss.com/xe/board_PKGm60";
    public final static String FREEBOARD_URL = "http://bandmoss.com/xe/freeboard_m";
    public final static String MEMBERBOARD_URL = "http://bandmoss.com/xe/memberboard_m";
    public final static String GALLERY_URL = "http://bandmoss.com/xe/gallery";
    public final static String VIDEO_URL = "http://bandmoss.com/xe/mossvideo";
    public final static String TIMETABLE_URL = "https://docs.google.com/spreadsheet/ccc?key=0AjDBoQ9adDT-dEk1MHZQcFBYMkZsV09fZmU2eWtOM1E";
    public final static String LOGOUT_URL = "http://bandmoss.com/xe/index.php?act=dispMemberLogout";
    public final static String MEMBERINFO_URL = "http://bandmoss.com/xe/index.php?act=dispMemberInfo";

    public final static int DRAWER_ID_HOME = 1;
    public final static int DRAWER_ID_NOTICE = 2;
    public final static int DRAWER_ID_FREEBOARD = 3;
    public final static int DRAWER_ID_MEMBERBOARD = 4;
    public final static int DRAWER_ID_GALLERY = 5;
    public final static int DRAWER_ID_VIDEO = 6;
    public final static int DRAWER_ID_TIMETABLE = 7;
    public final static int DRAWER_ID_LOGOUT = 8;
    public final static int DRAWER_ID_SETTING = 9;

    public final static PrimaryDrawerItem DRAWER_ITEM_HOME = new PrimaryDrawerItem().withName(R.string.title_home).withTag(HOME_URL).withIcon(FontAwesome.Icon.faw_home).withIdentifier(DRAWER_ID_HOME).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_NOTICE = new PrimaryDrawerItem().withName(R.string.title_notice).withTag(NOTICE_URL).withIcon(FontAwesome.Icon.faw_bullhorn).withIdentifier(DRAWER_ID_NOTICE).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_FREEBOARD = new PrimaryDrawerItem().withName(R.string.title_freeboard).withTag(FREEBOARD_URL).withIcon(FontAwesome.Icon.faw_group).withIdentifier(DRAWER_ID_FREEBOARD).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_MEMBERBOARD = new PrimaryDrawerItem().withName(R.string.title_memberboard).withTag(MEMBERBOARD_URL).withIcon(FontAwesome.Icon.faw_folder_open).withIdentifier(DRAWER_ID_MEMBERBOARD).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_GALLERY = new PrimaryDrawerItem().withName(R.string.title_gallery).withTag(GALLERY_URL).withIcon(FontAwesome.Icon.faw_photo).withIdentifier(DRAWER_ID_GALLERY).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_VIDEO = new PrimaryDrawerItem().withName(R.string.title_video).withTag(VIDEO_URL).withIcon(FontAwesome.Icon.faw_youtube_play).withIdentifier(DRAWER_ID_VIDEO).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_TIMETABLE = new PrimaryDrawerItem().withName(R.string.title_timetable).withTag(TIMETABLE_URL).withIcon(FontAwesome.Icon.faw_table).withIdentifier(DRAWER_ID_TIMETABLE).withCheckable(false);
    public final static PrimaryDrawerItem DRAWER_ITEM_LOGOUT = new PrimaryDrawerItem().withName(R.string.title_logout).withTag(AppConstants.LOGOUT_URL).withIcon(FontAwesome.Icon.faw_sign_out).withIdentifier(AppConstants.DRAWER_ID_LOGOUT).withCheckable(false);
//    public final static PrimaryDrawerItem DRAWER_ITEM_SETTING = new PrimaryDrawerItem().withName("Setting").withTag(SettingActivity.class).withIcon(FontAwesome.Icon.faw_sign_out).withIdentifier(AppConstants.DRAWER_ID_LOGOUT).withCheckable(false);
}
