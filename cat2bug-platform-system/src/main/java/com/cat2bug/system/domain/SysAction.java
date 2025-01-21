package com.cat2bug.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-19 17:30
 * @Version: 1.0.0
 */
@Data
public class SysAction {
    private String id;
    private String title;
    private String state;
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:ss")
    private Date time;
    private String nickName;
    private String userName;
    private String avatar;
}
