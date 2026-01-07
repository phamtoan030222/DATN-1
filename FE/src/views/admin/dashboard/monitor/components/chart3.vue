<script setup lang="ts">
import { useEcharts } from "@/hooks";
import type { ECOption } from "@/hooks";

const option = ref<ECOption>({
  tooltip: {
    trigger: "item",
    formatter: "{b} : {d}%",
  },
  legend: {
    orient: "horizontal",
    top: 30,
    padding: 5,
    itemWidth: 20,
    itemHeight: 12,
    textStyle: {
      color: "#777",
    },
  },
  series: [
    {
      type: "pie",
      radius: ["45%", "60%"],
      center: ["50%", "50%"],
      label: {
        show: true,
        formatter: "{b} : {d}%",
        color: "#777",
      },
      labelLine: {
        show: true,
        length2: 10,
      },
      data: [
        {
          value: 0,
          name: "Đang xử lý",
        },
        {
          value: 1,
          name: "Đã thanh toán",
        },
        {
          value: 0,
          name: "Chờ xác nhận",
        },
        {
          value: 0,
          name: "Đơn huỷ",
        },
      ],
    },
  ],
}) as Ref<ECOption>;
useEcharts("lineRef", option);
</script>

<template>
  <div ref="lineRef" class="h-400px" />
</template>

<style scoped></style>
