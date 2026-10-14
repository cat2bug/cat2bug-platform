package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.constant.CacheConstants;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysDictData;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.cache.AppCache;
import com.cat2bug.quarkus.system.mapper.SysDictDataMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysDictDataService {

    private static final Logger LOG = Logger.getLogger(SysDictDataService.class);
    private static final String REGION = "dict";
    private static final TypeReference<List<SysDictData>> DICT_DATA_LIST = new TypeReference<>() {};

    @Inject
    AppCache appCache;

    @Inject
    SysDictDataMapper sysDictDataMapper;

    public List<SysDictData> selectDictDataByType(String dictType) {
        List<SysDictData> cached = appCache.get(REGION, dictType, DICT_DATA_LIST);
        if (StringUtils.isNotEmpty(cached)) {
            return cached;
        }
        List<SysDictData> list = sysDictDataMapper.selectDictDataByType(dictType);
        if (StringUtils.isNotEmpty(list)) {
            appCache.set(REGION, dictType, list);
        }
        return list != null ? list : List.of();
    }

    public List<SysDictData> selectDictDataList(SysDictData query) {
        List<SysDictData> list = sysDictDataMapper.selectDictDataList(query);
        return list != null ? list : List.of();
    }

    public TableDataInfo selectDictDataPage(SysDictData query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysDictDataMapper.countDictDataList(query);
        List<SysDictData> all = selectDictDataList(query);
        int from = Math.min(page.getOffset(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        List<SysDictData> rows = all.subList(from, to);
        return new TableDataInfo(rows, total);
    }

    public SysDictData selectDictDataById(Long dictCode) {
        return sysDictDataMapper.selectDictDataById(dictCode);
    }

    public void loadDictCache() {
        appCache.clearRegion(REGION);
        List<SysDictData> all = sysDictDataMapper.selectDictDataList(new SysDictData());
        if (all == null || all.isEmpty()) {
            return;
        }
        Map<String, List<SysDictData>> grouped = all.stream()
                .filter(d -> "0".equals(d.getStatus()))
                .collect(Collectors.groupingBy(SysDictData::getDictType));
        grouped.forEach((type, list) -> appCache.set(REGION, type, new ArrayList<>(list)));
        LOG.infof("字典缓存已加载：%d 个类型", grouped.size());
    }

    public void clearDictCache() {
        appCache.clearRegion(REGION);
    }

    public void resetDictCache() {
        clearDictCache();
        loadDictCache();
    }

    public void removeDictCache(String dictType) {
        if (dictType != null) {
            appCache.remove(REGION, dictType);
        }
    }

    public void refreshDictCacheByType(String dictType) {
        refreshDictCache(dictType);
    }

    public int insertDictData(SysDictData data) {
        int row = sysDictDataMapper.insertDictData(data);
        if (row > 0 && data.getDictType() != null) {
            refreshDictCache(data.getDictType());
        }
        return row;
    }

    public int updateDictData(SysDictData data) {
        int row = sysDictDataMapper.updateDictData(data);
        if (row > 0 && data.getDictType() != null) {
            refreshDictCache(data.getDictType());
        }
        return row;
    }

    public void deleteDictDataByIds(Long[] dictCodes) {
        for (Long dictCode : dictCodes) {
            SysDictData data = selectDictDataById(dictCode);
            if (data == null) {
                continue;
            }
            sysDictDataMapper.deleteDictDataById(dictCode);
            refreshDictCache(data.getDictType());
        }
    }

    private void refreshDictCache(String dictType) {
        appCache.remove(REGION, dictType);
        List<SysDictData> list = sysDictDataMapper.selectDictDataByType(dictType);
        if (StringUtils.isNotEmpty(list)) {
            appCache.set(REGION, dictType, new ArrayList<>(list));
        }
    }
}
