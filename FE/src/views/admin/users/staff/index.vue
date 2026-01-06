<script setup lang="tsx">
import { h, onMounted, reactive, ref, watch } from 'vue'
import type { UploadFileInfo } from 'naive-ui'
import {
  NAvatar,
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NModal,
  NPagination,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
  NTooltip,
  NUpload,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import {
  createStaff,
  getAllStaff,
  updateStaffStatus,
} from '@/service/api/admin/users/staff.api'
import type { CreateStaffRequest, ParamGetStaff, StaffResponse } from '@/service/api/admin/users/staff.api'
import {
  getCommunes,
  getDistricts,
  getProvinces,
} from '@/service/api/admin/geo.api'

// --- QUAN TRỌNG: Import bộ dịch Mã -> Tên ---
import {
  getDistrictName,
  getProvinceName,
  getWardName,
  initLocationData,
} from '@/service/api/admin/users/location-service'

// ================= STATE =================
const message = useMessage()
const tableData = ref<StaffResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filter = reactive<ParamGetStaff>({
  code: '',
  fullName: '',
  role: '',
  email: '',
  phone: '',
  status: null,
})

const checkedRowKeys = ref<(string | number)[]>([])

// ================= MODAL =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalRow = ref<StaffResponse | null>(null)

const formData = reactive<CreateStaffRequest>({
  fullName: '',
  role: undefined as any,
  birthday: null as any,
  citizenIdentifyCard: '',
  hometown: '',
  avatarUrl: '',
  phone: '',
  email: '',
  gender: true,
  provinceCode: null as any,
  districtCode: null as any,
  communeCode: null as any,
})

const avatarFile = ref<File | null>(null)

// ================= GEO =================
const provinces = ref<{ label: string, value: string }[]>([])
const districts = ref<{ label: string, value: string }[]>([])
const communes = ref<{ label: string, value: string }[]>([])

async function loadProvinces() {
  const data = await getProvinces()
  provinces.value = data.map((p: any) => ({
    label: p.name,
    value: p.code,
  }))
}

watch(
  () => formData.provinceCode,
  async (newCode) => {
    formData.districtCode = null as any
    formData.communeCode = null as any
    districts.value = []
    communes.value = []
    if (newCode) {
      const data = await getDistricts(newCode)
      districts.value = data.map((d: any) => ({
        label: d.name,
        value: d.code,
      }))
    }
  },
)

watch(
  () => formData.districtCode,
  async (newCode) => {
    formData.communeCode = null as any
    communes.value = []
    if (newCode) {
      const data = await getCommunes(newCode)
      communes.value = data.map((c: any) => ({
        label: c.name,
        value: c.code,
      }))
    }
  },
)

// ================= API =================
async function fetchStaff() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...filter,
      status: (filter.status === null || filter.status === '') ? undefined : filter.status,
    }
    const res = await getAllStaff(params)
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (err) {
    // message.error('Lỗi tải danh sách nhân viên')
  }
  finally {
    loading.value = false
  }
}

function resetFilters() {
  filter.code = ''
  filter.fullName = ''
  filter.status = null
  filter.phone = ''
  filter.email = ''
  currentPage.value = 1
  fetchStaff()
}

// === QUAN TRỌNG: Load dữ liệu địa chính khi vào trang ===
onMounted(async () => {
  await initLocationData() // Tải bộ từ điển Mã -> Tên
  loadProvinces()
  fetchStaff()
})

// === HÀM CHUYỂN ĐỔI: Mã -> Tên ===
function resolveFullAddress(row: StaffResponse) {
  // Hàm lấy tên từ mã code (được import từ location-service)
  const province = getProvinceName(row.provinceCode)
  const district = getDistrictName(row.districtCode)
  const ward = getWardName(row.communeCode)
  const detail = row.hometown

  // Ghép lại thành chuỗi: "Số 1, Phường A, Quận B, Tỉnh C"
  return [detail, ward, district, province]
    .filter(Boolean) // Loại bỏ các giá trị null/rỗng
    .join(', ')
}

// ================= CRUD =================
function openModal(mode: 'add' | 'edit', row?: StaffResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    modalRow.value = row
    Object.assign(formData, {
      ...row,
      birthday: row.birthday ? new Date(row.birthday).getTime() : null,
    })

    // Load lại dữ liệu quận/huyện cho dropdown khi sửa
    if (row.provinceCode) {
      getDistricts(row.provinceCode).then((data) => {
        districts.value = data.map((d: any) => ({ label: d.name, value: d.code }))
      })
    }
    if (row.districtCode) {
      getCommunes(row.districtCode).then((data) => {
        communes.value = data.map((c: any) => ({ label: c.name, value: c.code }))
      })
    }
  }
  else {
    modalRow.value = null
    Object.assign(formData, {
      fullName: '',
      role: null,
      birthday: null,
      citizenIdentifyCard: '',
      hometown: '',
      avatarUrl: '',
      phone: '',
      email: '',
      gender: true,
      provinceCode: null,
      districtCode: null,
      communeCode: null,
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveStaff() {
  try {
    await createStaff(formData)
    message.success(modalMode.value === 'add' ? 'Thêm nhân viên thành công' : 'Cập nhật thành công')
    closeModal()
    fetchStaff()
  }
  catch (e: any) {
    message.error(e.message || 'Lưu thất bại')
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchStaff()
}

function handleUploadChange(data: { file: UploadFileInfo }) {
  if (data.file.status === 'removed')
    return

  if (data.file.file) {
    const rawFile = data.file.file as File
    if (!rawFile.type.startsWith('image/')) {
      message.error('Vui lòng chỉ chọn file ảnh!')
      return
    }
    const previewUrl = URL.createObjectURL(rawFile)
    formData.avatarUrl = previewUrl
    avatarFile.value = rawFile
    message.success('Đã chọn ảnh thành công')
  }
}

function removeAvatar() {
  formData.avatarUrl = ''
  avatarFile.value = null
  message.info('Đã gỡ ảnh đại diện')
}

// ================= ACTIONS =================
async function handleStatusChange(row: StaffResponse, val: boolean) {
  try {
    loading.value = true
    await updateStaffStatus(row.id, val ? 'ACTIVE' : 'INACTIVE')
    message.success('Cập nhật trạng thái thành công')
    row.status = val ? 'ACTIVE' : 'INACTIVE'
  }
  catch {
    message.error('Cập nhật trạng thái thất bại')
    fetchStaff()
  }
  finally {
    loading.value = false
  }
}

function openCamera() {
  message.info('Tính năng quét CCCD đang phát triển')
}

// ================= TABLE COLUMNS =================
const columns = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    render: (_, index) => index + 1 + (currentPage.value - 1) * pageSize.value,
  },
  {
    title: 'Nhân viên',
    key: 'fullName',
    width: 250,
    render: (row: StaffResponse) =>
      h(
        NSpace,
        { align: 'center', justify: 'start' },
        {
          default: () => [
            h(NAvatar, { size: 'small', round: true, src: row.avatarUrl || undefined, fallbackSrc: 'https://via.placeholder.com/150' }),
            h('div', [
              h('div', { style: 'font-weight: 500' }, row.fullName),
              h('div', { style: 'font-size: 12px; color: #888' }, row.code),
            ]),
          ],
        },
      ),
  },
  {
    title: 'Chức vụ',
    key: 'role',
    align: 'center',
    width: 120,
    render: (row) => {
      const type = row.role === 'QUAN_LY' ? 'warning' : 'info'
      const text = row.role === 'QUAN_LY' ? 'Quản lý' : 'Nhân viên'
      return h(NTag, { type, size: 'small', bordered: false }, { default: () => text })
    },
  },
  {
    title: 'Giới tính',
    key: 'gender',
    width: 100,
    align: 'center',
    render: (row: StaffResponse) => h(NTag, {
      type: row.gender ? 'success' : 'error',
      size: 'small',
      bordered: false,
    }, {
      default: () => (row.gender ? 'Nam' : 'Nữ'),
    }),
  },
  {
    title: 'Thông tin liên hệ',
    key: 'contact',
    width: 250,
    render: row =>
      h('div', [
        h('div', { class: 'flex items-center gap-1' }, [
          h(Icon, { icon: 'carbon:phone', class: 'text-gray-400' }),
          h('span', row.phone),
        ]),
        h('div', { class: 'flex items-center gap-1' }, [
          h(Icon, { icon: 'carbon:email', class: 'text-gray-400' }),
          h('span', row.email),
        ]),
      ]),
  },
  // --- CỘT ĐỊA CHỈ: Sử dụng hàm resolveFullAddress để hiển thị Tên ---
  {
    title: 'Địa chỉ',
    key: 'address',
    width: 250,
    render: (row: StaffResponse) => {
      const fullAddress = resolveFullAddress(row)

      if (!fullAddress)
        return h('span', { class: 'text-gray-400 italic text-xs' }, 'Chưa cập nhật')

      return h(
        NTooltip,
        { trigger: 'hover', style: { maxWidth: '400px' } },
        {
          trigger: () => h(
            'div',
            { class: 'truncate cursor-help' },
            fullAddress,
          ),
          default: () => fullAddress,
        },
      )
    },
  },
  // -------------------------------------------------------------------
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 120,
    render(row: StaffResponse) {
      return (
        <NSwitch
          size="small"
          value={row.status === 'ACTIVE'}
          onUpdateValue={(val: boolean) => handleStatusChange(row, val)}
        >
        </NSwitch>
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 100,
    fixed: 'right',
    render(row: StaffResponse) {
      return (
        <NTooltip trigger="hover">
          {{
            trigger: () => (
              <NButton
                size="small"
                secondary
                type="warning"
                circle
                onClick={() => openModal('edit', row)}
              >
                <NIcon><Icon icon="carbon:edit" /></NIcon>
              </NButton>
            ),
            default: () => 'Sửa thông tin',
          }}
        </NTooltip>
      )
    },
  },
]
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-red-500">
            <Icon icon="icon-park-outline:address-book" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Nhân viên
          </span>
        </NSpace>
        <span>Quản lý danh sách nhân viên và phân quyền hệ thống</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-md mb-4">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton
                size="large" circle secondary type="primary"
                class="transition-all duration-200 hover:scale-110 hover:shadow-md" @click="resetFilters"
              >
                <NIcon size="24">
                  <Icon icon="carbon:filter-reset" />
                </NIcon>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </template>

      <NForm label-placement="top" :model="filter">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-2">
          <NFormItem label="Tìm kiếm chung">
            <NInput v-model:value="filter.fullName" placeholder="Nhập tên hoặc mã nhân viên..." clearable>
              <template #prefix>
                <Icon icon="carbon:search" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Số điện thoại / Email">
            <NInput v-model:value="filter.phone" placeholder="Nhập SĐT hoặc Email..." clearable />
          </NFormItem>

          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filter.status" name="radiogroup">
              <NSpace>
                <NRadio value="">
                  Tất cả
                </NRadio>
                <NRadio value="ACTIVE">
                  Đang làm việc
                </NRadio>
                <NRadio value="INACTIVE">
                  Đã nghỉ
                </NRadio>
              </NSpace>
            </NRadioGroup>
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <NCard title="Danh sách nhân viên" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="openModal('add')"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Thêm mới
              </span>
            </NButton>

            <NButton type="info" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:layers-external" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Nhập Excel
              </span>
            </NButton>

            <NButton type="success" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="file-icons:microsoft-excel" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Xuất danh sách
              </span>
            </NButton>

            <NButton type="warning" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="icon-park-outline:align-text-bottom" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Tải mẫu nhập
              </span>
            </NButton>

            <NButton
              type="info"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="fetchStaff"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tải lại
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="tableData"
        :row-key="(row) => row.id" :loading="loading" :pagination="false" striped
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="currentPage" v-model:page-size="pageSize" :item-count="total"
          :page-sizes="[5, 10, 20, 50]" show-size-picker @update:page="handlePageChange"
          @update:page-size="(val) => { pageSize = val; currentPage = 1; fetchStaff() }"
        />
      </div>
    </NCard>

    <NModal
      v-model:show="showModal"
      :title="modalMode === 'add' ? 'Thêm nhân viên mới' : 'Cập nhật thông tin nhân viên'" preset="card"
      class="w-[90%] md:w-[800px]"
    >
      <NForm
        label-placement="left" :model="formData" label-width="140" require-mark-placement="right-hanging"
        size="large"
      >
        <div class="flex justify-center mb-6">
          <div class="flex flex-col items-center gap-3">
            <NUpload
              accept="image/*"
              :show-file-list="false"
              :default-upload="false"
              @change="handleUploadChange"
            >
              <div class="group relative cursor-pointer overflow-hidden rounded-full border-4 border-white shadow-lg transition-transform hover:scale-105">
                <NAvatar
                  :key="formData.avatarUrl"
                  :size="120"
                  class="bg-gray-100 flex items-center justify-center object-cover"
                  :src="formData.avatarUrl || undefined"
                >
                  <template #default>
                    <NIcon v-if="!formData.avatarUrl" size="60" class="text-gray-300">
                      <Icon icon="carbon:user-avatar-filled-alt" />
                    </NIcon>
                  </template>
                </NAvatar>

                <div class="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                  <div class="flex flex-col items-center text-white">
                    <NIcon size="24">
                      <Icon icon="carbon:camera" />
                    </NIcon>
                    <span class="text-[10px] font-medium uppercase tracking-wider mt-1">Đổi ảnh</span>
                  </div>
                </div>
              </div>
            </NUpload>

            <div v-if="formData.avatarUrl" class="flex gap-2">
              <NButton size="tiny" type="error" secondary class="rounded-full px-3" @click="removeAvatar">
                <template #icon>
                  <Icon icon="carbon:trash-can" />
                </template>
                Gỡ ảnh
              </NButton>
            </div>

            <span v-else class="text-gray-400 text-xs italic">
              Chấp nhận: .jpg, .png, .jpeg
            </span>
          </div>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <NFormItem label="Họ và tên" required>
              <NInput v-model:value="formData.fullName" placeholder="VD: Nguyễn Văn A" />
            </NFormItem>
            <NFormItem label="Giới tính">
              <NRadioGroup v-model:value="formData.gender">
                <NSpace>
                  <NRadio :value="true">
                    Nam
                  </NRadio>
                  <NRadio :value="false">
                    Nữ
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <NFormItem label="Chức vụ" required>
              <NSelect
                v-model:value="formData.role" :options="[
                  { label: 'Quản lý', value: 'QUAN_LY' },
                  { label: 'Nhân viên', value: 'NHAN_VIEN' },
                ]" placeholder="Chọn chức vụ"
              />
            </NFormItem>
            <NFormItem label="Ngày sinh">
              <NDatePicker v-model:value="formData.birthday" type="date" placeholder="Ngày/Tháng/Năm" class="w-full" />
            </NFormItem>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <NFormItem label="Số điện thoại" required>
              <NInput v-model:value="formData.phone" placeholder="09xx..." />
            </NFormItem>
            <NFormItem label="Email" required>
              <NInput v-model:value="formData.email" placeholder="example@gmail.com" />
            </NFormItem>
          </div>

          <NFormItem label="CCCD/CMND" required>
            <NInput v-model:value="formData.citizenIdentifyCard" placeholder="Số thẻ căn cước...">
              <template #suffix>
                <NTooltip trigger="hover">
                  <template #trigger>
                    <NButton text type="info" @click="openCamera">
                      <Icon icon="carbon:qr-code" width="20" />
                    </NButton>
                  </template>
                  Quét mã QR CCCD
                </NTooltip>
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Địa chỉ">
            <div class="flex flex-col gap-2 w-full">
              <div class="grid grid-cols-3 gap-2">
                <NSelect v-model:value="formData.provinceCode" :options="provinces" placeholder="Tỉnh/TP" filterable />
                <NSelect
                  v-model:value="formData.districtCode" :options="districts" placeholder="Quận/Huyện" filterable
                  :disabled="!formData.provinceCode"
                />
                <NSelect
                  v-model:value="formData.communeCode" :options="communes" placeholder="Xã/Phường" filterable
                  :disabled="!formData.districtCode"
                />
              </div>
              <NInput v-model:value="formData.hometown" placeholder="Số nhà, tên đường cụ thể..." />
            </div>
          </NFormItem>
        </div>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="closeModal">
            Đóng
          </NButton>
          <NButton type="primary" icon-placement="right" @click="saveStaff">
            {{ modalMode === "add" ? "Thêm mới" : "Lưu thay đổi" }}
            <template #icon>
              <Icon icon="carbon:save" />
            </template>
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
