package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-10 16:03
 * @Version: 1.0.0
 */
@Service
public class DefectMessageOfNoticeTemplateImpl implements IMessageTemplate<SysDefect> {
    private final static String DEFECT_CONFIG_KEY = "defect";
    private final static String OPTION_CONFIG_KEY = "option";
    private final static String EVENT_CONFIG_KEY = "event";
    private final static String OPTION_REALTIME_CONFIG_KEY = "realtime";
    private final static String SWITCH_KEY = "switch";
    private final static String RECEIVER_CONFIG_KEY = "receiver";


    @Override
    public String toText(SysDefect obj, Map<String, Object> params) {
        if(this.isValid(params)==false) return null;
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s\n",MessageUtils.message("defect.notice")));
        sb.append(String.format("%s %s/#/project/defect?defectId=%d \n", MessageUtils.message("defect.click-view"), obj.getSrcHost(), obj.getDefectId()));
        sb.append(String.format("%s: %s \n", MessageUtils.message("defectLevel"),MessageUtils.message(obj.getDefectLevel())));
        sb.append(String.format("%s: %s \n", MessageUtils.message("defectTypeName"),MessageUtils.message(obj.getDefectType().name())));
        sb.append(String.format("%s: %s \n", MessageUtils.message("defectStateName"),MessageUtils.message(obj.getDefectState().name())));
        sb.append(String.format("%s: %s \n", MessageUtils.message("moduleName"),obj.getModuleName()));
        sb.append(String.format("%s: %s \n", MessageUtils.message("moduleVersion"),obj.getModuleVersion()));
        sb.append(String.format("%s: %s \n", MessageUtils.message("defectDescribe"),obj.getDefectDescribe()));
        return sb.toString();
    }

    @Override
    public String toHtml(SysDefect obj, Map<String, Object> params) {
        if(this.isValid(params)==false) return null;
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s <br />",MessageUtils.message("defect.notice")));
        sb.append(String.format("%s <a style=\"color: #409eff;\" href=\"%s/#/project/defect?defectId=%d\">%s/#/project/defect?defectId=%d</a> <br />", MessageUtils.message("defect.click-view"), obj.getSrcHost(), obj.getDefectId(), obj.getSrcHost(), obj.getDefectId()));
        sb.append(String.format("%s: %s <br />", MessageUtils.message("defectLevel"),MessageUtils.message(obj.getDefectLevel())));
        sb.append(String.format("%s: %s <br />", MessageUtils.message("defectTypeName"),MessageUtils.message(obj.getDefectType().name())));
        sb.append(String.format("%s: %s <br />", MessageUtils.message("defectStateName"),MessageUtils.message(obj.getDefectState().name())));
        sb.append(String.format("%s: %s <br />", MessageUtils.message("moduleName"),obj.getModuleName()));
        sb.append(String.format("%s: %s <br />", MessageUtils.message("moduleVersion"),obj.getModuleVersion()));
        sb.append(String.format("%s: %s <br />", MessageUtils.message("defectDescribe"),obj.getDefectDescribe()));
        return sb.toString();
    }

    @Override
    public String toMarkdown(SysDefect obj, Map<String, Object> params) {
        if(this.isValid(params)==false) return null;
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

    private boolean isValid(Map<String, Object> params) {
        if(params.containsKey(DEFECT_CONFIG_KEY)==false) return false;
        Map<String, Object> defectParams = (Map<String, Object>) params.get(DEFECT_CONFIG_KEY);
        if(defectParams.containsKey(OPTION_CONFIG_KEY)==false) return false;
        Map<String, Object> optionParams = (Map<String, Object>) defectParams.get(OPTION_CONFIG_KEY);
        if(optionParams.containsKey(OPTION_REALTIME_CONFIG_KEY)==false) return false;
        Map<String, Object> intervalParams = (Map<String, Object>) optionParams.get(OPTION_REALTIME_CONFIG_KEY);
        if(intervalParams.containsKey(SWITCH_KEY)==false) return false;
        return (boolean) intervalParams.get(SWITCH_KEY);
    }
}
