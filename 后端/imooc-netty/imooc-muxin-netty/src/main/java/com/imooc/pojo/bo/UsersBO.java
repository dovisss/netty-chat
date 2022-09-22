package com.imooc.pojo.bo;

import org.springframework.web.multipart.MultipartFile;

public class UsersBO {
    private String userId;
    private MultipartFile faceData;

    private String nickname;
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public MultipartFile getFaceData() {
        return faceData;
    }

    public void setFaceData(MultipartFile faceData) {
        this.faceData = faceData;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}