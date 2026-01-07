<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  NButton,
  NDatePicker,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NInput,
  NModal,
  NSelect,
  NSpace,
  useNotification,
} from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { getGPUById, modifyGPU } from '@/service/api/admin/product/gpu.api'
import type { ADProductGPUCreateUpdateRequest } from '@/service/api/admin/product/gpu.api'
import { convertMsToYear, convertYearToMs } from '@/utils/helper'

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  isDetail: boolean
}>()

const emit = defineEmits(['success', 'close', 'update:isOpen'])

const notification = useNotification()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)

// Init model
const detailGPU = ref({
  id: '',
  code: '', // Giữ code trong model để hứng dữ liệu BE, nhưng không hiển thị input
  name: '',
  description: '',
  generation: '',
  series: '',
  brand: null as string | null,
  releaseYear: null as number | null, // Timestamp cho DatePicker
})

const brandOptionsSelect = [
  { label: 'NVIDIA', value: 'NVIDIA' },
  { label: 'AMD', value: 'AMD' },
  { label: 'Intel', value: 'Intel' },
  { label: 'Apple', value: 'Apple' },
]

// Rules validate
const rules: FormRules = {
  name: [{ required: true, message: 'Vui lòng nhập tên GPU', trigger: 'blur' }],
  brand: [{ required: true, message: 'Vui lòng chọn hãng sản xuất', trigger: ['blur', 'change'] }],
  releaseYear: [{ required: true, type: 'number', message: 'Vui lòng chọn năm phát hành', trigger: ['blur', 'change'] }],
  generation: [{ required: true, message: 'Vui lòng nhập thế hệ', trigger: 'blur' }],
  series: [{ required: true, message: 'Vui lòng nhập dòng GPU', trigger: 'blur' }],
}

// ================= METHODS =================

async function fetchDetailGPU() {
  if (!props.id)
    return
  loading.value = true
  try {
    const res = await getGPUById(props.id)
    if (res.data) {
      detailGPU.value = {
        ...res.data,
        // Convert Year (2024) -> Timestamp để hiển thị lên DatePicker
        releaseYear: res.data.releaseYear ? convertYearToMs(res.data.releaseYear) : null,
      }
    }
  }
  catch (error) {
    notification.error({ content: 'Lỗi tải thông tin GPU', duration: 3000 })
  }
  finally {
    loading.value = false
  }
}

function resetField() {
  detailGPU.value = {
    id: '',
    code: '',
    name: '',
    description: '',
    generation: '',
    series: '',
    brand: null,
    releaseYear: null,
  }
  formRef.value?.restoreValidation()
}

watch(
  () => props.isOpen,
  (newVal) => {
    if (newVal) {
      if (props.id)
        fetchDetailGPU()
      else resetField()
    }
  },
)

function handleClickCancel() {
  emit('close')
  emit('update:isOpen', false)
}

async function handleClickOK() {
  formRef.value?.validate(async (errors) => {
    if (!errors) {
      loading.value = true
      try {
        // Convert Timestamp -> Year (number) trước khi gửi API
        const releaseYearNumber = detailGPU.value.releaseYear
          ? convertMsToYear(detailGPU.value.releaseYear)
          : new Date().getFullYear()

        const payload: ADProductGPUCreateUpdateRequest = {
          ...detailGPU.value,
          brand: detailGPU.value.brand || '',
          releaseYear: releaseYearNumber,
        }

        const res = await modifyGPU(payload)

        if (res.success) {
          notification.success({
            content: props.id ? 'Cập nhật GPU thành công' : 'Thêm GPU thành công',
            duration: 3000,
          })
          emit('success')
          emit('update:isOpen', false)
        }
        else {
          notification.error({
            content: props.id ? 'Cập nhật thất bại' : 'Thêm mới thất bại',
            duration: 3000,
          })
        }
      }
      catch (error) {
        notification.error({ content: 'Lỗi hệ thống', duration: 3000 })
      }
      finally {
        loading.value = false
      }
    }
  })
}
</script>

<template>
  <NModal
    :show="isOpen"
    preset="card"
    :style="{ width: '600px' }"
    :title="id ? 'Cập nhật thông tin GPU' : 'Thêm mới GPU'"
    :bordered="false"
    size="huge"
    :mask-closable="false"
    @close="handleClickCancel"
  >
    <NForm
      ref="formRef"
      :model="detailGPU"
      :rules="rules"
      label-placement="top"
      size="medium"
    >
      <NGrid :x-gap="12" :cols="2">
        <NGridItem :span="2">
          <NFormItem label="Tên GPU" path="name">
            <NInput v-model:value="detailGPU.name" placeholder="VD: NVIDIA GeForce RTX 4090" />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Hãng sản xuất" path="brand">
            <NSelect
              v-model:value="detailGPU.brand"
              :options="brandOptionsSelect"
              placeholder="Chọn hãng"
            />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Năm phát hành" path="releaseYear">
            <NDatePicker
              v-model:value="detailGPU.releaseYear"
              type="year"
              clearable
              placeholder="Chọn năm"
              style="width: 100%"
            />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Thế hệ" path="generation">
            <NInput v-model:value="detailGPU.generation" placeholder="VD: RTX 40 Series, Radeon RX 7000..." />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Dòng GPU" path="series">
            <NInput v-model:value="detailGPU.series" placeholder="VD: GeForce, Radeon..." />
          </NFormItem>
        </NGridItem>

        <NGridItem :span="2">
          <NFormItem label="Mô tả thêm">
            <NInput
              v-model:value="detailGPU.description"
              type="textarea"
              placeholder="Ghi chú thêm về sản phẩm..."
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </NFormItem>
        </NGridItem>
      </NGrid>
    </NForm>

    <template #footer>
      <NSpace justify="end">
        <NButton @click="handleClickCancel">
          Hủy
        </NButton>

        <NButton
          type="primary"
          :loading="loading"
          icon-placement="right"
          @click="handleClickOK"
        >
          {{ id ? 'Lưu thay đổi' : 'Thêm mới' }}
          <template #icon>
            <Icon icon="carbon:save" />
          </template>
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>
