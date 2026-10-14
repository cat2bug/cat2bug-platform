package com.cat2bug.quarkus.common.api;

public class RegisterBody extends LoginBody {

    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
