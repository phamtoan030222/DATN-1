<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useEcharts } from "@/hooks"; // Hook của bạn
import type { ECOption } from "@/hooks";
import { graphic } from "echarts";

// Biến lưu trạng thái đang xem theo Tuần, Tháng hay Năm
const timeRange = ref<'week' | 'month' | 'year'>('week');

// Hàm tạo danh sách nhãn trục X (Thứ 2->CN, 1->31, T1->T12)
const getXAxisData = (type: string) => {
  if (type === 'week') return ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'CN'];
  if (type === 'month') return Array.from({ length: 31 }, (_, i) => `${i + 1}`);
  if (type === 'year') return Array.from({ length: 12 }, (_, i) => `T${i + 1}`);
  return [];
};

// Hàm tạo dữ liệu Doanh thu giả lập khớp với yêu cầu (Ngày 5/1/2026 bán 240k)
const getSeriesData = (type: string) => {
  // Dữ liệu so sánh (Tuần trước/Tháng trước) - Đang để 0 hoặc random thấp
  const previousData = []; 
  
  // Dữ liệu hiện tại (Tuần này/Tháng này)
  let currentData = [];

  if (type === 'week') {
    // Tuần: 7 ngày. Ngày 5/1/2026 là Thứ 2 (index 0)
    currentData = [240000, 0, 0, 0, 0, 0, 0];
    previousData.push(0, 0, 0, 0, 0, 0, 0); // Ví dụ tuần trước không có gì
  } else if (type === 'month') {
    // Tháng: 31 ngày. Ngày 5 là index 4
    currentData = new Array(31).fill(0);
    currentData[4] = 240000; // Ngày 5
  } else {
    // Năm: 12 tháng. Tháng 1 là index 0
    currentData = new Array(12).fill(0);
    currentData[0] = 240000; // Tháng 1
  }

  return { currentData, previousData };
};

// Cấu hình Chart
const lineOptions = ref<ECOption>({
  tooltip: {
    trigger: "axis",
    formatter: (params: any) => {
      // Format tiền Việt Nam
      const formatCurrency = (val: number) => 
        new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
      
      let result = `<div style="margin-bottom: 4px; font-weight:bold">${params[0].axisValue}</div>`;
      params.forEach((item: any) => {
        result += `
          <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 12px">
            <span>${item.marker} ${item.seriesName}</span>
            <span style="font-weight: bold;">${formatCurrency(item.value)}</span>
          </div>`;
      });
      return result;
    }
  },
  legend: { show: true, top: '0%', icon: 'circle' },
  grid: { left: "1%", right: "1%", bottom: "2%", top: "15%", containLabel: true },
  xAxis: {
    type: "category",
    boundaryGap: false,
    data: [], // Sẽ được cập nhật dynamic
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#9ca3af' }
  },
  yAxis: {
    type: "value",
    splitLine: { show: true, lineStyle: { type: 'dashed', color: '#f3f4f6' } },
    axisLabel: { 
      formatter: (value: number) => value >= 1000000 ? `${value/1000000}M` : `${value/1000}k`
    }
  },
  series: [
    {
      name: "Hiện tại",
      type: "line",
      smooth: true,
      showSymbol: true,
      symbolSize: 8,
      data: [], // Sẽ cập nhật dynamic
      itemStyle: { color: '#3b82f6' },
      areaStyle: {
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "rgba(59,102,246,0.5)" },
          { offset: 1, color: "rgba(59,102,246,0.01)" }
        ])
      }
    },
    {
      name: "So sánh", // Tuần trước/Tháng trước...
      type: "line",
      smooth: true,
      showSymbol: false,
      data: [],
      lineStyle: { type: 'dashed', width: 2, color: '#fb7185' },
      itemStyle: { color: '#fb7185' }
    }
  ]
}) as any;

// Hàm cập nhật dữ liệu khi người dùng bấm nút
const updateChart = () => {
  const type = timeRange.value;
  const { currentData, previousData } = getSeriesData(type);
  
  lineOptions.value.xAxis.data = getXAxisData(type);
  lineOptions.value.series[0].data = currentData;
  lineOptions.value.series[1].data = previousData;
  
  // Tùy chỉnh tên Legend cho hợp lý
  if(type === 'week') {
    lineOptions.value.series[0].name = 'Tuần này';
    lineOptions.value.series[1].name = 'Tuần trước';
  } else if (type === 'month') {
    lineOptions.value.series[0].name = 'Tháng này';
    lineOptions.value.series[1].name = 'Tháng trước';
  } else {
    lineOptions.value.series[0].name = 'Năm nay';
    lineOptions.value.series[1].name = 'Năm ngoái';
  }
};

// Khởi chạy
useEcharts("revenueChart", lineOptions);

// Watch để cập nhật khi biến timeRange thay đổi
watch(timeRange, () => {
  updateChart();
});

// Chạy lần đầu
onMounted(() => {
  updateChart();
});
</script>

<template>
  <div style="background: white; padding: 20px; border-radius: 12px; position: relative;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <div>
        <div style="font-weight: 700; font-size: 16px;">Biểu đồ doanh thu</div>
        <div style="font-size: 12px; color: #9ca3af;">Tổng quan doanh thu theo thời gian</div>
      </div>
      
      <n-radio-group v-model:value="timeRange" size="small">
        <n-radio-button value="week" label="Tuần" />
        <n-radio-button value="month" label="Tháng" />
        <n-radio-button value="year" label="Năm" />
      </n-radio-group>
    </div>

    <div ref="revenueChart" class="h-300px" />
  </div>
</template>

<style scoped>
.h-300px {
  height: 350px;
  width: 100%;
}
</style>