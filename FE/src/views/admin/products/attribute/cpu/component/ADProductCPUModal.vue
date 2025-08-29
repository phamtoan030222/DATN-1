<template>

</template>

<script setup lang="ts">
import { ADProductCPUResponse, getCPUById, modifyCPU } from '@/service/api/admin/product/cpu.api';
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

  detailCPU.value = res.data
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
  const res = await modifyCPU(detailCPU.value)
  console.log(res.success)
  if (res.success) notification.success({content: props.id ? 'Cập nhật cpu thành công' : 'Thêm CPU thành công'})
  else notification.error({content: props.id ? 'Cập nhật CPU thất bại' : 'Thêm CPU thất bại'})
  resetField()
  emit('success')
}
</script>

<style scoped>
.container {
  max-height: 400px;
  overflow-y: auto;
}
</style>
