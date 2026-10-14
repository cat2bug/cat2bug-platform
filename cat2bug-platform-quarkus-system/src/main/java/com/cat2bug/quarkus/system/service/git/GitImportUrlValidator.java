package com.cat2bug.quarkus.system.service.git;

import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.MessageUtils;
import com.cat2bug.quarkus.common.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * 导入 / 远端列仓 URL 校验：仅允许 http(s)，拒绝空值与畸形 URL。
 */
public final class GitImportUrlValidator
{
    private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");

    private GitImportUrlValidator()
    {
    }

    /**
     * 校验 Git 远端 clone URL（导入用）。
     *
     * @return 规范化后的 URL 字符串（保留原始 path/query 语义，仅 trim）
     */
    public static String validateCloneUrl(String sourceUrl)
    {
        if (StringUtils.isEmpty(sourceUrl))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.url-required"));
        }
        String trimmed = sourceUrl.trim();
        URI uri = parseHttpUri(trimmed, "git.repo.import.url-invalid");
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.url-scheme-rejected"));
        }
        if (StringUtils.isEmpty(uri.getHost()))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.url-invalid"));
        }
        return trimmed;
    }

    /**
     * 校验 GitLab 等平台 host 基址（默认 {@code https://gitlab.com}）。
     *
     * @return 规范化后的基址（无尾部斜杠）
     */
    public static String validateHostBase(String host)
    {
        if (StringUtils.isEmpty(host))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.host-invalid"));
        }
        String trimmed = host.trim();
        URI uri = parseHttpUri(trimmed, "git.repo.import.host-invalid");
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.url-scheme-rejected"));
        }
        if (StringUtils.isEmpty(uri.getHost()))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.host-invalid"));
        }
        String base = scheme.toLowerCase(Locale.ROOT) + "://" + uri.getHost();
        if (uri.getPort() > 0)
        {
            base = base + ":" + uri.getPort();
        }
        return base;
    }

    private static URI parseHttpUri(String value, String invalidMessageKey)
    {
        try
        {
            URI uri = new URI(value);
            if (uri.getScheme() == null)
            {
                throw new ApiException(MessageUtils.message(invalidMessageKey));
            }
            return uri;
        }
        catch (URISyntaxException e)
        {
            throw new ApiException(MessageUtils.message(invalidMessageKey));
        }
    }
}
