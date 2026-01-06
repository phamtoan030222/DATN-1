<script setup lang="tsx">
import { h, onMounted, reactive, ref, watch } from 'vue'
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
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { debounce } from 'lodash'
import {
  createOperatingSystem,
  getAllOperatingSystems,
  updateOperatingSystem,
  updateOperatingSystemStatus,
} from '@/service/api/admin/product/operating.api'
import type { CreateOperatingSystemRequest, OperatingSystemResponse } from '@/service/api/admin/product/operating.api'

// ================= STATE =================
const message = useMessage()
const tableData = ref<OperatingSystemResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filter = reactive({
  name: '',
  status: null as string | null,
})

const checkedRowKeys = ref<(string | number)[]>([])

// ================= MODAL =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalRow = ref<OperatingSystemResponse | null>(null)

const formData = reactive<CreateOperatingSystemRequest>({
  code: '',
  name: '',
  version: '',
  description: '',
})

// ================= API CALL =================
async function fetchOperatingSystems() {
  loading.value = true
  try {
    const res = await getAllOperatingSystems({
      page: currentPage.value,
      size: pageSize.value,
      key: filter.name || undefined,
      status: filter.status || undefined,
    })
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (e) {
    message.error('Không thể tải dữ liệu Hệ điều hành')
  }
  finally {
    loading.value = false
  }
}

// Debounce search input
const debouncedFetch = debounce(() => {
  currentPage.value = 1
  fetchOperatingSystems()
}, 500)

function resetFilters() {
  filter.name = ''
  filter.status = null
  currentPage.value = 1
  fetchOperatingSystems()
}

// Auto-fetch on status change
watch(() => filter.status, () => {
  currentPage.value = 1
  fetchOperatingSystems()
})

onMounted(fetchOperatingSystems)

// ================= CRUD =================
function openModal(mode: 'add' | 'edit', row?: OperatingSystemResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    modalRow.value = row
    formData.code = row.code
    formData.name = row.name
    formData.version = row.version
    formData.description = row.description
  }
  else {
    modalRow.value = null
    formData.code = ''
    formData.name = ''
    formData.version = ''
    formData.description = ''
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveOperatingSystem() {
  if (!formData.name || !formData.version) {
    message.warning('Vui lòng nhập đầy đủ Mã, Tên, Phiên bản')
    return
  }

  try {
    if (modalMode.value === 'add') {
      await createOperatingSystem(formData)
      message.success('Thêm Hệ điều hành thành công')
    }
    else if (modalMode.value === 'edit' && modalRow.value) {
      await updateOperatingSystem(modalRow.value.id, formData)
      message.success('Cập nhật Hệ điều hành thành công')
    }
    closeModal()
    fetchOperatingSystems()
  }
  catch (e) {
    message.error('Có lỗi xảy ra khi lưu dữ liệu')
  }
}

async function handleStatusChange(row: OperatingSystemResponse, value: boolean) {
  try {
    loading.value = true
    await updateOperatingSystemStatus(row.id, value ? 'ACTIVE' : 'INACTIVE')
    message.success(`Cập nhật trạng thái ${row.name} thành công`)
    row.status = value ? 'ACTIVE' : 'INACTIVE'
  }
  catch {
    message.error('Cập nhật trạng thái thất bại')
  }
  finally {
    loading.value = false
  }
}

// ================= TABLE COLUMNS =================
const columns: DataTableColumns<OperatingSystemResponse> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Mã',
    key: 'code',
    width: 120,
    render: row => h('strong', { class: 'text-primary' }, row.code),
  },
  {
    title: 'Tên hệ điều hành',
    key: 'name',
    minWidth: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: 'Phiên bản',
    key: 'version',
    width: 120,
  },
  {
    title: 'Mô tả',
    key: 'description',
    width: 200,
    ellipsis: { tooltip: true },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    render(row: OperatingSystemResponse) {
      return h(
        NSwitch,
        {
          value: row.status === 'ACTIVE',
          size: 'small',
          onUpdateValue: (val: boolean) => handleStatusChange(row, val),
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    align: 'center',
    fixed: 'right',
    render(row: OperatingSystemResponse) {
      return h('div', { class: 'flex justify-center' }, [
        // Edit Button (Warning - Cam)
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            secondary: true,
            type: 'warning',
            circle: true,
            class: 'transition-all duration-200 hover:scale-125 hover:shadow-md',
            onClick: () => openModal('edit', row),
          }, { icon: () => h(Icon, { icon: 'carbon:edit' }) }),
          default: () => 'Sửa thông tin',
        }),
      ])
    },
  },
]

// ================= PAGINATION =================
function handlePageChange(page: number) {
  currentPage.value = page
  fetchOperatingSystems()
}
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="carbon:carbon-for-ibm-dotcom" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Hệ điều hành
          </span>
        </NSpace>
        <span>Quản lý danh sách hệ điều hành (OS) của sản phẩm</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-md mb-4">
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
                @click="resetFilters"
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
        <NGrid :x-gap="24" :cols="2">
          <NGridItem>
            <NFormItem label="Tìm kiếm chung">
              <NInput
                v-model:value="filter.name"
                placeholder="Nhập tên OS, mã, phiên bản..."
                clearable
                @input="debouncedFetch"
                @keydown.enter="fetchOperatingSystems"
              >
                <template #prefix>
                  <Icon icon="carbon:search" />
                </template>
              </NInput>
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Trạng thái">
              <NRadioGroup v-model:value="filter.status" name="radiogroup">
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

    <NCard title="Danh sách Hệ điều hành" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="openModal('add')"
            >
              <template #icon>
                <NIcon size="24">
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
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="fetchOperatingSystems"
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
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        striped
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="total"
          :page-sizes="[5, 10, 20, 50]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="(val) => { pageSize = val; currentPage = 1; fetchOperatingSystems() }"
        />
      </div>
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="card"
      style="width: 500px"
      :title="modalMode === 'add' ? 'Thêm Hệ điều hành' : 'Cập nhật Hệ điều hành'"
    >
      <NForm size="large">
        <NGrid cols="1" x-gap="12">
          <NGridItem>
            <NFormItem label="Tên hệ điều hành" required>
              <NInput v-model:value="formData.name" placeholder="VD: Android, iOS, Windows..." />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="Phiên bản" required>
              <NInput v-model:value="formData.version" placeholder="VD: 14.0, 17.2..." />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="Mô tả">
              <NInput
                v-model:value="formData.description"
                type="textarea"
                placeholder="Nhập mô tả chi tiết..."
              />
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="closeModal">
            Đóng
          </NButton>
          <NButton type="primary" icon-placement="right" @click="saveOperatingSystem">
            Lưu thay đổi
            <template #icon>
              <Icon icon="carbon:save" />
            </template>
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
