// 共享格式化方法只处理展示语义，不承载权限判断；权限边界必须以后端 Token 和接口响应为准。
export const DITU_BRAND = {
  name: '滴兔 IP',
  appName: '滴兔智能体',
  adminName: '滴兔管理端',
  slogan: '一站式知识产权全链条提供商',
  assistantLabel: '滴兔'
} as const

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
