export const buildActivityImageUrl = (imageKey) => {
  if (!imageKey) return ''
  return `/api/activity/image?objectKey=${encodeURIComponent(imageKey)}`
}

export const buildActivityImageUrls = (imageKeys = []) => {
  if (!Array.isArray(imageKeys)) return []
  return imageKeys.filter(Boolean).map(buildActivityImageUrl)
}
