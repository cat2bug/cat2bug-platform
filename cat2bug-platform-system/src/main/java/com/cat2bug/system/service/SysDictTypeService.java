package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysDictType;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysDictDataMapper;
import com.cat2bug.system.mapper.SysDictTypeMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysDictTypeService {

    @Inject
    SysDictTypeMapper sysDictTypeMapper;

    @Inject
    SysDictDataMapper sysDictDataMapper;

    @Inject
    SysDictDataService sysDictDataService;

    public List<SysDictType> selectDictTypeList(SysDictType query) {
        List<SysDictType> list = sysDictTypeMapper.selectDictTypeList(query);
        return list != null ? list : List.of();
    }

    public TableDataInfo selectDictTypePage(SysDictType query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysDictTypeMapper.countDictTypeList(query);
        List<SysDictType> all = selectDictTypeList(query);
        int from = Math.min(page.getOffset(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        return new TableDataInfo(all.subList(from, to), total);
    }

    public List<SysDictType> selectDictTypeAll() {
        return sysDictTypeMapper.selectDictTypeAll();
    }

    public SysDictType selectDictTypeById(Long dictId) {
        return sysDictTypeMapper.selectDictTypeById(dictId);
    }

    public boolean checkDictTypeUnique(SysDictType dict) {
        Long dictId = dict.getDictId() == null ? -1L : dict.getDictId();
        SysDictType info = sysDictTypeMapper.checkDictTypeUnique(dict.getDictType());
        return info == null || info.getDictId().longValue() == dictId.longValue();
    }

    public int insertDictType(SysDictType dict) {
        int row = sysDictTypeMapper.insertDictType(dict);
        if (row > 0 && dict.getDictType() != null) {
            sysDictDataService.removeDictCache(dict.getDictType());
        }
        return row;
    }

    @Transactional
    public int updateDictType(SysDictType dict) {
        SysDictType oldDict = sysDictTypeMapper.selectDictTypeById(dict.getDictId());
        if (oldDict == null) {
            return 0;
        }
        sysDictDataMapper.updateDictDataType(oldDict.getDictType(), dict.getDictType());
        int row = sysDictTypeMapper.updateDictType(dict);
        if (row > 0) {
            sysDictDataService.removeDictCache(oldDict.getDictType());
            sysDictDataService.refreshDictCacheByType(dict.getDictType());
        }
        return row;
    }

    public void deleteDictTypeByIds(Long[] dictIds) {
        for (Long dictId : dictIds) {
            SysDictType dictType = selectDictTypeById(dictId);
            if (dictType == null) {
                continue;
            }
            if (sysDictDataMapper.countDictDataByType(dictType.getDictType()) > 0) {
                throw new ApiException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            sysDictTypeMapper.deleteDictTypeById(dictId);
            sysDictDataService.removeDictCache(dictType.getDictType());
        }
    }

    public void resetDictCache() {
        sysDictDataService.resetDictCache();
    }
}
