package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Anonymous;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.system.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.system.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.system.service.ISysMemberInviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        MemberInvitePreviewVo preview = sysMemberInviteService.preview(token);
        return success(preview);
    }

    @PostMapping("/{token}/accept")
    public AjaxResult accept(@PathVariable("token") String token) {
        MemberInviteAcceptResultVo result = sysMemberInviteService.accept(
                token, getUserId(), String.valueOf(getUserId()));
        return success(result);
    }
}
