package com.cat2bug.admin.service.im;

import java.util.List;
import java.util.Map;

/**
 * 一次通知事件。
 *
 * @param group         站内通知分组（{@code defect} / {@code report} / 前端传入的分组）
 * @param src           点击通知跳转的前端完整链接
 * @param moduleKey     IM 个人配置 {@code modules} 下的模块键；非空时要求该模块
 *                      {@code option.realtime.switch} 为真才通知该接收人（对齐 Spring 模版 {@code isValid}）
 * @param defaultOption 接收人尚未保存个人配置时使用的默认 {@code modules}
 */
public record ImEvent(Long projectId, String group, Long senderId, List<Long> receiverIds, String title,
                      String src, ImMessageBody body, String moduleKey, Map<String, Object> defaultOption) {
}
