import axios from 'axios'

export function getWithPagination<T>(
  baseUrl: string,
  params: {
    page?: number
    size?: number
    sort?: string
    [key: string]: any
  }
) {
  return axios.get<T>(baseUrl, {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 10,
      sort: params.sort ?? undefined,
      ...params,
    },
  })
}
