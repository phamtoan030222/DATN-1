// // src/service/brand.ts
// import { request, blankInstance } from "@/service/http";

// // --- Interfaces ---
// export namespace AppBrand {
//   export interface RowBrand {
//     id: number;
//     code: string;
//     name: string;
//     status: "active" | "inactive";
//   }

//   export interface BrandPayload {
//     code: string;
//     name: string;
//     status: "active" | "inactive";
//   }
// }

// // --- API Functions ---

// /**
//  * Lấy tất cả thương hiệu
//  */
// export async function fetchAllBrands(): Promise<{ data: AppBrand.RowBrand[] }> {
//   const res = await request.get("/brands");
//   return res.data;
// }

// /**
//  * Thêm mới thương hiệu
//  * @param payload
//  */
// export async function createBrand(
//   payload: Partial<AppBrand.BrandPayload>
// ): Promise<AppBrand.RowBrand> {
//   const res = await request.post("/brands", payload);
//   return res.data;
// }

// /**
//  * Cập nhật thương hiệu
//  * @param id
//  * @param payload
//  */
// export async function updateBrand(
//   id: number,
//   payload: Partial<AppBrand.BrandPayload>
// ): Promise<AppBrand.RowBrand> {
//   const res = await request.put(`/brands/${id}`, payload);
//   return res.data;
// }

// /**
//  * Xóa thương hiệu theo id
//  * @param id
//  */
// export async function deleteBrand(id: number): Promise<void> {
//   await request.delete(`/brands/${id}`);
// }

// /**
//  * Cập nhật trạng thái thương hiệu
//  * @param id
//  * @param status 'active' | 'inactive'
//  */
// export async function updateBrandStatus(
//   id: number,
//   status: "active" | "inactive"
// ): Promise<void> {
//   await request.patch(`/brands/${id}/status`, { status });
// }
