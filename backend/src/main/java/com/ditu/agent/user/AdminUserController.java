package com.ditu.agent.user;

import com.ditu.agent.auth.SecurityUtils;
import com.ditu.agent.common.ApiResponse;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.user.ModelConfigService.SaveModelConfigCommand;
import com.ditu.agent.user.UserDtos.ModelConfigDto;
import com.ditu.agent.user.UserDtos.QuotaLedgerDto;
import com.ditu.agent.user.UserDtos.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户、次数、套餐、模型链接接口。
 *
 * <p>所有敏感操作要求 ADMIN，并由服务层写 audit_log，普通用户无法通过接口参数越权管理其他账号。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
  private final UserService userService;
  private final QuotaService quotaService;
  private final ModelConfigService modelConfigService;

  public AdminUserController(UserService userService, QuotaService quotaService,
                             ModelConfigService modelConfigService) {
    this.userService = userService;
    this.quotaService = quotaService;
    this.modelConfigService = modelConfigService;
  }

  @GetMapping
  public ApiResponse<PageResponse<UserSummary>> users(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String planCode,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(userService.pageUsers(keyword, status, planCode, page, pageSize));
  }

  @PostMapping
  public ApiResponse<UserSummary> create(@Valid @RequestBody CreateUserRequest request) {
    var command = new UserService.CreateUserCommand(request.username(), request.password(), request.displayName(),
        request.phone(), request.email(), request.planCode(), request.quotaTotal());
    return ApiResponse.ok(userService.createUser(command, SecurityUtils.currentUser().id()));
  }

  @GetMapping("/{userId}")
  public ApiResponse<UserSummary> detail(@PathVariable Long userId) {
    return ApiResponse.ok(userService.detail(userId));
  }

  @PostMapping("/{userId}/disable")
  public ApiResponse<UserSummary> disable(@PathVariable Long userId) {
    return ApiResponse.ok(userService.disable(userId, SecurityUtils.currentUser().id()));
  }

  @PostMapping("/{userId}/enable")
  public ApiResponse<UserSummary> enable(@PathVariable Long userId) {
    return ApiResponse.ok(userService.enable(userId, SecurityUtils.currentUser().id()));
  }

  @PostMapping("/{userId}/quota-adjustments")
  public ApiResponse<UserService.QuotaAdjustmentResult> adjustQuota(@PathVariable Long userId,
                                                                    @Valid @RequestBody QuotaRequest request) {
    return ApiResponse.ok(userService.adjustQuota(userId, request.deltaCount(), request.reason(),
        SecurityUtils.currentUser().id()));
  }

  @GetMapping("/{userId}/quota-ledgers")
  public ApiResponse<PageResponse<QuotaLedgerDto>> quotaLedgers(@PathVariable Long userId,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(quotaService.pageLedger(userId, page, pageSize));
  }

  @PostMapping("/{userId}/plan")
  public ApiResponse<UserSummary> changePlan(@PathVariable Long userId, @Valid @RequestBody PlanRequest request) {
    return ApiResponse.ok(userService.changePlan(userId, request.planCode(), request.reason(),
        SecurityUtils.currentUser().id()));
  }

  @GetMapping("/{userId}/model-config")
  public ApiResponse<ModelConfigDto> modelConfig(@PathVariable Long userId) {
    return ApiResponse.ok(modelConfigService.get(userId));
  }

  @PutMapping("/{userId}/model-config")
  public ApiResponse<ModelConfigDto> saveModelConfig(@PathVariable Long userId,
                                                    @Valid @RequestBody SaveModelConfigRequest request) {
    var command = new SaveModelConfigCommand(request.configName(), request.providerCode(), request.baseUrl(),
        request.modelName(), request.authType(), request.apiKey(), request.enabled());
    return ApiResponse.ok(modelConfigService.save(userId, command, SecurityUtils.currentUser().id()));
  }

  @PostMapping("/{userId}/model-config/test")
  public ApiResponse<ModelConfigDto> testModelConfig(@PathVariable Long userId,
                                                    @RequestBody(required = false) TestModelRequest request) {
    return ApiResponse.ok(modelConfigService.test(userId, request == null ? "ping" : request.message(),
        SecurityUtils.currentUser().id()));
  }

  @PostMapping("/{userId}/model-config/disable")
  public ApiResponse<Void> disableModelConfig(@PathVariable Long userId) {
    modelConfigService.disable(userId, SecurityUtils.currentUser().id());
    return ApiResponse.ok(null);
  }

  public record CreateUserRequest(@NotBlank String username, @NotBlank String password,
                                  @NotBlank String displayName, String phone, String email,
                                  @NotBlank String planCode, int quotaTotal) {
  }

  public record QuotaRequest(int deltaCount, @NotBlank String reason) {
  }

  public record PlanRequest(@NotBlank String planCode, String reason) {
  }

  public record SaveModelConfigRequest(@NotBlank String configName, String providerCode, @NotBlank String baseUrl,
                                       @NotBlank String modelName, @NotBlank String authType, String apiKey,
                                       boolean enabled) {
  }

  public record TestModelRequest(String message) {
  }
}
