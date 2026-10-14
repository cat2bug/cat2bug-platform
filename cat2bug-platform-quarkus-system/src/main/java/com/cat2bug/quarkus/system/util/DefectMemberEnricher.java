package com.cat2bug.quarkus.system.util;

import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.system.mapper.SysUserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class DefectMemberEnricher {

    @Inject
    SysUserMapper sysUserMapper;

    public void enrich(List<SysDefect> defects) {
        if (defects == null || defects.isEmpty()) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (SysDefect defect : defects) {
            collectUserIds(defect, userIds);
        }
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, SysUser> users = sysUserMapper.selectUsersByIds(new ArrayList<>(userIds)).stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> left));
        for (SysDefect defect : defects) {
            applyMembers(defect, users);
        }
    }

    public void enrich(SysDefect defect) {
        if (defect == null) {
            return;
        }
        enrich(List.of(defect));
    }

    private void collectUserIds(SysDefect defect, Set<Long> userIds) {
        if (defect.getCreateById() != null) {
            userIds.add(defect.getCreateById());
        }
        if (defect.getHandleBy() == null) {
            return;
        }
        for (Long userId : defect.getHandleBy()) {
            if (userId != null) {
                userIds.add(userId);
            }
        }
    }

    private void applyMembers(SysDefect defect, Map<Long, SysUser> users) {
        if (defect.getCreateById() != null) {
            SysUser creator = users.get(defect.getCreateById());
            defect.setCreateMember(creator);
            if (creator != null && creator.getNickName() != null) {
                defect.setCreateMemberName(creator.getNickName());
            }
        }
        List<Long> handleBy = defect.getHandleBy();
        if (handleBy == null || handleBy.isEmpty()) {
            defect.setHandleByList(Collections.emptyList());
            return;
        }
        List<SysUser> handleByList = new ArrayList<>();
        for (Long userId : handleBy) {
            if (userId == null) {
                continue;
            }
            SysUser user = users.get(userId);
            if (user != null) {
                handleByList.add(user);
            }
        }
        defect.setHandleByList(handleByList);
    }
}
