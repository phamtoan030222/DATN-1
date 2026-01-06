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
  createRam,
  getAllRams,
  updateRam,
  updateRamStatus,
} from '@/service/api/admin/product/ram.api'
import type { CreateRamRequest, RamResponse } from '@/service/api/admin/product/ram.api'
import type { DataTableColumns } from 'naive-ui'

// ================= STATE =================
const message = useMessage()
const notification = useNotification()
const tableData = ref<RamResponse[]>([])
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
const modalRow = ref<RamResponse | null>(null)

const formData = reactive<CreateRamRequest>({
  name: '',
  code: '', // Vẫn giữ biến này để binding dữ liệu cũ, nhưng không hiển thị input
  brand: '',
  type: '',
  capacity: 0,
  busSpeed: 0,
  slotConFig: 0,
  maxSupported: 0,
  description: '',
})

// ================= API CALL =================
async function fetchRams() {
  loading.value = true
  try {
    const res = await getAllRams({
      page: currentPage.value,
      size: pageSize.value,
      key: searchState.keyword || undefined,
      status: searchState.status || undefined,
    })
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (e) {
    message.error('Không thể tải dữ liệu RAM')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchRams)

// ================= CRUD LOGIC =================
function openModal(mode: 'add' | 'edit', row?: RamResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    modalRow.value = row
    Object.assign(formData, {
      name: row.name,
      code: row.code,
      brand: row.brand,
      type: row.type,
      capacity: row.capacity,
      busSpeed: row.busSpeed,
      slotConFig: row.slotConFig,
      maxSupported: row.maxSupported,
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
      capacity: 0,
      busSpeed: 0,
      slotConFig: 0,
      maxSupported: 0,
      description: '',
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveRam() {
  if (!formData.name || !formData.brand || !formData.type) {
    message.warning('Vui lòng nhập đầy đủ Tên, Thương hiệu, Loại')
    return
  }

  try {
    if (modalMode.value === 'add') {
      await createRam(formData)
      notification.success({ content: 'Thêm RAM thành công', duration: 3000 })
    }
    else if (modalMode.value === 'edit' && modalRow.value) {
      await updateRam(modalRow.value.id, formData)
      notification.success({ content: 'Cập nhật RAM thành công', duration: 3000 })
    }
    closeModal()
    fetchRams()
  }
  catch (e) {
    notification.error({ content: 'Có lỗi xảy ra khi lưu RAM', duration: 3000 })
  }
}

async function handleStatusChange(row: RamResponse, value: boolean) {
  try {
    loading.value = true
    await updateRamStatus(row.id, value ? 'ACTIVE' : 'INACTIVE')
    notification.success({ content: `Đã cập nhật trạng thái: ${row.name}`, duration: 2000 })
    fetchRams()
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
  fetchRams()
}

function handleSearch() {
  currentPage.value = 1
  fetchRams()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchRams()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchRams()
}

// ================= TABLE COLUMNS =================
const columns: DataTableColumns<RamResponse> = [
  {
    title: 'STT',
    key: 'index',
    width: 60,
    align: 'center',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Mã RAM',
    key: 'code',
    width: 150,
    render: row => h('strong', { class: 'text-primary' }, row.code || '---'),
  },
  {
    title: 'Tên RAM',
    key: 'name',
    minWidth: 150,
    render: row => h('span', { class: 'text-gray-700 cursor-pointer hover:text-primary transition-colors', onClick: () => openModal('edit', row) }, row.name),
  },
  { title: 'Thương hiệu', key: 'brand', width: 120 },
  { title: 'Loại', key: 'type', width: 100, align: 'center' },
  {
    title: 'Dung lượng',
    key: 'capacity',
    width: 120,
    render: row => `${row.capacity} GB`,
  },
  {
    title: 'Bus Speed',
    key: 'busSpeed',
    width: 120,
    render: row => `${row.busSpeed} MHz`,
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
    width: 80,
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
            <Icon icon="carbon:chip" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">
            Quản lý RAM
          </span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách bộ nhớ trong (RAM) và thông số kỹ thuật</span>
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
                placeholder="Tìm theo Tên RAM, Mã, Thương hiệu..."
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

    <NCard title="Danh sách RAM" class="border border-gray-100 rounded-2xl shadow-sm">
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
      :title="modalMode === 'add' ? 'Thêm mới RAM' : 'Cập nhật RAM'"
      :bordered="false"
      size="huge"
      class="shadow-2xl rounded-2xl"
      :closable="false"
    >
      <div class="max-h-[600px] overflow-y-auto pr-4 custom-scrollbar">
        <NForm label-placement="top" :model="formData">
          <NGrid cols="2" x-gap="24" y-gap="12">
            <NGridItem>
              <NFormItem label="Tên RAM" required>
                <NInput v-model:value="formData.name" placeholder="VD: Kingston Fury Beast" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Thương hiệu" required>
                <NInput v-model:value="formData.brand" placeholder="VD: Kingston, Corsair" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Loại RAM" required>
                <NInput v-model:value="formData.type" placeholder="VD: DDR4, DDR5" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Dung lượng (GB)" required>
                <NInput v-model:value="formData.capacity" type="number" placeholder="VD: 8, 16" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Bus Speed (MHz)" required>
                <NInput v-model:value="formData.busSpeed" type="number" placeholder="VD: 3200" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Cấu hình Slot">
                <NInput v-model:value="formData.slotConFig" type="number" placeholder="Số khe cắm" class="w-full" />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Hỗ trợ tối đa (GB)">
                <NInput v-model:value="formData.maxSupported" type="number" placeholder="VD: 64" class="w-full" />
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
          <NButton type="primary" @click="saveRam">
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
