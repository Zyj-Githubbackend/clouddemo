export const buildActivityImageUrl = (imageKey) => {
  if (!imageKey) return ''
  return `/api/activity/image?objectKey=${encodeURIComponent(imageKey)}`
}
