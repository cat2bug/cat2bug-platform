package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.type.SysDefectStateEnum;
import com.cat2bug.system.domain.type.SysDefectTypeEnum;
import com.cat2bug.system.mapper.SysDefectStatisticMapper;
import com.cat2bug.system.service.ISysDefectStatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-22 02:03
 * @Version: 1.0.0
 */
@Service
public class SysDefectStatisticServiceImpl implements ISysDefectStatisticService {
    @Autowired
    private SysDefectStatisticMapper sysDefectStatisticMapper;

    @Override
    public List<Map<String,Object>> typeStatistic(Long projectId, Long memberId) {
        List<Map<String,Object>> ret = sysDefectStatisticMapper.typeStatistic(projectId,memberId);
        return ret.stream().map(m->{
            m.put("k", SysDefectTypeEnum.values()[Long.valueOf(m.get("k").toString()).intValue()].name());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> stateStatistic(Long projectId, Long memberId) {
        List<Map<String,Object>> ret = sysDefectStatisticMapper.stateStatistic(projectId,memberId);
        for(int i=0;i<SysDefectStateEnum.values().length;i++) {
            int index = SysDefectStateEnum.values()[i].ordinal();
            if(ret.stream().anyMatch(m->index==Long.valueOf(m.get("k").toString()))==false) {
                Map<String,Object> map = new HashMap<>();
                map.put("id",index);
                map.put("k",index);
                map.put("a",0);
                map.put("d",0);
                map.put("w",0);
                ret.add(map);
            }
        }
        return ret.stream().map(m->{
            m.put("id",m.get("k"));
            m.put("k", SysDefectStateEnum.values()[Long.valueOf(m.get("k").toString()).intValue()].name());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> moduleStatistic(Long projectId) {
        return sysDefectStatisticMapper.moduleStatistic(projectId);
    }
}
