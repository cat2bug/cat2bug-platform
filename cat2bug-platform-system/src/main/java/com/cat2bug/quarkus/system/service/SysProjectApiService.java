package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.constant.UserConstants;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysProjectApi;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysProjectApiMapper;
import com.cat2bug.quarkus.system.mapper.SysUserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class SysProjectApiService {

    @Inject
    SysProjectApiMapper sysProjectApiMapper;

    @Inject
    SysUserMapper sysUserMapper;

    public TableDataInfo selectProjectApiPage(SysProjectApi query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysProjectApiMapper.countSysProjectApiList(query);
        List<SysProjectApi> rows = sysProjectApiMapper.selectSysProjectApiList(
                query, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    public List<SysProjectApi> selectProjectApiListAll(SysProjectApi query) {
        return sysProjectApiMapper.selectSysProjectApiList(query, 0, Integer.MAX_VALUE);
    }

    public SysProjectApi selectSysProjectApiByApiId(String apiId) {
        SysProjectApi api = sysProjectApiMapper.selectSysProjectApiByApiId(apiId);
        if (api == null) {
            throw new ApiException("项目API不存在");
        }
        return api;
    }

    @Transactional
    public int insertSysProjectApi(SysProjectApi apiProjectApi) {
        String key = createKey();
        SysUser existing = sysUserMapper.checkUserNameUnique(key);
        if (existing != null) {
            throw new ApiException("用户名'" + key + "'已存在");
        }
        SysUser user = new SysUser();
        user.setUserName(key);
        user.setNickName(apiProjectApi.getApiName());
        user.setUserType(UserConstants.USER_TYPE_API);
        int num = sysUserMapper.insertUser(user);
        if (num <= 0) {
            throw new ApiException("创建API用户失败");
        }

        apiProjectApi.setUserId(user.getUserId());
        apiProjectApi.setApiId(key);
        if (apiProjectApi.getEnabled() == null) {
            apiProjectApi.setEnabled(Boolean.TRUE);
        }
        return sysProjectApiMapper.insertSysProjectApi(apiProjectApi);
    }

    @Transactional
    public int updateSysProjectApi(SysProjectApi sysProjectApi) {
        selectSysProjectApiByApiId(sysProjectApi.getApiId());
        if (sysProjectApi.getApiName() != null && sysProjectApi.getUserId() != null) {
            SysUser user = new SysUser();
            user.setUserId(sysProjectApi.getUserId());
            user.setNickName(sysProjectApi.getApiName());
            sysUserMapper.updateUser(user);
        }
        if (sysProjectApi.getEnabled() == null) {
            sysProjectApi.setEnabled(Boolean.TRUE);
        }
        return sysProjectApiMapper.updateSysProjectApi(sysProjectApi);
    }

    @Transactional
    public int deleteSysProjectApiByApiIds(String[] apiIds) {
        if (apiIds == null || apiIds.length == 0) {
            throw new ApiException("API ID不能为空");
        }
        List<Long> userIds = new ArrayList<>();
        for (String id : apiIds) {
            SysProjectApi sysProjectApi = sysProjectApiMapper.selectSysProjectApiByApiId(id);
            if (sysProjectApi == null) {
                return 0;
            }
            userIds.add(sysProjectApi.getUserId());
        }
        return sysUserMapper.deleteUserByIds(userIds.toArray(new Long[]{}));
    }

    private String createKey() {
        String[] chars = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z"
        };
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String time = formatter.format(new Date());
        SecureRandom random = new SecureRandom();
        StringBuilder randStr = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            randStr.append(chars[random.nextInt(36)]);
        }
        return time + randStr.toString().toUpperCase();
    }
}
