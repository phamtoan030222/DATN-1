<script setup lang="tsx">
import { h, onMounted, reactive, ref, watch } from 'vue'
import type { FormInst, FormRules, UploadFileInfo } from 'naive-ui'
import {
  NAvatar,
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NModal,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
  NTooltip,
  NUpload,
  useDialog,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'

// --- IMPORT API ---
import {
  createStaff,
  getAllStaff,
  updateStaff,
  updateStaffStatus,
} from '@/service/api/admin/users/staff.api'
import type { CreateStaffRequest, ParamGetStaff, StaffResponse } from '@/service/api/admin/users/staff.api'

// --- IMPORT GEO & LOCATION SERVICE ---
import {
  getCommunes,
  getDistricts,
  getProvinces,
} from '@/service/api/admin/geo.api'
import {
  getDistrictName,
  getProvinceName,
  getWardName,
  initLocationData,
} from '@/service/api/admin/users/location-service'

// ================= STATE =================
const message = useMessage()
const dialog = useDialog()
const formRef = ref<FormInst | null>(null)
const tableData = ref<StaffResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// Biến cờ để chặn watcher khi đang load dữ liệu Edit
const isUpdating = ref(false)

// Filter
const filter = reactive({
  fullName: '',
  keyword: '',
  status: null as string | null,
})

const checkedRowKeys = ref<(string | number)[]>([])

// ================= MODAL STATE =================
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

// Rules
const rules: FormRules = {
  fullName: [{ required: true, message: 'Vui lòng nhập họ tên', trigger: ['blur', 'input'] }],
  role: [{ required: true, message: 'Vui lòng chọn chức vụ', trigger: ['blur', 'change'] }],
  phone: [{ required: true, message: 'Vui lòng nhập số điện thoại', trigger: ['blur', 'input'] }],
  email: [{ required: true, message: 'Vui lòng nhập email', trigger: ['blur', 'input'] }],
  citizenIdentifyCard: [{ required: true, message: 'Vui lòng nhập CCCD', trigger: ['blur', 'input'] }],
}

// ================= GEO LOGIC =================
const provinces = ref<{ label: string, value: string }[]>([])
const districts = ref<{ label: string, value: string }[]>([])
const communes = ref<{ label: string, value: string }[]>([])

async function loadProvinces() {
  const data = await getProvinces()
  provinces.value = data.map((p: any) => ({ label: p.name, value: p.code }))
}

// Watchers: Có chặn bằng biến isUpdating
watch(
  () => formData.provinceCode,
  async (newCode) => {
    if (isUpdating.value)
      return // Chặn reset khi đang edit

    formData.districtCode = null as any
    formData.communeCode = null as any
    districts.value = []
    communes.value = []
    if (newCode) {
      const data = await getDistricts(newCode)
      districts.value = data.map((d: any) => ({ label: d.name, value: d.code }))
    }
  },
)

watch(
  () => formData.districtCode,
  async (newCode) => {
    if (isUpdating.value)
      return // Chặn reset khi đang edit

    formData.communeCode = null as any
    communes.value = []
    if (newCode) {
      const data = await getCommunes(newCode)
      communes.value = data.map((c: any) => ({ label: c.name, value: c.code }))
    }
  },
)

// ================= API FETCH =================
async function fetchStaff() {
  loading.value = true
  try {
    const params: ParamGetStaff = {
      page: currentPage.value,
      size: pageSize.value,
      name: filter.fullName || undefined,
      key: filter.keyword || undefined,
      status: filter.status || undefined,
    }
    const res = await getAllStaff(params)
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (err) {
    message.error('Lỗi tải danh sách nhân viên')
  }
  finally {
    loading.value = false
  }
}

const handleSearch = debounce(() => {
  currentPage.value = 1
  fetchStaff()
}, 500)

function resetFilters() {
  filter.fullName = ''
  filter.status = null
  filter.keyword = ''
  currentPage.value = 1
  fetchStaff()
}

onMounted(async () => {
  await initLocationData()
  loadProvinces()
  fetchStaff()
})

function resolveFullAddress(row: StaffResponse) {
  const province = getProvinceName(row.provinceCode)
  const district = getDistrictName(row.districtCode)
  const ward = getWardName(row.communeCode)
  const detail = row.hometown
  return [detail, ward, district, province].filter(Boolean).join(', ')
}

// ================= CRUD OPERATIONS =================
async function openModal(mode: 'add' | 'edit', row?: StaffResponse) {
  modalMode.value = mode

  // Reset options
  districts.value = []
  communes.value = []

  if (mode === 'edit' && row) {
    modalRow.value = row
    isUpdating.value = true

    try {
      // 1. Gán các field KHÔNG phụ thuộc select
      Object.assign(formData, {
        fullName: row.fullName,
        role: row.role,
        birthday: row.birthday,
        citizenIdentifyCard: row.citizenIdentifyCard,
        hometown: row.hometown,
        avatarUrl: row.avatarUrl,
        phone: row.phone,
        email: row.email,
        gender: row.gender,
        provinceCode: null,
        districtCode: null,
        communeCode: null,
      })

      // ---------------------------------------------------------
      // SỬA ĐOẠN NÀY: Ép kiểu Number() cho các mã code
      // ---------------------------------------------------------

      // 2. Load tỉnh → set tỉnh
      if (row.provinceCode) {
        // Ép kiểu về Number để khớp với value trong options của NSelect
        formData.provinceCode = Number(row.provinceCode) as any

        const dts = await getDistricts(row.provinceCode)
        districts.value = dts.map(d => ({ label: d.name, value: d.code }))
      }

      // 3. Load huyện → set huyện
      if (row.districtCode) {
        // Ép kiểu về Number
        formData.districtCode = Number(row.districtCode) as any

        const cms = await getCommunes(row.districtCode)
        communes.value = cms.map(c => ({ label: c.name, value: c.code }))
      }

      // 4. Set xã
      if (row.communeCode) {
        // Ép kiểu về Number
        formData.communeCode = Number(row.communeCode) as any
      }
    }
    finally {
      setTimeout(() => (isUpdating.value = false), 0)
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
  formRef.value?.validate((errors) => {
    if (errors)
      return

    dialog.warning({
      title: 'Xác nhận',
      content: modalMode.value === 'add'
        ? 'Bạn có chắc chắn muốn thêm nhân viên này?'
        : 'Bạn có chắc chắn muốn cập nhật thông tin nhân viên này?',
      positiveText: 'Đồng ý',
      negativeText: 'Hủy',
      onPositiveClick: async () => {
        try {
          if (modalMode.value === 'add') {
            await createStaff(formData)
            message.success('Thêm nhân viên thành công')
          }
          else {
            if (!modalRow.value?.id) {
              message.error('Lỗi: Không tìm thấy ID')
              return
            }
            await updateStaff(modalRow.value.id, formData)
            message.success('Cập nhật thành công')
          }
          closeModal()
          fetchStaff()
        }
        catch (e: any) {
          message.error(e.response?.data?.message || 'Thao tác thất bại')
        }
      },
    })
  })
}

async function handleStatusChange(row: StaffResponse) {
  if (!row.id)
    return
  try {
    loading.value = true
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    // Gọi API update status (đảm bảo file api đã import đúng hàm)
    await updateStaffStatus(row.id, newStatus)

    message.success(`Đã cập nhật trạng thái: ${row.fullName}`)
    row.status = newStatus // Cập nhật UI ngay
  }
  catch (e) {
    console.error(e)
    message.error('Cập nhật trạng thái thất bại')
    fetchStaff() // Revert UI nếu lỗi
  }
  finally {
    loading.value = false
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
}

function openCamera() {
  message.info('Tính năng đang phát triển')
}

// ================= TABLE COLUMNS =================
const columns = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    fixed: 'left',
    align: 'center',
    render: (_, index) => index + 1 + (currentPage.value - 1) * pageSize.value,
  },
  {
    title: 'Nhân viên',
    key: 'fullName',
    width: 250,
    fixed: 'left',
    render: (row: StaffResponse) =>
      h(NSpace, { align: 'center', justify: 'start' }, {
        default: () => [
          h(NAvatar, { size: 'small', round: true, src: row.avatarUrl || undefined, fallbackSrc: 'https://via.placeholder.com/150' }),
          h('div', [
            h('div', { style: 'font-weight: 500' }, row.fullName),
            h('div', { style: 'font-size: 12px; color: #888' }, row.code),
          ]),
        ],
      }),
  },
  {
    title: 'Chức vụ',
    key: 'role',
    align: 'center',
    width: 120,
    render: (row: StaffResponse) => {
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
    }, { default: () => (row.gender ? 'Nam' : 'Nữ') }),
  },
  {
    title: 'Thông tin liên hệ',
    key: 'contact',
    width: 250,
    render: (row: StaffResponse) =>
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
  {
    title: 'Địa chỉ',
    key: 'address',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: (row: StaffResponse) => {
      const fullAddress = resolveFullAddress(row)
      return fullAddress || h('span', { class: 'text-gray-400 italic text-xs' }, 'Chưa cập nhật')
    },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 120,
    fixed: 'right',
    render(row: StaffResponse) {
      return h(
        NPopconfirm,
        {
          onPositiveClick: () => handleStatusChange(row),
          positiveText: 'Đồng ý',
          negativeText: 'Hủy',
        },
        {
          trigger: () => h(NSwitch, {
            value: row.status === 'ACTIVE',
            size: 'small',
            disabled: loading.value,
          }),
          default: () => `Bạn có chắc muốn ${row.status === 'ACTIVE' ? 'ngưng hoạt động' : 'kích hoạt'} nhân viên này?`,
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 80,
    fixed: 'right',
    render(row: StaffResponse) {
      return (
        <NTooltip trigger="hover">
          {{
            trigger: () => (
              <NButton size="small" secondary type="warning" circle onClick={() => openModal('edit', row)}>
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
          <span style="font-weight: 600; font-size: 24px">Quản lý Nhân viên</span>
        </NSpace>
        <span>Quản lý danh sách nhân viên và phân quyền hệ thống</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-md mb-4">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton size="large" circle secondary type="primary" class="transition-all duration-200 hover:scale-110 hover:shadow-md" @click="resetFilters">
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
            <NInput v-model:value="filter.fullName" placeholder="Nhập tên nhân viên..." clearable @input="handleSearch" @keydown.enter="handleSearch">
              <template #prefix>
                <Icon icon="carbon:search" />
              </template>
            </NInput>
          </NFormItem>
          <NFormItem label="Số điện thoại / Email">
            <NInput v-model:value="filter.keyword" placeholder="Nhập SĐT hoặc Email..." clearable @input="handleSearch" @keydown.enter="handleSearch" />
          </NFormItem>
          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filter.status" name="radiogroup" @update:value="handleSearch">
              <NSpace>
                <NRadio :value="null">
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
            <NButton type="primary" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out" @click="openModal('add')">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">Thêm mới</span>
            </NButton>
            <NButton type="info" secondary class="group rounded-full px-3 transition-all duration-300 ease-in-out" @click="fetchStaff">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">Tải lại</span>
            </NButton>
          </NSpace>
        </div>
      </template>
      <NDataTable v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="tableData" :row-key="(row) => row.id" :loading="loading" :pagination="false" :scroll-x="1200" striped />
      <div class="flex justify-end mt-4">
        <NPagination v-model:page="currentPage" v-model:page-size="pageSize" :item-count="total" :page-sizes="[5, 10, 20, 50]" show-size-picker @update:page="handlePageChange" @update:page-size="(val) => { pageSize = val; currentPage = 1; fetchStaff() }" />
      </div>
    </NCard>

    <NModal v-model:show="showModal" :title="modalMode === 'add' ? 'Thêm nhân viên mới' : 'Cập nhật thông tin nhân viên'" preset="card" class="w-[90%] md:w-[900px]">
      <div class="max-h-[600px] overflow-y-auto pr-4 custom-scrollbar">
        <NForm ref="formRef" label-placement="left" :model="formData" :rules="rules" label-width="120" require-mark-placement="right-hanging" size="medium">
          <NGrid :x-gap="100" :y-gap="12" cols="1 600:3">
            <NGridItem span="1 600:2">
              <NFormItem label="Họ và tên" path="fullName" required>
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
              <NFormItem label="Ngày sinh">
                <NDatePicker v-model:value="formData.birthday" type="date" placeholder="Ngày/Tháng/Năm" class="w-full" />
              </NFormItem>
            </NGridItem>

            <NGridItem span="1 600:1">
              <div class="flex flex-col items-center justify-center h-full gap-3 pb-4">
                <NUpload accept="image/*" :show-file-list="false" :default-upload="false" @change="handleUploadChange">
                  <div class="group relative cursor-pointer overflow-hidden rounded-full border-4 border-gray-100 shadow-lg transition-transform hover:scale-105">
                    <NAvatar :key="formData.avatarUrl" :size="120" class="bg-gray-50 flex items-center justify-center object-cover" :src="formData.avatarUrl || undefined">
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
                <div v-if="formData.avatarUrl">
                  <NButton size="tiny" type="error" secondary class="rounded-full px-3 me-20" @click="removeAvatar">
                    <template #icon>
                      <Icon icon="carbon:trash-can" />
                    </template> Gỡ ảnh
                  </NButton>
                </div>
              </div>
            </NGridItem>

            <NGridItem span="1 600:3">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2">
                <NFormItem label="Chức vụ" path="role" required>
                  <NSelect v-model:value="formData.role" :options="[{ label: 'Quản lý', value: 'QUAN_LY' }, { label: 'Nhân viên', value: 'NHAN_VIEN' }]" placeholder="Chọn chức vụ" />
                </NFormItem>

                <NFormItem label="Số điện thoại" path="phone" required>
                  <NInput v-model:value="formData.phone" placeholder="09xx..." />
                </NFormItem>

                <NFormItem label="Email" path="email" required>
                  <NInput v-model:value="formData.email" placeholder="example@gmail.com" />
                </NFormItem>

                <NFormItem label="CCCD/CMND" path="citizenIdentifyCard" required>
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
              </div>

              <NFormItem label="Địa chỉ" class="mt-2">
                <div class="flex flex-col gap-2 w-full">
                  <div class="grid grid-cols-3 gap-2">
                    <NSelect v-model:value="formData.provinceCode" :options="provinces" placeholder="Tỉnh/TP" filterable />
                    <NSelect v-model:value="formData.districtCode" :options="districts" placeholder="Quận/Huyện" filterable :disabled="!formData.provinceCode" />
                    <NSelect v-model:value="formData.communeCode" :options="communes" placeholder="Xã/Phường" filterable :disabled="!formData.districtCode" />
                  </div>
                  <NInput v-model:value="formData.hometown" placeholder="Số nhà, tên đường cụ thể..." />
                </div>
              </NFormItem>
            </NGridItem>
          </NGrid>
        </NForm>
      </div>

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

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: #f1f1f1; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #c1c1c1; border-radius: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #a8a8a8; }
</style>
