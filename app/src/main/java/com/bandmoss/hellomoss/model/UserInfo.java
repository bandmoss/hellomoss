package com.bandmoss.hellomoss.model;

/**
 * Created by rok on 2015. 3. 29..
 */
public class UserInfo {

    private String nickname;
    private String imageUrl;

    public UserInfo(String nickname, String imageUrl) {
        this.nickname = nickname;
        this.imageUrl = imageUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
