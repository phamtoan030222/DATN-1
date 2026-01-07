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
import type { DataTableColumns } from 'naive-ui'
import {
  createMaterial,
  getAllMaterials,
  updateMaterial,
  updateMaterialStatus,
} from '@/service/api/admin/product/material.api'
import type {
  CreateMaterialRequest,
  MaterialResponse,
} from '@/service/api/admin/product/material.api'

// ================= STATE =================
const message = useMessage()
const notification = useNotification()
const tableData = ref<MaterialResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchState = reactive({
  keyword: '',
  status: null as string | null,
})

// ================= MODAL STATE =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')

const formData = reactive<CreateMaterialRequest>({
  id: undefined,
  topCaseMaterial: '',
  bottomCaseMaterial: '',
  keyboardMaterial: '',
})

// ================= API CALL =================
async function fetchMaterials() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value,
      key: searchState.keyword || undefined,
      status: searchState.status || undefined,
    }

    const res = await getAllMaterials(params)

    if (res) {
      tableData.value = res.data || []
      total.value = res.totalElements || 0
    }
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu chất liệu')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchMaterials)

// ================= CRUD LOGIC =================
// SỬA: Hàm openModal lấy dữ liệu trực tiếp từ row
function openModal(mode: 'add' | 'edit', row?: MaterialResponse) {
  modalMode.value = mode

  if (mode === 'edit' && row) {
    // Fill data trực tiếp từ row vào form
    Object.assign(formData, {
      id: row.id,
      topCaseMaterial: row.topCaseMaterial,
      bottomCaseMaterial: row.bottomCaseMaterial,
      keyboardMaterial: row.keyboardMaterial,
    })
  }
  else {
    // Reset form khi thêm mới
    Object.assign(formData, {
      id: undefined,
      topCaseMaterial: '',
      bottomCaseMaterial: '',
      keyboardMaterial: '',
    })
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveMaterial() {
  if (!formData.topCaseMaterial || !formData.bottomCaseMaterial || !formData.keyboardMaterial) {
    message.warning('Vui lòng nhập đầy đủ thông tin các mặt chất liệu')
    return
  }

  try {
    if (modalMode.value === 'add') {
      await createMaterial(formData)
      notification.success({ content: 'Thêm mới chất liệu thành công', duration: 3000 })
    }
    else {
      if (!formData.id)
        return
      await updateMaterial(formData.id, formData)
      notification.success({ content: 'Cập nhật chất liệu thành công', duration: 3000 })
    }

    closeModal()
    fetchMaterials()
  }
  catch (e) {
    notification.error({ content: 'Có lỗi xảy ra khi lưu dữ liệu', duration: 3000 })
  }
}

async function handleStatusChange(row: MaterialResponse, value: boolean) {
  if (!row.id)
    return
  try {
    loading.value = true
    await updateMaterialStatus(row.id)
    notification.success({ content: `Đã cập nhật trạng thái: ${row.code}`, duration: 2000 })
    fetchMaterials()
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
  fetchMaterials()
}

function handleSearch() {
  currentPage.value = 1
  fetchMaterials()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchMaterials()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchMaterials()
}

// ================= TABLE COLUMNS =================
const columns: DataTableColumns<MaterialResponse> = [
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
    title: 'Mặt trên (Top)',
    key: 'topCaseMaterial',
    minWidth: 150,
  },
  {
    title: 'Mặt dưới (Bottom)',
    key: 'bottomCaseMaterial',
    minWidth: 150,
  },
  {
    title: 'Bàn phím (Keyboard)',
    key: 'keyboardMaterial',
    minWidth: 150,
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
            <Icon icon="mdi:material-design" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">
            Quản lý Chất liệu
          </span>
        </NSpace>
        <span class="text-gray-500">Quản lý các loại chất liệu cấu thành sản phẩm</span>
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
          <NGridItem span="1 600:1 1200:2">
            <NFormItem label="Tìm kiếm chung">
              <NInput
                v-model:value="searchState.keyword"
                placeholder="Nhập tên, mã chất liệu..."
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
        </NGrid>
      </NForm>
    </NCard>

    <NCard title="Danh sách Chất liệu" class="border border-gray-100 rounded-2xl shadow-sm">
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
      style="width: 600px"
      :title="modalMode === 'add' ? 'Thêm mới Chất liệu' : 'Cập nhật Chất liệu'"
      :bordered="false"
      size="huge"
      class="shadow-2xl rounded-2xl"
      :closable="false"
    >
      <div class="max-h-[600px] overflow-y-auto pr-4 custom-scrollbar">
        <NForm label-placement="top" :model="formData">
          <NFormItem label="Chất liệu mặt trên (Top Case)" required>
            <NInput v-model:value="formData.topCaseMaterial" placeholder="VD: Nhôm nguyên khối..." />
          </NFormItem>

          <NFormItem label="Chất liệu mặt dưới (Bottom Case)" required>
            <NInput v-model:value="formData.bottomCaseMaterial" placeholder="VD: Nhựa cao cấp..." />
          </NFormItem>

          <NFormItem label="Chất liệu bàn phím (Keyboard)" required>
            <NInput v-model:value="formData.keyboardMaterial" placeholder="VD: Nhựa ABS..." />
          </NFormItem>
        </NForm>
      </div>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="closeModal">
            Hủy
          </NButton>
          <NButton type="primary" @click="saveMaterial">
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
