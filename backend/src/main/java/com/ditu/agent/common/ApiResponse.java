package com.ditu.agent.common;

import java.util.UUID;

/**
 * 统一 API 响应包。
 *
 * <p>用户端、管理端和小程序共享同一错误码契约，前端可据此处理 Token 失效、次数不足、越权等业务状态。</p>
 */
public record ApiResponse<T>(boolean success, String code, String message, T data, String requestId) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, ErrorCode.OK.name(), "ok", data, UUID.randomUUID().toString());
  }

  public static <T> ApiResponse<T> error(ErrorCode code, String message) {
    return new ApiResponse<>(false, code.name(), message, null, UUID.randomUUID().toString());
  }
}
