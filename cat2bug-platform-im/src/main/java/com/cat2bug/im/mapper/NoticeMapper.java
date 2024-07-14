package com.cat2bug.im.mapper;

import com.cat2bug.im.domain.Member;
import com.cat2bug.im.domain.NoticeMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 21:41
 * @Version: 1.0.0
 */
@Mapper
public interface NoticeMapper {
    public int insertNotice(NoticeMessage notice);
}
