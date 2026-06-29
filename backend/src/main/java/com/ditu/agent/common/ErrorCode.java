package com.ditu.agent.common;

import org.springframework.http.HttpStatus;

/**
 * 滴兔智能体的稳定错误码集合。
 *
 * <p>错误码是前后端契约的一部分，不把数据库异常或 Java 异常名直接暴露给用户界面。</p>
 */
public enum ErrorCode {
  OK(HttpStatus.OK),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
  FORBIDDEN(HttpStatus.FORBIDDEN),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
  USER_DISABLED(HttpStatus.FORBIDDEN),
  QUOTA_NOT_ENOUGH(HttpStatus.CONFLICT),
  CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND),
  AGENT_RUN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),
  MODEL_CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND),
  MODEL_CONNECTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY),
  RAG_DOCUMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY),
  TICKET_NOT_FOUND(HttpStatus.NOT_FOUND),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

  ErrorCode(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
