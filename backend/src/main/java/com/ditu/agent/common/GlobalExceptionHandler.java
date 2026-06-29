package com.ditu.agent.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * HTTP 异常边界。
 *
 * <p>这里集中把鉴权、参数校验和业务错误转换为文档中的统一响应格式，避免敏感异常堆栈进入三端页面。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
    return ResponseEntity.status(ex.errorCode().status()).body(ApiResponse.error(ex.errorCode(), ex.getMessage()));
  }

  @ExceptionHandler({BadCredentialsException.class})
  public ResponseEntity<ApiResponse<Void>> handleUnauthorized(Exception ex) {
    return ResponseEntity.status(ErrorCode.UNAUTHORIZED.status())
        .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleForbidden(AccessDeniedException ex) {
    return ResponseEntity.status(ErrorCode.FORBIDDEN.status())
        .body(ApiResponse.error(ErrorCode.FORBIDDEN, "无权限访问该资源"));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
  public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
    return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
        .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, "请求参数不符合业务规则"));
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateKeyException ex) {
    return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status())
        .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, "业务数据已存在，请检查唯一字段"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
        .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统处理失败，请稍后重试"));
  }
}
