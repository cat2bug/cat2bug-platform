package com.cat2bug.system.mapper;

import io.quarkiverse.mybatis.runtime.meta.MapperDataSource;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import com.cat2bug.common.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@MapperDataSource(QuarkusMapperDataSources.RUNTIME_MYSQL)
public interface SysUserMapper {

    SysUser selectUserByUserName(@Param("userName") String userName);

    List<SysUser> selectUserList(@Param("user") SysUser user,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    long countUserList(@Param("user") SysUser user);

    long countAllocatedList(@Param("user") SysUser user);

    List<SysUser> selectAllocatedList(@Param("user") SysUser user,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    long countUnallocatedList(@Param("user") SysUser user);

    List<SysUser> selectUnallocatedList(@Param("user") SysUser user,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    SysUser selectUserById(@Param("userId") Long userId);

    List<SysUser> selectUsersByIds(@Param("userIds") List<Long> userIds);

    long countSysUserListByTeamId(@Param("teamId") Long teamId, @Param("search") String search);

    List<SysUser> selectSysUserListByTeamId(@Param("teamId") Long teamId,
                                            @Param("search") String search,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    long countSysUserListByTeamIdNotMember(@Param("teamId") Long teamId, @Param("search") String search);

    List<SysUser> selectSysUserListByTeamIdNotMember(@Param("teamId") Long teamId,
                                                    @Param("search") String search,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    SysUser checkUserNameUnique(@Param("userName") String userName);

    int insertUser(SysUser user);

    int updateUser(SysUser user);

    int deleteUserById(@Param("userId") Long userId);

    int deleteUserByIds(@Param("userIds") Long[] userIds);

    SysUser checkPhoneUnique(@Param("phoneNumber") String phoneNumber);

    SysUser checkEmailUnique(@Param("email") String email);

    int deleteUserRoleByUserId(@Param("userId") Long userId);

    int deleteUserRoleByUserIds(@Param("userIds") Long[] userIds);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    int updateUserAvatar(@Param("userName") String userName, @Param("avatar") String avatar);

    int resetUserPwd(@Param("userName") String userName, @Param("password") String password);

    SysUser selectUserByWechatMp(@Param("openId") String openId);

    int updateWechatMp(@Param("userName") String userName, @Param("openId") String openId);

    int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp);

    int clearLoginInfo(@Param("userId") Long userId);
}
