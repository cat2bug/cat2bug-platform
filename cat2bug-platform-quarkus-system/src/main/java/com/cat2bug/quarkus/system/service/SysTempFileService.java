package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysTempFile;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysTempFileMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Date;

@ApplicationScoped
public class SysTempFileService {

    public static final int FILE_TYPE_IMAGE = 1;

    @Inject
    SysTempFileMapper sysTempFileMapper;

    @Transactional
    public void insertSysTempFile(SysTempFile sysTempFile) {
        sysTempFile.setCreateTime(new Date());
        if (sysTempFileMapper.insertSysTempFile(sysTempFile) <= 0) {
            throw new ApiException("保存临时文件失败");
        }
    }
}
