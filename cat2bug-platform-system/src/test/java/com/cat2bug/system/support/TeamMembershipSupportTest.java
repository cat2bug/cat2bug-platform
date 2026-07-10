package com.cat2bug.system.support;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TeamMembershipSupportTest {

    @Mock
    private SysUserTeamMapper sysUserTeamMapper;

    @InjectMocks
    private TeamMembershipSupport teamMembershipSupport;

    @Test
    public void isTeamMember_trueWhenActiveMembershipExists() {
        SysUserTeam row = new SysUserTeam();
        row.setUserId(1L);
        row.setTeamId(10L);
        row.setTeamLock(0);
        when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.singletonList(row));

        assertTrue(teamMembershipSupport.isTeamMember(1L, 10L));
    }

    @Test
    public void isTeamMember_falseWhenOnlyLockedMembership() {
        SysUserTeam row = new SysUserTeam();
        row.setUserId(1L);
        row.setTeamId(10L);
        row.setTeamLock(1);
        when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.singletonList(row));

        assertFalse(teamMembershipSupport.isTeamMember(1L, 10L));
    }

    @Test
    public void isTeamMember_falseWhenNoRows() {
        when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.emptyList());
        assertFalse(teamMembershipSupport.isTeamMember(1L, 10L));
    }

    @Test(expected = ServiceException.class)
    public void requireTeamMember_throwsForbiddenForNonMember() {
        when(sysUserTeamMapper.selectSysUserTeamList(any())).thenReturn(Collections.emptyList());
        try {
            teamMembershipSupport.requireTeamMember(1L, 10L);
        } catch (ServiceException ex) {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), ex.getCode());
            throw ex;
        }
    }
}
