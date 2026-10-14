package com.cat2bug.quarkus.system.service.git;

import java.util.Set;

/**
 * 仓库 Webhook 订阅事件名
 */
public final class GitWebhookEventTypes
{
    public static final String SUBSCRIPTION_PUSH = "push";
    public static final String SUBSCRIPTION_TAG = "tag";
    public static final String SUBSCRIPTION_PULL_REQUEST = "pull_request";

    public static final Set<String> ALL_SUBSCRIPTIONS = Set.of(
            SUBSCRIPTION_PUSH, SUBSCRIPTION_TAG, SUBSCRIPTION_PULL_REQUEST);

    /** GitHub {@code X-GitHub-Event} 头 */
    public static final String GITHUB_PUSH = "push";
    public static final String GITHUB_CREATE = "create";
    public static final String GITHUB_DELETE = "delete";
    public static final String GITHUB_PULL_REQUEST = "pull_request";
    public static final String GITHUB_PING = "ping";

    private GitWebhookEventTypes()
    {
    }
}
