package com.cat2bug.im.mapper;

import com.cat2bug.im.domain.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-07 21:41
 * @Version: 1.0.0
 */
@Mapper
public interface MemberMapper {
    public Member findMember(Long memberId);

    public List<Member> selectMemberList(@Param("memberIds")List<Long> memberIds);
}
