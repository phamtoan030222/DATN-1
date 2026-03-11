<script setup lang="tsx">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NModal,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTooltip,
  useDialog,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui'
import { Icon } from '@iconify/vue'
import {
  createBattery,
  getAllBattery,
  updateBattery,
  updateBatteryStatus,
} from '@/service/api/admin/product/battery.api'
import type { BatteryResponse, CreateBatteryRequest } from '@/service/api/admin/product/battery.api'

// ================= STATE =================
const message = useMessage()
const dialog = useDialog()
const formRef = ref<FormInst | null>(null)
const tableData = ref<BatteryResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchState = reactive({
  keyword: '',
  status: null as string | null,
})

// ================= ENUM OPTIONS =================
// Đúng với backend: [SMART, REVERSE, FAST, STANDARD, WIRELESS]
const technolyChargingOptions = [
  { label: 'Sạc tiêu chuẩn', value: 'STANDARD' },
  { label: 'Sạc nhanh', value: 'FAST' },
  { label: 'Sạc không dây', value: 'WIRELESS' },
  { label: 'Sạc thông minh', value: 'SMART' },
  { label: 'Sạc ngược', value: 'REVERSE' },
]

const technolyChargingLabel: Record<string, string> = {
  STANDARD: 'Tiêu chuẩn',
  FAST: 'Sạc nhanh',
  WIRELESS: 'Không dây',
  SMART: 'Thông minh',
  REVERSE: 'Sạc ngược',
}

// Đúng với backend enum TypeBattery
const typeBatteryOptions = [
  { label: 'Li-Ion', value: 'LI_ION' },
  { label: 'Li-Po', value: 'LI_PO' },
  { label: 'NiMH', value: 'NIMH' },
  { label: 'Solid State', value: 'SOLID_STATE' },
]

const typeBatteryLabel: Record<string, string> = {
  LI_ION: 'Li-Ion',
  LI_PO: 'Li-Po',
  NIMH: 'NiMH',
  SOLID_STATE: 'Solid State',
}

// ================= MODAL STATE =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')

// ⚠️ Dùng "type" để khớp với ADCreateBatteryRequest backend
const formData = reactive<CreateBatteryRequest & { id?: string }>({
  name: '',
  brand: '',
  type: '', // backend DTO field là "type"
  technolyCharging: '',
  capacity: 0,
  removeBattery: false,
})

const rules: FormRules = {
  name: [{ required: true, message: 'Vui lòng nhập tên Pin', trigger: ['blur', 'input'] }],
  brand: [{ required: true, message: 'Vui lòng nhập hãng sản xuất', trigger: ['blur', 'input'] }],
  type: [{ required: true, message: 'Vui lòng chọn loại pin', trigger: ['blur', 'change'] }],
  technolyCharging: [{ required: true, message: 'Vui lòng chọn công nghệ sạc', trigger: ['blur', 'change'] }],
  capacity: [
    {
      trigger: ['blur', 'input'],
      validator: (_rule, value) => {
        if (value === null || value === undefined || value === '')
          return new Error('Vui lòng nhập dung lượng pin')
        if (Number(value) <= 0)
          return new Error('Phải lớn hơn 0')
        return true
      },
    },
  ],
}

// ================= API CALL =================
async function fetchBatteries() {
  loading.value = true
  try {
    const res = await getAllBattery({
      page: currentPage.value,
      size: pageSize.value,
      key: searchState.keyword || undefined,
      status: searchState.status || undefined,
    })
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch {
    message.error('Không thể tải dữ liệu Pin')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchBatteries)

// ================= CRUD LOGIC =================
function openModal(mode: 'add' | 'edit', row?: BatteryResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    Object.assign(formData, {
      id: row.id,
      name: row.name,
      brand: row.brand,
      type: row.typeBattery, // response trả "typeBattery", map vào form "type"
      technolyCharging: row.technolyCharging,
      capacity: row.capacity,
      removeBattery: row.removeBattery,
    })
  }
  else {
    Object.assign(formData, {
      id: undefined,
      name: '',
      brand: '',
      type: '',
      technolyCharging: '',
      capacity: null,
      removeBattery: false,
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function saveBattery() {
  formRef.value?.validate((errors) => {
    if (errors)
      return

    dialog.success({
      title: 'Xác nhận',
      content: modalMode.value === 'add'
        ? 'Bạn có chắc chắn muốn thêm Pin này?'
        : 'Bạn có chắc chắn muốn cập nhật thông tin Pin này?',
      positiveText: 'Đồng ý',
      negativeText: 'Hủy',
      onPositiveClick: async () => {
        try {
          const payload: CreateBatteryRequest = {
            name: formData.name,
            brand: formData.brand,
            type: formData.type, // gửi đúng tên "type"
            technolyCharging: formData.technolyCharging,
            capacity: Number(formData.capacity),
            removeBattery: Boolean(formData.removeBattery),
          }

          if (modalMode.value === 'add') {
            await createBattery(payload)
            message.success('Thêm Pin thành công')
          }
          else {
            if (!formData.id)
              return
            await updateBattery(formData.id, payload)
            message.success('Cập nhật Pin thành công')
          }
          closeModal()
          fetchBatteries()
        }
        catch {
          message.error('Có lỗi xảy ra khi lưu Pin')
        }
      },
    })
  })
}

async function handleStatusChange(row: BatteryResponse) {
  if (!row.id)
    return
  try {
    loading.value = true
    await updateBatteryStatus(row.id, row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
    message.success(`Đã cập nhật trạng thái: ${row.name}`)
    fetchBatteries()
  }
  catch {
    message.error('Cập nhật trạng thái thất bại')
    loading.value = false
  }
}

function refreshTable() {
  searchState.keyword = ''
  searchState.status = null
  currentPage.value = 1
  fetchBatteries()
}

function handleSearch() {
  currentPage.value = 1
  fetchBatteries()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchBatteries()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchBatteries()
}

// ================= TABLE COLUMNS =================
const columns: DataTableColumns<BatteryResponse> = [
  {
    title: 'STT',
    key: 'index',
    width: 60,
    align: 'center',
    fixed: 'left',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Tên Pin',
    key: 'name',
    minWidth: 160,
    fixed: 'left',
    ellipsis: { tooltip: true },
    render: row => h('span', {
      class: 'text-gray-700 cursor-pointer hover:text-primary transition-colors',
      onClick: () => openModal('edit', row),
    }, row.name),
  },
  { title: 'Hãng', key: 'brand', width: 110 },
  {
    title: 'Loại pin',
    key: 'typeBattery',
    width: 110,
    align: 'center',
    render: row => typeBatteryLabel[row.typeBattery] ?? row.typeBattery ?? '---',
  },
  {
    title: 'Công nghệ sạc',
    key: 'technolyCharging',
    width: 140,
    render: row => technolyChargingLabel[row.technolyCharging] ?? row.technolyCharging ?? '---',
  },
  {
    title: 'Dung lượng',
    key: 'capacity',
    width: 130,
    align: 'center',
    render: row => row.capacity ? `${row.capacity} mAh` : '---',
  },
  {
    title: 'Tháo rời',
    key: 'removeBattery',
    width: 90,
    align: 'center',
    render: row => h('span', {
      class: row.removeBattery ? 'text-green-600 font-medium' : 'text-gray-400',
    }, row.removeBattery ? 'Có' : 'Không'),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    render(row) {
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
          default: () => `Bạn có chắc muốn ${row.status === 'ACTIVE' ? 'ngưng hoạt động' : 'kích hoạt'} pin này?`,
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 100,
    fixed: 'right',
    align: 'center',
    render(row) {
      return h('div', { class: 'flex justify-center' }, [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            secondary: true,
            type: 'warning',
            circle: true,
            class: 'transition-all duration-200 hover:scale-125 hover:shadow-md',
            onClick: () => openModal('edit', row),
          }, { icon: () => h(Icon, { icon: 'carbon:edit' }) }),
          default: () => 'Cập nhật thông tin',
        }),
      ])
    },
  },
]
</script>

<template>
  <div>
    <!-- Header -->
    <NCard class="mb-3 shadow-sm border-none">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="carbon:battery-half" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">
            Quản lý Pin
          </span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách pin và thông số kỹ thuật</span>
      </NSpace>
    </NCard>

    <!-- Bộ lọc -->
    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-sm mb-4 border border-gray-100">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton
                size="large"
                circle
                secondary
                type="primary"
                class="transition-all duration-200 hover:scale-110 hover:shadow-md"
                @click="refreshTable"
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

      <NForm label-placement="top">
        <NGrid :x-gap="24" :y-gap="12" cols="1 600:2 800:4">
          <NGridItem span="2">
            <NFormItem label="Tìm kiếm">
              <NInput
                v-model:value="searchState.keyword"
                placeholder="Tìm theo Tên, Hãng, Loại pin..."
                clearable
                @input="handleSearch"
                @keydown.enter="handleSearch"
              >
                <template #prefix>
                  <Icon icon="carbon:search" class="text-gray-400" />
                </template>
              </NInput>
            </NFormItem>
          </NGridItem>

          <NGridItem span="2">
            <NFormItem label="Trạng thái">
              <NRadioGroup v-model:value="searchState.status" name="status-group" @update:value="handleSearch">
                <NSpace>
                  <NRadio :value="null">
                    Tất cả
                  </NRadio>
                  <NRadio value="ACTIVE">
                    Hoạt động
                  </NRadio>
                  <NRadio value="INACTIVE">
                    Ngưng hoạt động
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <!-- Bảng dữ liệu -->
    <NCard title="Danh sách Pin" class="border border-gray-100 rounded-2xl shadow-sm">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary"
              secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              @click="openModal('add')"
            >
              <template #icon>
                <NIcon size="20">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Thêm mới
              </span>
            </NButton>

            <NButton
              type="info"
              secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              @click="refreshTable"
            >
              <template #icon>
                <NIcon size="20">
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
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        :scroll-x="1000"
        striped
        :bordered="false"
        class="rounded-lg overflow-hidden"
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="total"
          :page-sizes="[5, 10, 20, 50]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </NCard>

    <!-- Modal Thêm / Sửa -->
    <NModal
      v-model:show="showModal"
      preset="card"
      style="width: 640px"
      :title="modalMode === 'add' ? 'Thêm mới Pin' : 'Cập nhật Pin'"
      :bordered="false"
      size="huge"
      class="shadow-2xl rounded-2xl"
      :closable="false"
    >
      <div class="max-h-[560px] overflow-y-auto pr-4 custom-scrollbar">
        <NForm
          ref="formRef"
          label-placement="top"
          :model="formData"
          :rules="rules"
        >
          <NFormItem label="Tên Pin" path="name" required>
            <NInput v-model:value="formData.name" placeholder="VD: Samsung 5000mAh" />
          </NFormItem>

          <NGrid cols="2" x-gap="24" y-gap="12">
            <NGridItem>
              <NFormItem label="Hãng sản xuất" path="brand" required>
                <NInput v-model:value="formData.brand" placeholder="VD: Samsung, LG, ATL" />
              </NFormItem>
            </NGridItem>

            <!-- Loại pin dùng NSelect vì backend là Enum TypeBattery -->
            <NGridItem>
              <NFormItem label="Loại pin" path="type" required>
                <NSelect
                  v-model:value="formData.type"
                  :options="typeBatteryOptions"
                  placeholder="Chọn loại pin"
                />
              </NFormItem>
            </NGridItem>

            <!-- Công nghệ sạc dùng NSelect vì backend là Enum TechnolyCharging -->
            <NGridItem>
              <NFormItem label="Công nghệ sạc" path="technolyCharging" required>
                <NSelect
                  v-model:value="formData.technolyCharging"
                  :options="technolyChargingOptions"
                  placeholder="Chọn công nghệ sạc"
                />
              </NFormItem>
            </NGridItem>

            <NGridItem>
              <NFormItem label="Dung lượng (mAh)" path="capacity" required>
                <NInput
                  v-model:value="formData.capacity"
                  type="number"
                  placeholder="VD: 4500, 5000"
                  class="w-full"
                />
              </NFormItem>
            </NGridItem>

            <NGridItem span="2">
              <NFormItem label="Có thể tháo rời" path="removeBattery">
                <NSpace align="center">
                  <NSwitch v-model:value="formData.removeBattery" />
                  <span class="text-gray-500 text-sm">
                    {{ formData.removeBattery ? 'Pin có thể tháo rời' : 'Pin không tháo rời' }}
                  </span>
                </NSpace>
              </NFormItem>
            </NGridItem>
          </NGrid>
        </NForm>
      </div>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="closeModal">
            Hủy
          </NButton>
          <NButton type="primary" @click="saveBattery">
            {{ modalMode === 'add' ? 'Thêm mới' : 'Lưu thay đổi' }}
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: #f1f1f1;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
