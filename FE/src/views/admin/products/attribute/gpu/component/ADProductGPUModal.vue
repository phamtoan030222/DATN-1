<template>
  <n-modal :show="isOpen">
    <n-card style="width: 600px" :title="id ? (isDetail ? 'Chi tiết GPU' : 'Cập nhật GPU') : 'Thêm GPU'"
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
            <n-input v-model:value="detailGPU.code" placeholder="Nhập mã"></n-input>
          </n-form-item>
          <n-form-item label="Tên">
            <n-input v-model:value="detailGPU.name" placeholder="Nhập tên"></n-input>
          </n-form-item>
          <n-form-item label="Thế hệ">
            <n-input v-model:value="detailGPU.generation" placeholder="Nhập thế hệ"></n-input>
          </n-form-item>
          <n-form-item label="Dòng GPU">
            <n-input v-model:value="detailGPU.series" placeholder="Nhập dòng GPU"></n-input>
          </n-form-item>
          <n-form-item label="Hãng">
            <n-select v-model:value="detailGPU.brand" :options="brandOptionsSelect"
              :placeholder="'Chọn hãng'"></n-select>
          </n-form-item>
          <n-form-item label="Năm phát hành">
            <n-date-picker v-model:value="detailGPU.releaseYear" type="year" clearable
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
import { ADProductGPUResponse, getGPUById, modifyGPU } from '@/service/api/admin/product/gpu.api';
import { Ref, ref, watch } from 'vue'
import { Icon } from '@iconify/vue';
import { convertMsToYear, convertYearToMs } from '@/utils/helper';

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  isDetail: boolean
}>()

const notification = useNotification();

const emit = defineEmits(['success', 'close'])

const detailGPU: Ref<ADProductGPUResponse> = ref({
  code: '',
  name: '',
  description: '',
  generation: '',
  series: '',
  brand: '',
  releaseYear: 0,
})

const fetchDetailGPU = async () => {
  const res = await getGPUById(props.id as string)

  const data = res.data;

  data.releaseYear = convertYearToMs(data.releaseYear)

  detailGPU.value = data
}

const resetField = () => {
  detailGPU.value = {
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
    if (newId) fetchDetailGPU()
    else resetField()
  }
)

const handleClickCancel = () => {
  emit('close')
}

const handleClickOK = async () => {
  const data = detailGPU.value;

  data.releaseYear = convertMsToYear(data.releaseYear)

  const res = await modifyGPU(data)
  console.log(res.success)
  if (res.success) notification.success({ content: props.id ? 'Cập nhật GPU thành công' : 'Thêm GPU thành công', duration: 3000 })
  else notification.error({ content: props.id ? 'Cập nhật GPU thất bại' : 'Thêm GPU thất bại', duration: 3000 })
  emit('success')
}

const brandOptionsSelect = [
  {
    value: 'AMD',
    label: 'AMD'
  },
  {
    value: 'NVIDIA',
    label: 'NVIDIA'
  },
]
</script>

<style scoped>
.container {
  max-height: 400px;
  overflow-y: auto;
}
</style>
