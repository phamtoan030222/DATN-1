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
import { getCPUById, modifyCPU } from '@/service/api/admin/product/cpu.api'
import type { ADProductCPUCreateUpdateRequest, ADProductCPUResponse } from '@/service/api/admin/product/cpu.api'
import { convertMsToYear, convertYearToMs } from '@/utils/helper'

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  isDetail: boolean
}>()

const emit = defineEmits(['success', 'close', 'update:isOpen']) // Thêm update:isOpen để v-model hoạt động chuẩn

const notification = useNotification()
const formRef = ref<FormInst | null>(null)
const loading = ref(false)

// Init model với kiểu dữ liệu chính xác
const detailCPU = ref({
  id: '',
  code: '',
  name: '',
  description: '',
  generation: '',
  series: '',
  brand: null as string | null, // Sửa thành null, không phải 'null' string
  releaseYear: null as number | null, // Timestamp (number) hoặc null
})

// Options: bỏ string 'null', dùng null thật
const brandOptionsSelect = [
  { label: 'Intel', value: 'Intel' },
  { label: 'AMD', value: 'AMD' },
  { label: 'Apple', value: 'Apple' },
]

const rules: FormRules = {
  name: [{ required: true, message: 'Vui lòng nhập tên CPU', trigger: 'blur' }],
  brand: [{ required: true, message: 'Vui lòng chọn hãng sản xuất', trigger: ['blur', 'change'] }],
  releaseYear: [{ required: true, type: 'number', message: 'Vui lòng chọn năm phát hành', trigger: ['blur', 'change'] }],
  generation: [{ required: true, message: 'Vui lòng nhập thế hệ', trigger: 'blur' }],
  series: [{ required: true, message: 'Vui lòng nhập dòng CPU', trigger: 'blur' }],
}

async function fetchDetailCPU() {
  if (!props.id)
    return
  loading.value = true
  try {
    const res = await getCPUById(props.id)
    if (res.data) {
      // Spread để tránh tham chiếu, xử lý date
      detailCPU.value = {
        ...res.data,
        // Backend trả về 2024 -> Convert sang timestamp để hiện lên DatePicker
        releaseYear: res.data.releaseYear ? convertYearToMs(res.data.releaseYear) : null,
      }
    }
  }
  catch (error) {
    notification.error({ content: 'Lỗi tải thông tin CPU', duration: 3000 })
  }
  finally {
    loading.value = false
  }
}

function resetField() {
  detailCPU.value = {
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
        fetchDetailCPU()
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
        // Prepare payload
        // DatePicker trả về timestamp -> Cần convert về Year (VD: 2024)
        // Cần check null trước khi convert
        const releaseYearNumber = detailCPU.value.releaseYear
          ? convertMsToYear(detailCPU.value.releaseYear)
          : new Date().getFullYear()

        const payload: ADProductCPUCreateUpdateRequest = {
          ...detailCPU.value,
          brand: detailCPU.value.brand || '',
          releaseYear: releaseYearNumber,
        }

        const res = await modifyCPU(payload)

        if (res.success) {
          notification.success({
            content: props.id ? 'Cập nhật CPU thành công' : 'Thêm CPU thành công',
            duration: 3000,
          })
          emit('success')
          emit('update:isOpen', false) // Đóng modal
        }
      }
      catch (error) {
        notification.error({ content: 'Có lỗi xảy ra', duration: 3000 })
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
    :title="id ? 'Cập nhật thông tin CPU' : 'Thêm mới CPU'"
    :bordered="false"
    size="huge"
    :mask-closable="false"
    @close="handleClickCancel"
  >
    <NForm
      ref="formRef"
      :model="detailCPU"
      :rules="rules"
      label-placement="top"
      size="medium"
    >
      <NGrid :x-gap="12" :cols="2">
        <NGridItem :span="2">
          <NFormItem label="Tên CPU" path="name">
            <NInput v-model:value="detailCPU.name" placeholder="VD: Intel Core i9 14900K" />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Hãng sản xuất" path="brand">
            <NSelect
              v-model:value="detailCPU.brand"
              :options="brandOptionsSelect"
              placeholder="Chọn hãng"
            />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Năm phát hành" path="releaseYear">
            <NDatePicker
              v-model:value="detailCPU.releaseYear"
              type="year"
              clearable
              placeholder="Chọn năm"
              style="width: 100%"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Thế hệ" path="generation">
            <NInput v-model:value="detailCPU.generation" placeholder="VD: Gen 14, Zen 4..." />
          </NFormItem>
        </NGridItem>

        <NGridItem>
          <NFormItem label="Dòng CPU" path="series">
            <NInput v-model:value="detailCPU.series" placeholder="VD: Core i9, Ryzen 9..." />
          </NFormItem>
        </NGridItem>

        <NGridItem :span="2">
          <NFormItem label="Mô tả thêm">
            <NInput
              v-model:value="detailCPU.description"
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
        <NButton type="primary" :loading="loading" icon-placement="right" @click="handleClickOK">
          {{ id ? 'Lưu thay đổi' : 'Thêm mới' }}
          <template #icon>
            <Icon icon="carbon:save" />
          </template>
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>
