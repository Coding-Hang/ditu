package com.ditu.agent.auth;

import com.ditu.agent.common.BusinessException;
import com.ditu.agent.common.ErrorCode;
import com.ditu.agent.infra.DituProperties;
import com.ditu.agent.user.UserDtos.UserSummary;
import com.ditu.agent.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录与当前用户服务。
 *
 * <p>密码校验、停用账号拦截和 Token 签发集中在这里，三端登录行为保持一致。</p>
 */
@Service
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final DituProperties properties;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                     DituProperties properties) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.properties = properties;
  }

  @Transactional
  public LoginResponse login(String username, String password) {
    var user = userRepository.findByUsername(username)
        .orElseThrow(() -> new BadCredentialsException("bad credentials"));
    if (!passwordEncoder.matches(password, user.passwordHash())) {
      throw new BadCredentialsException("bad credentials");
    }
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.USER_DISABLED, "账号已停用");
    }
    userRepository.markLogin(user.id());
    return new LoginResponse(tokenService.issueAccessToken(user.id(), user.roleCode()),
        tokenService.issueRefreshToken(user.id(), user.roleCode()), properties.auth().accessTokenSeconds(),
        UserSummary.from(user));
  }

  public LoginResponse refresh(String refreshToken) {
    var parsed = tokenService.parse(refreshToken, "REFRESH");
    var user = userRepository.findById(parsed.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Token 对应用户不存在"));
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.USER_DISABLED, "账号已停用");
    }
    return new LoginResponse(tokenService.issueAccessToken(user.id(), user.roleCode()),
        tokenService.issueRefreshToken(user.id(), user.roleCode()), properties.auth().accessTokenSeconds(),
        UserSummary.from(user));
  }

  public UserSummary me(Long userId) {
    return userRepository.findById(userId).map(UserSummary::from)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在"));
  }

  public record LoginResponse(String accessToken, String refreshToken, long expiresIn, UserSummary user) {
  }
}
