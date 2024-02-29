package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysComment;
import com.cat2bug.system.service.ISysCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
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
     */
    @PreAuthorize("@ss.hasPermi('system:comment:list') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list')")
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
    @PreAuthorize("@ss.hasPermi('system:comment:query') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list')")
    @GetMapping(value = "/{commentId}")
    public AjaxResult getInfo(@PathVariable("commentId") Long commentId)
    {
        return success(sysCommentService.selectSysCommentByCommentId(commentId));
    }

    /**
     * 新增评论
     */
    @PreAuthorize("@ss.hasPermi('system:comment:add') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list')")
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
     */
    @PreAuthorize("@ss.hasPermi('system:comment:remove') || @ss.hasPermi('system:defect:list') || @ss.hasPermi('system:defect:log:list')")
    @Log(title = "评论", businessType = BusinessType.DELETE)
	@DeleteMapping("/{commentIds}")
    public AjaxResult remove(@PathVariable Long[] commentIds)
    {
        return toAjax(sysCommentService.deleteSysCommentByCommentIds(commentIds));
    }
}
