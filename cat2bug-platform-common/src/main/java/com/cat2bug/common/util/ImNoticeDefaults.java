package com.cat2bug.common.util;

import java.util.HashMap;
import java.util.Map;

public final class ImNoticeDefaults {

    private ImNoticeDefaults() {
    }

    public static Map<String, Object> defaultDefectNoticeOption() {
        Map<String, Object> moduleOption = new HashMap<>();

        Map<String, Object> defectOption = new HashMap<>();
        moduleOption.put("defect", defectOption);

        Map<String, Object> eventOption = new HashMap<>();
        defectOption.put("event", eventOption);
        eventOption.put("receiver", true);
        eventOption.put("collect", true);

        Map<String, Object> optionOption = new HashMap<>();
        defectOption.put("option", optionOption);
        Map<String, Object> realtimeOption = new HashMap<>();
        optionOption.put("realtime", realtimeOption);
        realtimeOption.put("switch", true);
        return moduleOption;
    }
}
