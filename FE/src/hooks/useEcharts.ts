import type {
  BarSeriesOption,
  LineSeriesOption,
  PieSeriesOption,
  RadarSeriesOption,
} from "echarts/charts";
import type {
  DatasetComponentOption,
  GridComponentOption,
  LegendComponentOption,
  TitleComponentOption,
  ToolboxComponentOption,
  TooltipComponentOption,
} from "echarts/components";
import { useAppStore } from "@/store";
import { BarChart, LineChart, PieChart, RadarChart } from "echarts/charts";

import {
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  TransformComponent,
} from "echarts/components";
import * as echarts from "echarts/core";

import { LabelLayout, UniversalTransition } from "echarts/features";
import { CanvasRenderer } from "echarts/renderers";
import { useTemplateRef } from "vue";

export type ECOption = echarts.ComposeOption<
  | BarSeriesOption
  | PieSeriesOption
  | LineSeriesOption
  | TitleComponentOption
  | TooltipComponentOption
  | GridComponentOption
  | LegendComponentOption
  | DatasetComponentOption
  | ToolboxComponentOption
  | RadarSeriesOption
>;

echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DatasetComponent,
  TransformComponent,
  BarChart,
  PieChart,
  LineChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer,
  ToolboxComponent,
  RadarChart,
]);

export function useEcharts(ref: string, chartOptions: Ref<ECOption>) {
  const el = useTemplateRef<HTMLLIElement>(ref);

  const appStore = useAppStore();

  let chart: echarts.ECharts | null = null;

  const { width, height } = useElementSize(el);

  const isRendered = () => Boolean(el && chart);

  async function render() {
    if (!width || !height) return;

    const chartTheme = appStore.colorMode ? "dark" : "light";
    await nextTick();
    if (el) {
      chart = echarts.init(el.value, chartTheme);
      update(chartOptions.value);
    }
  }

  async function update(updateOptions: ECOption) {
    if (isRendered()) {
      chart!.setOption({ backgroundColor: "transparent", ...updateOptions });
    }
  }

  function destroy() {
    chart?.dispose();
    chart = null;
  }

  watch([width, height], async ([newWidth, newHeight]) => {
    if (isRendered() && newWidth && newHeight) chart?.resize();
  });

  watch(chartOptions, (newValue) => {
    update(newValue);
  });

  onMounted(() => {
    render();
  });
  onUnmounted(() => {
    destroy();
  });

  return {
    destroy,
    update,
  };
}
