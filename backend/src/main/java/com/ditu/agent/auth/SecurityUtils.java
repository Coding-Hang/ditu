package com.ditu.agent.auth;

import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前认证用户读取工具。
 *
 * <p>Controller 不接收 userId 作为用户端资源归属依据，统一从 SecurityContext 获取可信身份。</p>
 */
public final class SecurityUtils {
  private SecurityUtils() {
  }

  public static CurrentUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或 Token 已失效");
    }
    return currentUser;
  }
}
