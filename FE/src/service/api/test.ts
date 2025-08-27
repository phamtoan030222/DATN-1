import { blankInstance, request } from "../http";

export function fetchGet(params?: any) {
  return request.Get("/getAPI", { params });
}

export function fetchPost(data: any) {
  return request.Post("/postAPI", data);
}
export function fetchFormPost(data: any) {
  const methodInstance = request.Post("/postFormAPI", data);
  methodInstance.meta = {
    isFormPost: true,
  };
  return methodInstance;
}
export function fetchDelete() {
  return request.Delete("/deleteAPI");
}
export function fetchPut(data: any) {
  return request.Put("/putAPI", data);
}
export function withoutToken() {
  const methodInstance = request.Get("/getAPI");
  methodInstance.meta = {
    authRole: null,
  };
  return methodInstance;
}
export function dictData() {
  return request.Get("/getDictData", {
    transform(rawData, _headers) {
      const response = rawData as any;
      return {
        ...response,
        data: {
          ...response.data,
          gender: response.data.gender === 0 ? "男" : "女",
          status: `状态是${response.data.status}`,
        },
      };
    },
  });
}
export function getBlob(url: string) {
  const methodInstance = blankInstance.Get<Blob>(url);
  methodInstance.meta = {
    isBlob: true,
  };
  return methodInstance;
}

export function downloadFile(url: string) {
  const methodInstance = blankInstance.Get<Blob>(url);
  methodInstance.meta = {
    isBlob: true,
  };
  return methodInstance;
}
export function FailedRequest() {
  return request.Get("/serverError");
}

export function FailedResponse() {
  return request.Post("/businessError");
}
export function FailedResponseWithoutTip() {
  return request.Post("/businessErrorWithoutTip");
}
export function expiredTokenRequest() {
  return request.Get("/expiredToken");
}
export function refreshToken() {
  return request.Get("/updataToken");
}
