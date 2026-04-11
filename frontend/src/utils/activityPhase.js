import dayjs from 'dayjs'

/**
 * 活动日程阶段展示：优先显示取消和完成状态，否则按当前时间与开始/结束时间判断。
 * @returns {{ text: string, type: 'success' | 'warning' | 'info' | 'danger' }}
 */
export function getActivityPhaseDisplay(activity) {
  if (!activity) {
    return { text: '--', type: 'info' }
  }
  if (activity.status === 'CANCELLED') {
    return { text: '活动已取消', type: 'danger' }
  }
  if (activity.status === 'COMPLETED') {
    return { text: '已结束', type: 'success' }
  }
  if (!activity.startTime || !activity.endTime) {
    return { text: '--', type: 'info' }
  }
  const now = dayjs()
  const start = dayjs(activity.startTime)
  const end = dayjs(activity.endTime)
  if (now.isBefore(start)) {
    return { text: '活动未开始', type: 'warning' }
  }
  if (now.isAfter(end)) {
    return { text: '活动已结束', type: 'info' }
  }
  return { text: '活动进行中', type: 'success' }
}
