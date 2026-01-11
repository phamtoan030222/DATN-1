package com.sd20201.datn.infrastructure.exception;

import com.sd20201.datn.core.common.base.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseObject<?>> handleBusinessException(
            BusinessException ex) {

        return ResponseEntity
                .badRequest()
                .body(ResponseObject.errorForward(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST
                ));
    }
}
