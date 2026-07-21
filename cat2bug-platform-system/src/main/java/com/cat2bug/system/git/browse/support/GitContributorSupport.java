package com.cat2bug.system.git.browse.support;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.StringUtils;

import java.util.Locale;

/**
 * 贡献者聚合与项目成员匹配（与 browse.vue {@code memberForAuthor} 口径一致）。
 */
public final class GitContributorSupport
{
    private GitContributorSupport()
    {
    }

    /**
     * 聚合键：作者 email 存在时优先（小写 trim），否则用 author name（小写 trim）。
     */
    public static String aggregationKey(String authorName, String authorEmail)
    {
        String email = normalizeKeyPart(authorEmail);
        if (StringUtils.isNotBlank(email))
        {
            return "e:" + email;
        }
        String name = normalizeKeyPart(authorName);
        if (StringUtils.isNotBlank(name))
        {
            return "n:" + name;
        }
        return "?:unknown";
    }

    /**
     * 按 git author 匹配项目成员：email 相等，或 author name 与 userName/nickName 相等（忽略大小写）。
     */
    public static SysUser matchProjectMember(Iterable<SysUser> members, String authorName, String authorEmail)
    {
        if (members == null)
        {
            return null;
        }
        String nameKey = normalizeKeyPart(authorName);
        String emailKey = normalizeKeyPart(authorEmail);
        for (SysUser member : members)
        {
            if (member == null)
            {
                continue;
            }
            String userName = normalizeKeyPart(member.getUserName());
            String nickName = normalizeKeyPart(member.getNickName());
            String mail = normalizeKeyPart(member.getEmail());
            boolean nameMatch = StringUtils.isNotBlank(nameKey)
                    && (nameKey.equals(userName) || nameKey.equals(nickName));
            boolean emailMatch = StringUtils.isNotBlank(emailKey) && emailKey.equals(mail);
            if (nameMatch || emailMatch)
            {
                return member;
            }
        }
        return null;
    }

    /**
     * 与 browse.vue 一致：未命中项目成员时，若 author name 与当前登录用户 userName/nickName 相同则视为本人。
     */
    public static SysUser matchCurrentUser(SysUser currentUser, String authorName)
    {
        if (currentUser == null)
        {
            return null;
        }
        String nameKey = normalizeKeyPart(authorName);
        if (StringUtils.isBlank(nameKey))
        {
            return null;
        }
        String userName = normalizeKeyPart(currentUser.getUserName());
        String nickName = normalizeKeyPart(currentUser.getNickName());
        if (nameKey.equals(userName) || nameKey.equals(nickName))
        {
            return currentUser;
        }
        return null;
    }

    static String normalizeKeyPart(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
