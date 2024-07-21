package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.im.service.IMService;
import com.cat2bug.im.service.impl.DefaultMessageTemplateImpl;
import com.cat2bug.system.domain.SysNotice;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysNoticeService;
import com.cat2bug.web.vo.SendNotice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公告 信息操作处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;
    @Autowired
    private IMService imService;
    @Autowired
    private ISysDefectService sysDefectService;
    @Autowired
    private DefaultMessageTemplateImpl defaultMessageTemplate;
    /**
     * 获取通知公告列表
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')||@ss.hasPermi('notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:notice:list')||@ss.hasPermi('notice:list')")
    @GetMapping("/statistics/group")
    public TableDataInfo groupStatistics()
    {
        List<Map<String, Object>> list = noticeService.noticeGroupStatistics(getUserId());
        return getDataTable(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:notice:query')||@ss.hasPermi('notice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable String noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')||@ss.hasPermi('notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable String[] noticeIds)
    {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }

    /**
     * 发送通知公告
     */
    @PreAuthorize("@ss.hasPermi('notice:send')")
    @PostMapping("/send")
    public AjaxResult send(@RequestBody SendNotice notice)
    {
        imService.sendMessage(
                notice.getProjectId(),
                notice.getGroupName(),
                getUserId(),
                notice.getReceiveIds(),
                notice.getTitle(),
                notice.getContent(),
                String.format("%s/#/notice/index?projectId=%d",notice.getSrc(), notice.getProjectId()),
                defaultMessageTemplate,
                sysDefectService.getDefaultDefectNoticeOption()
                );
        return success();
    }
}
