export const buildAnnouncementImageUrl = (imageKey) => {
  if (!imageKey) return ''
  return `/api/announcement/image?objectKey=${encodeURIComponent(imageKey)}`
}

export const buildAnnouncementImageUrls = (imageKeys = []) => {
  if (!Array.isArray(imageKeys)) return []
  return imageKeys.filter(Boolean).map(buildAnnouncementImageUrl)
}
