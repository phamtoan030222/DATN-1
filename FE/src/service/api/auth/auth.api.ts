import { API_AUTH_LOGIN, API_AUTH_REGISTER_CUSTOMER } from "@/constants/url"
import request from "@/service/request"
import { AxiosResponse } from "axios"

export type RegisterRequest = {
    fullname: string;
    birthday?: number; // or string, depending on how you handle dates
    email: string;
    phone: string;
    username: string;
    password: string;
    repassword: string;
}

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
    })) as AxiosResponse<{ state: string }>

    return res.data
}

export const postRegister = async (data: RegisterRequest) => {
    const res = (await request({
        url: `${API_AUTH_REGISTER_CUSTOMER}`,
        method: "POST",
        data,
    })) as AxiosResponse<{ state: string }>

    return res.data
}