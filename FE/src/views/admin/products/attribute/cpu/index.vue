<script setup lang="tsx">
import { h, onMounted, reactive, ref, watch } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTooltip,
  useMessage,
  // Xóa useNotification
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { debounce } from 'lodash'
import {
  changeCPUStatus,
  getCPUs,
} from '@/service/api/admin/product/cpu.api'
import type { ADProductCPUResponse } from '@/service/api/admin/product/cpu.api'
import ADProductCPUModal from './component/ADProductCPUModal.vue'

// ================= STATE =================
const message = useMessage() // Sử dụng useMessage
// const notification = useNotification() -> Xóa dòng này

const state = reactive({
  search: {
    q: '',
    brand: null as string | null,
    generation: '',
    series: '',
    releaseYear: null as number | null,
    status: null as string | null,
  },
  data: {
    cpus: [] as ADProductCPUResponse[],
  },
  pagination: {
    page: 1,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  },
})

const loading = ref(false)

// Select Options
const brandOptionsSelect = [
  { label: 'Intel', value: 'Intel' },
  { label: 'AMD', value: 'AMD' },
  { label: 'Apple', value: 'Apple' },
]

// ================= API CALL =================
async function fetchCPUs() {
  loading.value = true
  try {
    const yearToSend = state.search.releaseYear
      ? new Date(state.search.releaseYear).getFullYear()
      : null

    const res = await getCPUs({
      page: state.pagination.page,
      size: state.pagination.size,
      name: state.search.q || undefined,
      brand: state.search.brand || undefined,
      releaseYear: yearToSend,
      series: state.search.series || undefined,
      generation: state.search.generation || undefined,
      status: state.search.status || undefined,
    })

    state.data.cpus = res.data.data || []
    state.pagination.totalElements = res.data.totalItems || 0
    state.pagination.totalPages = Math.ceil(res.data.totalItems / state.pagination.size)
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu CPU')
  }
  finally {
    loading.value = false
  }
}

const debouncedFetch = debounce(() => {
  state.pagination.page = 1
  fetchCPUs()
}, 500)

function refreshFilter() {
  state.search.q = ''
  state.search.brand = null
  state.search.generation = ''
  state.search.series = ''
  state.search.releaseYear = null
  state.search.status = null
  state.pagination.page = 1
  fetchCPUs()
}

watch(
  () => [state.search.brand, state.search.releaseYear, state.search.status],
  () => {
    state.pagination.page = 1
    fetchCPUs()
  },
)

onMounted(fetchCPUs)

// ================= CRUD =================
const isOpenModal = ref(false)
const isDetailModal = ref(true)
const cpuIDSelected = ref<string | undefined>()

function clickOpenModal(id?: string, isDetail?: boolean) {
  cpuIDSelected.value = id
  isOpenModal.value = true
  isDetailModal.value = isDetail ?? false
}

function closeModal() {
  isOpenModal.value = false
  cpuIDSelected.value = undefined
}

function handleSuccessModifyModal() {
  fetchCPUs()
  closeModal()
}

// Cập nhật hàm này để dùng message
async function handleChangeStatus(id: string) {
  try {
    loading.value = true
    const res: any = await changeCPUStatus(id)
    // Kiểm tra response tùy theo format BE (res.success hoặc res.data)
    if (res.success || res.code === 200 || res) {
      message.success('Thay đổi trạng thái thành công') // Dùng message
      fetchCPUs()
    }
    else {
      message.error('Thay đổi trạng thái thất bại')
    }
  }
  catch (e) {
    message.error('Lỗi hệ thống khi cập nhật trạng thái')
  }
  finally {
    loading.value = false
  }
}

// ================= COLUMNS =================
const columns: DataTableColumns<ADProductCPUResponse> = [
  {
    title: 'STT',
    key: 'orderNumber',
    width: 60,
    align: 'center',
    fixed: 'left',
    render: (_, index) => (state.pagination.page - 1) * state.pagination.size + index + 1,
  },
  {
    title: 'Mã CPU',
    key: 'code',
    width: 100,
    fixed: 'left',
    render: row => h('strong', { class: 'text-primary' }, row.code),
  },
  {
    title: 'Tên CPU',
    key: 'name',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: row => h('span', { class: 'text-gray-700 cursor-pointer hover:text-primary transition-colors', onClick: () => clickOpenModal(row.id, false) }, row.name),
  },
  {
    title: 'Hãng',
    key: 'brand',
    width: 100,
    align: 'center',
  },
  { title: 'Dòng', key: 'series', align: 'center', width: 120 },
  { title: 'Thế hệ', key: 'generation', align: 'center', width: 120 },
  { title: 'Năm SX', key: 'releaseYear', width: 100, align: 'center' },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: (row) => {
      // Bọc NSwitch bằng NPopconfirm để xác nhận trước khi đổi
      return h(
        NPopconfirm,
        {
          onPositiveClick: () => handleChangeStatus(row.id),
        },
        {
          trigger: () => h(
            NSwitch,
            {
              value: row.status === 'ACTIVE',
              size: 'small',
              disabled: loading.value,
              // Không binding onUpdateValue để chặn thay đổi trực tiếp
              // Sự thay đổi chỉ xảy ra khi fetchCPUs lại sau khi API thành công
            },
          ),
          default: () => `Bạn có chắc muốn ${row.status === 'ACTIVE' ? 'ngưng hoạt động' : 'kích hoạt'} CPU này?`,
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'action',
    width: 80,
    align: 'center',
    fixed: 'right',
    render: (row) => {
      return h('div', { class: 'flex justify-center gap-2' }, [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            secondary: true,
            type: 'warning',
            circle: true,
            class: 'transition-all duration-200 hover:scale-125 hover:shadow-md',
            onClick: () => clickOpenModal(row.id, true),
          }, { icon: () => h(Icon, { icon: 'carbon:edit' }) }),
          default: () => 'Sửa thông tin',
        }),
      ])
    },
  },
]

// ================= PAGINATION HANDLER =================
function handlePageChange(page: number) {
  state.pagination.page = page
  fetchCPUs()
}

function handlePageSizeChange(pageSize: number) {
  state.pagination.size = pageSize
  state.pagination.page = 1
  fetchCPUs()
}
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="icon-park-outline:cpu" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý CPU
          </span>
        </NSpace>
        <span>Quản lý danh sách CPU và thông số kỹ thuật</span>
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
                @click="refreshFilter"
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
            <NFormItem label="Tìm kiếm chung">
              <NInput
                v-model:value="state.search.q"
                placeholder="Tên CPU, mã..."
                clearable
                @input="debouncedFetch"
                @keydown.enter="fetchCPUs"
              >
                <template #prefix>
                  <Icon icon="carbon:search" />
                </template>
              </NInput>
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Hãng sản xuất">
              <NSelect
                v-model:value="state.search.brand"
                :options="brandOptionsSelect"
                placeholder="Chọn hãng"
                clearable
              />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Năm phát hành">
              <NDatePicker
                v-model:value="state.search.releaseYear"
                type="year"
                placeholder="Chọn năm"
                clearable
                class="w-full"
              />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Thế hệ">
              <NInput
                v-model:value="state.search.generation"
                placeholder="VD: Gen 12, Ryzen 5000..."
                clearable
                @input="debouncedFetch"
              />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Dòng CPU">
              <NInput
                v-model:value="state.search.series"
                placeholder="VD: Core i5, Ryzen 7..."
                clearable
                @input="debouncedFetch"
              />
            </NFormItem>
          </NGridItem>

          <NGridItem span="2">
            <NFormItem label="Trạng thái">
              <NRadioGroup v-model:value="state.search.status" name="radiogroup">
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

    <NCard title="Danh sách CPU" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="clickOpenModal()"
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
              @click="fetchCPUs"
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
        :data="state.data.cpus"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        :scroll-x="1000"
        striped
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="state.pagination.page"
          v-model:page-size="state.pagination.size"
          :item-count="state.pagination.totalElements"
          :page-sizes="[5, 10, 20, 50]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </NCard>

    <ADProductCPUModal
      :id="cpuIDSelected"
      :is-detail="isDetailModal"
      :is-open="isOpenModal"
      @success="handleSuccessModifyModal"
      @close="closeModal"
    />
  </div>
</template>
