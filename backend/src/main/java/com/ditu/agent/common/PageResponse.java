package com.ditu.agent.common;

import java.util.List;

/**
 * 与接口契约一致的分页响应。
 *
 * <p>所有列表接口都显式返回当前页码和总量，管理端筛选页与用户历史页可以保持一致的分页交互。</p>
 */
public record PageResponse<T>(List<T> records, int page, int pageSize, long total) {
}
