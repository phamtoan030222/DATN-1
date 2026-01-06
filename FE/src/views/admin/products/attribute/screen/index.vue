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
  NSelect,
  NSpace,
  NSwitch,
  NTooltip,
  useMessage,
  useNotification,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import {
  changeScreenStatus,
  getScreenById,
  getScreens,
  modifyScreen,
} from '@/service/api/admin/product/screen.api'
import type {
  ADProductScreenCreateUpdateRequest,
  ADProductScreenResponse,
} from '@/service/api/admin/product/screen.api'
import type { DataTableColumns } from 'naive-ui'

// ================= STATE =================
const message = useMessage()
const notification = useNotification()
const tableData = ref<ADProductScreenResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// State tìm kiếm & bộ lọc
const searchState = reactive({
  keyword: '',
  resolution: null as string | null,
  panelType: null as string | null,
  physicalSize: null as number | null,
  technology: '',
  status: null as string | null, // null = Tất cả
})

// ================= OPTIONS =================
const screenResolutionOptionsSelect = [
  { label: '1920 x 1080 pixels', value: '1920x1080' },
  { label: '1366 x 768 pixels', value: '1366x768' },
  { label: '2880 x 1800 pixels', value: '2880x1800' },
  { label: '3840 x 2160 pixels', value: '3840x2160' },
  { label: '2560 x 1440 pixels', value: '2560x1440' },
]

const panelTypeOptionsSelect = [
  { label: 'LCD', value: 'LCD' },
  { label: 'OLED', value: 'OLED' },
  { label: 'LED Backlit', value: 'LED_BACKLIT_LCD' },
  { label: 'AMOLED', value: 'AMOLED' },
  { label: 'Mini LED', value: 'MINI_LED' },
  { label: 'Micro LED', value: 'MICRO_LED' },
]

const screenSizeOptionsSelect = [
  { label: '11.6 inch', value: 11.6 },
  { label: '13.3 inch', value: 13.3 },
  { label: '14.0 inch', value: 14.0 },
  { label: '15.6 inch', value: 15.6 },
  { label: '16.0 inch', value: 16.0 },
  { label: '17.3 inch', value: 17.3 },
]

// ================= MODAL STATE =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')

const formData = reactive<ADProductScreenCreateUpdateRequest>({
  id: undefined,
  code: '',
  name: '',
  physicalSize: 0,
  resolution: '',
  panelType: '',
  technology: '',
})

// ================= API CALL =================
async function fetchScreens() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value,
      q: searchState.keyword || undefined,
      resolution: searchState.resolution || undefined,
      physicalSize: searchState.physicalSize || undefined,
      panelType: searchState.panelType || undefined,
      technology: searchState.technology || undefined,
      status: searchState.status || undefined,
    }

    const res = await getScreens(params)

    if (res.data) {
      tableData.value = res.data.data || []
      total.value = res.data.totalItems || 0
    }
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu màn hình')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchScreens)

// ================= CRUD LOGIC =================
async function openModal(mode: 'add' | 'edit', row?: ADProductScreenResponse) {
  modalMode.value = mode

  if (mode === 'edit' && row && row.id) {
    try {
      const res = await getScreenById(row.id)
      const detail = res.data
      if (detail) {
        Object.assign(formData, {
          id: detail.id,
          code: detail.code,
          name: detail.name,
          physicalSize: detail.physicalSize,
          resolution: detail.resolution,
          panelType: detail.panelType,
          technology: detail.technology,
        })
      }
    }
    catch (e) {
      message.error('Không thể lấy chi tiết màn hình')
      return
    }
  }
  else {
    Object.assign(formData, {
      id: undefined,
      code: '',
      name: '',
      physicalSize: null,
      resolution: null,
      panelType: null,
      technology: '',
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveScreen() {
  if (!formData.name || !formData.resolution || !formData.physicalSize || !formData.panelType) {
    message.warning('Vui lòng nhập đầy đủ: Tên, Kích thước, Độ phân giải, Tấm nền')
    return
  }

  try {
    await modifyScreen(formData)
    notification.success({
      content: modalMode.value === 'add' ? 'Thêm mới thành công' : 'Cập nhật thành công',
      duration: 3000,
    })
    closeModal()
    fetchScreens()
  }
  catch (e) {
    notification.error({ content: 'Có lỗi xảy ra', duration: 3000 })
  }
}

async function handleStatusChange(row: ADProductScreenResponse, value: boolean) {
  if (!row.id)
    return
  try {
    loading.value = true
    await changeScreenStatus(row.id)
    notification.success({ content: `Đã cập nhật trạng thái: ${row.name}`, duration: 2000 })
    fetchScreens()
  }
  catch {
    notification.error({ content: 'Cập nhật trạng thái thất bại', duration: 3000 })
    loading.value = false
  }
}

function refreshTable() {
  searchState.keyword = ''
  searchState.resolution = null
  searchState.panelType = null
  searchState.physicalSize = null
  searchState.technology = ''
  searchState.status = null // Reset về Tất cả

  currentPage.value = 1
  fetchScreens()
}

function handleSearch() {
  currentPage.value = 1
  fetchScreens()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchScreens()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchScreens()
}

// ================= TABLE COLUMNS =================
const columns: DataTableColumns<ADProductScreenResponse> = [
  {
    title: 'STT',
    key: 'index',
    width: 60,
    align: 'center',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Mã',
    key: 'code',
    width: 120,
    render: row => h('strong', { class: 'text-primary' }, row.code || '---'),
  },
  {
    title: 'Tên Màn hình',
    key: 'name',
    minWidth: 150,
    render: row => h('span', { class: 'text-gray-700 cursor-pointer hover:text-primary transition-colors', onClick: () => openModal('edit', row) }, row.name),
  },
  { title: 'Độ phân giải', key: 'resolution', width: 150 },
  {
    title: 'Kích thước',
    key: 'physicalSize',
    width: 150,
    align: 'center',
    render: row => `${row.physicalSize} inch`,
  },
  { title: 'Tấm nền', key: 'panelType', width: 150, align: 'center' },
  { title: 'Công nghệ', key: 'technology', width: 150, ellipsis: { tooltip: true } },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
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
    <NCard class="mb-3 shadow-sm border-none">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="icon-park-outline:monitor" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">
            Quản lý Màn hình
          </span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách và thông số kỹ thuật màn hình</span>
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
        <NGrid :x-gap="24" :y-gap="12" cols="1 600:2 900:3 1200:4">
          <NGridItem span="1 600:2 1200:2">
            <NFormItem label="Tìm kiếm chung">
              <NInput
                v-model:value="searchState.keyword"
                placeholder="Nhập tên, mã màn hình..."
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

          <NGridItem span="1 600:2 900:2 1200:2">
            <NFormItem label="Trạng thái">
              <NRadioGroup v-model:value="searchState.status" name="status-radio-group" @update:value="handleSearch">
                <NSpace align="center" style="height: 34px;">
                  <NRadio :value="null">
                    Tất cả
                  </NRadio>
                  <NRadio value="ACTIVE" class="ml-4">
                    Hoạt động
                  </NRadio>
                  <NRadio value="INACTIVE" class="ml-4">
                    Ngừng hoạt động
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Độ phân giải">
              <NSelect
                v-model:value="searchState.resolution"
                :options="screenResolutionOptionsSelect"
                placeholder="Chọn độ phân giải"
                clearable
                @update:value="handleSearch"
              />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Tấm nền">
              <NSelect
                v-model:value="searchState.panelType"
                :options="panelTypeOptionsSelect"
                placeholder="Chọn tấm nền"
                clearable
                @update:value="handleSearch"
              />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Kích thước">
              <NSelect
                v-model:value="searchState.physicalSize"
                :options="screenSizeOptionsSelect"
                placeholder="Chọn kích thước"
                clearable
                @update:value="handleSearch"
              />
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <NCard title="Danh sách Màn hình" class="border border-gray-100 rounded-2xl shadow-sm">
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
      :title="modalMode === 'add' ? 'Thêm mới Màn hình' : 'Cập nhật Màn hình'"
      :bordered="false"
      size="huge"
      class="shadow-2xl rounded-2xl"
      :closable="false"
    >
      <div class="max-h-[600px] overflow-y-auto pr-4 custom-scrollbar">
        <NForm label-placement="top" :model="formData">
          <NFormItem label="Tên màn hình" required>
            <NInput v-model:value="formData.name" placeholder="VD: Màn hình OLED 2.8K" />
          </NFormItem>

          <NGrid cols="2" x-gap="24" y-gap="12">
            <NGridItem>
              <NFormItem label="Kích thước (inch)" required>
                <NSelect
                  v-model:value="formData.physicalSize"
                  :options="screenSizeOptionsSelect"
                  placeholder="Chọn kích thước"
                />
              </NFormItem>
            </NGridItem>

            <NGridItem>
              <NFormItem label="Độ phân giải" required>
                <NSelect
                  v-model:value="formData.resolution"
                  :options="screenResolutionOptionsSelect"
                  placeholder="Chọn độ phân giải"
                />
              </NFormItem>
            </NGridItem>

            <NGridItem>
              <NFormItem label="Loại tấm nền" required>
                <NSelect
                  v-model:value="formData.panelType"
                  :options="panelTypeOptionsSelect"
                  placeholder="Chọn tấm nền"
                />
              </NFormItem>
            </NGridItem>

            <NGridItem>
              <NFormItem label="Công nghệ màn hình">
                <NInput v-model:value="formData.technology" placeholder="VD: Anti-glare, 100% sRGB..." />
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
          <NButton type="primary" @click="saveScreen">
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
