package com.cat2bug.admin.mcp;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptArg;
import io.quarkiverse.mcp.server.PromptMessage;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * 引导命令（MCP Prompts）：只生成分步指令，不读写数据；权限仍由工具校验。
 */
public class McpGuidePrompts {

    @Inject
    McpGuideContent content;

    @Prompt(name = "report_defect", title = "提交缺陷",
            description = "引导你一步步补全信息并提交缺陷：类型、复现步骤、优先级、交付物、处理人。")
    PromptMessage reportDefect(@PromptArg(name = "summary", title = "问题简述",
            description = "用一句话描述遇到的问题", required = false) String summary) {
        return message("report_defect", Map.of("summary", nz(summary)));
    }

    @Prompt(name = "fix_defect", title = "修复缺陷",
            description = "引导开发处理缺陷：读缺陷 → 定位代码 → 修复 → 提交修复并关联提交、指定审核人。")
    PromptMessage fixDefect(@PromptArg(name = "defect_num", title = "缺陷编号",
            description = "缺陷列表里的 # 编号；不填则从指派给我的缺陷里选", required = false) String defectNum) {
        return message("fix_defect", Map.of("defect_num", nz(defectNum)));
    }

    @Prompt(name = "review_defect", title = "审核缺陷",
            description = "引导审核已修复的缺陷：看修复说明与提交 → 验证 → 通过或驳回。")
    PromptMessage reviewDefect(@PromptArg(name = "defect_num", title = "缺陷编号",
            description = "缺陷列表里的 # 编号；不填则从等我审核的缺陷里选", required = false) String defectNum) {
        return message("review_defect", Map.of("defect_num", nz(defectNum)));
    }

    @Prompt(name = "run_test_plan", title = "执行测试计划",
            description = "引导逐条执行测试计划子项、记录结果，不通过时提缺陷并关联。")
    PromptMessage runTestPlan(@PromptArg(name = "plan_name", title = "计划名称",
            description = "要执行的测试计划名称；不填则先列出计划让我选", required = false) String planName) {
        return message("run_test_plan", Map.of("plan_name", nz(planName)));
    }

    @Prompt(name = "write_test_cases", title = "编写测试用例",
            description = "引导为某个交付物编写测试用例：确认需求 → 拟用例 → 确认后批量创建。")
    PromptMessage writeTestCases(
            @PromptArg(name = "module_name", title = "交付物名称",
                    description = "用例归属的交付物（功能模块）", required = false) String moduleName,
            @PromptArg(name = "requirement", title = "需求描述",
                    description = "要测试的功能或需求说明", required = false) String requirement) {
        Map<String, String> args = new HashMap<>();
        args.put("module_name", nz(moduleName));
        args.put("requirement", nz(requirement));
        return message("write_test_cases", args);
    }

    @Prompt(name = "project_status", title = "项目现状",
            description = "汇总我的待办、缺陷分布、测试进度与最近报告，并建议下一步做什么。")
    PromptMessage projectStatus() {
        return message("project_status", Map.of());
    }

    private PromptMessage message(String name, Map<String, String> args) {
        return PromptMessage.withUserRole(content.prompt(name, args));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
