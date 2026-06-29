package com.ditu.agent.user;

import com.ditu.agent.common.ApiResponse;
import com.ditu.agent.user.UserDtos.PlanDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 套餐接口。
 *
 * <p>用户端和管理端都读取同一份套餐数据，确保基础、Pro、Plus 权益展示一致。</p>
 */
@RestController
public class PlanController {
  private final PlanRepository planRepository;

  public PlanController(PlanRepository planRepository) {
    this.planRepository = planRepository;
  }

  @GetMapping("/api/v1/plans")
  public ApiResponse<List<PlanDto>> plans() {
    return ApiResponse.ok(planRepository.listEnabled());
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/api/v1/admin/plans")
  public ApiResponse<List<PlanDto>> adminPlans() {
    return ApiResponse.ok(planRepository.listEnabled());
  }
}
