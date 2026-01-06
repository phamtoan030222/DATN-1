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
import type { DataTableColumns } from 'naive-ui'
import { debounce } from 'lodash'
import {
  changeGPUStatus,
  getGPUs,
} from '@/service/api/admin/product/gpu.api'
import type { ADProductGPUResponse } from '@/service/api/admin/product/gpu.api'
import ADProductGPUModal from './component/ADProductGPUModal.vue'

// ================= STATE =================
const message = useMessage()
const notification = useNotification()
const loading = ref(false)

const state = reactive({
  search: {
    q: '',
    brand: null as string | null,
    generation: '',
    series: '',
    releaseYear: null as number | null,
    status: null as string | null, // <--- Thêm state status
  },
  data: {
    GPUs: [] as ADProductGPUResponse[],
  },
  pagination: {
    page: 1,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  },
})

// Options
const brandOptionsSelect = [
  { label: 'NVIDIA', value: 'NVIDIA' },
  { label: 'AMD', value: 'AMD' },
  { label: 'Intel', value: 'Intel' },
  { label: 'Apple', value: 'Apple' },
]

// ================= API CALL =================
async function fetchGPUs() {
  loading.value = true
  try {
    const yearToSend = state.search.releaseYear
      ? new Date(state.search.releaseYear).getFullYear()
      : null

    const res = await getGPUs({
      page: state.pagination.page,
      size: state.pagination.size,
      q: state.search.q || undefined, // Backend có thể dùng 'q' hoặc 'name'
      name: state.search.q || undefined,
      brand: state.search.brand || undefined,
      generation: state.search.generation || undefined,
      series: state.search.series || undefined,
      releaseYear: yearToSend,
      status: state.search.status || undefined, // <--- Gửi status
    })

    state.data.GPUs = res.data.data || []
    state.pagination.totalPages = res.data.totalPages || 0
    state.pagination.totalElements = res.data.totalElements || 0
  }
  catch (error) {
    message.error('Lỗi tải dữ liệu GPU')
  }
  finally {
    loading.value = false
  }
}

const debouncedFetch = debounce(() => {
  state.pagination.page = 1
  fetchGPUs()
}, 500)

function refreshFilter() {
  state.search.q = ''
  state.search.brand = null
  state.search.generation = ''
  state.search.series = ''
  state.search.releaseYear = null
  state.search.status = null // <--- Reset status
  state.pagination.page = 1
  fetchGPUs()
}

// Watch filters
watch(
  () => [state.search.brand, state.search.releaseYear, state.search.status], // <--- Watch status
  () => {
    state.pagination.page = 1
    fetchGPUs()
  },
)

onMounted(fetchGPUs)

// ================= CRUD =================
const isOpenModal = ref(false)
const isDetailModal = ref(true)
const GPUIDSelected = ref<string | undefined>()

function clickOpenModal(id?: string, isDetail?: boolean) {
  GPUIDSelected.value = id
  isOpenModal.value = true
  isDetailModal.value = isDetail ?? false
}

function closeModal() {
  isOpenModal.value = false
  GPUIDSelected.value = undefined
}

function handleSuccessModifyModal() {
  fetchGPUs()
  closeModal()
}

async function handleChangeStatus(id: string) {
  try {
    loading.value = true
    const res = await changeGPUStatus(id)
    if (res.success) {
      notification.success({ content: 'Thay đổi trạng thái thành công', duration: 3000 })
      fetchGPUs()
    }
    else {
      notification.error({ content: 'Thay đổi trạng thái thất bại', duration: 3000 })
    }
  }
  catch (e) {
    notification.error({ content: 'Lỗi hệ thống', duration: 3000 })
  }
  finally {
    loading.value = false
  }
}

// ================= COLUMNS =================
const columns: DataTableColumns<ADProductGPUResponse> = [
  {
    title: 'STT',
    key: 'orderNumber',
    width: 60,
    align: 'center',
    render: (_, index) => (state.pagination.page - 1) * state.pagination.size + index + 1,
  },
  {
    title: 'Mã GPU',
    key: 'code',
    width: 150,
    render: row => h('strong', { class: 'text-primary' }, row.code),
  },
  {
    title: 'Tên GPU',
    key: 'name',
    minWidth: 200,
    ellipsis: { tooltip: true },
  },
  { title: 'Hãng', key: 'brand', width: 120, align: 'center' },
  { title: 'Thế hệ', key: 'generation', width: 120, align: 'center' },
  { title: 'Dòng', key: 'series', width: 120, align: 'center' },
  { title: 'Năm SX', key: 'releaseYear', width: 100, align: 'center' },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: (row) => {
      return h(
        NSwitch,
        {
          value: row.status === 'ACTIVE',
          size: 'small',
          disabled: loading.value,
          onUpdateValue: () => handleChangeStatus(row.id as string),
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'action',
    width: 100,
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

// ================= PAGINATION =================
function handlePageChange(page: number) {
  state.pagination.page = page
  fetchGPUs()
}

function handlePageSizeChange(pageSize: number) {
  state.pagination.size = pageSize
  state.pagination.page = 1
  fetchGPUs()
}
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-green-600">
            <Icon icon="icon-park-outline:graphic-design" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý GPU
          </span>
        </NSpace>
        <span>Quản lý danh sách card đồ họa (GPU) và thông số kỹ thuật</span>
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
                placeholder="Tên GPU, mã..."
                clearable
                @input="debouncedFetch"
                @keydown.enter="fetchGPUs"
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
                placeholder="VD: RTX 40 Series..."
                clearable
                @input="debouncedFetch"
              />
            </NFormItem>
          </NGridItem>

          <NGridItem>
            <NFormItem label="Dòng GPU">
              <NInput
                v-model:value="state.search.series"
                placeholder="VD: GeForce, Radeon..."
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

    <NCard title="Danh sách GPU" class="border rounded-3">
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
              @click="fetchGPUs"
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
        :data="state.data.GPUs"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
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

    <ADProductGPUModal
      :id="GPUIDSelected"
      :is-detail="isDetailModal"
      :is-open="isOpenModal"
      @success="handleSuccessModifyModal"
      @close="closeModal"
    />
  </div>
</template>
