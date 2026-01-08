<script setup lang="ts">
import { useAppStore } from "@/store";
import Chart from "./components/chart.vue";
import Chart3 from "./components/chart3.vue";
import { LogoUsd } from '@vicons/ionicons5'
import { CubeOutline } from '@vicons/ionicons5'
import { PeopleOutline } from '@vicons/ionicons5'

import { 
  CartOutline, 
  TimeOutline, 
  SettingsOutline, 
  CheckmarkCircleOutline, 
  CloseCircleOutline 
} from '@vicons/ionicons5'
const appStore = useAppStore();

const tableData = [
  {
    id: 1,
    stt: "1",
    name: "Dell imposen 1",
    brand: "Dell",
    soLuong: "3",
  },
  {
    id: 2,
    stt: "2",
    name: "Pegasus",
    brand: "AS",
    soLuong: "2",
  },
];

const tableData2 = [
  {
    id: 1,
    stt: "1",
    name: "Dell imposen 1",
    brand: "Dell",
    soLuong: "14",
  },
   {
    id: 2,
    stt: "2",
    name: "Asus",
    brand: "Asus",
    soLuong: "6",
  },
   {
    id: 3,
    stt: "3",
    name: "Lenovo",
    brand: "AS",
    soLuong: "6",
  },
];
</script>

<template>
  <n-grid
    :x-gap="16"
    :y-gap="16"
    :cols="12"
    item-responsive
    responsive="screen"
  >
   <n-gi span="6 m:3">
    <n-card :bordered="false" content-style="padding: 16px;">
      <div style="display: flex; align-items: center; margin-bottom: 16px;">
        <n-avatar
          round
          size="medium"
          color="#e7fcf3"
          style="color: #18a058; margin-right: 12px;"
        >
          <n-icon size="20">
            <logo-usd />
          </n-icon>
        </n-avatar>
        <div>
          <div style="font-weight: 700; font-size: 16px; line-height: 1.2;">
            Doanh Thu
          </div>
          <div style="font-size: 12px; color: #9ca3af;">
            Tổng quan doanh thu
          </div>
        </div>
      </div>

      <div style="text-align: center; margin-bottom: 24px;">
        <div style="color: #18a058; font-size: 28px; font-weight: 800; line-height: 1.2;">
          <n-number-animation
            :from="0"
            :to="600000"
            show-separator
          />
          <span style="font-size: 20px; margin-left: 4px">₫</span>
        </div>
        <div style="font-size: 12px; color: #6b7280; margin: 4px 0 8px 0;">
          Doanh thu tháng 1
        </div>
        <!-- <n-tag color="{ color: '#0ea5e9', textColor: '#fff' }" round size="small" style="background-color: #0ea5e9; color: white; border: none; font-weight: 600;">
          -0% <span style="font-weight: normal; margin-left: 4px;">so với tháng trước</span>
        </n-tag>  -->
      </div>

      <n-grid x-gap="12" cols="2" style="margin-bottom: 16px;">
        <n-gi>
          <div style="border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px 8px; text-align: center;">
            <div style="font-weight: 700; font-size: 15px;">
              <n-number-animation :from="0" :to="0" show-separator /> ₫
            </div>
            <div style="font-size: 11px; color: #6b7280; margin-top: 4px;">
              Hôm nay
            </div>
          </div>
        </n-gi>
        <n-gi>
          <div style="border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px 8px; text-align: center;">
            <div style="font-weight: 700; font-size: 15px;">
              <n-number-animation :from="0" :to="600000" show-separator /> ₫
            </div>
            <div style="font-size: 11px; color: #6b7280; margin-top: 4px;">
              Tuần 1
            </div>
          </div>
        </n-gi>
      </n-grid>

      <div style="background-color: #f0fdf4; border-radius: 8px; padding: 12px; text-align: center;">
        <div style="color: #18a058; font-weight: 800; font-size: 18px;">
          <n-number-animation :from="0" :to="600000" show-separator /> ₫
        </div>
        <div style="color: #15803d; font-size: 11px; margin-top: 2px;">
          Doanh thu năm 2026
        </div>
      </div>
    </n-card>
  </n-gi>

   <n-gi span="6 m:3">
    <n-card :bordered="false" content-style="padding: 16px;">
      <div style="display: flex; align-items: center; margin-bottom: 20px;">
        <n-avatar
          round
          size="medium"
          color="#eff6ff"
          style="color: #3b82f6; margin-right: 12px;"
        >
          <n-icon size="20">
            <cart-outline />
          </n-icon>
        </n-avatar>
        <div>
          <div style="font-weight: 700; font-size: 16px; line-height: 1.2;">
            Đơn Hàng
          </div>
          <div style="font-size: 12px; color: #9ca3af;">
            Tình trạng đơn hàng
          </div>
        </div>
      </div>

      <div style="text-align: center; margin-bottom: 20px;">
        <div style="color: #2563eb; font-size: 36px; font-weight: 800; line-height: 1.2;">
          <n-number-animation :from="0" :to="1" />
        </div>
        <div style="font-size: 13px; color: #6b7280;">
          Tổng số đơn hàng
        </div>
      </div>

      <div style="margin-bottom: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <span style="font-size: 13px; color: #374151;">Tỷ lệ hoàn thành</span>
          <n-tag :color="{ color: '#ef4444', textColor: '#fff' }" size="small" style="background-color: #ef4444; color: white; border: none; font-weight: 700; height: 20px; padding: 0 6px;">
            100%
          </n-tag>
        </div>
        <n-progress
          type="line"
          :percentage="100"
          :show-indicator="false"
          color="#10b981"
          rail-color="#f3f4f6"
          :height="8"
          style="margin-top: 4px;"
        />
      </div>

      <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 20px;">
        <div class="status-item">
          <div style="display: flex; align-items: center;">
            <n-icon color="#eab308" size="20" style="margin-right: 10px; background: #fef9c3; padding: 0.5px; border-radius: 50%;">
              <time-outline />
            </n-icon>
            <span style="font-size: 13px; color: #4b5563;">Chờ xác nhận</span>
          </div>
          <span style="font-weight: 700; color: #eab308;">0</span>
        </div>

        <div class="status-item">
          <div style="display: flex; align-items: center;">
            <n-icon color="#3b82f6" size="20" style="margin-right: 10px; background: #eff6ff; padding: 0.1px; border-radius: 50%;">
              <settings-outline />
            </n-icon>
            <span style="font-size: 13px; color: #4b5563;">Đang xử lý</span>
          </div>
          <span style="font-weight: 700; color: #3b82f6;">0</span>
        </div>

        <div class="status-item">
          <div style="display: flex; align-items: center;">
            <n-icon color="#10b981" size="20" style="margin-right: 10px; background: #ecfdf5; padding: 0.5px; border-radius: 50%;">
              <checkmark-circle-outline />
            </n-icon>
            <span style="font-size: 13px; color: #4b5563;">Hoàn thành</span>
          </div>
          <span style="font-weight: 700; color: #10b981;">1</span>
        </div>

        <div class="status-item">
          <div style="display: flex; align-items: center;">
            <n-icon color="#ef4444" size="20" style="margin-right: 10px; background: #fef2f2; padding: 0.1px; border-radius: 50%;">
              <close-circle-outline />
            </n-icon>
            <span style="font-size: 13px; color: #4b5563;">Đã hủy</span>
          </div>
          <span style="font-weight: 700; color: #ef4444;">0</span>
        </div>
      </div>

      <n-grid x-gap="12" cols="2">
        <n-gi>
          <div style="background-color: #ecfdf5; border-radius: 8px; padding: 12px; text-align: center;">
            <div style="color: #10b981; font-weight: 700; font-size: 18px;">
              <n-number-animation :from="0" :to="1" />
            </div>
            <div style="color: #059669; font-size: 11px;">Thành công</div>
          </div>
        </n-gi>
        <n-gi>
          <div style="background-color: #fef2f2; border-radius: 8px; padding: 12px; text-align: center;">
            <div style="color: #ef4444; font-weight: 700; font-size: 18px;">
              <n-number-animation :from="0" :to="0" />
            </div>
            <div style="color: #b91c1c; font-size: 11px;">Đã hủy</div>
          </div>
        </n-gi>
      </n-grid>

    </n-card>
  </n-gi>

   <n-gi span="6 m:3">
    <n-card :bordered="false" content-style="padding: 16px;">
      <div style="display: flex; align-items: center; margin-bottom: 20px;">
        <n-avatar
          round
          size="medium"
          color="#f3e8ff"
          style="color: #9333ea; margin-right: 12px;"
        >
          <n-icon size="20">
            <cube-outline />
          </n-icon>
        </n-avatar>
        <div>
          <div style="font-weight: 700; font-size: 16px; line-height: 1.2;">
            Sản Phẩm
          </div>
          <div style="font-size: 12px; color: #9ca3af;">
            Quản lý kho hàng
          </div>
        </div>
      </div>

      <div style="text-align: center; margin-bottom: 24px;">
        <div style="color: #9333ea; font-size: 36px; font-weight: 800; line-height: 1.2;">
          <n-number-animation :from="0" :to="3" />
        </div>
        <div style="font-size: 13px; color: #6b7280;">
          Tổng số sản phẩm
        </div>
      </div>

      <div style="background-color: #fafafa; border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;">
        <div>
          <div style="font-weight: 600; font-size: 13px; color: #374151;">Sắp hết hàng</div>
          <div style="font-size: 11px; color: #9ca3af;">sản phẩm cần nhập thêm</div>
        </div>
        <div style="background-color: #22c55e; color: white; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700;">
         3
        </div>
      </div>

      <div style="margin-bottom: 20px;">
        <div style="font-weight: 700; font-size: 13px; color: #374151; margin-bottom: 12px;">
          Sản phẩm bán chạy
        </div>
        <div style="display: flex; flex-direction: column; gap: 10px;">
          <div class="product-item">
            <div style="display: flex; align-items: center;">
              <span class="rank-badge rank-1">1</span>
              <span style="font-size: 13px; color: #4b5563;">Dell imposen 1</span>
            </div>
            <n-tag size="small" :bordered="false" style="background-color: #ecfdf5; color: #10b981; font-weight: 700;">3</n-tag>
          </div>
          <div class="product-item">
            <div style="display: flex; align-items: center;">
              <span class="rank-badge rank-1">2</span>
              <span style="font-size: 13px; color: #4b5563;">Pegasus</span>
            </div>
            <n-tag size="small" :bordered="false" style="background-color: #ecfdf5; color: #10b981; font-weight: 700;">2</n-tag>
          </div>
        </div>
      </div>

      <n-grid x-gap="12" cols="2">
        <n-gi>
          <div style="background-color: #f0fdf4; border-radius: 8px; padding: 12px; text-align: center;">
            <div style="color: #15803d; font-weight: 700; font-size: 18px;">
              <n-number-animation :from="0" :to="3" />
            </div>
            <div style="color: #15803d; font-size: 11px;">Còn hàng</div>
          </div>
        </n-gi>
        <n-gi>
          <div style="background-color: #fff7ed; border-radius: 8px; padding: 12px; text-align: center;">
            <div style="color: #c2410c; font-weight: 700; font-size: 18px;">
              <n-number-animation :from="0" :to="3" />
            </div>
            <div style="color: #c2410c; font-size: 11px;">Cần nhập</div>
          </div>
        </n-gi>
      </n-grid>

    </n-card>
  </n-gi>

    <n-gi span="6 m:3">
    <n-card :bordered="false" content-style="padding: 16px;">
      <div style="display: flex; align-items: center; margin-bottom: 20px;">
        <n-avatar
          round
          size="medium"
          color="#ccfbf1"
          style="color: #0d9488; margin-right: 12px;"
        >
          <n-icon size="20">
            <people-outline />
          </n-icon>
        </n-avatar>
        <div>
          <div style="font-weight: 700; font-size: 16px; line-height: 1.2;">
            Khách Hàng
          </div>
          <div style="font-size: 12px; color: #9ca3af;">
            Thông tin khách hàng
          </div>
        </div>
      </div>

      <div style="text-align: center; margin-bottom: 24px;">
        <div style="color: #0f766e; font-size: 36px; font-weight: 800; line-height: 1.2;">
          <n-number-animation :from="0" :to="4" />
        </div>
        <div style="font-size: 13px; color: #6b7280;">
          Tổng số khách hàng
        </div>
      </div>

      <div style="border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px; margin-bottom: 20px;">
        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 4px;">
          <span style="font-size: 13px; color: #374151; font-weight: 600;">Khách hàng mới tháng này</span>
          <div style="background-color: #0ea5e9; color: white; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700;">
            1
          </div>
        </div>
        <div style="font-size: 11px; color: #9ca3af;">
          25% tổng khách hàng
        </div>
      </div>

      <div style="margin-bottom: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
          <span style="font-size: 13px; color: #374151;">Tỷ lệ giữ chân</span>
          <n-tag size="small" style="background-color: #ef4444; color: white; border: none; font-weight: 700; height: 20px; padding: 0 6px;">
            0%
          </n-tag>
        </div>
        <n-progress
          type="line"
          :percentage="0"
          :show-indicator="false"
          color="#10b981"
          rail-color="#f3f4f6"
          :height="8"
          style="margin-top: 4px;"
        />
        <div style="font-size: 11px; color: #6b7280; margin-top: 6px;">
          Khách hàng quay lại mua hàng
        </div>
      </div>

      <n-grid x-gap="12" cols="2">
        <n-gi>
          <div style="background-color: #ecfdf5; border-radius: 8px; padding: 12px; text-align: center;">
            <div style="color: #10b981; font-weight: 700; font-size: 18px;">
              <n-number-animation :from="0" :to="0" />
            </div>
            <div style="color: #059669; font-size: 11px;">Khách trung thành</div>
          </div>
        </n-gi>
        <n-gi>
          <div style="background-color: #eff6ff; border-radius: 8px; padding: 12px; text-align: center;">
            <div style="color: #3b82f6; font-weight: 700; font-size: 18px;">
              <n-number-animation :from="0" :to="1" />
            </div>
            <div style="color: #2563eb; font-size: 11px;">Khách mới</div>
          </div>
        </n-gi>
      </n-grid>

    </n-card>
  </n-gi>

    <!-- Biểu đồ - toàn bộ chiều rộng -->
    <n-gi :span="12">
      <n-card content-style="padding: 0;">
        <n-tabs
          type="line"
          size="large"
          :tabs-padding="20"
          pane-style="padding: 20px;"
        >
          <n-tab-pane name="Doanh thu">
            <Chart />
          </n-tab-pane>
           <n-tab-pane name="Đơn hàng">
           <Chart3 />
          </n-tab-pane>
        </n-tabs>
      </n-card>
    </n-gi>


    <!-- Giao dịch - Mobile full, Desktop 2/3 -->
    <n-gi  span="6 m:6">
      <n-card title="Sản phẩm bán chạy" :segmented="{ content: true }">
        <n-table
          :bordered="false"
          :single-line="false"
          :scroll-x="appStore.isMobile ? 600 : undefined"
        >
          <thead>
            <tr>
              <th>STT</th>
              <th>Tên Sản phẩm</th>
              <th>Thương hiệu</th>
              <th>Số lượng bán</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tableData" :key="item.id">
              <td>{{ item.stt }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.brand }}</td>
              <td>{{ item.soLuong }}</td>
            </tr>
          </tbody>
        </n-table>
      </n-card>
    </n-gi>


      <n-gi  span="6 m:6">
      <n-card title="Sản phẩm sắp hết" :segmented="{ content: true }">
        <n-table
          :bordered="false"
          :single-line="false"
          :scroll-x="appStore.isMobile ? 600 : undefined"
        >
          <thead>
            <tr>
              <th>STT</th>
              <th>Tên Sản phẩm</th>
              <th>Thương hiệu</th>
              <th>Số lượng tồn kho</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tableData2" :key="item.id">
              <td>{{ item.stt }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.brand }}</td>
              <td>{{ item.soLuong }}</td>
            </tr>
          </tbody>
        </n-table>
      </n-card>
    </n-gi>
  </n-grid>
</template>

<style  scoped>
  .status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #f3f4f6;
  border-radius: 8px;
  background-color: #fff;
  transition: all 0.3s;
}
.status-item:hover {
  border-color: #d1d5db;
}

.product-item, .category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #f9fafb;
  border-radius: 8px;
  background-color: #fff;
}

.rank-badge {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  margin-right: 8px;
}
.rank-1 { background-color: #ffedd5; color: #ea580c; } 
.rank-2 { background-color: #ffedd5; color: #ea580c; opacity: 0.8; }
.rank-3 { background-color: #ffedd5; color: #ea580c; opacity: 0.6; }
</style>
