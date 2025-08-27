<script setup lang="ts">
import { ref } from "vue";
import { NConfigProvider, NPagination } from "naive-ui";

// ✅ Custom locale tiếng Việt cho Pagination
const viVN = {
  Pagination: {
    goto: "Đi đến trang",
    selectionSuffix: " bản ghi",
  },
};

interface Props {
  count?: number;
}
const { count = 0 } = defineProps<Props>();

const emit = defineEmits<{
  change: [page: number, pageSize: number];
}>();

const page = ref(1);
const pageSize = ref(10);
const displayOrder: Array<"pages" | "size-picker" | "quick-jumper"> = [
  "size-picker",
  "pages",
];

function changePage() {
  emit("change", page.value, pageSize.value);
}
</script>

<template>
  <n-config-provider :locale="viVN">
    <n-pagination
      v-if="count > 0"
      v-model:page="page"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 30, 50]"
      :item-count="count"
      :display-order="displayOrder"
      show-size-picker
      @update-page="changePage"
      @update-page-size="changePage"
    >
      <template #page-size-option="{ option }"> {{ option }} bản ghi </template>

      <template #suffix="{ itemCount }">
        Tổng số: {{ itemCount }} bản ghi
      </template>
    </n-pagination>
  </n-config-provider>
</template>
