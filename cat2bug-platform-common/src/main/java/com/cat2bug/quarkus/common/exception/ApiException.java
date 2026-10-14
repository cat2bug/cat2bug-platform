package com.cat2bug.quarkus.common.exception;

/**
 * 业务异常，由 {@link com.cat2bug.quarkus.framework.web.GlobalExceptionMapper} 转为 AjaxResult。
 */
public class ApiException extends RuntimeException {

    private final int httpStatus;
    private String detailMessage;

    public ApiException(String message) {
        this(500, message);
    }

    public ApiException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /** 对齐 Spring {@code ServiceException(String message, Integer code)} 参数顺序。 */
    public ApiException(String message, int httpStatus) {
        this(httpStatus, message);
    }

    public ApiException(String message, Integer httpStatus) {
        this(httpStatus != null ? httpStatus : 500, message);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    /** 对齐 Spring ServiceException 链式细节信息。 */
    public ApiException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
