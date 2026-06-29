package com.ditu.agent.auth;

/**
 * 认证后的当前用户上下文。
 *
 * <p>服务层用 userId 做数据归属校验，用 roleCode 做管理端权限判断，避免从请求参数信任用户身份。</p>
 */
public record CurrentUser(Long id, String username, String displayName, String roleCode) {
}
