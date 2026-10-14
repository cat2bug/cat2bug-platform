package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthContext {

    private LoginUser loginUser;

    public LoginUser getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(LoginUser loginUser) {
        this.loginUser = loginUser;
    }

    public boolean isAuthenticated() {
        return loginUser != null;
    }
}
