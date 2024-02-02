package com.cat2bug.system.domain.handle;

import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.poi.ExcelComboHandlerAdapter;
import com.cat2bug.common.utils.spring.SpringUtils;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysUserConfigService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-02 16:25
 * @Version: 1.0.0
 */
public class SysModuleComboHandlerAdapter implements ExcelComboHandlerAdapter {
    @Override
    public List<String> format(Map<String,Object> args, Cell cell, Workbook wb) {
        ISysUserConfigService sysUserConfigService = SpringUtils.getBean(ISysUserConfigService.class);
        SysUserConfig config = sysUserConfigService.selectSysUserConfigByUserId(SecurityUtils.getUserId());
        if(config!=null){
            Long projectId = config.getCurrentProjectId();
            ISysModuleService sysModuleService = SpringUtils.getBean(ISysModuleService.class);
            List<SysModule> moduleList = sysModuleService.selectSysModulePathList(projectId);
            return moduleList.stream().map(SysModule::getModulePath).collect(Collectors.toList());
        }
        return Arrays.asList();
    }
}
