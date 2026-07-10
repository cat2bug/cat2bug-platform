package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Anonymous;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.system.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.system.service.ISysMemberInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 成员邀请链接公开接口
 */
@RestController
@RequestMapping("/invite")
public class SysMemberInviteController extends BaseController {

    @Autowired
    private ISysMemberInviteService sysMemberInviteService;

    @Anonymous
    @GetMapping("/{token}/preview")
    public AjaxResult preview(@PathVariable("token") String token) {
        MemberInvitePreviewVo preview = sysMemberInviteService.preview(token, resolveOptionalUserId());
        return success(preview);
    }

    /**
     * 兼容旧版 /invite/{token} 直链：重定向到 hash 路由邀请页。
     */
    @Anonymous
    @GetMapping("/{token}")
    public void openInvitePage(@PathVariable("token") String token, HttpServletResponse response) throws IOException {
        response.sendRedirect("/#/invite/" + token);
    }

    @PostMapping("/{token}/accept")
    public AjaxResult accept(@PathVariable("token") String token) {
        MemberInviteAcceptResultVo result = sysMemberInviteService.accept(
                token, getUserId(), String.valueOf(getUserId()));
        return success(result);
    }

    private Long resolveOptionalUserId() {
        Authentication authentication = SecurityUtils.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getUserId();
        }
        return null;
    }
}
