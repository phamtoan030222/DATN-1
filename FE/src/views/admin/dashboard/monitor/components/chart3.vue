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

// Map màu sắc cố định theo tên trạng thái
const statusColorMap: Record<string, string> = {
 'Hoàn thành': '#00c292',
        'Đơn huỷ': '#FF4D4F',    // MÀU ĐỎ CỦA BẠN ĐÂY
        'Đã hủy': '#FF4D4F',     // Dự phòng nếu API trả về "Đã hủy" thay vì "Đơn huỷ"
        'Chờ xác nhận': '#409eff',
        'Đang giao': '#67c23a',
        'Chờ giao': '#e6a23c',
        'Đã xác nhận': '#f5c242',
        'Lưu tạm': '#909399'
};

const initChart = async () => {
  if (!chartDom.value) return;
  if (!myChart) myChart = echarts.init(chartDom.value);

  myChart.showLoading();
  try {
    const data = await statisticsApi.getOrderStatusChart(props.filterType, props.rangeDate);
    myChart.hideLoading();

    if (data && data.length > 0) {
      // Gán màu cho dữ liệu
      const processedData = data.map((item: any) => ({
        name: item.name,
        value: item.value,
        itemStyle: {
          color: statusColorMap[item.name] || '#f0a020'
        }
      }));

      myChart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} đơn ({d}%)'
        },
        legend: {
          bottom: '0%',
          left: 'center',
          itemWidth: 10,
          itemHeight: 10
        },
        series: [
          {
            name: 'Trạng thái',
            type: 'pie',
            radius: ['40%', '65%'], // Thu nhỏ bán kính chút để có chỗ cho nhãn
            avoidLabelOverlap: true, // Tránh đè nhãn lên nhau
            itemStyle: {
              borderRadius: 5,
              borderColor: '#fff',
              borderWidth: 2
            },
            // --- CẤU HÌNH HIỂN THỊ CHỈ SỐ Ở ĐÂY ---
            label: {
              show: true,           // Luôn hiện
              position: 'outside',  // Hiện bên ngoài nối dây vào
              formatter: '{b}\n{c} đơn ({d}%)', // Dòng 1: Tên, Dòng 2: SL + %
              fontSize: 12,
              lineHeight: 16
            },
            labelLine: {
              show: true,
              length: 15,
              length2: 10
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 14,
                fontWeight: 'bold'
              }
            },
            data: processedData
          }
        ]
      }, true);
    } else {
      myChart.setOption({
        title: { text: 'Chưa có đơn hàng', left: 'center', top: 'center', textStyle: { color: '#999' } },
        series: []
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