<template>
  <a-modal
    :width="800"
    :visible="props.isOpen"
    :title="!props.id ? 'Thêm Phiếu giảm giá' : props.isDetail ? 'Chi tiết Phiếu giảm giá' : 'Cập nhật Phiếu giảm giá'"
    @cancel="handleClickCancel"
  >
    <template #footer>
      <a-button @click="handleClickCancel">Hủy</a-button>
      <a-popconfirm content="Bạn chắc chắn muốn thao tác" @ok="handleClickOK">
        <a-button type="primary" :disabled="props.isDetail">Xác nhận</a-button>
      </a-popconfirm>
    </template>

    <div class="container">
      <a-form :model="detailVoucher" auto-label-width>
        <a-row class="modal">
          <!-- Cột bên trái -->
          <a-col class="left" :span="12" :xs="24" :sm="12">
            <a-form-item label="Mã">
              <a-input v-model="detailVoucher.code" placeholder="Nhập mã" />
            </a-form-item>

            <a-form-item label="Loại">
              <a-select v-model="detailVoucher.typeVoucher" placeholder="Chọn loại">
                <a-option value="PERCENTAGE">PERCENTAGE</a-option>
                <a-option value="VND">VND</a-option>
              </a-select>
            </a-form-item>

            <a-form-item label="Giá trị">
              <a-input v-model="detailVoucher.decreaseUnit" placeholder="Nhập giá trị" />
            </a-form-item>

            <a-form-item label="Điều kiện">
              <a-input v-model="detailVoucher.conditionOfUse" placeholder="Nhập điều kiện áp dụng" />
            </a-form-item>

            <a-form-item label="Ngày bắt đầu">
              <a-date-picker v-model="detailVoucher.startTime" show-time format="DD/MM/YYYY HH:mm:ss" placeholder="Chọn ngày và giờ" />
            </a-form-item>
          </a-col>

          <!-- Cột bên phải -->
          <a-col class="right" :span="12">
            <a-form-item label="Tên">
              <a-input v-model="detailVoucher.name" placeholder="Nhập tên" />
            </a-form-item>

            <a-form-item label="Phạm vi">
              <a-select v-model="detailVoucher.voucherScope" placeholder="Chọn phạm vi">
                <a-option value="PUBLIC">PUBLIC</a-option>
                <a-option value="PERSONAL">PERSONAL</a-option>
              </a-select>
            </a-form-item>

            <a-form-item label="Email Khách Hàng" name="customerEmail">
              <a-select
                v-model:value="detailVoucher.customerEmail"
                mode="multiple"
                placeholder="Chọn hoặc nhập email"
                :options="emailOptions"
                allowClear
                showSearch
                :filterOption="(input, option) => option.label.toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>

            <a-form-item label="Giá trị tối đa">
              <a-input v-model="detailVoucher.maximumReduction" placeholder="Nhập giá trị tối đa" />
            </a-form-item>

            <a-form-item label="Ngày kết thúc">
              <a-date-picker v-model="detailVoucher.endTime" show-time format="DD/MM/YYYY HH:mm:ss" placeholder="Chọn ngày và giờ" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ADVoucherResponse, getVoucherById } from '@/service/api/admin/product/api.voucher'
import { Ref, ref, watch } from 'vue'

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  isDetail: boolean
}>()

// const rules = {
//   code: [{ required: true, message: 'Vui lòng nhập mã!' }],
//   name: [{ required: true, message: 'Vui lòng nhập tên!' }],
//   typeVoucher: [{ required: true, message: 'Vui lòng chọn loại!' }],
//   decreaseUnit: [{ required: true, message: 'Vui lòng nhập giá trị!' }],
//   voucherScope: [{ required: true, message: 'Vui lòng chọn phạm vi!' }],
//   customerEmail: [
//     { required: true, message: 'Vui lòng nhập email!' },
//     { type: 'email', message: 'Email không hợp lệ!' },
//   ],
//   maximumReduction: [{ required: true, message: 'Vui lòng nhập giá trị tối đa!' }],
//   startTime: [{ required: true, message: 'Vui lòng chọn ngày bắt đầu!' }],
//   endTime: [
//     { required: true, message: 'Vui lòng chọn ngày kết thúc!' },
//     {
//       validator: (_, value) => {
//         if (!value || !detailVoucher.value.startTime) return Promise.resolve()
//         if (dayjs(value).isAfter(detailVoucher.value.startTime)) {
//           return Promise.resolve()
//         }
//         return Promise.reject('Ngày kết thúc phải sau ngày bắt đầu!')
//       },
//     },
//   ],
//   conditionOfUse: [{ required: true, message: 'Vui lòng nhập điều kiện!' }],
// }
const emailOptions = ref([
  { value: 'john.doe@example.com', label: 'John Doe (john.doe@example.com)' },
  { value: 'jane.smith@example.com', label: 'Jane Smith (jane.smith@example.com)' },
  { value: 'alice.wong@example.com', label: 'Alice Wong (alice.wong@example.com)' },
  { value: 'bob.lee@example.com', label: 'Bob Lee (bob.lee@example.com)' },
])
const emit = defineEmits(['success', 'close'])

const detailVoucher: Ref<ADVoucherResponse> = ref({
  id: '',
  code: '',
  name: '',
  typeVoucher: 'PERCENTAGE',
  voucherScope: 'PUBLIC',
  customerEmail: '',
  decreaseUnit: 0,
  maximumReduction: 0,
  startTime: Date.now(), // hoặc 0
  endTime: Date.now(), // hoặc 0
  conditionOfUse: 0,
})

const fetchDetailVoucher = async () => {
  const res = await getVoucherById(props.id as string)

  detailVoucher.value = res.data
}

const resetField = () => {
  detailVoucher.value = {
    id: '',
    code: '',
    name: '',
    typeVoucher: 'PERCENTAGE',
    voucherScope: 'PUBLIC',
    customerEmail: '',
    decreaseUnit: 0,
    maximumReduction: 0,
    startTime: Date.now(), // hoặc 0
    endTime: Date.now(), // hoặc 0
    conditionOfUse: 0,
  }
}

watch(
  () => props.id,
  (newId) => {
    if (newId) fetchDetailVoucher()
    else resetField()
  }
)

const handleClickCancel = () => {
  emit('close')
}

const handleClickOK = async () => {}
</script>

<style scoped>
.container {
  max-height: 400px;
  overflow-y: auto;
}

.modal {
  padding-right: 40px;
  overflow: hidden !important;
}

.left {
  padding-right: 20px;
}

.right {
  padding-left: 20px;
}
</style>
