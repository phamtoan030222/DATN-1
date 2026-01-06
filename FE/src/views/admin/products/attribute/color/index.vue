<script setup lang="tsx">
import { onMounted, reactive, ref } from 'vue'
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
  NPopconfirm,
  NSpace,
  NSwitch,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import {

  createColor,

  getAllColors,
  updateColor,
  updateColorStatus,
} from '@/service/api/admin/product/color.api'
import type { ColorResponse, CreateColorRequest } from '@/service/api/admin/product/color.api'

// ================= STATE =================
const message = useMessage()
const tableData = ref<ColorResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(5)
const loading = ref(false)
const searchKeyword = ref('')

const checkedRowKeys = ref<(string | number)[]>([])

// ================= MODAL =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalRow = ref<ColorResponse | null>(null)

const formData = reactive<CreateColorRequest>({
  colorName: '',
  colorCode: '',
})

// ================= API CALL =================
async function fetchColors() {
  loading.value = true
  try {
    const res = await getAllColors({
      page: currentPage.value,
      size: pageSize.value,
      q: searchKeyword.value || undefined,
    })
    tableData.value = res.items
    total.value = res.totalItems
  }
  catch (e) {
    message.error('Không thể tải dữ liệu Màu sắc')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchColors)

// ================= CRUD =================
function openModal(mode: 'add' | 'edit', row?: ColorResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    modalRow.value = row
    formData.colorName = row.colorName
    formData.colorCode = row.colorCode
  }
  else {
    modalRow.value = null
    formData.colorName = ''
    formData.colorCode = ''
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveColor() {
  if (!formData.colorName || !formData.colorCode) {
    message.warning('Vui lòng nhập đầy đủ Tên và Mã màu')
    return
  }

  try {
    if (modalMode.value === 'add') {
      await createColor(formData)
      message.success('Thêm màu thành công')
    }
    else if (modalMode.value === 'edit' && modalRow.value) {
      await updateColor(modalRow.value.id, formData)
      message.success('Cập nhật màu thành công')
    }
    closeModal()
    fetchColors()
  }
  catch (e) {
    message.error('Có lỗi xảy ra khi lưu Màu')
  }
}

async function handleDelete(id: string) {
  try {
    message.info('TODO: Thêm API xóa màu')
  }
  catch {
    message.error('Xóa thất bại')
  }
}

async function handleStatusChange(row: ColorResponse, value: boolean) {
  try {
    await updateColorStatus(row.id, value ? 'ACTIVE' : 'INACTIVE')
    message.success(`Cập nhật trạng thái ${row.colorName} thành công`)
    fetchColors()
  }
  catch {
    message.error('Cập nhật trạng thái thất bại')
  }
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: 'selection' as const },
  { title: 'Tên màu', key: 'colorName' },
  {
    title: 'Mã màu',
    key: 'colorCode',
    render(row: ColorResponse) {
      return (
        <div class="flex items-center gap-2">
          <span>{row.colorCode}</span>
          <div
            style={{
              width: '16px',
              height: '16px',
              backgroundColor: row.colorCode,
              border: '1px solid #ccc',
              borderRadius: '4px',
            }}
          />
        </div>
      )
    },
  },
  {
    title: 'Trạng thái',
    key: 'colorStatus',
    render(row: ColorResponse) {
      return (
        <NSwitch
          value={row.colorStatus === 'ACTIVE'}
          onUpdateValue={(val: boolean) => handleStatusChange(row, val)}
        />
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    render(row: ColorResponse) {
      return (
        <NSpace>
          <NButton
            size="small"
            quaternary
            circle
            onClick={() => openModal('edit', row)}
          >
            <Icon icon="carbon:edit" width="18" />
          </NButton>
          <NPopconfirm onPositiveClick={() => handleDelete(row.id)}>
            {{
              trigger: () => (
                <NButton size="small" quaternary circle type="error">
                  <Icon icon="carbon:trash-can" width="18" />
                </NButton>
              ),
              default: () => 'Bạn có chắc muốn xóa?',
            }}
          </NPopconfirm>
        </NSpace>
      )
    },
  },
]

// ================= SEARCH & PAGINATION =================
function handleSearch() {
  currentPage.value = 1
  fetchColors()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchColors()
}
</script>

<template>
  <!-- Header -->
  <NCard>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon icon="carbon:color-palette" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px"> Quản lý Màu sắc </span>
      </NSpace>
      <span>Quản lý danh sách màu sản phẩm</span>
    </NSpace>
  </NCard>

  <!-- Table -->
  <NCard title="Danh sách Màu sắc" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NInput
          v-model:value="searchKeyword"
          placeholder="Tìm kiếm màu..."
          clearable
          style="width: 220px"
          @input="handleSearch"
        >
          <template #prefix>
            <NIcon size="18">
              <Icon icon="carbon:search" />
            </NIcon>
          </template>
        </NInput>
        <NButton
          type="primary"
          circle
          title="Thêm mới"
          @click="openModal('add')"
        >
          <NIcon size="24">
            <Icon icon="carbon:add" />
          </NIcon>
        </NButton>
        <NButton
          type="primary"
          secondary
          circle
          title="Làm mới"
          @click="fetchColors"
        >
          <NIcon size="24">
            <Icon icon="carbon:rotate" />
          </NIcon>
        </NButton>
        <NPopconfirm
          @positive-click="() => message.info('TODO: API xóa hàng loạt')"
        >
          <template #trigger>
            <NButton type="error" secondary circle title="Xóa hàng loạt">
              <NIcon size="24">
                <Icon icon="icon-park-outline:delete" />
              </NIcon>
            </NButton>
          </template>
          Xác nhận xóa tất cả màu đã chọn?
        </NPopconfirm>
      </NSpace>
    </template>

    <NDataTable
      v-model:checked-row-keys="checkedRowKeys"
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :row-key="(row) => row.id"
      :pagination="false"
      bordered
    />

    <div class="flex justify-center mt-4">
      <NPagination
        :page="currentPage"
        :page-size="pageSize"
        :page-count="Math.ceil(total / pageSize)"
        @update:page="handlePageChange"
      />
    </div>
  </NCard>

  <!-- Modal thêm/sửa -->
  <NModal
    v-model:show="showModal"
    preset="card"
    style="width: 500px"
    title="Màu sắc"
  >
    <NForm>
      <NGrid cols="1" x-gap="12">
        <NGridItem>
          <NFormItem label="Tên màu" required>
            <NInput
              v-model:value="formData.colorName"
              placeholder="Nhập tên màu"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Mã màu" required>
            <NColorPicker
              v-model:value="formData.colorCode"
              :modes="['hex']"
              show-alpha="{false}"
            />
          </NFormItem>
        </NGridItem>
      </NGrid>
    </NForm>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="closeModal">
          Hủy
        </NButton>
        <NButton type="primary" @click="saveColor">
          Lưu
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>
