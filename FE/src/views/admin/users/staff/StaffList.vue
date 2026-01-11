<script setup lang="tsx">
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router' // Import Router
import {
  NAvatar,
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSpace,
  NSwitch,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'

// API
import { getAllStaff, updateStaffStatus } from '@/service/api/admin/users/staff.api'
import type { ParamGetStaff, StaffResponse } from '@/service/api/admin/users/staff.api'
import { getDistrictName, getProvinceName, getWardName, initLocationData } from '@/service/api/admin/users/location-service'

const router = useRouter() // Khởi tạo router
const message = useMessage()

// State
const tableData = ref<StaffResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const checkedRowKeys = ref<(string | number)[]>([])

const filter = reactive({
  fullName: '',
  keyword: '',
  status: null as string | null,
})

// --- API FETCH ---
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

// --- NAVIGATION (THAY ĐỔI QUAN TRỌNG) ---
function navigateToAdd() {
  router.push('/users/staff/add')
}

function navigateToEdit(row: StaffResponse) {
  router.push(`/users/staff/edit/${row.id}`)
}

// --- LOGIC KHÁC ---
function resolveFullAddress(row: StaffResponse) {
  const province = getProvinceName(row.provinceCode)
  const district = getDistrictName(row.districtCode)
  const ward = getWardName(row.communeCode)
  const detail = row.hometown
  return [detail, ward, district, province].filter(Boolean).join(', ')
}

async function handleStatusChange(row: StaffResponse) {
  if (!row.id)
    return
  try {
    loading.value = true
    const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await updateStaffStatus(row.id, newStatus)
    message.success(`Đã cập nhật trạng thái: ${row.fullName}`)
    row.status = newStatus
  }
  catch (e) {
    message.error('Cập nhật trạng thái thất bại')
    fetchStaff()
  }
  finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchStaff()
}

onMounted(async () => {
  await initLocationData()
  fetchStaff()
})

// --- COLUMNS ---
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
    render: (row: StaffResponse) => h(NSpace, { align: 'center', justify: 'start' }, {
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
    render: (row: StaffResponse) => h(NTag, { type: row.gender ? 'success' : 'error', size: 'small', bordered: false }, { default: () => (row.gender ? 'Nam' : 'Nữ') }),
  },
  {
    title: 'Liên hệ',
    key: 'contact',
    width: 250,
    render: (row: StaffResponse) => h('div', [
      h('div', { class: 'flex items-center gap-1' }, [h(Icon, { icon: 'carbon:phone', class: 'text-gray-400' }), h('span', row.phone)]),
      h('div', { class: 'flex items-center gap-1' }, [h(Icon, { icon: 'carbon:email', class: 'text-gray-400' }), h('span', row.email)]),
    ]),
  },
  {
    title: 'Địa chỉ',
    key: 'address',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: (row: StaffResponse) => resolveFullAddress(row) || h('span', { class: 'text-gray-400 italic text-xs' }, 'Chưa cập nhật'),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 120,
    fixed: 'right',
    render(row: StaffResponse) {
      return h(NPopconfirm, { onPositiveClick: () => handleStatusChange(row) }, {
        trigger: () => h(NSwitch, { value: row.status === 'ACTIVE', size: 'small', disabled: loading.value }),
        default: () => `Bạn có chắc muốn ${row.status === 'ACTIVE' ? 'ngưng hoạt động' : 'kích hoạt'} nhân viên này?`,
      })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 80,
    fixed: 'right',
    render(row: StaffResponse) {
      return h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h(NButton, { size: 'small', secondary: true, type: 'warning', circle: true, onClick: () => navigateToEdit(row) }, { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) }),
        default: () => 'Sửa thông tin',
      })
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
          <NButton size="large" circle secondary type="primary" @click="resetFilters">
            <NIcon size="24">
              <Icon icon="carbon:filter-reset" />
            </NIcon>
          </NButton>
        </div>
      </template>
      <NForm label-placement="top" :model="filter">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-2">
          <NFormItem label="Tìm kiếm chung">
            <NInput v-model:value="filter.fullName" placeholder="Nhập tên nhân viên..." clearable @input="handleSearch" />
          </NFormItem>
          <NFormItem label="Số điện thoại / Email">
            <NInput v-model:value="filter.keyword" placeholder="Nhập SĐT hoặc Email..." clearable @input="handleSearch" />
          </NFormItem>
          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filter.status" @update:value="handleSearch">
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
            <NButton type="primary" secondary class="rounded-full px-3" @click="navigateToAdd">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              Thêm mới
            </NButton>
            <NButton type="info" secondary class="rounded-full px-3" @click="fetchStaff">
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              Tải lại
            </NButton>
          </NSpace>
        </div>
      </template>
      <NDataTable v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="tableData" :row-key="(row) => row.id" :loading="loading" :pagination="false" :scroll-x="1200" striped />
      <div class="flex justify-end mt-4">
        <NPagination v-model:page="currentPage" v-model:page-size="pageSize" :item-count="total" :page-sizes="[5, 10, 20, 50]" show-size-picker @update:page="handlePageChange" @update:page-size="(val) => { pageSize = val; currentPage = 1; fetchStaff() }" />
      </div>
    </NCard>
  </div>
</template>
