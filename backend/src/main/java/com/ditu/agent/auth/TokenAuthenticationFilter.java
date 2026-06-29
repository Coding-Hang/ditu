package com.ditu.agent.auth;

import com.ditu.agent.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer Token 认证过滤器。
 *
 * <p>每次请求都会重新读取用户状态，停用账号即使持有未过期 Token 也不能继续访问受保护接口。</p>
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
  private final TokenService tokenService;
  private final UserRepository userRepository;

  public TokenAuthenticationFilter(TokenService tokenService, UserRepository userRepository) {
    this.tokenService = tokenService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        var parsed = tokenService.parse(header.substring(7), "ACCESS");
        userRepository.findById(parsed.userId()).filter(user -> "ACTIVE".equals(user.status())).ifPresent(user -> {
          CurrentUser currentUser = new CurrentUser(user.id(), user.username(), user.displayName(), user.roleCode());
          var authentication = new UsernamePasswordAuthenticationToken(currentUser, null,
              List.of(new SimpleGrantedAuthority("ROLE_" + user.roleCode())));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        });
      } catch (Exception ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }
}
