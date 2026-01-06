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
  createBrand,
  getAllBrands,
  updateBrand,
  updateBrandStatus,
} from '@/service/api/admin/product/brand.api'
import type { BrandResponse, CreateBrandRequest } from '@/service/api/admin/product/brand.api'

// ================= STATE =================
const message = useMessage()
const tableData = ref<BrandResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const filter = reactive({
  name: '',
  status: null as string | null, // null = Tất cả, 'ACTIVE', 'INACTIVE'
})

const checkedRowKeys = ref<(string | number)[]>([])

// ================= MODAL =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const modalRow = ref<BrandResponse | null>(null)

const formData = reactive<CreateBrandRequest>({
  code: '',
  name: '',
})

// ================= API CALL =================
async function fetchBrands() {
  loading.value = true
  try {
    const res = await getAllBrands({
      page: currentPage.value,
      size: pageSize.value,
      name: filter.name || undefined,
      status: filter.status || undefined, // Truyền trạng thái lọc xuống API
    })
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (e) {
    message.error('Không thể tải dữ liệu thương hiệu')
  }
  finally {
    loading.value = false
  }
}

// Tìm kiếm tự động khi gõ chữ (Debounce 500ms)
const debouncedFetch = debounce(() => {
  currentPage.value = 1
  fetchBrands()
}, 500)

function resetFilters() {
  filter.name = ''
  filter.status = null
  currentPage.value = 1
  fetchBrands()
}

// Tự động tải lại khi đổi trạng thái (Radio)
watch(() => filter.status, () => {
  currentPage.value = 1
  fetchBrands()
})

onMounted(fetchBrands)

// ================= CRUD =================
function openModal(mode: 'add' | 'edit', row?: BrandResponse) {
  modalMode.value = mode
  if (mode === 'edit' && row) {
    modalRow.value = row
    formData.code = row.code
    formData.name = row.name
  }
  else {
    modalRow.value = null
    formData.code = ''
    formData.name = ''
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

async function saveBrand() {
  if (!formData.code || !formData.name) {
    message.warning('Vui lòng nhập đầy đủ Mã và Tên thương hiệu')
    return
  }

  try {
    if (modalMode.value === 'add') {
      await createBrand({
        code: formData.code,
        name: formData.name,
      })
      message.success('Thêm thương hiệu thành công')
    }
    else if (modalMode.value === 'edit' && modalRow.value) {
      const req: CreateBrandRequest = {
        code: formData.code,
        name: formData.name,
      }
      await updateBrand(modalRow.value.id, req)
      message.success('Cập nhật thương hiệu thành công')
    }
    closeModal()
    fetchBrands()
  }
  catch (e) {
    message.error('Có lỗi xảy ra khi lưu thương hiệu')
  }
}

async function handleStatusChange(row: BrandResponse, value: boolean) {
  try {
    loading.value = true
    await updateBrandStatus(row.id, value ? 'ACTIVE' : 'INACTIVE')
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
const columns: DataTableColumns<BrandResponse> = [
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
    width: 150,
    render: row => h('strong', { class: 'text-primary' }, row.code),
  },
  {
    title: 'Tên thương hiệu',
    key: 'name',
    minWidth: 200,
    ellipsis: { tooltip: true },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    render(row: BrandResponse) {
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
    render(row: BrandResponse) {
      return h('div', { class: 'flex justify-center' }, [
        // Nút Sửa (Màu Cam - Warning)
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            secondary: true,
            type: 'warning', // Màu cam
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
  fetchBrands()
}
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-green-600">
            <Icon icon="carbon:carbon-ui-builder" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Thương hiệu
          </span>
        </NSpace>
        <span>Quản lý danh sách thương hiệu sản phẩm có mặt tại cửa hàng</span>
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
                placeholder="Nhập tên thương hiệu, mã..."
                clearable
                @input="debouncedFetch"
                @keydown.enter="fetchBrands"
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

    <NCard title="Danh sách thương hiệu" class="border rounded-3">
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
              @click="fetchBrands"
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
          @update:page-size="(val) => { pageSize = val; currentPage = 1; fetchBrands() }"
        />
      </div>
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="card"
      style="width: 500px"
      :title="modalMode === 'add' ? 'Thêm thương hiệu mới' : 'Cập nhật thương hiệu'"
    >
      <NForm size="large">
        <NGrid cols="1" x-gap="12">
          <NGridItem>
            <NFormItem label="Tên thương hiệu" required>
              <NInput v-model:value="formData.name" placeholder="VD: Samsung, Apple..." />
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="closeModal">
            Đóng
          </NButton>
          <NButton type="primary" icon-placement="right" @click="saveBrand">
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
