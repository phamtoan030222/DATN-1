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
  NInput,
  NModal,
  NPagination,
  NRadio,
  NRadioGroup,
  NSpace,
  NSwitch,
  NTooltip,
  useMessage,
  useNotification,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import {
  createHardDrive,
  getAllHardDrives,
  updateHardDrive,
  updateHardDriveStatus,
} from '@/service/api/admin/product/harddrive.api'
import type { CreateHardDriveRequest, HardDriveResponse } from '@/service/api/admin/product/harddrive.api'
import type { DataTableColumns } from 'naive-ui'

// ================= STATE =================
const message = useMessage()
const notification = useNotification()
const tableData = ref<HardDriveResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// State tìm kiếm & bộ lọc
const searchState = reactive({
  keyword: '',
  status: null as string | null, // null: Tất cả, 'ACTIVE', 'INACTIVE'
})

// ================= MODAL STATE =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalRow = ref<HardDriveResponse | null>(null)

const formData = reactive<CreateHardDriveRequest>({
  name: '',
  code: '',
  brand: '',
  type: '', // HDD, SSD
  typeConnect: '', // SATA, NVMe...
  capacity: 0,
  readSpeed: 0,
  writeSpeed: 0,
  cacheMemory: 0,
  physicalSize: 0,
  description: '',
})

// ================= API CALL =================
async function fetchHardDrives() {
  loading.value = true
  try {
    const res = await getAllHardDrives({
      page: currentPage.value,
      size: pageSize.value,
      name: searchState.keyword || undefined,
      status: searchState.status || undefined,
    })
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (e) {
    message.error('Không thể tải dữ liệu Ổ cứng')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchHardDrives)

// ================= CRUD LOGIC =================
function openModal(mode: 'add' | 'edit', row?: HardDriveResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    modalRow.value = row
    Object.assign(formData, {
      name: row.name,
      code: row.code,
      brand: row.brand,
      type: row.type,
      typeConnect: row.typeConnect,
      capacity: row.capacity,
      readSpeed: row.readSpeed,
      writeSpeed: row.writeSpeed,
      cacheMemory: row.cacheMemory,
      physicalSize: row.physicalSize,
      description: row.description,
    })
  }
  else {
    modalRow.value = null
    // Reset form
    Object.assign(formData, {
      name: '',
      code: '',
      brand: '',
      type: '',
      typeConnect: '',
      capacity: 0,
      readSpeed: 0,
      writeSpeed: 0,
      cacheMemory: 0,
      physicalSize: 0,
      description: '',
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveHardDrive() {
  if (!formData.name || !formData.brand || !formData.type) {
    message.warning('Vui lòng nhập đầy đủ Tên, Thương hiệu, Loại')
    return
  }

  try {
    if (modalMode.value === 'add') {
      await createHardDrive(formData)
      notification.success({ content: 'Thêm Ổ cứng thành công', duration: 3000 })
    }
    else if (modalMode.value === 'edit' && modalRow.value) {
      await updateHardDrive(modalRow.value.id, formData)
      notification.success({ content: 'Cập nhật Ổ cứng thành công', duration: 3000 })
    }
    closeModal()
    fetchHardDrives()
  }
  catch (e) {
    notification.error({ content: 'Có lỗi xảy ra khi lưu Ổ cứng', duration: 3000 })
  }
}

async function handleStatusChange(row: HardDriveResponse, value: boolean) {
  try {
    loading.value = true
    await updateHardDriveStatus(row.id, value ? 'ACTIVE' : 'INACTIVE')
    notification.success({ content: `Đã cập nhật trạng thái: ${row.name}`, duration: 2000 })
    fetchHardDrives()
  }
  catch {
    notification.error({ content: 'Cập nhật trạng thái thất bại', duration: 3000 })
    loading.value = false
  }
}

function refreshTable() {
  searchState.keyword = ''
  searchState.status = null
  currentPage.value = 1
  fetchHardDrives()
}

function handleSearch() {
  currentPage.value = 1
  fetchHardDrives()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchHardDrives()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchHardDrives()
}

// ================= TABLE COLUMNS =================
const columns: DataTableColumns<HardDriveResponse> = [
  {
    title: 'STT',
    key: 'index',
    width: 60,
    align: 'center',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Mã Ổ cứng',
    key: 'code',
    width: 130,
    render: row => h('strong', { class: 'text-primary' }, row.code || '---'),
  },
  {
    title: 'Tên Ổ cứng',
    key: 'name',
    minWidth: 150,
    render: row => h('span', { class: 'text-gray-700 cursor-pointer hover:text-primary transition-colors', onClick: () => openModal('edit', row) }, row.name),
  },
  { title: 'Hãng', key: 'brand', width: 100 },
  { title: 'Loại', key: 'type', width: 80, align: 'center' }, // HDD/SSD
  { title: 'Kết nối', key: 'typeConnect', width: 100 },
  {
    title: 'Dung lượng',
    key: 'capacity',
    width: 110,
    render: row => `${row.capacity} GB`,
  },
  {
    title: 'Kích thước',
    key: 'physicalSize',
    width: 100,
    align: 'center',
    render: row => `${row.physicalSize}"`,
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    render(row) {
      return h(NSwitch, {
        value: row.status === 'ACTIVE',
        size: 'small',
        disabled: loading.value,
        onUpdateValue: val => handleStatusChange(row, val),
      })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 120,
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
    <NCard class="mb-3 shadow-sm border-none">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="icon-park-outline:hard-disk" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">
            Quản lý Ổ cứng
          </span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách ổ cứng (HDD/SSD) và thông số kỹ thuật</span>
      </NSpace>
    </NCard>

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
                placeholder="Tìm theo Tên, Mã, Hãng, Loại..."
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
                    Ngưng
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <NCard title="Danh sách Ổ cứng" class="border border-gray-100 rounded-2xl shadow-sm">
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

    <NModal
      v-model:show="showModal"
      preset="card"
      style="width: 700px"
      :title="modalMode === 'add' ? 'Thêm mới Ổ cứng' : 'Cập nhật Ổ cứng'"
      :bordered="false"
      size="huge"
      class="shadow-2xl rounded-2xl"
      :closable="false"
    >
      <div class="max-h-[600px] overflow-y-auto pr-4 custom-scrollbar">
        <NForm label-placement="top" :model="formData">
          <NGrid cols="2" x-gap="24" y-gap="12">
            <NGridItem>
              <NFormItem label="Tên Ổ cứng" required>
                <NInput v-model:value="formData.name" placeholder="VD: Samsung 980 Pro" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Hãng sản xuất" required>
                <NInput v-model:value="formData.brand" placeholder="VD: Samsung, WD, Seagate" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Loại (Type)" required>
                <NInput v-model:value="formData.type" placeholder="VD: SSD, HDD" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Chuẩn kết nối" required>
                <NInput v-model:value="formData.typeConnect" placeholder="VD: NVMe PCIe, SATA 3" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Dung lượng (GB)" required>
                <NInput v-model:value="formData.capacity" type="number" placeholder="VD: 512, 1024" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Kích thước (inch)" required>
                <NInput v-model:value="formData.physicalSize" type="number" placeholder="VD: 2.5, 3.5" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Tốc độ đọc (MB/s)">
                <NInput v-model:value="formData.readSpeed" type="number" placeholder="VD: 3500" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Tốc độ ghi (MB/s)">
                <NInput v-model:value="formData.writeSpeed" type="number" placeholder="VD: 3000" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Bộ nhớ đệm (MB)">
                <NInput v-model:value="formData.cacheMemory" type="number" placeholder="VD: 512" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem span="2">
              <NFormItem label="Mô tả chi tiết">
                <NInput
                  v-model:value="formData.description"
                  type="textarea"
                  placeholder="Thông tin thêm về sản phẩm..."
                  :rows="3"
                />
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
          <NButton type="primary" @click="saveHardDrive">
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
