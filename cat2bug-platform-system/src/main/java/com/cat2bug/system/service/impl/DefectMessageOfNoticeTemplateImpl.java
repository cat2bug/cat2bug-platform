package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-10 16:03
 * @Version: 1.0.0
 */
@Service
public class DefectMessageOfNoticeTemplateImpl implements IMessageTemplate<SysDefect> {

    @Override
    public String toText(SysDefect obj) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s。\n",MessageUtils.message("defect.notice")));
        sb.append(String.format("%s %s/#/project/defect?defectId=%d \n", MessageUtils.message("defect.click-view"), obj.getSrcHost(), obj.getDefectId(), obj.getSrcHost(), obj.getDefectId()));
        sb.append(String.format("%s: %s \n", MessageUtils.message("defectLevel"),MessageUtils.message(obj.getDefectLevel())));
        sb.append(String.format("%s %s \n", MessageUtils.message("defectTypeName"),MessageUtils.message(obj.getDefectType().name())));
        sb.append(String.format("%s %s \n", MessageUtils.message("defectStateName"),MessageUtils.message(obj.getDefectState().name())));
        sb.append(String.format("%s %s \n", MessageUtils.message("moduleName"),obj.getModuleName()));
        sb.append(String.format("%s %s \n", MessageUtils.message("moduleVersion"),obj.getModuleVersion()));
        sb.append(String.format("%s %s \n", MessageUtils.message("defectDescribe"),obj.getDefectDescribe()));
        return sb.toString();
    }

    @Override
    public String toHtml(SysDefect obj) {
        return toText(obj);
    }

    @Override
    public String toMarkdown(SysDefect obj) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("## #%d %s \n\n", obj.getProjectNum(), obj.getDefectName()));
        sb.append(String.format("%s [%s/#/project/defect?defectId=%d](%s/#/project/defect?defectId=%d) \n\n", MessageUtils.message("defect.click-view"), obj.getSrcHost(), obj.getDefectId(), obj.getSrcHost(), obj.getDefectId()));
        sb.append(String.format("**%s:** %s \n\n", MessageUtils.message("defectLevel"),MessageUtils.message(obj.getDefectLevel())));
        sb.append(String.format("**%s:** %s \n\n", MessageUtils.message("defectTypeName"),MessageUtils.message(obj.getDefectType().name())));
        sb.append(String.format("**%s:** %s \n\n", MessageUtils.message("defectStateName"),MessageUtils.message(obj.getDefectState().name())));
        sb.append(String.format("**%s:** %s \n\n", MessageUtils.message("moduleName"),obj.getModuleName()));
        sb.append(String.format("**%s:** %s \n\n", MessageUtils.message("moduleVersion"),obj.getModuleVersion()));
        sb.append(String.format("**%s:** %s \n\n", MessageUtils.message("defectDescribe"),obj.getDefectDescribe()));
        return sb.toString();
    }
}
