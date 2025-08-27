import { local } from "@/utils";
import { createAlova } from "alova";
import { createServerTokenAuthentication } from "alova/client";
import adapterFetch from "alova/fetch";
import VueHook from "alova/vue";
import type { VueHookType } from "alova/vue";
import { DEFAULT_ALOVA_OPTIONS, DEFAULT_BACKEND_OPTIONS } from "./config";
import {
  handleBusinessError,
  handleRefreshToken,
  handleResponseError,
  handleServiceResult,
} from "./handle";

const { onAuthRequired, onResponseRefreshToken } =
  createServerTokenAuthentication<VueHookType>({
    refreshTokenOnSuccess: {
      isExpired: async (response, method) => {
        const res = await response.clone().json();

        const isExpired = method.meta && method.meta.isExpired;
        return (response.status === 401 || res.code === 401) && !isExpired;
      },

      handler: async (_response, method) => {
        if (!method.meta) method.meta = { isExpired: true };
        else method.meta.isExpired = true;

        await handleRefreshToken();
      },
    },
    assignToken: (method) => {
      method.config.headers.Authorization = `Bearer ${local.get(
        "accessToken"
      )}`;
    },
  });

// docs path of alova.js https://alova.js.org/
export function createAlovaInstance(
  alovaConfig: Service.AlovaConfig,
  backendConfig?: Service.BackendConfig
) {
  const _backendConfig = { ...DEFAULT_BACKEND_OPTIONS, ...backendConfig };
  const _alovaConfig = { ...DEFAULT_ALOVA_OPTIONS, ...alovaConfig };

  return createAlova({
    statesHook: VueHook,
    requestAdapter: adapterFetch(),
    cacheFor: null,
    baseURL: _alovaConfig.baseURL,
    timeout: _alovaConfig.timeout,

    beforeRequest: onAuthRequired((method) => {
      if (method.meta?.isFormPost) {
        method.config.headers["Content-Type"] =
          "application/x-www-form-urlencoded";
        method.data = new URLSearchParams(
          method.data as URLSearchParams
        ).toString();
      }
      alovaConfig.beforeRequest?.(method);
    }),
    responded: onResponseRefreshToken({
      onSuccess: async (response, method) => {
        const { status } = response;

        if (status === 200) {
          if (method.meta?.isBlob) return response.blob();

          const apiData = await response.json();
          if (apiData[_backendConfig.codeKey] === _backendConfig.successCode)
            return handleServiceResult(apiData);

          const errorResult = handleBusinessError(apiData, _backendConfig);
          return handleServiceResult(errorResult, false);
        }
        const errorResult = handleResponseError(response);
        return handleServiceResult(errorResult, false);
      },
      onError: (error, method) => {
        const tip = `[${method.type}] - [${method.url}] - ${error.message}`;
        window.$message?.warning(tip);
      },

      onComplete: async (_method) => {},
    }),
  });
}
