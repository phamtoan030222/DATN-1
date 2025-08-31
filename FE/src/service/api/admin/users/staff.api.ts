import { API_ADMIN_STAFF } from "@/constants/url";
import { AxiosResponse } from "axios";
import {
  DefaultResponse,
  PaginationParams,
  ResponseList,
} from "@/typings/api/api.common";
import request from "@/service/request";

export interface ParamGetStaff extends PaginationParams {
  code?: string;
  fullName?: string;
  role?: string;
  email?: string;
  phone?: string;
  status?: string;
}

export type StaffResponse = ResponseList & {
  id: string;
  fullName: string;
  code?: string;
  status: string; // "ACTIVE", "INACTIVE"
  role: string; // "QUAN_LY", ...
  birthday?: number; // timestamp
  citizenIdentifyCard?: string;
  hometown: string;
  provinceCode: string;
  districtCode: string;
  communeCode: string;
  avatarUrl: string;
  phone: string;
  gender: boolean; // backend trả true/false
};

export type CreateStaffRequest = {
  fullName: string;
  role: string;
  birthday?: number;
  citizenIdentifyCard?: string;
  hometown: string;
  avatarUrl: string;
  phone: string;
  email: string;
  gender: boolean; // gửi true/false
  provinceCode: string;
  districtCode: string;
  communeCode: string;
};

export const getAllStaff = async (params: ParamGetStaff) => {
  const res = (await request({
    url: API_ADMIN_STAFF,
    method: "GET",
    params,
  })) as AxiosResponse<
    DefaultResponse<{
      data: StaffResponse[];
      totalPages: number;
      currentPage: number;
      totalElements: number;
    }>
  >;

  return {
    items: res.data.data.data || [],
    totalItems: res.data.data.totalElements || 0,
    totalPages: res.data.data.totalPages || 0,
    currentPage: res.data.data.currentPage || 1,
  };
};

export const createStaff = async (data: CreateStaffRequest) => {
  const res = await request({
    url: `${API_ADMIN_STAFF}/add`,
    method: "POST",
    data,
  });
  return res.data.data;
};

// Cập nhật trạng thái nhân viên (ACTIVE / INACTIVE)
export const updateStaffStatus = async (
  id: string,
  status: "ACTIVE" | "INACTIVE"
) => {
  const res = await request({
    url: `${API_ADMIN_STAFF}/${id}/status`,
    method: "PATCH",
    data: { status },
  });
  return res.data.data;
};

// Xóa 1 nhân viên
export const deleteStaff = async (id: string) => {
  const res = await request({
    url: `${API_ADMIN_STAFF}/${id}`,
    method: "DELETE",
  });
  return res.data.data;
};

// Xóa nhiều nhân viên
export const deleteManyStaff = async (ids: string[]) => {
  const res = await request({
    url: `${API_ADMIN_STAFF}/delete-many`,
    method: "POST", // hoặc DELETE tuỳ backend định nghĩa
    data: { ids },
  });
  return res.data.data;
};
