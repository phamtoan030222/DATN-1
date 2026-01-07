import { API_AUTH_LOGIN } from "@/constants/url"
import request from "@/service/request"
import { AxiosResponse } from "axios"

export const postLogin = async (username: string, password: string, screen?: string, redirectUri?: string) => {
    const formData = new FormData()

    formData.append("username", username)
    formData.append("password", password)
    if (screen) {
        formData.append("screen", screen)
    }

    const res = (await request({
        url: `${API_AUTH_LOGIN}`,
        method: "POST",
        data: formData
    }) ) as AxiosResponse<{state: string}>

    return res.data
}