package com.cat2bug.system.exception;

import lombok.Getter;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-12-22 12:02
 * @Version: 1.0.0
 */
@Getter
public class AjaxResultException extends RuntimeException {
    private int code;

    public AjaxResultException(int code, String message) {
        super(message);
        this.code = code;
    }
}
