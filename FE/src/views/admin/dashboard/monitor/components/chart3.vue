<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useEcharts } from "@/hooks";
import { statisticsApi } from "@/service/api/admin/statistics/api";

const props = defineProps<{ filterType: string }>();

const pieOptions = ref<any>({
  tooltip: { 
    trigger: "item",
    formatter: "{b}: {c} đơn ({d}%)"
  },
  legend: { 
    orient: "horizontal",
    bottom: '0%', 
    left: 'center',
    itemWidth: 14,
    itemHeight: 14
  },
  series: [
    {
      type: "pie",
      radius: ["40%", "70%"],
      center: ['50%', '45%'],
      itemStyle: { 
        borderRadius: 8, 
        borderColor: '#fff', 
        borderWidth: 2 
      },
      label: { 
        show: true, 
        formatter: "{b}\n{c}",
        color: "#666"
      },
      data: [] 
    }
  ]
});

const fetchPieData = async () => {
  try {
    const res = await statisticsApi.getOrderStatusChart(props.filterType);
    if (res) {
      const coloredData = res.map((item: any) => {
        let color = '#ccc'; 
        if (item.name === 'Đã thanh toán') color = '#00c292'; // Xanh lá
        if (item.name === 'Đơn huỷ') color = '#f56c6c';     // Đỏ
        if (item.name === 'Đang xử lý') color = '#409eff';// Xanh dương
         if (item.name === 'Chờ xác nhận') color = '#f5c242'; 
        
        return {
          ...item,
          itemStyle: { color: color } 
        };
      });

      pieOptions.value.series[0].data = coloredData;
      pieOptions.value = { ...pieOptions.value };
    }
  } catch(e) { console.error(e); }
};

useEcharts("pieChart", pieOptions);
watch(() => props.filterType, fetchPieData);
onMounted(fetchPieData);
</script>

<template>
  <div ref="pieChart" style="height: 350px; width: 100%;" />
</template>