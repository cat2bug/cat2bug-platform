package com.cat2bug.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-14 17:12
 * @Version: 1.0.0
 */
@Data
public class SysVersion {
    private String version;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createTime;
}
