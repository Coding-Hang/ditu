package com.ditu.agent.auth;

import com.ditu.agent.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * <p>H5、小程序和管理端共用同一登录入口，角色差异由后续接口授权控制。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<AuthService.LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request.username(), request.password()));
  }

  @PostMapping("/refresh")
  public ApiResponse<AuthService.LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ApiResponse.ok(authService.refresh(request.refreshToken()));
  }

  @GetMapping("/me")
  public ApiResponse<?> me() {
    return ApiResponse.ok(authService.me(SecurityUtils.currentUser().id()));
  }

  public record LoginRequest(@NotBlank String username, @NotBlank String password) {
  }

  public record RefreshRequest(@NotBlank String refreshToken) {
  }
}
