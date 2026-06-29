// 共享格式化方法只处理展示语义，不承载权限判断；权限边界必须以后端 Token 和接口响应为准。
export function formatQuota(total: number, used: number) {
  return `${Math.max(total - used, 0)} / ${total}`
}

export function ticketStatusLabel(status: string) {
  const labels: Record<string, string> = {
    OPEN: '待处理',
    PENDING: '待补充',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    CLOSED: '已关闭'
  }
  return labels[status] ?? status
}

export function planTone(code: string) {
  return code === 'PLUS' ? 'success' : code === 'PRO' ? 'warning' : 'info'
}
