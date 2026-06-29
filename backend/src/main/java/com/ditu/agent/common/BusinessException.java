package com.ditu.agent.common;

/**
 * 可预期的业务异常。
 *
 * <p>服务层用它表达“账号停用”“次数不足”“跨用户访问”等业务拒绝原因，统一异常处理器会转换为稳定错误码。</p>
 */
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
