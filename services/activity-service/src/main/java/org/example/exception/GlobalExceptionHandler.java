package org.example.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.common.exception.BusinessException;
import org.example.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn(
                "business exception method={} path={} code={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getCode(),
                e.getMessage()
        );
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error(
                "unexpected exception method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                e
        );
        return Result.error("System exception: " + e.getMessage());
    }
}
