<script setup lang="tsx">
import { h, onMounted, reactive, ref, watch } from 'vue'
import {
  NButton,
  NCard,
  NColorPicker,
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
  NRadio,
  NRadioGroup,
  NSpace,
  NSwitch,
  NTooltip,
  useDialog,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui'
import { Icon } from '@iconify/vue'
import {
  createColor,
  getAllColors,
  updateColor,
  updateColorStatus,
} from '@/service/api/admin/product/color.api'
import type {
  AdColorResponse,
  ColorCreateUpdateRequest,
} from '@/service/api/admin/product/color.api'

// ================= STATE =================
const message = useMessage()
const dialog = useDialog() // Init Dialog
const formRef = ref<FormInst | null>(null) // Ref Form
const loading = ref(false)

// Data Table
const tableData = ref<AdColorResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// Search State
const searchState = reactive({
  keyword: '',
  status: null as string | null,
})

// ================= MODAL STATE =================
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')

const formData = reactive<ColorCreateUpdateRequest>({
  id: undefined,
  name: '',
  code: '#000000',
  status: 'ACTIVE',
})

// 2. Định nghĩa Rules Validate
const rules: FormRules = {
  name: [
    { required: true, message: 'Vui lòng nhập tên màu', trigger: ['blur', 'input'] },
  ],
  code: [
    { required: true, message: 'Vui lòng chọn mã màu', trigger: ['blur', 'change'] },
  ],
}

// ================= API CALLS =================
async function fetchData() {
  loading.value = true
  try {
    const res = await getAllColors({
      page: currentPage.value,
      size: pageSize.value,
      q: searchState.keyword || undefined,
      status: searchState.status || undefined,
    })
    tableData.value = res.data || []
    total.value = res.totalElements || 0
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu màu sắc')
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchData)

// ================= HANDLERS =================
function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleRefresh() {
  searchState.keyword = ''
  searchState.status = null
  currentPage.value = 1
  fetchData()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchData()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

// ================= CRUD LOGIC =================
function openModal(mode: 'add' | 'edit', row?: AdColorResponse) {
  modalMode.value = mode

  if (mode === 'edit' && row) {
    Object.assign(formData, {
      id: row.id,
      name: row.name,
      code: row.code,
      status: row.status,
    })
  }
  else {
    Object.assign(formData, {
      id: undefined,
      name: '',
      code: '#000000',
      status: 'ACTIVE',
    })
  }
  showModal.value = true
}

// 3. Hàm lưu sử dụng Validate và Dialog
function handleSave() {
  formRef.value?.validate((errors) => {
    if (errors) {
      return // Dừng nếu có lỗi validate
    }

    // Hiển thị Dialog xác nhận
    dialog.warning({
      title: 'Xác nhận',
      content: modalMode.value === 'add'
        ? 'Bạn có chắc chắn muốn thêm màu sắc này?'
        : 'Bạn có chắc chắn muốn cập nhật thông tin màu sắc này?',
      positiveText: 'Đồng ý',
      negativeText: 'Hủy',
      onPositiveClick: async () => {
        try {
          if (modalMode.value === 'add') {
            await createColor(formData)
            message.success('Thêm màu thành công')
          }
          else {
            if (!formData.id)
              return
            await updateColor(formData.id, formData)
            message.success('Cập nhật màu thành công')
          }
          showModal.value = false
          fetchData()
        }
        catch (e) {
          message.error('Có lỗi xảy ra')
        }
      },
    })
  })
}

// Hàm đổi trạng thái (từ Popconfirm)
async function handleStatusChange(row: AdColorResponse) {
  try {
    loading.value = true
    await updateColorStatus(row.id)
    message.success(`Đã cập nhật trạng thái: ${row.name}`)
    fetchData()
  }
  catch {
    message.error('Cập nhật trạng thái thất bại')
    loading.value = false
  }
}

// ================= COLUMNS =================
const columns: DataTableColumns<AdColorResponse> = [
  {
    title: 'STT',
    key: 'index',
    width: 60, // Cố định
    align: 'center',
    fixed: 'left', // Ghim trái
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Tên màu',
    key: 'name',
    minWidth: 150, // Co giãn
    fixed: 'left', // Ghim trái (tùy chọn, nếu bảng rộng)
    ellipsis: { tooltip: true },
    render: row => h('strong', { class: 'text-gray-700' }, row.name),
  },
  {
    title: 'Mã màu',
    key: 'code',
    width: 120, // Cố định
    align: 'center',
    render: (row) => {
      return h('div', { class: 'flex items-center justify-center gap-2' }, [
        h('div', {
          style: {
            width: '24px',
            height: '24px',
            backgroundColor: row.code,
            borderRadius: '6px',
            border: '1px solid #e5e7eb',
            boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
          },
        }),
        h('span', { class: 'font-mono text-xs text-gray-500' }, row.code),
      ])
    },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120, // Cố định
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
          trigger: () => h(
            NSwitch,
            {
              value: row.status === 'ACTIVE',
              size: 'small',
              disabled: loading.value,
              // Không bind update trực tiếp
            },
          ),
          default: () => `Bạn có chắc muốn ${row.status === 'ACTIVE' ? 'ngưng hoạt động' : 'kích hoạt'} màu này?`,
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 100, // Cố định
    fixed: 'right', // Ghim phải
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
            <Icon icon="icon-park-outline:platte" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">
            Quản lý Màu sắc
          </span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách các tùy chọn màu sắc sản phẩm</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-sm mb-4 border border-gray-100">
      <template #header-extra>
        <NButton circle secondary type="primary" @click="handleRefresh">
          <template #icon>
            <Icon icon="carbon:filter-reset" />
          </template>
        </NButton>
      </template>

      <NForm label-placement="top" :show-feedback="false">
        <NGrid :x-gap="24" :y-gap="12" cols="1 600:2 900:3">
          <NGridItem>
            <NFormItem label="Tìm kiếm">
              <NInput
                v-model:value="searchState.keyword"
                placeholder="Nhập tên màu..."
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

          <NGridItem>
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

    <NCard title="Danh sách Màu sắc" class="border border-gray-100 rounded-2xl shadow-sm">
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
              @click="fetchData"
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
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        :scroll-x="800"
        striped
        :bordered="false"
        class="rounded-lg overflow-hidden"
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="card"
      style="width: 500px"
      :title="modalMode === 'add' ? 'Thêm Màu sắc' : 'Cập nhật Màu sắc'"
      :bordered="false"
      size="huge"
    >
      <NForm
        ref="formRef"
        label-placement="top"
        :model="formData"
        :rules="rules"
      >
        <NGrid cols="1" :y-gap="12">
          <NGridItem>
            <NFormItem label="Tên màu" required path="name">
              <NInput v-model:value="formData.name" placeholder="VD: Xám không gian, Bạc..." />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Mã màu (Hex)" required path="code">
              <div class="w-full">
                <NColorPicker
                  v-model:value="formData.code"
                  :modes="['hex']"
                  :show-alpha="false"
                  :swatches="[
                    '#000000', '#FFFFFF', '#FF0000', '#00FF00', '#0000FF',
                    '#FFFF00', '#00FFFF', '#FF00FF', '#C0C0C0', '#808080',
                  ]"
                />
              </div>
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">
            Hủy
          </NButton>
          <NButton type="primary" @click="handleSave">
            Lưu thay đổi
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>
