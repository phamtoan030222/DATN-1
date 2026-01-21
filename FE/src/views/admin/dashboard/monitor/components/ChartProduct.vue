<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useEcharts } from "@/hooks";
import { graphic } from "echarts";
import { statisticsApi } from "@/service/api/admin/statistics/api";

const props = defineProps<{ filterType: string }>();

const barOptions = ref<any>({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '5%', bottom: '3%', top: '5%', containLabel: true },
  xAxis: { type: 'value', boundaryGap: [0, 0.01], splitLine: { show: false } },
  yAxis: { 
    type: 'category', data: [], inverse: true, 
    axisLine: { show: false }, axisTick: { show: false },
    axisLabel: { width: 140, overflow: 'truncate', color: '#6b7280' }
  },
  series: [
    {
      name: 'Số lượng bán', type: 'bar', barWidth: 16, data: [],
      label: { show: true, position: 'right', color: '#666' },
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#60a5fa' }, { offset: 1, color: '#3b82f6' }])
      },
    }
  ]
});

const fetchTopProducts = async () => {
  const res = await statisticsApi.getTopProductsChart(props.filterType);
  if (res) {
    barOptions.value.yAxis.data = res.map((i: any) => i.name);
    barOptions.value.series[0].data = res.map((i: any) => i.value);
    barOptions.value = { ...barOptions.value };
  }
};

useEcharts("productBarChart", barOptions);
watch(() => props.filterType, fetchTopProducts);
onMounted(fetchTopProducts);
</script>

<template>
  <div ref="productBarChart" style="height: 350px; width: 100%;" ></div>
</template>