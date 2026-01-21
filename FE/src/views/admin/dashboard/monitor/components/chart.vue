<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useEcharts } from "@/hooks"; 
import { graphic } from "echarts";
import { statisticsApi } from "@/service/api/admin/statistics/api";

const props = defineProps<{ filterType: string }>();

const lineOptions = ref<any>({
  tooltip: {
    trigger: "axis",
    formatter: (params: any) => {
      const formatCurrency = (val: number) => 
        new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
      
      let res = `<div style="margin-bottom: 4px; font-weight:bold">${params[0].axisValue}</div>`;
      
      params.forEach((item: any) => {
        res += `
          <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 12px">
            <span style="display:flex; align-items:center;">
                ${item.marker} ${item.seriesName}
            </span>
            <span style="font-weight: bold; margin-left: 10px;">${formatCurrency(item.value)}</span>
          </div>`;
      });
      return res;
    }
  },
  legend: { show: true, top: '0%' },
  grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
  xAxis: { 
    type: "category", 
    boundaryGap: false, 
    data: [], 
    axisLine: { show: false },
    axisTick: { show: false }
  },
  yAxis: { 
    type: "value",
    splitLine: { show: true, lineStyle: { type: 'dashed', color: '#f3f4f6' } },
    axisLabel: { 
       formatter: (val: number) => val >= 1000000 ? `${val/1000000}M` : (val >= 1000 ? `${val/1000}k` : val)
    }
  },
  series: [
    {
      name: "Hiện tại", 
      type: "line", 
      smooth: true, 
      showSymbol: true,
      symbolSize: 6,
      itemStyle: { color: '#3b82f6' }, 
      areaStyle: { 
        color: new graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: "rgba(59,102,246,0.5)" }, 
          { offset: 1, color: "rgba(59,102,246,0.01)" }
        ]) 
      },
      data: []
    },
    {
      name: "Kỳ trước", 
      type: "line", 
      smooth: true, 
      showSymbol: false,
      lineStyle: { type: 'dashed', color: '#fb7185' },
      itemStyle: { color: '#fb7185' },
      data: []
    }
  ]
});

const fetchChartData = async () => {
  try {
    const res = await statisticsApi.getRevenueChart(props.filterType);
    if(res) {
      lineOptions.value.xAxis.data = res.timeLabels || [];
      lineOptions.value.series[0].data = res.currentData || [];
      lineOptions.value.series[1].data = res.previousData || [];
      
      let currentLabel = 'Tuần này';
      let prevLabel = 'Tuần trước';

      switch(props.filterType) {
          case 'today':
              currentLabel = 'Hôm nay';
              prevLabel = 'Hôm qua';
              break;
          case 'month':
              currentLabel = 'Tháng này';
              prevLabel = 'Tháng trước';
              break;
          case 'year':
              currentLabel = 'Năm nay';
              prevLabel = 'Năm ngoái';
              break;
          case 'week':
          default:
              currentLabel = 'Tuần này';
              prevLabel = 'Tuần trước';
              break;
      }

      lineOptions.value.series[0].name = currentLabel;
      lineOptions.value.series[1].name = prevLabel;

      lineOptions.value = { ...lineOptions.value };
    }
  } catch(e) { console.error(e); }
};

useEcharts("revenueChart", lineOptions);

watch(() => props.filterType, fetchChartData);
onMounted(fetchChartData);
</script>

<template>
  <div ref="revenueChart" style="height: 350px; width: 100%;" />
</template>