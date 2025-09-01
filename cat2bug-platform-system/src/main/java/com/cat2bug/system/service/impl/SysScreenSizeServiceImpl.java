package com.cat2bug.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysScreenSizeMapper;
import com.cat2bug.system.domain.SysScreenSize;
import com.cat2bug.system.service.ISysScreenSizeService;

/**
 * 屏幕尺寸Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-12-10
 */
@Service
public class SysScreenSizeServiceImpl implements ISysScreenSizeService 
{
    @Autowired
    private SysScreenSizeMapper sysScreenSizeMapper;

    /**
     * 查询屏幕尺寸
     * 
     * @param screenSizeId 屏幕尺寸主键
     * @return 屏幕尺寸
     */
    @Override
    public SysScreenSize selectSysScreenSizeByScreenSizeId(Long screenSizeId)
    {
        return sysScreenSizeMapper.selectSysScreenSizeByScreenSizeId(screenSizeId);
    }

    /**
     * 查询屏幕尺寸列表
     * 
     * @param sysScreenSize 屏幕尺寸
     * @return 屏幕尺寸
     */
    @Override
    public List<SysScreenSize> selectSysScreenSizeList(SysScreenSize sysScreenSize)
    {
        return sysScreenSizeMapper.selectSysScreenSizeList(sysScreenSize);
    }

    /**
     * 新增屏幕尺寸
     * 
     * @param sysScreenSize 屏幕尺寸
     * @return 结果
     */
    @Override
    public int insertSysScreenSize(SysScreenSize sysScreenSize)
    {
        return sysScreenSizeMapper.insertSysScreenSize(sysScreenSize);
    }

    /**
     * 修改屏幕尺寸
     * 
     * @param sysScreenSize 屏幕尺寸
     * @return 结果
     */
    @Override
    public int updateSysScreenSize(SysScreenSize sysScreenSize)
    {
        return sysScreenSizeMapper.updateSysScreenSize(sysScreenSize);
    }

    /**
     * 批量删除屏幕尺寸
     * 
     * @param screenSizeIds 需要删除的屏幕尺寸主键
     * @return 结果
     */
    @Override
    public int deleteSysScreenSizeByScreenSizeIds(Long[] screenSizeIds)
    {
        return sysScreenSizeMapper.deleteSysScreenSizeByScreenSizeIds(screenSizeIds);
    }

    /**
     * 删除屏幕尺寸信息
     * 
     * @param screenSizeId 屏幕尺寸主键
     * @return 结果
     */
    @Override
    public int deleteSysScreenSizeByScreenSizeId(Long screenSizeId)
    {
        return sysScreenSizeMapper.deleteSysScreenSizeByScreenSizeId(screenSizeId);
    }
}
