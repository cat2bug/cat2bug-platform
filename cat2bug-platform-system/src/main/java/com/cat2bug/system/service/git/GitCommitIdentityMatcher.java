package com.cat2bug.system.service.git;

import com.cat2bug.common.core.domain.entity.SysUser;
import org.eclipse.jgit.lib.PersonIdent;
import org.springframework.util.StringUtils;

/**
 * 判断 Git commit 的 author/committer 是否与平台用户一致。
 *
 * <p>匹配规则（满足其一即可）：
 * <ul>
 *   <li>双方邮箱均非空且忽略大小写相等</li>
 *   <li>Git 姓名忽略大小写等于平台 {@code userName} 或 {@code nickName}</li>
 * </ul>
 */
public final class GitCommitIdentityMatcher
{
    private GitCommitIdentityMatcher()
    {
    }

    public static boolean matches(PersonIdent person, SysUser user)
    {
        if (person == null || user == null)
        {
            return false;
        }
        String personEmail = trimToNull(person.getEmailAddress());
        String personName = trimToNull(person.getName());
        String userEmail = trimToNull(user.getEmail());
        String userName = trimToNull(user.getUserName());
        String nickName = trimToNull(user.getNickName());

        if (personEmail != null && userEmail != null && personEmail.equalsIgnoreCase(userEmail))
        {
            return true;
        }
        if (personName != null)
        {
            if (userName != null && personName.equalsIgnoreCase(userName))
            {
                return true;
            }
            if (nickName != null && personName.equalsIgnoreCase(nickName))
            {
                return true;
            }
        }
        return false;
    }

    public static String formatPerson(PersonIdent person)
    {
        if (person == null)
        {
            return "(unknown)";
        }
        String name = StringUtils.hasText(person.getName()) ? person.getName() : "?";
        String email = StringUtils.hasText(person.getEmailAddress()) ? person.getEmailAddress() : "";
        return name + " <" + email + ">";
    }

    private static String trimToNull(String value)
    {
        if (!StringUtils.hasText(value))
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
