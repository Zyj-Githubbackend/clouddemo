export const buildAnnouncementAttachmentUrl = (attachmentKey, fileName = '') => {
  if (!attachmentKey) return ''
  const params = new URLSearchParams({ objectKey: attachmentKey })
  if (fileName) params.set('fileName', fileName)
  return `/api/announcement/attachment?${params.toString()}`
}
