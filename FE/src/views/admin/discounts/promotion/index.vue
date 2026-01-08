<script setup lang="tsx">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NPagination,
  NRadio,
  NRadioGroup,
  NSpace,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { getAllDiscounts } from '@/service/api/admin/discount/discountApi'
import type { DiscountResponse, ParamsGetDiscount } from '@/service/api/admin/discount/discountApi'
import { useRouter } from 'vue-router'

// ================= CONSTANTS & STATE =================
const router = useRouter()
const message = useMessage()

const tableData = ref<DiscountResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// Filter Form
const searchForm = reactive({
  q: '',
  discountStatus: null as number | null,
  percentageRange: [null, null] as [number | null, number | null],
  dateRange: null as [number, number] | null,
  sortBy: 'createdDate_desc',
})

// ================= HELPERS =================
function formatDateTime(timestamp: number | undefined) {
  if (!timestamp)
    return '-'
  return new Date(timestamp).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function getStatusInfo(item: DiscountResponse) {
  const now = Date.now()
  if (item.startTime && item.endTime) {
    if (now >= item.startTime && now <= item.endTime)
      return { text: 'Đang diễn ra', type: 'success', value: 0 } // Active
    if (now < item.startTime)
      return { text: 'Sắp diễn ra', type: 'info', value: 1 } // Upcoming
  }
  return { text: 'Đã hết hạn', type: 'default', value: 3 } // Expired
}

// ================= API & ACTIONS =================
async function fetchDiscounts() {
  loading.value = true
  try {
    const params: ParamsGetDiscount = {
      page: 1, // Lấy tất cả về client filter
      size: 1000,
      sortBy: searchForm.sortBy,
    }
    const res = await getAllDiscounts(params)
    tableData.value = res.items || []
    total.value = res.totalItems || 0
  }
  catch (e) {
    message.error('Không thể tải dữ liệu')
  }
  finally {
    loading.value = false
  }
}

function handleReset() {
  searchForm.q = ''
  searchForm.discountStatus = null
  searchForm.percentageRange = [null, null]
  searchForm.dateRange = null
  currentPage.value = 1
}

function navigateToAddPage() {
  router.push('/discounts/campaign/add')
}

function navigateToEditPage(id: string) {
  router.push(`/discounts/campaign/edit/${id}`)
}

// ================= COMPUTED DATA =================
const filteredTableData = computed(() => {
  let filtered = tableData.value

  // Filter Status
  if (searchForm.discountStatus !== null) {
    filtered = filtered.filter(item => getStatusInfo(item).value === searchForm.discountStatus)
  }

  // Filter Search Text
  if (searchForm.q && searchForm.q.trim()) {
    const query = searchForm.q.trim().toLowerCase()
    filtered = filtered.filter(item =>
      item.discountName.toLowerCase().includes(query)
      || item.discountCode.toLowerCase().includes(query),
    )
  }

  // Filter Percentage
  if (searchForm.percentageRange[0] !== null) {
    filtered = filtered.filter(item => item.percentage >= searchForm.percentageRange[0]!)
  }
  if (searchForm.percentageRange[1] !== null) {
    filtered = filtered.filter(item => item.percentage <= searchForm.percentageRange[1]!)
  }

  // Filter Date
  if (searchForm.dateRange) {
    const [startTs, endTs] = searchForm.dateRange
    const filterStart = new Date(startTs).setHours(0, 0, 0, 0)
    const filterEnd = new Date(endTs).setHours(23, 59, 59, 999)
    filtered = filtered.filter((item) => {
      return item.startTime >= filterStart && item.endTime <= filterEnd
    })
  }

  return filtered
})

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTableData.value.slice(start, end)
})

const totalFiltered = computed(() => filteredTableData.value.length)


const columns: DataTableColumns<DiscountResponse> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Mã giảm giá',
    key: 'discountCode',
    width: 200,
    render: row => h('strong', { class: 'text-primary' }, row.discountCode.toUpperCase()),
  },
  {
    title: 'Tên chương trình',
    key: 'discountName',
    minWidth: 200,
    ellipsis: { tooltip: true },
    render: row => row.discountName,
  },
  {
    title: 'Giá trị',
    key: 'percentage',
    width: 150,
    align: 'center',
    render: row => h(NTag, { type: 'error', size: 'small' }, { default: () => `${row.percentage}%` }),
  },
  {
    title: 'Ngày bắt đầu',
    key: 'startTime',
    width: 180,
    render: row => formatDateTime(row.startTime),
  },
  {
    title: 'Ngày kết thúc',
    key: 'endTime',
    width: 170,
    render: row => formatDateTime(row.endTime),
  },
  // --------------------------
  {
    title: 'Trạng thái',
    key: 'status',
    width: 150,
    align: 'center',
    render: (row) => {
      const statusInfo = getStatusInfo(row)
      return h(
        NTag,
        {
          type: statusInfo.type,
          size: 'small',
          style: { cursor: 'pointer' },
          onClick: () => {
            searchForm.discountStatus = statusInfo.value
            currentPage.value = 1
          },
        },
        { default: () => statusInfo.text },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 90,
    align: 'center',
    fixed: 'right',
    render(row) {
      const isEditable = true

      return h(
        NTooltip,
        { trigger: 'hover' },
        {
          trigger: () => h(NButton, {
            size: 'small',
            type: 'warning',
            secondary: true,
            circle: true,
            disabled: !isEditable,
            class: 'transition-all hover:scale-125 hover:shadow-lg',
            onClick: () => navigateToEditPage(row.id),
          }, { icon: () => h(Icon, { icon: 'carbon:edit' }) }),
          default: () => 'Chỉnh sửa chương trình',
        },
      )
    },
  },
]

// ================= HOOKS =================
onMounted(fetchDiscounts)

// Reset page when filter changes
watch(searchForm, () => { currentPage.value = 1 })
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="carbon:carbon-ui-builder" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Đợt giảm giá
          </span>
        </NSpace>
        <span>Quản lý các chiến dịch giảm giá theo phần trăm sản phẩm</span>
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
                title="Làm mới bộ lọc"
                @click="handleReset"
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
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-2">
          <NFormItem label="Tìm kiếm chung">
            <NInput
              v-model:value="searchForm.q"
              placeholder="Tên chương trình, mã..."
              clearable
            >
              <template #prefix>
                <Icon icon="carbon:search" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="searchForm.discountStatus" name="statusGroup">
              <div class="flex flex-wrap gap-2">
                <NRadio :value="null">
                  Tất cả
                </NRadio>
                <NRadio :value="1">
                  Sắp diễn ra
                </NRadio>
                <NRadio :value="0">
                  Đang diễn ra
                </NRadio>
                <NRadio :value="3">
                  Đã kết thúc
                </NRadio>
              </div>
            </NRadioGroup>
          </NFormItem>

          <NFormItem label="Mức giảm giá (%)">
            <div class="flex items-center w-full gap-2">
              <NInputNumber
                v-model:value="searchForm.percentageRange[0]"
                placeholder="0"
                :min="0" :max="100" :show-button="false"
                class="flex-1"
              />
              <span class="text-gray-400">-</span>
              <NInputNumber
                v-model:value="searchForm.percentageRange[1]"
                placeholder="100"
                :min="0" :max="100" :show-button="false"
                class="flex-1"
              />
            </div>
          </NFormItem>

          <NFormItem label="Thời gian áp dụng">
            <NDatePicker
              v-model:value="searchForm.dateRange"
              type="daterange"
              clearable
              class="w-full"
              start-placeholder="Bắt đầu"
              end-placeholder="Kết thúc"
              format="dd/MM/yyyy"
            />
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <NCard title="Danh sách đợt giảm giá" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="navigateToAddPage"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tạo đợt giảm giá
              </span>
            </NButton>


            <NButton
              type="info"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="fetchDiscounts"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tải lại dữ liệu
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable
        :columns="columns"
        :data="paginatedData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        striped
        :scroll-x="1000"
      />


      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="totalFiltered"
          :page-sizes="[5, 10, 20, 50]"
          show-size-picker
        />
      </div>
    </NCard>
  </div>
</template>
