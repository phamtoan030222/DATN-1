import { ACCESS_TOKEN_STORAGE_KEY, REFRESH_TOKEN_STORAGE_KEY, USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { PREFIX_API_AUTH, VITE_BASE_URL_SERVER } from '@/constants/url'
import { localStorageAction } from '@/utils/storage.helper'
import axios, { AxiosResponse } from 'axios'
import { DefaultResponse } from './api.common'
import { getUserInformation } from '@/utils/token.helper'

const request = axios.create({
  baseURL: VITE_BASE_URL_SERVER,
})

request.interceptors.request.use((config) => {
  const accessToken = localStorageAction.get(ACCESS_TOKEN_STORAGE_KEY)
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    return response
  },
  async (error) => {
    const originalRequest = error.config

    if (
      error.response &&
      error.response.status === 401 &&
      !originalRequest._retry &&
      window.location.pathname !== "/login" &&
      window.location.pathname !== "/login-admin"
    ) {
      originalRequest._retry = true

      const refreshToken = localStorageAction.get(REFRESH_TOKEN_STORAGE_KEY)
      if (refreshToken) {
        try {
          const response = (await axios.post(`${PREFIX_API_AUTH}/refresh`, {
            refreshToken
          })) as AxiosResponse<DefaultResponse<{ accessToken: string; refreshToken: string }>>
          const newAccessToken = response.data.data.accessToken
          const newRefreshToken = response.data.data.refreshToken
          localStorageAction.set(ACCESS_TOKEN_STORAGE_KEY, newAccessToken)
          localStorageAction.set(REFRESH_TOKEN_STORAGE_KEY, newRefreshToken)
          localStorageAction.set(USER_INFO_STORAGE_KEY, getUserInformation(newAccessToken))
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
          return request(originalRequest)
        } catch (refreshError) {
          console.log('🚀 ~ refreshError:', refreshError)
        }
      }

      localStorageAction.remove(ACCESS_TOKEN_STORAGE_KEY)
      localStorageAction.remove(REFRESH_TOKEN_STORAGE_KEY)
      localStorageAction.remove(USER_INFO_STORAGE_KEY)
      window.location.href = "/error/401"
    } else if (
      error.response &&
      error.response.status === 403 &&
      window.location.pathname !== "/login" &&
      window.location.pathname !== "/login-admin"
    ) {
      window.location.href = "/error/403"
      console.log("lỗi ",error.response)
    }

    return Promise.reject(error)
  }
)

export default request
