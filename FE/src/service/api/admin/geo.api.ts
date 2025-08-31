import { AxiosResponse } from "axios";
import { API_ADMIN_GEO } from "@/constants/url";
import request from "@/service/request";
import { DefaultResponse } from "@/typings/api/api.common";

// ---- Province ----
export interface Province {
  code: number;
  name: string;
  division_type: string;
  codename: string;
  phone_code?: number;
  districts?: District[]; // Có thể kèm danh sách huyện
}

// ---- District ----
export interface District {
  code: number;
  name: string;
  division_type?: string;
  codename?: string;
  province_code?: number;
  wards?: Ward[];
}

// ---- Ward ----
export interface Ward {
  code: number;
  name: string;
  division_type?: string;
  codename?: string;
  district_code?: number;
}

// ---- API ----
export const getProvinces = async (): Promise<Province[]> => {
  const res = (await request({
    url: `${API_ADMIN_GEO}/provinces`,
    method: "GET",
  })) as AxiosResponse<Province[]>;
  return res.data;
};

export const getDistricts = async (
  provinceCode: string
): Promise<District[]> => {
  const res = (await request({
    url: `${API_ADMIN_GEO}/districts/${provinceCode}`,
    method: "GET",
  })) as AxiosResponse<District[]>;
  return res.data;
};

export const getCommunes = async (districtCode: string): Promise<Ward[]> => {
  const res = (await request({
    url: `${API_ADMIN_GEO}/communes/${districtCode}`,
    method: "GET",
  })) as AxiosResponse<Ward[]>;
  return res.data;
};
