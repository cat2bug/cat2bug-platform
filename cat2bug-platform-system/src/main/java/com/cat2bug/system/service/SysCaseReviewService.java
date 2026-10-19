package com.cat2bug.system.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysCaseReview;
import com.cat2bug.common.domain.SysCaseReviewItem;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.mapper.SysCaseReviewMapper;
import com.cat2bug.system.mapper.SysCaseTypeMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class SysCaseReviewService {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_COMPLETED = "completed";
    public static final String VERDICT_PENDING = "pending";
    public static final String VERDICT_PASSED = "passed";
    public static final String VERDICT_CHANGES = "changes";
    public static final String STAMP_IN_REVIEW = "in_review";

    @Inject
    SysCaseReviewMapper reviewMapper;

    @Inject
    SysCaseMapper sysCaseMapper;

    @Inject
    SysCaseTypeMapper sysCaseTypeMapper;

    @Inject
    SysCaseTypeService sysCaseTypeService;

    @Inject
    AuthContext authContext;

    public List<SysCaseReview> list(Long projectId) {
        return reviewMapper.selectByProject(projectId);
    }

    public SysCaseReview get(Long reviewId) {
        SysCaseReview review = requireReview(reviewId);
        review.setItems(reviewMapper.selectItems(reviewId));
        return review;
    }

    @Transactional
    public SysCaseReview start(Long projectId, List<Long> caseIds) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        if (caseIds == null || caseIds.isEmpty()) {
            throw new ApiException("请勾选要评审的用例");
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : caseIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        if (unique.isEmpty()) {
            throw new ApiException("请勾选要评审的用例");
        }
        List<String> busy = new ArrayList<>();
        List<SysCase> cases = new ArrayList<>();
        for (Long caseId : unique) {
            SysCase sysCase = sysCaseMapper.selectSysCaseByCaseId(caseId);
            if (sysCase == null || sysCase.getProjectId() == null || !sysCase.getProjectId().equals(projectId)) {
                throw new ApiException("用例不存在");
            }
            Long openId = reviewMapper.selectOpenReviewIdByCase(caseId);
            if (openId != null) {
                busy.add(String.valueOf(sysCase.getCaseNum() == null ? caseId : sysCase.getCaseNum()));
            }
            cases.add(sysCase);
        }
        if (!busy.isEmpty()) {
            throw new ApiException("用例已在进行中的评审里：" + String.join("、", busy));
        }
        LoginUser user = requireLoginUser();
        Date now = new Date();
        SysCaseReview review = new SysCaseReview();
        review.setProjectId(projectId);
        review.setStatus(STATUS_OPEN);
        review.setCreateBy(user.getUsername());
        review.setCreateTime(now);
        if (reviewMapper.insertReview(review) <= 0 || review.getReviewId() == null) {
            throw new ApiException("发起评审失败");
        }
        for (SysCase sysCase : cases) {
            SysCaseReviewItem item = new SysCaseReviewItem();
            item.setReviewId(review.getReviewId());
            item.setCaseId(sysCase.getCaseId());
            item.setVerdict(VERDICT_PENDING);
            reviewMapper.insertItem(item);
            reviewMapper.updateCaseReviewStatus(sysCase.getCaseId(), STAMP_IN_REVIEW);
        }
        return get(review.getReviewId());
    }

    /**
     * 页面点完成评审时一次写入：跳过已在评审中的用例，其余直接生成已完成评审并写章。
     * 打开弹框时不调用本方法，因此不会提前把用例改成评审中。
     */
    @Transactional
    public SysCaseReview submit(Long projectId, List<SysCaseReviewItem> items) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        LinkedHashMap<Long, SysCaseReviewItem> unique = new LinkedHashMap<>();
        if (items != null) {
            for (SysCaseReviewItem item : items) {
                if (item != null && item.getCaseId() != null) {
                    unique.put(item.getCaseId(), item);
                }
            }
        }
        if (unique.isEmpty()) {
            throw new ApiException("请勾选要评审的用例");
        }
        for (SysCaseReviewItem item : unique.values()) {
            item.setVerdict(normalizeVerdict(item.getVerdict(), false));
            if (item.getComment() == null) {
                item.setComment("");
            }
        }
        List<Long> ids = new ArrayList<>(unique.keySet());
        Map<Long, SysCase> byId = new HashMap<>();
        List<SysCase> found = sysCaseMapper.selectSysCaseBriefByIds(ids);
        if (found != null) {
            for (SysCase sysCase : found) {
                byId.put(sysCase.getCaseId(), sysCase);
            }
        }
        Set<Long> openCaseIds = new HashSet<>();
        List<Long> open = reviewMapper.selectOpenCaseIds(ids);
        if (open != null) {
            openCaseIds.addAll(open);
        }
        List<String> skipped = new ArrayList<>();
        List<SysCaseReviewItem> accepted = new ArrayList<>();
        for (SysCaseReviewItem item : unique.values()) {
            SysCase sysCase = byId.get(item.getCaseId());
            if (sysCase == null || sysCase.getProjectId() == null || !sysCase.getProjectId().equals(projectId)) {
                throw new ApiException("用例不存在");
            }
            if (openCaseIds.contains(item.getCaseId()) || STAMP_IN_REVIEW.equals(sysCase.getReviewStatus())) {
                skipped.add(String.valueOf(sysCase.getCaseNum() == null ? item.getCaseId() : sysCase.getCaseNum()));
                continue;
            }
            accepted.add(item);
        }
        if (accepted.isEmpty()) {
            throw new ApiException("所选用例都已在评审中");
        }
        LoginUser user = requireLoginUser();
        SysCaseReview review = new SysCaseReview();
        review.setProjectId(projectId);
        review.setStatus(STATUS_OPEN);
        review.setCreateBy(user.getUsername());
        review.setCreateTime(new Date());
        if (reviewMapper.insertReview(review) <= 0 || review.getReviewId() == null) {
            throw new ApiException("发起评审失败");
        }
        for (SysCaseReviewItem item : accepted) {
            item.setReviewId(review.getReviewId());
            reviewMapper.insertItem(item);
        }
        SysCaseReview done = complete(review.getReviewId());
        done.setSkippedCaseNums(skipped);
        return done;
    }

    @Transactional
    public SysCaseReview comment(Long reviewId, Long caseId, String verdict, String comment) {
        SysCaseReview review = requireOpen(reviewId);
        if (reviewMapper.selectItem(reviewId, caseId) == null) {
            throw new ApiException("评审中没有这条用例");
        }
        String normalized = normalizeVerdict(verdict, true);
        SysCaseReviewItem item = new SysCaseReviewItem();
        item.setReviewId(reviewId);
        item.setCaseId(caseId);
        item.setVerdict(normalized);
        item.setComment(comment == null ? "" : comment);
        reviewMapper.updateItem(item);
        return get(review.getReviewId());
    }

    @Transactional
    public SysCaseReview complete(Long reviewId) {
        SysCaseReview review = requireOpen(reviewId);
        List<SysCaseReviewItem> items = reviewMapper.selectItems(reviewId);
        if (items.isEmpty()) {
            throw new ApiException("评审里没有用例");
        }
        int passed = 0;
        int changes = 0;
        for (SysCaseReviewItem item : items) {
            if (VERDICT_PASSED.equals(item.getVerdict())) {
                passed++;
            } else if (VERDICT_CHANGES.equals(item.getVerdict())) {
                changes++;
            } else {
                throw new ApiException("还有用例未给出通过或需修改的结论");
            }
        }
        LoginUser user = requireLoginUser();
        String markdown = render(review, items, user.getUsername(), passed, changes);
        review.setReportMarkdown(markdown);
        review.setCompleteBy(user.getUsername());
        review.setCompleteTime(new Date());
        if (reviewMapper.completeReview(review) <= 0) {
            throw new ApiException("完成评审失败");
        }
        for (SysCaseReviewItem item : items) {
            reviewMapper.updateCaseReviewStatus(item.getCaseId(), item.getVerdict());
        }
        return get(reviewId);
    }

    @Transactional
    public SysCaseReview updateReport(Long reviewId, String markdown) {
        SysCaseReview review = requireReview(reviewId);
        if (!STATUS_COMPLETED.equals(review.getStatus())) {
            throw new ApiException("评审尚未完成");
        }
        reviewMapper.updateReport(reviewId, markdown == null ? "" : markdown);
        return get(reviewId);
    }

    @Transactional
    public void onCaseDeleted(Long caseId) {
        if (caseId == null) {
            return;
        }
        List<Long> openIds = reviewMapper.selectOpenReviewIdsByCase(caseId);
        reviewMapper.deleteOpenItemsByCase(caseId);
        if (openIds == null) {
            return;
        }
        for (Long reviewId : openIds) {
            if (reviewMapper.countItems(reviewId) == 0) {
                reviewMapper.deleteReview(reviewId);
            }
        }
    }

    private String render(SysCaseReview review, List<SysCaseReviewItem> items, String completer, int passed, int changes) {
        String template = sysCaseTypeMapper.selectReviewTemplate(review.getProjectId());
        if (template == null || template.isBlank()) {
            template = sysCaseTypeService.builtinReviewTemplate();
        }
        String project = sysCaseTypeMapper.selectProjectName(review.getProjectId());
        Set<String> reviewers = new LinkedHashSet<>();
        if (review.getCreateBy() != null && !review.getCreateBy().isBlank()) {
            reviewers.add(review.getCreateBy());
        }
        if (completer != null && !completer.isBlank()) {
            reviewers.add(completer);
        }
        return template
                .replace("{{project}}", project == null ? "" : project)
                .replace("{{caseCount}}", String.valueOf(items.size()))
                .replace("{{reviewers}}", String.join("、", reviewers))
                .replace("{{date}}", LocalDate.now().toString())
                .replace("{{passedCount}}", String.valueOf(passed))
                .replace("{{changesCount}}", String.valueOf(changes))
                .replace("{{cases}}", renderCasesTable(items));
    }

    private static String renderCasesTable(List<SysCaseReviewItem> items) {
        StringBuilder cases = new StringBuilder();
        cases.append("| 编号 | 标题 | 类型 | 结论 | 意见 |\n");
        cases.append("| --- | --- | --- | --- | --- |\n");
        for (SysCaseReviewItem item : items) {
            String typeName = item.getCaseTypeName() == null || item.getCaseTypeName().isBlank()
                    ? "未分类" : item.getCaseTypeName();
            cases.append("| ")
                    .append(mdCell(item.getCaseNum() == null ? "" : String.valueOf(item.getCaseNum())))
                    .append(" | ")
                    .append(mdCell(item.getCaseName()))
                    .append(" | ")
                    .append(mdCell(typeName))
                    .append(" | ")
                    .append(mdCell(verdictLabel(item.getVerdict())))
                    .append(" | ")
                    .append(mdCell(item.getComment()))
                    .append(" |\n");
        }
        return cases.toString().trim();
    }

    private static String mdCell(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("|", "\\|").replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String verdictLabel(String verdict) {
        if (VERDICT_PASSED.equals(verdict)) {
            return "通过";
        }
        if (VERDICT_CHANGES.equals(verdict)) {
            return "需修改";
        }
        return "待定";
    }

    private SysCaseReview requireOpen(Long reviewId) {
        SysCaseReview review = requireReview(reviewId);
        if (!STATUS_OPEN.equals(review.getStatus())) {
            throw new ApiException("评审已结束");
        }
        return review;
    }

    private SysCaseReview requireReview(Long reviewId) {
        if (reviewId == null) {
            throw new ApiException("评审不存在");
        }
        SysCaseReview review = reviewMapper.selectReview(reviewId);
        if (review == null) {
            throw new ApiException("评审不存在");
        }
        return review;
    }

    private static String normalizeVerdict(String verdict, boolean allowPending) {
        if (verdict == null) {
            throw new ApiException("请选择结论");
        }
        String value = verdict.trim();
        if (VERDICT_PASSED.equals(value) || VERDICT_CHANGES.equals(value) || (allowPending && VERDICT_PENDING.equals(value))) {
            return value;
        }
        throw new ApiException("结论只能是通过或需修改");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
