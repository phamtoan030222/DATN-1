/// <reference path="../global.d.ts"/>

namespace Entity {
  type RoleType = "QUAN_LY" | "NHAN_VIEN" | "KHACH_HANG";

  interface Role {
    id?: number;
    role?: RoleType;
  }
}
