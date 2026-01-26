<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, nextTick } from "vue";
import * as echarts from "echarts";
import { statisticsApi } from "@/service/api/admin/statistics/api";

const props = defineProps<{
  filterType: string;
  rangeDate?: [number, number] | null;
}>();

const chartDom = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const initChart = async () => {
  if (!chartDom.value) return;
  
  if (!myChart) {
    myChart = echarts.init(chartDom.value);
  }

  myChart.showLoading();
  try {
    // Gọi API và nhận về dữ liệu đúng kiểu RevenueChartResponse
    const data = await statisticsApi.getRevenueChart(props.filterType, props.rangeDate);
    myChart.hideLoading();

    // Kiểm tra dữ liệu an toàn với optional chaining và kiểm tra mảng
    if (data && data.timeLabels && Array.isArray(data.timeLabels) && data.timeLabels.length > 0) {
      myChart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: (params: any) => {
            const item = params[0];
            return `${item.name}<br/>Doanh thu: <b style="color: #2563eb">${formatCurrency(item.value)}</b>`;
          }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.timeLabels, // Sử dụng timeLabels từ API
          axisTick: { alignWithLabel: true }
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: (val: number) => val >= 1000000 ? `${val / 1000000}M` : val }
        },
        series: [
          {
            name: 'Doanh thu',
            type: 'line',
            smooth: true,
            areaStyle: { opacity: 0.1 },
            data: data.currentData, // Sử dụng currentData từ API
            itemStyle: { color: '#2563eb' },
            label: {
              show: true,
              position: 'top',
              formatter: (p: any) => formatCurrency(p.value).replace('₫', '')
            }
          }
        ]
      }, true);
    } else {
      myChart.setOption({
        title: { text: 'Không có dữ liệu', left: 'center', top: 'center', textStyle: { color: '#999' } },
        xAxis: { data: [] }, yAxis: {}, series: []
      });
    }
  } catch (error) {
    console.error(error);
    myChart.hideLoading();
  }
};

watch(() => [props.filterType, props.rangeDate], () => {
  initChart();
}, { deep: true });

const handleResize = () => myChart?.resize();

onMounted(() => {
  nextTick(() => {
    initChart();
    window.addEventListener("resize", handleResize);
  });
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  myChart?.dispose();
});
</script>

<template>
  <div ref="chartDom" style="width: 100%; height: 350px;"></div>
</template>