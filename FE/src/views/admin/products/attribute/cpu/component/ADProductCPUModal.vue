<template>
  <n-modal :show="isOpen">
    <n-card style="width: 600px" :title="id ? (isDetail ? 'Chi tiết CPU' : 'Cập nhật CPU') : 'Thêm CPU'"
      :bordered="false" size="huge" role="dialog" aria-modal="true">
      <template #header-extra>
        <n-button @click="handleClickCancel">
          <Icon icon="ic:outline-close" />
        </n-button>
      </template>

      <!-- content -->
      <div :style="{ maxHeight: '400px', overflowY: 'auto' }">
        <n-form>
          <n-form-item label="Mã">
            <n-input v-model:value="detailCPU.code" placeholder="Nhập mã"></n-input>
          </n-form-item>
          <n-form-item label="Tên">
            <n-input v-model:value="detailCPU.name" placeholder="Nhập tên"></n-input>
          </n-form-item>
          <n-form-item label="Thế hệ">
            <n-input v-model:value="detailCPU.generation" placeholder="Nhập thế hệ"></n-input>
          </n-form-item>
          <n-form-item label="Dòng CPU">
            <n-input v-model:value="detailCPU.series" placeholder="Nhập dòng CPU"></n-input>
          </n-form-item>
          <n-form-item label="Hãng">
            <n-select v-model:value="detailCPU.brand" :options="brandOptionsSelect"
              :placeholder="'Chọn hãng'"></n-select>
          </n-form-item>
          <n-form-item label="Năm phát hành">
            <n-date-picker v-model:value="detailCPU.releaseYear" type="year" clearable
              placeholder="Chọn năm phát hành" />
          </n-form-item>
        </n-form>
      </div>

      <!-- footer -->
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleClickCancel">Hủy</n-button>
          <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
            :positive-button-props="{ type: 'info' }">
            <template #trigger>
              <n-button>Xác nhận</n-button>
            </template>
            Bạn chắc chắn muốn thao tác
          </n-popconfirm>
        </n-space>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { ADProductCPUResponse, getCPUById, modifyCPU } from '@/service/api/admin/product/cpu.api';
import { convertMsToYear, convertYearToMs } from '@/utils/helper';
import { Icon } from '@iconify/vue';
import { Ref, ref, watch } from 'vue'

const notification = useNotification();

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  isDetail: boolean
}>()

const emit = defineEmits(['success', 'close'])

const detailCPU: Ref<ADProductCPUResponse> = ref({
  code: '',
  name: '',
  description: '',
  generation: '',
  series: '',
  brand: '',
  releaseYear: 0,
})

const fetchDetailCPU = async () => {
  const res = await getCPUById(props.id as string)

  const data = res.data;

  data.releaseYear = convertYearToMs(data.releaseYear)

  detailCPU.value = data
}

const resetField = () => {
  detailCPU.value = {
    code: '',
    name: '',
    description: '',
    generation: '',
    series: '',
    brand: '',
    releaseYear: 0,
  }
}

watch(
  () => props.id,
  (newId) => {
    if (newId) fetchDetailCPU()
    else resetField()
  }
)

const handleClickCancel = () => {
  emit('close')
}

const handleClickOK = async () => {
  const data = detailCPU.value;

  data.releaseYear = convertMsToYear(data.releaseYear)

  const res = await modifyCPU(data)
  console.log(res.success)
  if (res.success) notification.success({ content: props.id ? 'Cập nhật cpu thành công' : 'Thêm CPU thành công', duration: 3000 })
  else notification.error({ content: props.id ? 'Cập nhật CPU thất bại' : 'Thêm CPU thất bại', duration: 3000 })
  emit('success')
}

const brandOptionsSelect = [
  { label: 'Intel', value: 'Intel' },
  { label: 'AMD', value: 'AMD' },
  { label: 'Apple', value: 'Apple' },
]
</script>

<style scoped>
.container {
  max-height: 400px;
  overflow-y: auto;
}
</style>
