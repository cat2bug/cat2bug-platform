package com.cat2bug.common.domain;

public class SysComment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long commentId;
    private String commentContent;
    private String moduleType;
    private Long correlationId;
    /** NULL=主评论；非空=挂在主评论下的扁平回复（defect_log 不使用）。 */
    private Long parentId;
    /** line / selection，仅报告批注使用。 */
    private String anchorKind;
    private String quote;
    private String contextBefore;
    private String contextAfter;
    private String anchorHint;
    private SysUser createMember;

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public Long getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(Long correlationId) {
        this.correlationId = correlationId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getAnchorKind() {
        return anchorKind;
    }

    public void setAnchorKind(String anchorKind) {
        this.anchorKind = anchorKind;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public String getContextBefore() {
        return contextBefore;
    }

    public void setContextBefore(String contextBefore) {
        this.contextBefore = contextBefore;
    }

    public String getContextAfter() {
        return contextAfter;
    }

    public void setContextAfter(String contextAfter) {
        this.contextAfter = contextAfter;
    }

    public String getAnchorHint() {
        return anchorHint;
    }

    public void setAnchorHint(String anchorHint) {
        this.anchorHint = anchorHint;
    }

    public SysUser getCreateMember() {
        return createMember;
    }

    public void setCreateMember(SysUser createMember) {
        this.createMember = createMember;
    }
}
