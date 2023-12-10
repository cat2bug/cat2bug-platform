package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysScreenSize;

/**
 * 屏幕尺寸Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-12-10
 */
public interface SysScreenSizeMapper 
{
    /**
     * 查询屏幕尺寸
     * 
     * @param screenSizeId 屏幕尺寸主键
     * @return 屏幕尺寸
     */
    public SysScreenSize selectSysScreenSizeByScreenSizeId(Long screenSizeId);

    /**
     * 查询屏幕尺寸列表
     * 
     * @param sysScreenSize 屏幕尺寸
     * @return 屏幕尺寸集合
     */
    public List<SysScreenSize> selectSysScreenSizeList(SysScreenSize sysScreenSize);

    /**
     * 新增屏幕尺寸
     * 
     * @param sysScreenSize 屏幕尺寸
     * @return 结果
     */
    public int insertSysScreenSize(SysScreenSize sysScreenSize);

    /**
     * 修改屏幕尺寸
     * 
     * @param sysScreenSize 屏幕尺寸
     * @return 结果
     */
    public int updateSysScreenSize(SysScreenSize sysScreenSize);

    /**
     * 删除屏幕尺寸
     * 
     * @param screenSizeId 屏幕尺寸主键
     * @return 结果
     */
    public int deleteSysScreenSizeByScreenSizeId(Long screenSizeId);

    /**
     * 批量删除屏幕尺寸
     * 
     * @param screenSizeIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysScreenSizeByScreenSizeIds(Long[] screenSizeIds);
}
