package com.cat2bug.im.domain;

import lombok.Data;

import java.util.List;

/**
 * 飞书按邮箱查询用户返回结果
 */
@Data
public class FeishuUserLookupResult {
    private int code;
    private String msg;
    private FeishuUserLookupData data;

    @Data
    public static class FeishuUserLookupData {
        private List<FeishuUserInfo> user_list;
    }

    @Data
    public static class FeishuUserInfo {
        private String user_id;
        private String email;
        private String open_id;
        private String union_id;
        private String name;
    }
}
