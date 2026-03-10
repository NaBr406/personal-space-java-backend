package cn.nabr.personalspace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void returnsFrontendFriendlyMessageForOversizedUploads() {
        var response = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(100L * 1024 * 1024));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("图片不能超过 100MB", response.getBody().get("error"));
    }
}
