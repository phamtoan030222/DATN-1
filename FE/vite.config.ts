import { resolve } from "node:path";
import { defineConfig, loadEnv } from "vite";
import { createVitePlugins } from "./build/plugins";

export default defineConfig(({ mode }) => {
  // load env theo mode, prefix = "" để lấy tất cả biến
  const env = loadEnv(mode, process.cwd(), "");

  console.log("Loaded env:", env.VITE_BASE_URL); // kiểm tra

  return {
    base: env.VITE_BASE_URL,
    define: {
      global: "window",
    },
    plugins: createVitePlugins(env),
    resolve: {
      alias: { "@": resolve(__dirname, "src") },
    },
    server: {
      host: "0.0.0.0",
      port: 9980,
      proxy: {
        "/api": {
          target: env.VITE_BASE_URL, // dùng env ở đây
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ""),
        },
      },
    },
    build: { target: "esnext", reportCompressedSize: false },
    optimizeDeps: { include: ["echarts", "md-editor-v3", "quill"] },
    css: { preprocessorOptions: { scss: { api: "modern" } } },
  };
});
