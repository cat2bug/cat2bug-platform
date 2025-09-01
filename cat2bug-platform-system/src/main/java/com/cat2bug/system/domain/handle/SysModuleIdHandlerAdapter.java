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
 * @CreateTime: 2024-10-21 17:43
 * @Version: 1.0.0
 */
public class SysModuleIdHandlerAdapter  implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams) {
        if(value!=null) {
            Long moduleId = Long.parseLong(value.toString());
            ISysUserConfigService sysUserConfigService = SpringUtils.getBean(ISysUserConfigService.class);
            SysUserConfig config = sysUserConfigService.selectSysUserConfigByUserId(SecurityUtils.getUserId());
            if (config != null) {
                Long projectId = config.getCurrentProjectId();
                ISysModuleService sysModuleService = SpringUtils.getBean(ISysModuleService.class);
                Map<Long, String> moduleMap = sysModuleService.selectSysModulePathList(projectId).stream().
                        collect(Collectors.toMap(SysModule::getModuleId,SysModule::getModulePath));
                return moduleMap.containsKey(moduleId)?moduleMap.get(moduleId):null;
            }
        }
        return null;
    }
}
