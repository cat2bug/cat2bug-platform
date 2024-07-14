package com.cat2bug.system.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cat2bug.common.core.domain.WebSocketResult;
import com.cat2bug.common.websocket.MessageWebsocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.domain.SysNotice;
import com.cat2bug.system.mapper.SysNoticeMapper;
import com.cat2bug.system.service.ISysNoticeService;

/**
 * 公告 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService
{
    private final static String NOTICE_ACTION = "notice";
    @Autowired
    private SysNoticeMapper noticeMapper;
    @Autowired
    private MessageWebsocket messageWebsocket;
    @Override
    public List<Map<String, Object>> noticeGroupStatistics(Long receiveId) {
        return noticeMapper.noticeGroupStatistics(receiveId);
    }

    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNotice selectNoticeById(Long noticeId)
    {
        // 读取通知
        SysNotice notice = noticeMapper.selectNoticeById(noticeId);
        // 如果通知是未读状态，改吧其状态为已读
        if(notice.getIsRead()==false) {
            notice.setIsRead(true);
            // 更新通知已读
            SysNotice n = new SysNotice();
            n.setIsRead(true);
            n.setNoticeId(noticeId);
            noticeMapper.updateNotice(n);
            // 发送websocket消息通知前端通知有变化
            messageWebsocket.sendMessage(notice.getReceiverId(), WebSocketResult.success(NOTICE_ACTION,n));
        }
        return notice;
    }

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice)
    {
        return noticeMapper.selectNoticeList(notice);
    }

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNotice notice)
    {
        return noticeMapper.insertNotice(notice);
    }

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice)
    {
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId)
    {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }
}
