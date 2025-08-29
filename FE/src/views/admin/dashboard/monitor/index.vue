<script setup lang="ts">
import { useAppStore } from "@/store";
import Chart from "./components/chart.vue";
import Chart2 from "./components/chart2.vue";
import Chart3 from "./components/chart3.vue";

const appStore = useAppStore();

const tableData = [
  {
    id: 0,
    name: "Tên sản phẩm 1",
    start: "2022-02-02",
    end: "2022-02-02",
    prograss: "100",
    status: "Đã hoàn thành",
  },
  {
    id: 1,
    name: "Tên sản phẩm 2",
    start: "2022-02-02",
    end: "2022-02-02",
    prograss: "50",
    status: "Đang giao dịch",
  },
  {
    id: 2,
    name: "Tên sản phẩm 3",
    start: "2022-02-02",
    end: "2022-02-02",
    prograss: "100",
    status: "Đã hoàn thành",
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
    <!-- Thống kê - Mobile mỗi hàng 2, Desktop mỗi hàng 4 -->
    <n-gi span="6 m:3">
      <n-card>
        <n-space justify="space-between" align="center">
          <n-statistic label="Lượt truy cập">
            <n-number-animation :from="0" :to="12039" show-separator />
          </n-statistic>
          <n-icon color="#de4307" size="42">
            <icon-park-outline-chart-histogram />
          </n-icon>
        </n-space>
        <template #footer>
          <n-space justify="space-between">
            <span>Tổng lượt truy cập</span>
            <span
              ><n-number-animation :from="0" :to="322039" show-separator
            /></span>
          </n-space>
        </template>
      </n-card>
    </n-gi>

    <n-gi span="6 m:3">
      <n-card>
        <n-space justify="space-between" align="center">
          <n-statistic label="Lượt tải về">
            <n-number-animation :from="0" :to="12039" show-separator />
          </n-statistic>
          <n-icon color="#ffb549" size="42">
            <icon-park-outline-chart-graph />
          </n-icon>
        </n-space>
        <template #footer>
          <n-space justify="space-between">
            <span>Tổng lượt tải về</span>
            <span
              ><n-number-animation :from="0" :to="322039" show-separator
            /></span>
          </n-space>
        </template>
      </n-card>
    </n-gi>

    <n-gi span="6 m:3">
      <n-card>
        <n-space justify="space-between" align="center">
          <n-statistic label="Lượt xem">
            <n-number-animation :from="0" :to="12039" show-separator />
          </n-statistic>
          <n-icon color="#1687a7" size="42">
            <icon-park-outline-average />
          </n-icon>
        </n-space>
        <template #footer>
          <n-space justify="space-between">
            <span>Tổng lượt xem</span>
            <span
              ><n-number-animation :from="0" :to="322039" show-separator
            /></span>
          </n-space>
        </template>
      </n-card>
    </n-gi>

    <n-gi span="6 m:3">
      <n-card>
        <n-space justify="space-between" align="center">
          <n-statistic label="Số đăng ký">
            <n-number-animation :from="0" :to="12039" show-separator />
          </n-statistic>
          <n-icon color="#42218E" size="42">
            <icon-park-outline-chart-pie />
          </n-icon>
        </n-space>
        <template #footer>
          <n-space justify="space-between">
            <span>Tổng số đăng ký</span>
            <span
              ><n-number-animation :from="0" :to="322039" show-separator
            /></span>
          </n-space>
        </template>
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
          <n-tab-pane name="Xu hướng lưu lượng">
            <Chart />
          </n-tab-pane>
          <n-tab-pane name="Xu hướng lượt truy cập">
            <Chart2 />
          </n-tab-pane>
        </n-tabs>
      </n-card>
    </n-gi>

    <!-- Nguồn truy cập - Mobile full, Desktop 1/3 -->
    <n-gi span="12 m:4">
      <n-card title="Nguồn truy cập" :segmented="{ content: true }">
        <Chart3 />
      </n-card>
    </n-gi>

    <!-- Giao dịch - Mobile full, Desktop 2/3 -->
    <n-gi span="12 m:8">
      <n-card title="Giao dịch" :segmented="{ content: true }">
        <template #header-extra>
          <n-button type="primary" quaternary> Xem thêm </n-button>
        </template>
        <n-table
          :bordered="false"
          :single-line="false"
          :scroll-x="appStore.isMobile ? 600 : undefined"
        >
          <thead>
            <tr>
              <th>Tên giao dịch</th>
              <th>Thời gian bắt đầu</th>
              <th>Thời gian kết thúc</th>
              <th>Tiến độ</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tableData" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ item.start }}</td>
              <td>{{ item.end }}</td>
              <td>{{ item.prograss }}%</td>
              <td>
                <n-tag :bordered="false" type="info">{{ item.status }}</n-tag>
              </td>
            </tr>
          </tbody>
        </n-table>
      </n-card>
    </n-gi>
  </n-grid>
</template>
