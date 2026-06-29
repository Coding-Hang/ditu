package com.ditu.agent.ticket;

import com.ditu.agent.auth.SecurityUtils;
import com.ditu.agent.common.ApiResponse;
import com.ditu.agent.common.PageResponse;
import com.ditu.agent.ticket.TicketDtos.CustomerServiceProfileDto;
import com.ditu.agent.ticket.TicketDtos.TicketDetailDto;
import com.ditu.agent.ticket.TicketDtos.TicketDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端专属客服与工单接口。
 *
 * <p>工单创建、查看和回复全部绑定当前登录用户，前端不能传 userId 冒充他人。</p>
 */
@RestController
public class TicketController {
  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @GetMapping("/api/v1/customer-services")
  public ApiResponse<List<CustomerServiceProfileDto>> profiles() {
    return ApiResponse.ok(ticketService.profiles());
  }

  @PostMapping("/api/v1/tickets")
  public ApiResponse<TicketDetailDto> create(@Valid @RequestBody CreateTicketRequest request) {
    return ApiResponse.ok(ticketService.create(SecurityUtils.currentUser().id(), request.serviceProfileId(),
        request.conversationId(), request.title(), request.content()));
  }

  @GetMapping("/api/v1/tickets")
  public ApiResponse<PageResponse<TicketDto>> tickets(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.ok(ticketService.pageUserTickets(SecurityUtils.currentUser().id(), page, pageSize));
  }

  @GetMapping("/api/v1/tickets/{ticketId}")
  public ApiResponse<TicketDetailDto> detail(@PathVariable Long ticketId) {
    return ApiResponse.ok(ticketService.detailForUser(SecurityUtils.currentUser().id(), ticketId));
  }

  @PostMapping("/api/v1/tickets/{ticketId}/messages")
  public ApiResponse<TicketDetailDto> reply(@PathVariable Long ticketId, @Valid @RequestBody ReplyRequest request) {
    return ApiResponse.ok(ticketService.userReply(SecurityUtils.currentUser().id(), ticketId, request.content()));
  }

  public record CreateTicketRequest(Long serviceProfileId, Long conversationId, @NotBlank String title,
                                    @NotBlank String content) {
  }

  public record ReplyRequest(@NotBlank String content) {
  }
}
