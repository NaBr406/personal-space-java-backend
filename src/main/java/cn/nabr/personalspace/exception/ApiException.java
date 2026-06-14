package cn.nabr.personalspace.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 * 抛出时顺带携带 HTTP 状态码，交给统一异常处理器转换响应。
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
