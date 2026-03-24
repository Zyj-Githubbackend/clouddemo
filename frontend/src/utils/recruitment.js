import dayjs from 'dayjs'

/**
 * 根据报名开始/截止时间与活动状态，得到列表展示用招募状态（与后端 vol_activity.status 区分）
 * @returns {{ text: string, type: 'success' | 'warning' | 'info' }}
 */
export function getRecruitmentDisplay(activity) {
  if (!activity) return { text: '—', type: 'info' }
  if (activity.status === 'COMPLETED' || activity.status === 'CANCELLED') {
    return { text: '已结束', type: 'info' }
  }
  const now = dayjs()
  if (activity.registrationStartTime && now.isBefore(dayjs(activity.registrationStartTime))) {
    return { text: '未开始', type: 'warning' }
  }
  if (activity.registrationDeadline && now.isAfter(dayjs(activity.registrationDeadline))) {
    return { text: '已结束', type: 'info' }
  }
  return { text: '招募中', type: 'success' }
}
