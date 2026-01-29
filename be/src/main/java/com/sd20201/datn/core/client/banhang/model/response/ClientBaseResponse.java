package com.sd20201.datn.core.client.banhang.model.response;

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
public class ClientBaseResponse<T> {

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private int status = HttpStatus.OK.value();

    private String message;
    private T data;

    private String error;
    private String path;

    // Success methods
    public static <T> ClientBaseResponse<T> success(String message) {
        return ClientBaseResponse.<T>builder()
                .message(message)
                .build();
    }

    public static <T> ClientBaseResponse<T> success(String message, T data) {
        return ClientBaseResponse.<T>builder()
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ClientBaseResponse<T> success(T data) {
        return ClientBaseResponse.<T>builder()
                .message("Success")
                .data(data)
                .build();
    }

    // Error methods
    public static <T> ClientBaseResponse<T> error(String error) {
        return ClientBaseResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(error)
                .build();
    }

    public static <T> ClientBaseResponse<T> error(String error, HttpStatus status) {
        return ClientBaseResponse.<T>builder()
                .status(status.value())
                .error(error)
                .build();
    }

    public static <T> ClientBaseResponse<T> error(String error, String message) {
        return ClientBaseResponse.<T>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(error)
                .message(message)
                .build();
    }

    // Not found
    public static <T> ClientBaseResponse<T> notFound(String message) {
        return ClientBaseResponse.<T>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(message)
                .build();
    }

    // Validation error
    public static <T> ClientBaseResponse<T> validationError(String message) {
        return ClientBaseResponse.<T>builder()
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
