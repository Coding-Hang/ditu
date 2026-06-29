package com.ditu.agent.ticket;

import com.ditu.agent.auth.SecurityUtils;
import com.ditu.agent.common.ApiResponse;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.ticket.TicketDtos.TicketDetailDto;
import com.ditu.agent.ticket.TicketDtos.TicketDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端工单接口。
 *
 * <p>ADMIN 和 CS_MANAGER 可以处理工单，每次分配、回复、状态变化由 TicketService 写入 audit_log。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/tickets")
@PreAuthorize("hasAnyRole('ADMIN','CS_MANAGER')")
public class AdminTicketController {
  private final TicketService ticketService;

  public AdminTicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @GetMapping
  public ApiResponse<PageResponse<TicketDto>> tickets(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String serviceType,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(ticketService.pageAdminTickets(status, serviceType, keyword, page, pageSize));
  }

  @GetMapping("/{ticketId}")
  public ApiResponse<TicketDetailDto> detail(@PathVariable Long ticketId) {
    return ApiResponse.ok(ticketService.detailForAdmin(ticketId));
  }

  @PostMapping("/{ticketId}/assign")
  public ApiResponse<TicketDetailDto> assign(@PathVariable Long ticketId, @Valid @RequestBody AssignRequest request) {
    return ApiResponse.ok(ticketService.assign(ticketId, request.assignedTo(), SecurityUtils.currentUser().id()));
  }

  @PostMapping("/{ticketId}/messages")
  public ApiResponse<TicketDetailDto> reply(@PathVariable Long ticketId, @Valid @RequestBody ReplyRequest request) {
    var user = SecurityUtils.currentUser();
    return ApiResponse.ok(ticketService.adminReply(ticketId, user.id(), user.roleCode(), request.content()));
  }

  @PostMapping("/{ticketId}/status")
  public ApiResponse<TicketDetailDto> status(@PathVariable Long ticketId, @Valid @RequestBody StatusRequest request) {
    return ApiResponse.ok(ticketService.changeStatus(ticketId, request.status(), request.reason(),
        SecurityUtils.currentUser().id()));
  }

  public record AssignRequest(Long assignedTo) {
  }

  public record ReplyRequest(@NotBlank String content) {
  }

  public record StatusRequest(@NotBlank String status, String reason) {
  }
}
