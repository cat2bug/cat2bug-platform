package com.cat2bug.system.domain.handle;

import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import com.cat2bug.common.utils.spring.SpringUtils;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysUserConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-02 18:40
 * @Version: 1.0.0
 */
public class SysModuleHandlerAdapter implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb,Map<String, Object> requestParams) {
        if(value!=null) {
            String modulePath = value.toString();
            ISysUserConfigService sysUserConfigService = SpringUtils.getBean(ISysUserConfigService.class);
            SysUserConfig config = sysUserConfigService.selectSysUserConfigByUserId(SecurityUtils.getUserId());
            if (config != null) {
                Long projectId = config.getCurrentProjectId();
                ISysModuleService sysModuleService = SpringUtils.getBean(ISysModuleService.class);
                Map<String, Long> moduleMap = sysModuleService.selectSysModulePathList(projectId).stream().
                        collect(Collectors.toMap(SysModule::getModulePath, SysModule::getModuleId));
                return moduleMap.containsKey(modulePath)?moduleMap.get(modulePath):null;
            }
        }
        return null;
    }
}
