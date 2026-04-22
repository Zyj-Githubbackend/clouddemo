const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

const decodeBase64Url = (value) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=')
  return atob(padded)
}

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY) || ''

export const getStoredUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem(USER_INFO_KEY) || '{}')
  } catch {
    return {}
  }
}

export const parseJwtPayload = (token) => {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    return JSON.parse(decodeBase64Url(payload))
  } catch {
    return null
  }
}

export const isTokenExpired = (token = getStoredToken()) => {
  if (!token) return true
  const payload = parseJwtPayload(token)
  if (!payload?.exp) return true
  return payload.exp * 1000 <= Date.now()
}

export const clearAuthStorage = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

export const getValidToken = () => {
  const token = getStoredToken()
  if (!token) return ''
  if (isTokenExpired(token)) {
    clearAuthStorage()
    return ''
  }
  return token
}
