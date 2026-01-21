package com.sd20201.datn.core.admin.banhang.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private int status = HttpStatus.OK.value();

    private String message;
    private T data;

    private String error;
    private String path;

    // Success methods
    public static <T> BaseResponse<T> success(String message) {
        return BaseResponse.<T>builder()
                .message(message)
                .build();
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .message("Success")
                .data(data)
                .build();
    }

    // Error methods
    public static <T> BaseResponse<T> error(String error) {
        return BaseResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(error)
                .build();
    }

    public static <T> BaseResponse<T> error(String error, HttpStatus status) {
        return BaseResponse.<T>builder()
                .status(status.value())
                .error(error)
                .build();
    }

    public static <T> BaseResponse<T> error(String error, String message) {
        return BaseResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(error)
                .message(message)
                .build();
    }

    // Not found
    public static <T> BaseResponse<T> notFound(String message) {
        return BaseResponse.<T>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(message)
                .build();
    }

    // Validation error
    public static <T> BaseResponse<T> validationError(String message) {
        return BaseResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(message)
                .build();
    }

    // Check success
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }
}
