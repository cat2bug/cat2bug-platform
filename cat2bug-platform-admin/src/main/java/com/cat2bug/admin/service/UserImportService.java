package com.cat2bug.admin.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.SysConfigService;
import com.cat2bug.system.service.SysUserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class UserImportService {

    @Inject
    SysUserService sysUserService;

    @Inject
    SysConfigService sysConfigService;

    @Transactional
    public String importUsers(List<SysUser> userList, boolean updateSupport, String operName, Long currentUserId) {
        if (userList == null || userList.isEmpty()) {
            throw new ApiException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        String initPassword = sysConfigService.selectConfigByKey("sys.user.initPassword");
        if (StringUtils.isEmpty(initPassword)) {
            initPassword = "123456";
        }
        String hashedInitPassword = BCrypt.withDefaults().hashToString(12, initPassword.toCharArray());
        for (SysUser user : userList) {
            try {
                if (StringUtils.isBlank(user.getUserName())) {
                    throw new ApiException("登录名称不能为空");
                }
                SysUser existing = sysUserService.selectUserByUserName(user.getUserName());
                if (existing == null) {
                    user.setPassword(hashedInitPassword);
                    user.setCreateBy(operName);
                    if (StringUtils.isEmpty(user.getStatus())) {
                        user.setStatus("0");
                    }
                    sysUserService.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUserName()).append(" 导入成功");
                } else if (updateSupport) {
                    sysUserService.checkUserAllowed(existing);
                    sysUserService.checkUserDataScope(existing.getUserId(), currentUserId);
                    user.setUserId(existing.getUserId());
                    user.setUpdateBy(operName);
                    sysUserService.updateUserProfile(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUserName()).append(" 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、账号 ").append(user.getUserName()).append(" 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg).append(e.getMessage());
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ApiException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        return successMsg.toString();
    }
}
