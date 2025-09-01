package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 21:39
 * @Version: 1.0.0
 */
@Data
public class Member {
    private Long userId;

    private String name;

    private String mail;

    private String phone;
}
