package com.cat2bug.system.service.impl;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.uuid.UUID;
import com.cat2bug.system.mapper.SysUserMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysProjectApiMapper;
import com.cat2bug.system.domain.SysProjectApi;
import com.cat2bug.system.service.ISysProjectApiService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目APIService业务层处理
 * 
 * @author yuzhantao
 * @date 2024-02-11
 */
@Service
public class SysProjectApiServiceImpl implements ISysProjectApiService 
{
    @Autowired
    private SysProjectApiMapper sysProjectApiMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 创建密钥
     * @return 密钥
     */
    private String createKey() {
        //1，创建字符集合
        String[] chars = {
                "0","1","2","3","4","5","6","7","8","9",
                "a","b","c","d","e","f","g","h","i","j",
                "k","l","m","n","o","p","q","r","s","t",
                "u","v","w","x","y","z"
        };
        //2，获取当前时间字符串
        long l = System.currentTimeMillis();
        Date date = new Date(l);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String time = formatter.format(date);
        //3，获取随机字符串
        String randStr = "";
        //4，通过 SecureRandom强随机数生成器，生成16位随机数，
        //并去1步骤中创建的字符集随机获取字符，组成16位字符串，
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            int index = random.nextInt(36);
            randStr+=chars[index];
        }
        //5，把时间字符串和16位强随机数字符串组合，形成30位随机字符串
        return time+randStr;
    }

    /**
     * 查询项目API
     * 
     * @param apiId 项目API主键
     * @return 项目API
     */
    @Override
    public SysProjectApi selectSysProjectApiByApiId(String apiId)
    {
        return sysProjectApiMapper.selectSysProjectApiByApiId(apiId);
    }

    /**
     * 查询项目API列表
     * 
     * @param sysProjectApi 项目API
     * @return 项目API
     */
    @Override
    public List<SysProjectApi> selectSysProjectApiList(SysProjectApi sysProjectApi)
    {
        return sysProjectApiMapper.selectSysProjectApiList(sysProjectApi);
    }

    /**
     * 新增项目API
     * 
     * @param apiProjectApi 项目API
     * @return 结果
     */
    @Override
    public int insertSysProjectApi(SysProjectApi apiProjectApi)
    {
        String key = this.createKey();
        SysUser user = sysUserMapper.checkUserNameUnique(key);
        Preconditions.checkState(user==null, MessageUtils.message("user.register.fail.username-exists",key));
        user = new SysUser();
        user.setUserName(key);
        user.setNickName(apiProjectApi.getApiName());
        int num = sysUserMapper.insertUser(user);
        Preconditions.checkState(num>0,MessageUtils.message("user.register.fail"));
        
        apiProjectApi.setUserId(user.getUserId());
        apiProjectApi.setApiId(key);
        return sysProjectApiMapper.insertSysProjectApi(apiProjectApi);
    }

    /**
     * 修改项目API
     * 
     * @param sysProjectApi 项目API
     * @return 结果
     */
    @Override
    public int updateSysProjectApi(SysProjectApi sysProjectApi)
    {
        return sysProjectApiMapper.updateSysProjectApi(sysProjectApi);
    }

    /**
     * 批量删除项目API
     * 
     * @param apiIds 需要删除的项目API主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysProjectApiByApiIds(String[] apiIds)
    {
        List<Long> userIds = new ArrayList<>();
        for(String id : apiIds) {
            SysProjectApi sysProjectApi = sysProjectApiMapper.selectSysProjectApiByApiId(id);
            if(sysProjectApi==null)
                return 0;
            userIds.add(sysProjectApi.getUserId());
        }
        return sysUserMapper.deleteUserByIds(userIds.toArray(new Long[]{}));
    }

    /**
     * 删除项目API信息
     * 
     * @param apiId 项目API主键
     * @return 结果
     */
    @Override
    public int deleteSysProjectApiByApiId(String apiId)
    {
        SysProjectApi sysProjectApi = sysProjectApiMapper.selectSysProjectApiByApiId(apiId);
        if(sysProjectApi==null) return 0;

        return sysUserMapper.deleteUserById(sysProjectApi.getUserId());
    }
}
