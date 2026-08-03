package com.interviewai.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;
    private final Instant timestamp;
    private final String path;

    private ApiResponse(int status, String message, T data, String path) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
        this.path = path;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, null);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data, null);
    }

    public static <T> ApiResponse<T> of(int status, String message, T data) {
        return new ApiResponse<>(status, message, data, null);
    }

    public static <T> ApiResponse<T> error(int status, String message, String path) {
        return new ApiResponse<>(status, message, null, path);
    }
}
