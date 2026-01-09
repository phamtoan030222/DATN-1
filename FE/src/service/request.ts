import { ACCESS_TOKEN_STORAGE_KEY, REFRESH_TOKEN_STORAGE_KEY, USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { PREFIX_API_AUTH, VITE_BASE_URL_SERVER } from '@/constants/url'
import { localStorageAction } from '@/utils/storage.helper'
import { getUserInformation } from '@/utils/token.helper'
import axios from 'axios'

const request = axios.create({
  baseURL: VITE_BASE_URL_SERVER,
})

let isRefreshing = false

let refreshPromise: Promise<string> | null = null

type FailedRequest = {
  resolve: (token: string) => void
  reject: (error: any) => void
}

let failedQueue: FailedRequest[] = []

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token!)
    }
  })

  failedQueue = []
}

const refreshTokenRequest = async (): Promise<string> => {
  const refreshToken = localStorageAction.get(REFRESH_TOKEN_STORAGE_KEY)

  const userInfo = localStorageAction.get(USER_INFO_STORAGE_KEY)

  const response = await axios.post(
    `${PREFIX_API_AUTH}/refresh?screen=${userInfo.roleScreen || 'CUSTOMER'}`,
    { refreshToken }
  )

  const newAccessToken = response.data.data.accessToken
  const newRefreshToken = response.data.data.refreshToken

  localStorageAction.set(ACCESS_TOKEN_STORAGE_KEY, newAccessToken)
  localStorageAction.set(REFRESH_TOKEN_STORAGE_KEY, newRefreshToken)
  localStorageAction.set(USER_INFO_STORAGE_KEY, getUserInformation(newAccessToken))

  return newAccessToken
}


request.interceptors.request.use((config) => {
  const accessToken = localStorageAction.get(ACCESS_TOKEN_STORAGE_KEY)
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// request.interceptors.response.use(
//   (response) => {
//     return response
//   },
//   async (error) => {
//     const originalRequest = error.config

//     if (
//       error.response &&
//       error.response.status === 401 &&
//       !originalRequest._retry &&
//       window.location.pathname !== "/login" &&
//       window.location.pathname !== "/login-admin"
//     ) {
//       originalRequest._retry = true

//       const refreshToken = localStorageAction.get(REFRESH_TOKEN_STORAGE_KEY)
//       if (refreshToken) {
//         try {
//           const response = (await axios.post(`${PREFIX_API_AUTH}/refresh`, {
//             refreshToken
//           })) as AxiosResponse<DefaultResponse<{ accessToken: string; refreshToken: string }>>
//           const newAccessToken = response.data.data.accessToken
//           const newRefreshToken = response.data.data.refreshToken
//           localStorageAction.set(ACCESS_TOKEN_STORAGE_KEY, newAccessToken)
//           localStorageAction.set(REFRESH_TOKEN_STORAGE_KEY, newRefreshToken)
//           localStorageAction.set(USER_INFO_STORAGE_KEY, getUserInformation(newAccessToken))
//           originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
//           return request(originalRequest)
//         } catch (refreshError) {
//           console.log('🚀 ~ refreshError:', refreshError)
//         }
//       }

//       localStorageAction.remove(ACCESS_TOKEN_STORAGE_KEY)
//       localStorageAction.remove(REFRESH_TOKEN_STORAGE_KEY)
//       localStorageAction.remove(USER_INFO_STORAGE_KEY)
//       window.location.href = "/error/401"
//     } else if (
//       error.response &&
//       error.response.status === 403 &&
//       window.location.pathname !== "/login" &&
//       window.location.pathname !== "/login-admin"
//     ) {
//       window.location.href = "/error/403"
//       console.log("lỗi ",error.response)
//     }

//     return Promise.reject(error)
//   }
// )

request.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      window.location.pathname !== "/login" &&
      window.location.pathname !== "/login-admin"
    ) {
      originalRequest._retry = true

      if (!isRefreshing) {
        isRefreshing = true

        refreshPromise = refreshTokenRequest()
          .then(newToken => {
            processQueue(null, newToken)
            return newToken
          })
          .catch(err => {
            processQueue(err, null)
            throw err
          })
          .finally(() => {
            isRefreshing = false
            refreshPromise = null
          })
      }

      return new Promise((resolve, reject) => {
        failedQueue.push({
          resolve: (token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          },
          reject: err => {
            reject(err)
          }
        })
      })
    }

    // 403
    if (
      error.response?.status === 403 &&
      window.location.pathname !== "/login" &&
      window.location.pathname !== "/login-admin"
    ) {
      window.location.href = "/error/403"
    }

    return Promise.reject(error)
  }
)


export default request
