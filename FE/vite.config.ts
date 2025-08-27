import { resolve } from "node:path";
import { defineConfig, loadEnv } from "vite";
import { createVitePlugins } from "./build/plugins";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, "") as ImportMetaEnv;

  return {
    base: env.VITE_BASE_URL,
    plugins: createVitePlugins(env),
    resolve: {
      alias: {
        "@": resolve(__dirname, "src"),
      },
    },
    server: {
      host: "0.0.0.0",
    },
    build: {
      target: "esnext",
      reportCompressedSize: false,
    },
    optimizeDeps: {
      include: ["echarts", "md-editor-v3", "quill"],
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: "modern",
        },
      },
    },
  };
});
