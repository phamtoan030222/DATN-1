import { request } from "../http";

export function fetchAllRoutes() {
  return request.Get<Service.ResponseResult<AppRoute.RowRoute[]>>(
    "/getUserRoutes"
  );
}

export function fetchUserPage() {
  return request.Get<Service.ResponseResult<Entity.User[]>>("/userPage");
}
export function fetchRoleList() {
  return request.Get<Service.ResponseResult<Entity.Role[]>>("/role/list");
}

export function fetchDictList(code?: string) {
  const params = { code };
  return request.Get<Service.ResponseResult<Entity.Dict[]>>("/dict/list", {
    params,
  });
}
