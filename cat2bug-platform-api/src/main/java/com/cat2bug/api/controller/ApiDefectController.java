package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.domain.ApiDefectHandle;
import com.cat2bug.api.domain.ApiDefectRequest;
import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.api.service.IApiDeliverableService;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysDefectLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 23:30
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/api/defect")
//@Api(tags = "缺陷")
public class ApiDefectController extends BaseController {
    @Resource
    private IApiDefectService apiDefectService;
    @Resource
    private IApiDeliverableService apiDeliverableService;
    @Resource
    private ApiService apiService;
    /**
     * 查询缺陷列表
     * @param apiDefect 缺陷参数对象
     * @return  缺陷集合
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @GetMapping
    public TableDataInfo list(ApiDefectRequest apiDefect)
    {
        Long projectId = this.apiService.getProjectId();
        Map<Long, ApiDeliverable> allPathApiDeliverableMap = this.apiDeliverableService.selectApiDeliverablePathList(projectId).
                stream().collect(Collectors.toMap(ApiDeliverable::getDeliverableId, d->d));
        startPage();
        List<ApiDefect> list = this.apiDefectService.selectApiDefectList(projectId, apiDefect);
        TableDataInfo tableDataInfo = getDataTable(list);
        tableDataInfo.setRows(list.stream().map(d->{
            if(d.getDeliverableId()!=null && allPathApiDeliverableMap.containsKey(d.getDeliverableId())) {
                d.setDeliverableName(allPathApiDeliverableMap.get(d.getDeliverableId()).getDeliverablePath());
            }
            // 等级转换为大写输出
            if(StringUtils.isNotBlank(d.getDefectLevel())){
                d.setDefectLevel(d.getDefectLevel().toUpperCase());
            }
            return d;
        }).collect(Collectors.toList()));
        return tableDataInfo;
    }

    /**
     * 获取缺陷详细信息
     */
    @PreAuthorize("@ss.hasPermi('api:defect:query')")
    @GetMapping(value = "/{defectNumber}")
    public AjaxResult getInfo(@PathVariable("defectNumber") Long defectNumber)
    {
        return success(apiDefectService.selectSysDefectByDefectNumber(defectNumber));
    }

    /**
     * 新增缺陷
     */
//    @Operation(summary = "添加缺陷")
//    @Parameters({
//            @Parameter(name = "defectName", description = "缺陷名称", required = true, in = ParameterIn.QUERY),
//            @Parameter(name = "defectDescribe", description = "缺陷描述", required = true, in = ParameterIn.QUERY)
//    })
    @PreAuthorize("@ss.hasPermi('api:defect:edit')")
    @PostMapping
    public AjaxResult add(@RequestBody(required=false)  ApiDefectRequest apiDefect)
    {
        return success(apiDefectService.insertApiDefect(apiDefect));
    }

    /**
     * 修改缺陷
     */
    @PreAuthorize("@ss.hasPermi('api:defect:add')")
    @PutMapping
    public AjaxResult edit(@RequestBody(required=false)  ApiDefectRequest apiDefect)
    {
        if(apiDefect==null) {
            apiDefect = new ApiDefectRequest();
        }
        return success(apiDefectService.updateSysDefect(apiDefect));
    }

    /**
     * 指派
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/{defectNum}/assign")
    public AjaxResult assign(@PathVariable Long defectNum, @RequestBody(required=false)  ApiDefectHandle apiDefectHandle)
    {
        apiDefectHandle = this.initApiDefectHandle(defectNum, apiDefectHandle);
        return success(apiDefectService.assign(apiDefectHandle));
    }

    /**
     * 驳回
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/{defectNum}/reject")
    public AjaxResult reject(@PathVariable Long defectNum, @RequestBody(required=false)  ApiDefectHandle apiDefectHandle)
    {
        apiDefectHandle = this.initApiDefectHandle(defectNum, apiDefectHandle);
        return success(apiDefectService.reject(apiDefectHandle));
    }

    /**
     * 修复
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/{defectNum}/repair")
    public AjaxResult repair(@PathVariable Long defectNum, @RequestBody(required=false)  ApiDefectHandle apiDefectHandle)
    {
        apiDefectHandle = this.initApiDefectHandle(defectNum, apiDefectHandle);
        return success(apiDefectService.repair(apiDefectHandle));
    }

    /**
     * 通过
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/{defectNum}/pass")
    public AjaxResult pass(@PathVariable Long defectNum, @RequestBody(required=false)  ApiDefectHandle apiDefectHandle)
    {
        apiDefectHandle = this.initApiDefectHandle(defectNum, apiDefectHandle);
        return success(apiDefectService.pass(apiDefectHandle));
    }

    /**
     * 关闭
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/{defectNum}/close")
    public AjaxResult close(@PathVariable Long defectNum, @RequestBody(required=false)  ApiDefectHandle apiDefectHandle)
    {
        apiDefectHandle = this.initApiDefectHandle(defectNum, apiDefectHandle);
        return success(apiDefectService.close(apiDefectHandle));
    }

    /**
     * 启动
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/{defectNum}/open")
    public AjaxResult open(@PathVariable Long defectNum, @RequestBody(required=false) ApiDefectHandle apiDefectHandle)
    {
        apiDefectHandle = this.initApiDefectHandle(defectNum, apiDefectHandle);
        return success(apiDefectService.open(apiDefectHandle));
    }

    /**
     * 初始化ApiDefectHandle实例
     * @param defectNum     缺陷编号
     * @param apiDefectHandle   缺陷处理实例
     * @return  初始化后的缺陷处理实例
     */
    private ApiDefectHandle initApiDefectHandle(Long defectNum, ApiDefectHandle apiDefectHandle) {
        if(apiDefectHandle==null) {
            apiDefectHandle = new ApiDefectHandle();
        }
        apiDefectHandle.setDefectNum(defectNum);
        return apiDefectHandle;
    }
}
