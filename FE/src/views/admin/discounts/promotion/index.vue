<script setup lang="tsx">
import { computed, h, onMounted, reactive, ref } from 'vue'
import type {
  DataTableColumns,
} from 'naive-ui'
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
  NInputNumber,
  NPagination,
  NRadio,
  NRadioGroup,
  NSpace,
  NTag,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import {

  getAllDiscounts,

} from '@/service/api/admin/discount/discountApi'
import type { DiscountResponse, ParamsGetDiscount } from '@/service/api/admin/discount/discountApi'

import { useRouter } from 'vue-router'

const router = useRouter()

function navigateToAddPage() {
  router.push('/discounts/campaign/add')
}

function navigateToEditPage(row: DiscountResponse) {
  router.push(`/discounts/campaign/edit/${row.id}`)
}

const message = useMessage()
const tableData = ref<DiscountResponse[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchForm = reactive({
  q: '',
  discountStatus: 0 as number | null,
  percentageRange: [null, null] as [number | null, number | null],
  dateRange: null as [number, number] | null,
  sortBy: 'createdDate_desc',
})

const statistics = reactive({
  active: 0,
  upcoming: 0,
  expired: 0,
  total: 0,
})

function formatDateTime(timestamp: number | undefined) {
  if (!timestamp)
    return '-'
  return new Date(timestamp).toLocaleString('en-GB', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).replace(',', '')
}

function getStatus(item: DiscountResponse) {
  const now = Date.now()
  if (item.startTime && item.endTime) {
    if (now >= item.startTime && now <= item.endTime)
      return 'Đang diễn ra'
    if (now < item.startTime)
      return 'Sắp diễn ra'
  }
  return 'Đã hết hạn'
}

function getStatusType(item: DiscountResponse) {
  const status = getStatus(item)
  switch (status) {
    case 'Sắp diễn ra': return 'info'
    case 'Đang diễn ra': return 'success'
    case 'Đã hết hạn': return 'default'
    default: return 'default'
  }
}

function getStatusValue(item: DiscountResponse): number {
  const now = Date.now()
  if (item.startTime && item.endTime) {
    if (now >= item.startTime && now <= item.endTime)
      return 0
    if (now < item.startTime)
      return 1
  }
  return 3
}

function calculateStatistics(data: DiscountResponse[]) {
  const now = Date.now()
  statistics.total = data.length
  statistics.active = data.filter(item =>
    item.startTime && item.endTime && now >= item.startTime && now <= item.endTime,
  ).length
  statistics.upcoming = data.filter(item =>
    item.startTime && now < item.startTime,
  ).length
  statistics.expired = data.filter(item =>
    item.endTime && now > item.endTime,
  ).length
}

const filteredTableData = computed(() => {
  let filtered = tableData.value

  if (searchForm.discountStatus !== null) {
    filtered = filtered.filter(item => getStatusValue(item) === searchForm.discountStatus)
  }

  if (searchForm.q && searchForm.q.trim()) {
    const query = searchForm.q.trim().toLowerCase()
    filtered = filtered.filter(item =>
      item.discountName.toLowerCase().includes(query)
      || item.discountCode.toLowerCase().includes(query),
    )
  }

  if (searchForm.percentageRange[0] !== null) {
    filtered = filtered.filter(item => item.percentage >= searchForm.percentageRange[0]!)
  }
  if (searchForm.percentageRange[1] !== null) {
    filtered = filtered.filter(item => item.percentage <= searchForm.percentageRange[1]!)
  }

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

const totalFilteredPages = computed(() => {
  return Math.ceil(filteredTableData.value.length / pageSize.value)
})

function handleReset() {
  searchForm.q = ''
  searchForm.discountStatus = 0
  searchForm.percentageRange = [null, null]
  searchForm.dateRange = null
  currentPage.value = 1
}

function handleAdvancedSearch() {
  currentPage.value = 1
}

let searchTimeout: ReturnType<typeof setTimeout>
function debouncedSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleAdvancedSearch()
  }, 300)
}

async function fetchDiscounts() {
  loading.value = true
  try {
    const params: ParamsGetDiscount = {
      page: 1,
      size: 1000,
      sortBy: searchForm.sortBy,
    }
    const res = await getAllDiscounts(params)
    tableData.value = res.items
    total.value = res.totalItems
    calculateStatistics(res.items)
  }
  catch (e) {
    message.error('Không thể tải dữ liệu')
  }
  finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
}

onMounted(fetchDiscounts)

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
    render: row => row.discountCode.toUpperCase(),
  },
  {
    title: 'Tên chương trình',
    key: 'discountName',
    minWidth: 300,
    ellipsis: { tooltip: true },
    render: row => row.discountName,
  },
  {
    title: 'Giá trị',
    key: 'percentage',
    width: 100,
    align: 'center',
    render: row => `${row.percentage}%`,
  },
  {
    title: 'Ngày Bắt đầu',
    key: 'startTime',
    width: 160,
    render: row => formatDateTime(row.startTime),
  },
  {
    title: 'Ngày Kết thúc',
    key: 'endTime',
    width: 160,
    render: row => formatDateTime(row.endTime),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 140,
    render: row => (
      <NTag type={getStatusType(row)} size="small">
        {getStatus(row)}
      </NTag>
    ),
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    fixed: 'right',
    render(row) {
      return (
        <div class="flex gap-2 justify-center">
          <NButton size="small" quaternary circle onClick={() => navigateToEditPage(row)} title="Chỉnh sửa">
            {{ icon: () => <Icon icon="carbon:edit" width="18" /> }}
          </NButton>
        </div>
      )
    },
  },
]
</script>

<template>
  <NCard :bordered="false" class="mb-4">
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24" color="#2080f0">
          <Icon icon="carbon:carbon-ui-builder" />
        </NIcon>
        <span class="header-title">
          Quản lý Đợt giảm giá
        </span>
      </NSpace>
      <span style="color: #666">Quản lý danh sách đợt giảm giá</span>
    </NSpace>
  </NCard>

  <NCard title="Bộ lọc" style="margin-top: 16px">
    <NForm label-placement="top">
      <NGrid :cols="24" :x-gap="24" :y-gap="16">
        <NGridItem :span="9">
          <NFormItem label="Tìm kiếm">
            <NInput
              v-model:value="searchForm.q"
              placeholder="Nhập tên đợt giảm giá, mã..."
              @input="debouncedSearch"
            >
              <template #prefix>
                <NIcon><Icon icon="carbon:search" /></NIcon>
              </template>
            </NInput>
          </NFormItem>
        </NGridItem>

        <NGridItem :span="15">
          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="searchForm.discountStatus" name="statusGroup" @update:value="handleAdvancedSearch">
              <NSpace>
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
                  Đã hết hạn
                </NRadio>
              </NSpace>
            </NRadioGroup>
          </NFormItem>
        </NGridItem>

        <NGridItem :span="6">
          <NFormItem label="Phần trăm giảm giá (%)">
            <div class="percentage-group">
              <NInputNumber
                v-model:value="searchForm.percentageRange[0]"
                placeholder="0"
                :min="0" :max="100" :show-button="false"
                class="flex-1"
                @update:value="debouncedSearch"
              />
              <span class="separator">-</span>
              <NInputNumber
                v-model:value="searchForm.percentageRange[1]"
                placeholder="100"
                :min="0" :max="100" :show-button="false"
                class="flex-1"
                @update:value="debouncedSearch"
              />
            </div>
          </NFormItem>
        </NGridItem>

        <NGridItem :span="7">
          <NFormItem label="Ngày bắt đầu - ngày kết thúc">
            <NDatePicker
              v-model:value="searchForm.dateRange"
              type="daterange"
              start-placeholder="Start Date"
              end-placeholder="End Date"
              style="width: 100%"
              format="dd/MM/yyyy"
              @update:value="handleAdvancedSearch"
            />
          </NFormItem>
        </NGridItem>

        <NGridItem :span="2">
          <NFormItem label="&nbsp;">
            <NButton
              secondary
              circle
              type="default"
              ghost
              title="Làm mới bộ lọc"
              @click="handleReset"
            >
              <template #icon>
                <NIcon size="20">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
            </NButton>
          </NFormItem>
        </NGridItem>
      </NGrid>
    </NForm>
  </NCard>

  <NCard title="Danh sách đợt giảm giá" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NButton
          type="primary"
          circle
          size="large"
          title="Thêm mới"
          @click="navigateToAddPage"
        >
          <template #icon>
            <NIcon size="24">
              <Icon icon="carbon:add" />
            </NIcon>
          </template>
        </NButton>
      </NSpace>
    </template>

    <NDataTable
      :columns="columns"
      :data="paginatedData"
      :loading="loading"
      :row-key="(row) => row.id"
      :pagination="false"
      size="small"
      :bordered="false"
      striped
      max-height="400"
    />

    <div class="flex justify-end mt-4">
      <NPagination
     
        :page="currentPage"
        :page-size="pageSize"
        :page-count="totalFilteredPages"
        @update:page="handlePageChange"
      />
    </div>
  </NCard>
</template>

<style scoped>
.mb-4 { margin-bottom: 16px; }
.mt-4 { margin-top: 16px; }
.shadow-sm { box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03); }
.flex { display: flex; }
.items-end { align-items: flex-end; }
.justify-end { justify-content: flex-end; }
.justify-center { justify-content: center; } /* Căn giữa phân trang */
.gap-2 { gap: 8px; }
.flex-1 { flex: 1; }

.header-title {
  font-weight: 600;
  font-size: 24px;
}

.percentage-group {
  display: flex;
  align-items: center;
  width: 100%;
}
.separator {
  padding: 0 8px;
  color: #9ca3af;
}
</style>
