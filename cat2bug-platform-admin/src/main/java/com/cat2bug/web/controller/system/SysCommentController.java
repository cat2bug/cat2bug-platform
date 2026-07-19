package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.service.ISysCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 评论Controller
 * 
 * @author yuzhantao
 * @date 2024-02-29
 */
@RestController
@RequestMapping("/system/comment")
public class SysCommentController extends BaseController
{
    @Autowired
    private ISysCommentService sysCommentService;

    /**
     * 查询评论列表
     *
     * 报告批注（moduleType=report）与缺陷评论共用本接口：持报告查看权限即可拉取批注/回复列表，
     * 具体创建人/管理员校验在删除时由 Service 层处理。
     */
    @PreAuthorize("@ss.hasPermi('system:comment:list') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list') || @ss.hasPermi('system:report:list') || @ss.hasPermi('system:report:query')")
    @GetMapping("/list")
    public TableDataInfo list(SysComment sysComment)
    {
        startPage();
        List<SysComment> list = sysCommentService.selectSysCommentList(sysComment);
        return getDataTable(list);
    }

    /**
     * 获取评论详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:comment:query') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list') || @ss.hasPermi('system:report:list') || @ss.hasPermi('system:report:query')")
    @GetMapping(value = "/{commentId}")
    public AjaxResult getInfo(@PathVariable("commentId") Long commentId)
    {
        return success(sysCommentService.selectSysCommentByCommentId(commentId));
    }

    /**
     * 新增评论
     *
     * 报告批注 / 扁平回复（携带 parentId）与缺陷评论共用本接口，能打开报告详情的成员即可创建批注。
     */
    @PreAuthorize("@ss.hasPermi('system:comment:add') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list') || @ss.hasPermi('system:report:list') || @ss.hasPermi('system:report:query')")
    @Log(title = "评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysComment sysComment)
    {
        return toAjax(sysCommentService.insertSysComment(sysComment));
    }

    /**
     * 修改评论
     */
    @PreAuthorize("@ss.hasPermi('system:comment:edit') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list')")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysComment sysComment)
    {
        return toAjax(sysCommentService.updateSysComment(sysComment));
    }

    /**
     * 删除评论
     *
     * 接口层仅放行「能查看该模块」的成员发起删除请求；创建人 / 项目管理员 / 团队管理员的精确鉴权
     * 由 {@link ISysCommentService#deleteSysCommentByCommentIds} 内部校验（moduleType=report 时），
     * 未通过校验会抛出 ServiceException 并由全局异常处理器转换为无权限响应。
     */
    @PreAuthorize("@ss.hasPermi('system:comment:remove') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list') || @ss.hasPermi('system:report:list') || @ss.hasPermi('system:report:query')")
    @Log(title = "评论", businessType = BusinessType.DELETE)
	@DeleteMapping("/{commentIds}")
    public AjaxResult remove(@PathVariable Long[] commentIds)
    {
        return toAjax(sysCommentService.deleteSysCommentByCommentIds(commentIds));
    }
}
