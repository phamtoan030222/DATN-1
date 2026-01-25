<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  NButton,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NRadio,
  NRadioGroup,
  NSelect,
  useMessage,
} from 'naive-ui'

interface Props {
  show: boolean
  deliveryType: 'GIAO_HANG' | 'TAI_QUAY' // Nhận từ cha
  loading: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['update:show', 'submit'])

const formRef = ref<any>(null)

// Model dữ liệu form
const formData = ref({
  ten: '',
  sdt: '',
  diaChi: '',
  ghiChu: '',
  storeId: null, // Nếu nhận tại siêu thị
})

// Mock data cửa hàng (Thực tế bạn có thể gọi API lấy list cửa hàng)
const storeOptions = [
  { label: 'Siêu thị 123 Nguyễn Văn Linh, Đà Nẵng', value: 'ST01' },
  { label: 'Siêu thị 456 Cầu Giấy, Hà Nội', value: 'ST02' },
  { label: 'Siêu thị 789 Quận 1, TP.HCM', value: 'ST03' },
]

// Rules validate
const rules = computed(() => {
  return {
    ten: { required: true, message: 'Vui lòng nhập họ tên', trigger: 'blur' },
    sdt: { required: true, message: 'Vui lòng nhập SĐT', trigger: 'blur' },
    // Địa chỉ chỉ bắt buộc khi giao tận nơi
    diaChi: props.deliveryType === 'GIAO_HANG'
      ? { required: true, message: 'Vui lòng nhập địa chỉ nhận hàng', trigger: 'blur' }
      : { required: false },
    // Cửa hàng bắt buộc khi nhận tại quầy
    storeId: props.deliveryType === 'TAI_QUAY'
      ? { required: true, message: 'Vui lòng chọn siêu thị nhận hàng', trigger: ['blur', 'change'] }
      : { required: false },
  }
})

function handleSubmit() {
  formRef.value?.validate((errors: any) => {
    if (!errors) {
      // Chuẩn hóa dữ liệu trước khi gửi ra ngoài
      const submitData = {
        ten: formData.value.ten,
        sdt: formData.value.sdt,
        diaChi: props.deliveryType === 'GIAO_HANG'
          ? formData.value.diaChi
          : storeOptions.find(s => s.value === formData.value.storeId)?.label || 'Tại quầy',
        ghiChu: formData.value.ghiChu,
      }
      emit('submit', submitData)
    }
  })
}

function handleClose() {
  emit('update:show', false)
}
</script>

<template>
  <NModal
    :show="show" preset="card"
    :title="deliveryType === 'GIAO_HANG' ? 'Thông tin giao hàng' : 'Thông tin người nhận tại siêu thị'"
    style="width: 600px" :mask-closable="false" @close="handleClose"
  >
    <NForm
      ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="120"
      require-mark-placement="right-hanging"
    >
      <NFormItem label="Họ và tên" path="ten">
        <NInput v-model:value="formData.ten" placeholder="Nhập họ tên người nhận" />
      </NFormItem>

      <NFormItem label="Số điện thoại" path="sdt">
        <NInput v-model:value="formData.sdt" placeholder="Nhập số điện thoại" />
      </NFormItem>

      <template v-if="deliveryType === 'GIAO_HANG'">
        <NFormItem label="Địa chỉ" path="diaChi">
          <NInput
            v-model:value="formData.diaChi" type="textarea"
            placeholder="Số nhà, tên đường, phường/xã, quận/huyện..."
          />
        </NFormItem>
      </template>

      <template v-else>
        <NFormItem label="Chọn siêu thị" path="storeId">
          <NSelect v-model:value="formData.storeId" :options="storeOptions" placeholder="Chọn siêu thị gần bạn" />
        </NFormItem>
        <div class="mb-4 p-3 bg-yellow-50 text-yellow-800 text-sm rounded border border-yellow-200">
          Lưu ý: Vui lòng mang theo CMND/CCCD hoặc tin nhắn xác nhận khi đến nhận hàng.
        </div>
      </template>

      <NFormItem label="Ghi chú">
        <NInput v-model:value="formData.ghiChu" type="textarea" placeholder="Lời nhắn cho cửa hàng (không bắt buộc)" />
      </NFormItem>

      <div class="flex justify-end gap-3 mt-4">
        <NButton @click="handleClose">
          Hủy bỏ
        </NButton>
        <NButton type="primary" :loading="loading" color="#f97316" @click="handleSubmit">
          XÁC NHẬN ĐẶT HÀNG
        </NButton>
      </div>
    </NForm>
  </NModal>
</template>
