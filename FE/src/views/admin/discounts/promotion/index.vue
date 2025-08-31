
<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace :align="'center'">
        <NIcon size="24">
          <Icon :icon="'carbon:carbon-ui-builder'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Quản lý Đợt giảm giá
        </span>
      </NSpace>
      <span>Quản lý danh sách đợt giảm giá</span>
    </NSpace>
  </n-card>

  <!-- Filter Section -->
  <NCard title="Bộ lọc" style="margin-top: 16px ">
    <!-- Quick Filter -->
    <div style="margin-bottom: 16px">
      <NSpace>
        <NButton
          v-for="status in statusOptions" 
          :key="status.value"
          :type="quickFilter === status.value ? 'primary' : 'default'"
          @click="handleQuickFilterChange(status.value)"
        >
          {{ status.label }} ({{ getBadgeCount(status.value) }})
        </NButton>
      </NSpace>
    </div>

    <!-- Toggle Advanced Filter -->
    <div style="margin-bottom: 16px">
      <NButton text @click="toggleAdvancedFilter">
        <template #icon>
          <NIcon size="16">
            <Icon :icon="'carbon:search'" />
          </NIcon>
        </template>
        {{ showAdvancedFilter ? 'Ẩn bộ lọc' : 'Hiển thị bộ lọc' }}
      </NButton>
    </div>

    <!-- Advanced Filters -->
    <div v-if="showAdvancedFilter" style="border-top: 1px solid #f0f0f0; padding-top: 16px; margin-top: 16px " >
      <div style="margin-bottom: 16px; font-weight: 500">Bộ lọc nâng cao</div>  
      <NForm>
        <NGrid cols="3" x-gap="16" y-gap="16">
          <NGridItem>
            <NFormItem label="Tên đợt giảm giá">
              <NInput 
                v-model:value="searchForm.q"
                placeholder="Nhập tên đợt giảm giá"
                @input="debouncedSearch"
                clearable
              />
            </NFormItem>
          </NGridItem>
          
          <NGridItem>
            <NFormItem label="Mã giảm giá">
              <NInput 
                v-model:value="searchForm.discountCode"
                placeholder="Nhập mã giảm giá"
                @input="debouncedSearch"
                clearable
              />
            </NFormItem>
          </NGridItem>
          
          <NGridItem>
            <NFormItem label="Phần trăm giảm giá (%)">
              <NSpace>
                <NInputNumber 
                  v-model:value="searchForm.percentageRange[0]"
                  placeholder="0" 
                  :min="0" 
                  :max="100"
                  @update:value="handleAdvancedSearch"
                  style="width: 100px"
                />
                <span>đến</span>
                <NInputNumber 
                  v-model:value="searchForm.percentageRange[1]"
                  placeholder="100" 
                  :min="0" 
                  :max="100"
                  @update:value="handleAdvancedSearch"
                  style="width: 100px"
                />
              </NSpace>
            </NFormItem>
          </NGridItem>
          <NGridItem>
  <NFormItem label="Ngày bắt đầu">
    <input
      type="date"
      v-model="searchForm.startDate"
      @change="handleAdvancedSearch"
      placeholder="Chọn ngày bắt đầu"
      style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px;"
    />
  </NFormItem>
</NGridItem>

<NGridItem>
  <NFormItem label="Ngày kết thúc">
    <input
      type="date"
      v-model="searchForm.endDate"
      @change="handleAdvancedSearch"
      placeholder="Chọn ngày kết thúc"
      style="width: 100%; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px;"
    />
  </NFormItem>
</NGridItem>
        </NGrid>

        <NSpace style="margin-top: 16px">
          <NButton type="primary" @click="handleAdvancedSearch" :loading="loading">
            <template #icon>
              <NIcon>
                <Icon :icon="'carbon:search'" />
              </NIcon>
            </template>
            Tìm kiếm
          </NButton>
          <NButton @click="handleReset">
            <template #icon>
              <NIcon>
                <Icon :icon="'carbon:rotate'" />
              </NIcon>
            </template>
            Đặt lại
          </NButton>
        </NSpace>
      </NForm>
    </div>
  </NCard>

  <!-- Active Filters -->
  <NCard v-if="activeFilters.length > 0" style="margin-top: 16px">
    <template #header>
      <NSpace justify="space-between">
        <span>Bộ lọc đang áp dụng</span>
        <NButton text @click="clearAllFilters">Xóa tất cả</NButton>
      </NSpace>
    </template>
    <NSpace>
      <NTag 
        v-for="filter in activeFilters" 
        :key="filter.key"
        closable
        @close="removeFilter(filter.key)"
      >
        {{ filter.label }}
      </NTag>
    </NSpace>
  </NCard>

  <!-- Main Table -->
  <NCard title="Danh sách đợt giảm giá" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NInput
          v-model:value="searchForm.q"
          placeholder="Tìm kiếm đợt giảm giá..."
          clearable
          style="width: 220px"
          @input="debouncedSearch"
        >
          <template #prefix>
            <NIcon size="18">
              <Icon :icon="'carbon:search'" />
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
            <Icon :icon="'carbon:add'" />
          </NIcon>
        </NButton>
        <NButton
          type="primary"
          secondary
          circle
          title="Làm mới"
          @click="refreshTable"
        >
          <NIcon size="24">
            <Icon :icon="'carbon:rotate'" />
          </NIcon>
        </NButton>
        <NPopconfirm @positive-click="handleDeleteSelected"
         positive-text="Xóa"
        negative-text="Hủy bỏ"
         >
          <template #trigger>
            <NButton type="error" secondary circle title="Xóa hàng loạt">
              <NIcon size="24">
                <Icon :icon="'icon-park-outline:delete'" />
              </NIcon>
            </NButton>
          </template>
          Xác nhận xóa tất cả đợt giảm giá đã chọn?
        </NPopconfirm>
      </NSpace>
    </template>

    <NDataTable
      :columns="columns"
      :data="paginatedData"
      :loading="loading"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="checkedRowKeys"
      :pagination="false"
      bordered
    />

    <div class="flex justify-center mt-4">
      <NPagination
        :page="currentPage"
        :page-size="pageSize"
        :page-count="totalFilteredPages"
        @update:page="handlePageChange"
      />
    </div>
  </NCard>

  <!-- Modal thêm/sửa với tab quản lý sản phẩm -->
  <NModal
    v-model:show="showModal"
    preset="card"
    style="width: 1000px; max-width: 95vw"
    :title="modalMode === 'add' ? 'Thêm đợt giảm giá' : 'Chỉnh sửa đợt giảm giá'"
  >
    <NTabs v-model:value="modalTab" type="line" animated>
      <NTabPane name="info" tab="Thông tin cơ bản">
        <NForm ref="formRef">
          <NGrid cols="2" x-gap="12" y-gap="16">
            <NGridItem>
              <NFormItem label="Tên đợt giảm giá" required>
                <NInput 
                  v-model:value="formData.discountName" 
                  placeholder="Nhập tên đợt giảm giá"
                  maxlength="100"
                  show-count
                />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Mã giảm giá" required>
                <NInput 
                  v-model:value="formData.discountCode" 
                  placeholder="Nhập mã giảm giá"
                  maxlength="50"
                >
                  <template #suffix>
                    <NButton text @click="generateCode">
                      <NIcon size="16">
                        <Icon :icon="'carbon:rotate'" />
                      </NIcon>
                    </NButton>
                  </template>
                </NInput>
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Phần trăm giảm giá (%)" required>
                <NInputNumber
                  v-model:value="formData.percentage"
                  :min="1"
                  :max="100"
                  placeholder="Nhập % giảm giá"
                  style="width: 100%"
                />
              </NFormItem>
            </NGridItem>
            <NGridItem>
              <NFormItem label="Ngày bắt đầu" required>
                <NDatePicker
                  v-model:value="formData.startDate"
                  type="datetime"
                  placeholder="Chọn ngày bắt đầu"
                  style="width: 100%"
                />
              </NFormItem>
            </NGridItem>
            <NGridItem span="2">
              <NFormItem label="Ngày kết thúc" required>
                <NDatePicker
                  v-model:value="formData.endDate"
                  type="datetime"
                  placeholder="Chọn ngày kết thúc"
                  style="width: 100%"
                  :is-date-disabled="(ts: number) => ts <= formData.startDate"
                />
              </NFormItem>
            </NGridItem>
            <NGridItem span="2">
              <NFormItem label="Mô tả">
                <NInput
                  v-model:value="formData.description"
                  type="textarea"
                  placeholder="Nhập mô tả cho đợt giảm giá"
                  maxlength="500"
                  show-count
                  :rows="3"
                />
              </NFormItem>
            </NGridItem>
          </NGrid>
        </NForm>
      </NTabPane>

      <!-- Tab quản lý sản phẩm (chỉ hiện khi edit hoặc sau khi tạo thành công) -->
      <NTabPane 
        v-if="modalMode === 'edit' || (modalMode === 'add' && modalRow?.id)" 
        name="products" 
        tab="Quản lý sản phẩm"
      >
        <NTabs v-model:value="productTab" type="segment" size="small">
          <NTabPane name="applied" tab="Đã áp dụng">
            <div style="margin-bottom: 16px">
              <NSpace justify="space-between">
                <NInput
                  v-model:value="appliedSearchKeyword"
                  placeholder="Tìm sản phẩm đã áp dụng..."
                  clearable
                  style="width: 300px"
                  @input="debouncedSearchApplied"
                >
                  <template #prefix>
                    <NIcon size="18">
                      <Icon icon="carbon:search" />
                    </NIcon>
                  </template>
                </NInput>
                <NSpace>
                  <NTag type="info" size="small">
                    <template #icon>
                      <NIcon>
                        <Icon icon="carbon:checkmark-filled" />
                      </NIcon>
                    </template>
                    {{ appliedTotal }} sản phẩm
                  </NTag>
                  
<NButton
  v-if="appliedSelectedKeys.length > 0"
  type="error"
  secondary
  size="small"
  @click="handleRemoveProducts"
  :loading="removingProducts"
>
  <template #icon>
    <NIcon><Icon icon="carbon:trash-can" /></NIcon>
  </template>
  Gỡ bỏ ({{ appliedSelectedKeys.length }})
</NButton>
                </NSpace>
              </NSpace>
            </div>

            <NDataTable
              :columns="appliedColumns"
              :data="appliedProducts"
              :loading="loadingApplied"
              :row-key="(row) => row.id"
              v-model:checked-row-keys="appliedSelectedKeys"
              :pagination="false"
              size="small"
              max-height="400px"
              :scroll-x="800"
            />

            <div class="flex justify-center mt-4" v-if="appliedTotal > appliedPageSize">
              <NPagination
                :page="appliedCurrentPage"
                :page-size="appliedPageSize"
                :page-count="Math.ceil(appliedTotal / appliedPageSize)"
                @update:page="handleAppliedPageChange"
                size="small"
              />
            </div>
          </NTabPane>

          <NTabPane name="unapplied" tab="Chưa áp dụng">
            <div style="margin-bottom: 16px">
              <NSpace justify="space-between">
                <NInput
                  v-model:value="unappliedSearchKeyword"
                  placeholder="Tìm sản phẩm chưa áp dụng..."
                  clearable
                  style="width: 300px"
                  @input="debouncedSearchUnapplied"
                >
                  <template #prefix>
                    <NIcon size="18">
                      <Icon icon="carbon:search" />
                    </NIcon>
                  </template>
                </NInput>
                <NSpace>
                  <NTag type="warning" size="small">
                    <template #icon>
                      <NIcon>
                        <Icon icon="carbon:warning" />
                      </NIcon>
                    </template>
                    {{ unappliedTotal }} sản phẩm
                  </NTag>
                  <NButton
                    v-if="unappliedSelectedKeys.length > 0"
                    type="primary"
                    size="small"
                    @click="handleApplySelectedProducts"
                    :loading="applyingProducts"
                  >
                    <template #icon>
                      <NIcon>
                        <Icon icon="carbon:checkmark" />
                      </NIcon>
                    </template>
                    Áp dụng ({{ unappliedSelectedKeys.length }})
                  </NButton>
                </NSpace>
              </NSpace>
            </div>


            <NDataTable
              :columns="unappliedColumns"
              :data="unappliedProducts"
              :loading="loadingUnapplied"
              :row-key="(row) => row.id"
              v-model:checked-row-keys="unappliedSelectedKeys"
              :pagination="false"
              size="small"
              max-height="400px"
              :scroll-x="900"
            />

            <div class="flex justify-center mt-4" v-if="unappliedTotal > unappliedPageSize">
              <NPagination
                :page="unappliedCurrentPage"
                :page-size="unappliedPageSize"
                :page-count="Math.ceil(unappliedTotal / unappliedPageSize)"
                @update:page="handleUnappliedPageChange"
                size="small"
              />
            </div>
          </NTabPane>
        </NTabs>
      </NTabPane>
    </NTabs>

    <template #footer>
      <NSpace justify="space-between">
        <!-- Left side - Tab navigation buttons -->
        <NSpace v-if="modalMode === 'edit' || (modalMode === 'add' && modalRow?.id)">
          <NButton 
            v-if="modalTab === 'products'"
            @click="modalTab = 'info'"
            secondary
          >
            <template #icon>
              <NIcon>
                <Icon icon="carbon:arrow-left" />
              </NIcon>
            </template>
            Quay lại thông tin
          </NButton>
          <NButton 
            v-if="modalTab === 'info' && modalRow?.id"
            @click="() => { modalTab = 'products'; fetchAppliedProducts(); fetchUnappliedProducts(); }"
            secondary
            type="info"
          >
            Quản lý sản phẩm
            <template #icon>
              <NIcon>
                <Icon icon="carbon:arrow-right" />
              </NIcon>
            </template>
          </NButton>
        </NSpace>
        <div v-else></div>

        <!-- Right side - Action buttons -->
        <NSpace justify="end">
          <NButton @click="closeModal">Hủy</NButton>
          <NButton 
            v-if="modalTab === 'info'"
            type="primary" 
            @click="saveDiscount"
          >
            {{ modalMode === 'add' ? 'Thêm' : 'Cập nhật' }}
          </NButton>
        </NSpace>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
.n-data-table {
  border-radius: 8px;
}

.n-tabs .n-tab-pane {
  padding-top: 16px;
}

:deep(.n-data-table-wrapper) {
  border-radius: 8px;
}

:deep(.n-data-table-tbody .n-data-table-tr:hover) {
  background-color: #f8f9fa;
}

:deep(.n-modal .n-card .n-card__content) {
  padding: 20px;
}

.n-tag {
  margin: 2px;
}
</style>

<script setup lang="tsx">
import { onMounted, ref, reactive, computed, h, watch } from "vue";
import {
  NButton,
  NSpace,
  NCard,
  NDataTable,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NGrid,
  NGridItem,
  NPopconfirm,
  NTag,
  NDatePicker,
  NPagination,
  NIcon,
  NTabs,
  NTabPane,
  useMessage,
  FormInst,
  DataTableColumns
} from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  getAllDiscounts,
  createDiscount,
  updateDiscount,
  deleteDiscount,
  deactivateDiscount,
  startDiscount,
  getAppliedProducts,
  getUnappliedProducts,
  applySingleProductToDiscount,
  removeProductsFromDiscount,
  type ApplyDiscountRequest,
  type DiscountResponse,
  type ParamsGetDiscount,
  type CreateDiscountRequest,
  type ProductDetailResponse,
  type AppliedProductResponse,
} from '@/service/api/admin/discount/discountApi'


const message = useMessage();
const tableData = ref<DiscountResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const searchKeyword = ref("");
const showAdvancedFilter = ref(false);
const quickFilter = ref('all');

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= FILTER STATE =================
const searchForm = reactive({
  q: '',
  discountCode: '',
  discountStatus: undefined as number | undefined,
  percentageRange: [null, null] as [number | null, number | null], 
  startDate: '',
  endDate: '',
  sortBy: 'createdDate_desc'
});

const statistics = reactive({
  active: 0,
  upcoming: 0,
  expired: 0,
  total: 0
});

const statusOptions = [
  { value: 'all', label: 'Tất cả' },
  { value: 'active', label: 'Đang diễn ra' },
  { value: 'upcoming', label: 'Sắp diễn ra' },
  { value: 'expired', label: 'Đã hết hạn' }
];

// VAlidate nè
const isDiscountExpired = (discount: DiscountResponse) => {
  const now = Date.now()
  return discount.endTime && now > discount.endTime
}

const isDiscountActive = (discount: DiscountResponse) => {
  const now = Date.now()
  return discount.startTime && discount.endTime && 
         now >= discount.startTime && now <= discount.endTime
}



const hasActiveDiscount = () => {
  return tableData.value.some(discount => isDiscountActive(discount))
}

// ================= có pu tờ =================
const activeFilters = computed(() => {
  const filters: Array<{ key: string; label: string }> = []

  if (searchForm.q) {
    filters.push({ key: 'q', label: `Tên: "${searchForm.q}"` })
  }

  if (searchForm.discountCode) {
    filters.push({ key: 'discountCode', label: `Mã: "${searchForm.discountCode}"` })
  }

  if (searchForm.percentageRange && searchForm.percentageRange[0] !== null && searchForm.percentageRange[1] !== null) {
    filters.push({
      key: 'percentageRange',
      label: `Phần trăm: ${searchForm.percentageRange[0]}% - ${searchForm.percentageRange[1]}%`
    })
  }

  if (searchForm.startDate) {
    filters.push({
      key: 'startDate',
      label: `Từ ngày: ${formatDate(searchForm.startDate)}`
    })
  }

  if (searchForm.endDate) {
    filters.push({
      key: 'endDate',
      label: `Đến ngày: ${formatDate(searchForm.endDate)}`
    })
  }

  if (searchForm.discountStatus !== undefined) {
    const statusMap: { [key: number]: string } = {
      0: 'Đang diễn ra',
      1: 'Sắp diễn ra',
      3: 'Đã hết hạn'
    }
    filters.push({ key: 'discountStatus', label: `Trạng thái: ${statusMap[searchForm.discountStatus]}` })
  }

  return filters
});

// ================= mó đồ =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<DiscountResponse | null>(null);
const formRef = ref<FormInst>();
const modalTab = ref('info'); 
const productTab = ref('applied'); 

const formData = reactive<CreateDiscountRequest>({
  discountName: "",
  discountCode: "",
  percentage: 1,
  startDate: Date.now(),
  endDate: Date.now() + 24 * 60 * 60 * 1000,
  description: ""
});


const appliedProducts = ref<AppliedProductResponse[]>([])
const unappliedProducts = ref<ProductDetailResponse[]>([])
const appliedCurrentPage = ref(1)
const unappliedCurrentPage = ref(1)
const appliedPageSize = ref(10)
const unappliedPageSize = ref(10)
const appliedTotal = ref(0)
const unappliedTotal = ref(0)
const loadingApplied = ref(false)
const loadingUnapplied = ref(false)
const appliedSearchKeyword = ref('')
const unappliedSearchKeyword = ref('')
const appliedSelectedKeys = ref<(string | number)[]>([])
const unappliedSelectedKeys = ref<(string | number)[]>([])
const applyingProducts = ref(false)
const removingProducts = ref(false)

let appliedSearchTimeout: ReturnType<typeof setTimeout>
let unappliedSearchTimeout: ReturnType<typeof setTimeout>

// ================= Chuyển đổi time =================
const formatDateTime = (timestamp: number) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(price)
}

const getStatus = (item: DiscountResponse) => {
  const now = Date.now()
  if (item.startTime && item.endTime) {
    if (now >= item.startTime && now <= item.endTime) return 'Đang diễn ra'
    if (now < item.startTime) return 'Sắp diễn ra'
  }
  return 'Đã hết hạn'
}

const getStatusType = (item: DiscountResponse) => {
  const status = getStatus(item)
  switch (status) {
    case 'Sắp diễn ra': return 'info'
    case 'Đang diễn ra': return 'success'
    case 'Đã hết hạn': return 'default'
    default: return 'default'
  }
}

const generateCode = () => {
  const timestamp = Date.now().toString().slice(-6)
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  formData.discountCode = `PROMO${timestamp}${random}`
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('vi-VN')
}

// ================= chức năng của bộ lọc =================
const calculateStatistics = (data: DiscountResponse[]) => {
  const now = Date.now()

  statistics.total = data.length
  statistics.active = data.filter((item) => 
    item.startTime && item.endTime && now >= item.startTime && now <= item.endTime
  ).length
  statistics.upcoming = data.filter((item) => 
    item.startTime && now < item.startTime
  ).length
  statistics.expired = data.filter((item) => 
    item.endTime && now > item.endTime
  ).length
}

const getBadgeCount = (status: string) => {
  switch (status) {
    case 'all': return statistics.total
    case 'active': return statistics.active
    case 'upcoming': return statistics.upcoming
    case 'expired': return statistics.expired
    default: return 0
  }
}

const toggleAdvancedFilter = () => {
  console.log('Trước khi toggle:', showAdvancedFilter.value)
  showAdvancedFilter.value = !showAdvancedFilter.value
  console.log('Sau khi toggle:', showAdvancedFilter.value)
}

const handleQuickFilterChange = (status: string) => {
  quickFilter.value = status
  currentPage.value = 1
  fetchDiscounts() 
}

const filteredTableData = computed(() => {
  let filtered = tableData.value;
  
  if (quickFilter.value !== 'all') {
    const now = Date.now();
    filtered = filtered.filter(item => {
      switch (quickFilter.value) {
        case 'active':
          return item.startTime && item.endTime && now >= item.startTime && now <= item.endTime;
        case 'upcoming':
          return item.startTime && now < item.startTime;
        case 'expired':
          return item.endTime && now > item.endTime;
        default:
          return true;
      }
    });
  }
  
  if (searchForm.discountCode) {
    filtered = filtered.filter(item => 
      item.discountCode.toLowerCase().includes(searchForm.discountCode.toLowerCase())
    );
  }
  
  if (searchForm.percentageRange[0] !== null && searchForm.percentageRange[1] !== null) {
    filtered = filtered.filter(item => 
      item.percentage >= searchForm.percentageRange[0]! && 
      item.percentage <= searchForm.percentageRange[1]!
    );
  }
  
  if (searchForm.startDate) {
    const startTime = new Date(searchForm.startDate).getTime();
    filtered = filtered.filter(item => item.startTime && item.startTime >= startTime);
  }
  
  if (searchForm.endDate) {
    const endTime = new Date(searchForm.endDate).getTime();
    filtered = filtered.filter(item => item.endTime && item.endTime <= endTime);
  }
  
  return filtered;
});


const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTableData.value.slice(start, end)
})

const totalFilteredPages = computed(() => {
  return Math.ceil(filteredTableData.value.length / pageSize.value)
})  

const removeFilter = (filterKey: string) => {
  switch (filterKey) {
    case 'q':
      searchForm.q = ''
      break
    case 'discountCode':
      searchForm.discountCode = ''
      break
    case 'discountStatus':
      searchForm.discountStatus = undefined
      quickFilter.value = 'all'
      break
    case 'percentageRange':
      searchForm.percentageRange = [null, null]
      break
    case 'startDate':
      searchForm.startDate = ''
      break
    case 'endDate':
      searchForm.endDate = ''
      break
  }
  handleAdvancedSearch()
}

const clearAllFilters = () => {
  handleReset()
}

const handleReset = () => {
  searchForm.q = ''
  searchForm.discountCode = ''
  searchForm.discountStatus = undefined
  searchForm.percentageRange = [null, null]
  searchForm.startDate = ''
  searchForm.endDate = ''
  searchForm.sortBy = 'createdDate_desc'

  quickFilter.value = 'all'
  currentPage.value = 1
  fetchDiscounts()
}

const handleAdvancedSearch = () => {
  currentPage.value = 1
  fetchDiscounts()
}

let searchTimeout: ReturnType<typeof setTimeout>
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    handleAdvancedSearch()
  }, 500)
}


const debouncedSearchApplied = () => {
  clearTimeout(appliedSearchTimeout)
  appliedSearchTimeout = setTimeout(() => {
    appliedCurrentPage.value = 1
    fetchAppliedProducts()
  }, 500)
}

const debouncedSearchUnapplied = () => {
  clearTimeout(unappliedSearchTimeout)
  unappliedSearchTimeout = setTimeout(() => {
    unappliedCurrentPage.value = 1
    fetchUnappliedProducts()
  }, 500)
}

const fetchAppliedProducts = async () => {
  if (!modalRow.value?.id) return
  
  loadingApplied.value = true
  try {
    const res = await getAppliedProducts(modalRow.value.id, {
      page: appliedCurrentPage.value,
      size: appliedPageSize.value,
      q: appliedSearchKeyword.value
    })
    appliedProducts.value = res.items
    appliedTotal.value = res.totalItems
  } catch (error) {
    message.error('Không thể tải danh sách sản phẩm đã áp dụng')
  } finally {
    loadingApplied.value = false
  }
}

const fetchUnappliedProducts = async () => {
  if (!modalRow.value?.id) {
    console.warn('❌ No modalRow.value.id');
    return;
  }
  
  loadingUnapplied.value = true;
  console.log('🚀 Fetching unapplied products for discount:', modalRow.value.id);
  
  try {
    const params = {
      page: unappliedCurrentPage.value,
      size: unappliedPageSize.value,
      q: unappliedSearchKeyword.value.trim()
    };
    
    console.log('📤 API params:', params);
    
    const res = await getUnappliedProducts(modalRow.value.id, params);
    
    console.log('📥 API response:', res);
    console.log('📥 Items received:', res.items);
    console.log('📥 Total items:', res.totalItems);
    
    
    if (!Array.isArray(res.items)) {
      console.error('❌ res.items is not an array:', res.items);
      message.error('Dữ liệu trả về không đúng định dạng');
      unappliedProducts.value = [];
      unappliedTotal.value = 0;
      return;
    }
    
    unappliedProducts.value = res.items;
    unappliedTotal.value = res.totalItems;
    
    console.log('✅ Successfully updated unappliedProducts:', unappliedProducts.value);
    console.log('✅ Successfully updated unappliedTotal:', unappliedTotal.value);
    
  } catch (error) {
    console.error('❌ Error in fetchUnappliedProducts:', error);
    message.error('Không thể tải danh sách sản phẩm chưa áp dụng');
    unappliedProducts.value = [];
    unappliedTotal.value = 0;
  } finally {
    loadingUnapplied.value = false;
  }
}

const handleAppliedPageChange = (page: number) => {
  appliedCurrentPage.value = page
  fetchAppliedProducts()
}

const handleUnappliedPageChange = (page: number) => {
  unappliedCurrentPage.value = page
  fetchUnappliedProducts()
}

const handleApplySelectedProducts = async () => { 
  if (!modalRow.value?.id || unappliedSelectedKeys.value.length === 0) {
    message.warning('Chưa chọn sản phẩm nào để áp dụng')
    return
  }
  
if (isDiscountExpired(modalRow.value)) {
  message.error('Không thể áp dụng sản phẩm cho đợt giảm giá đã hết hạn')
  return
}

  applyingProducts.value = true
  let successCount = 0
  let failCount = 0

  try {
    console.log('Bắt đầu áp dụng sản phẩm...')
    console.log('Discount ID:', modalRow.value.id)
    console.log('Selected products:', unappliedSelectedKeys.value)
    console.log('Discount percentage:', modalRow.value.percentage)

    const discountPercentage = modalRow.value.percentage || formData.percentage
    if (!discountPercentage || discountPercentage <= 0) {
      message.error('Phần trăm giảm giá không hợp lệ')
      return
    }
    for (const productId of unappliedSelectedKeys.value) {
      try {
        const product = unappliedProducts.value.find(p => p.id === productId)
        if (!product) {
          console.error(`Không tìm thấy sản phẩm với ID: ${productId}`)
          failCount++
          continue
        }
        
        const discountedPrice = Math.round(product.price * (100 - discountPercentage) / 100)
        
        const requestData: ApplyDiscountRequest = {
          productDetailIds: [productId.toString()],
          discountId: modalRow.value.id,
          originalPrice: product.price,
          salePrice: discountedPrice,
          description: 'Áp dụng sản phẩm'
        }

        console.log(`Áp dụng sản phẩm ${product.productCode}:`, requestData)
        
        await applySingleProductToDiscount(requestData)
        successCount++
        
        console.log(`Thành công: ${product.productCode}`)
        
      } catch (error) {
        console.error(`Lỗi khi áp dụng sản phẩm ${productId}:`, error)
        failCount++
      }
    }
    if (successCount > 0) {
      message.success(`Đã áp dụng thành công ${successCount} sản phẩm`)
    }
    if (failCount > 0) {
      message.warning(`${failCount} sản phẩm không thể áp dụng`)
    }
    
    unappliedSelectedKeys.value = []
    
    await Promise.all([fetchAppliedProducts(), fetchUnappliedProducts()])
    
  } catch (error) {
    console.error('Lỗi tổng quát:', error)
    message.error('Có lỗi xảy ra trong quá trình áp dụng sản phẩm')
  } finally {
    applyingProducts.value = false
  }
}


const handleRemoveProducts = async () => {
  if (appliedSelectedKeys.value.length === 0) {
    message.warning('Chưa chọn sản phẩm nào');
    return;
  }
  if (modalRow.value && isDiscountExpired(modalRow.value)) {
  message.error('Không thể gỡ bỏ sản phẩm khỏi đợt giảm giá đã hết hạn')
  return
} 
  
  removingProducts.value = true;
  try {
    for (const productDetailDiscountId of appliedSelectedKeys.value) {
      await removeProductsFromDiscount(productDetailDiscountId.toString());
    }
    
    message.success(`Đã cập nhật trạng thái ${appliedSelectedKeys.value.length} sản phẩm`);
    appliedSelectedKeys.value = [];
    await Promise.all([fetchAppliedProducts(), fetchUnappliedProducts()])
  } catch {
    message.error('Có lỗi xảy ra khi cập nhật trạng thái');
  } finally {
    removingProducts.value = false;
  }
};

// ================= gọi api =================
async function fetchDiscounts() {
  loading.value = true;
  try {
    const params: ParamsGetDiscount = {
       page: 1,          
       size: 1000,        
       q: searchKeyword.value || searchForm.q || undefined,
       discountCode: searchForm.discountCode || undefined, 
       ...(searchForm.percentageRange &&
        searchForm.percentageRange[0] !== null &&
        searchForm.percentageRange[1] !== null && {
          minPercentage: searchForm.percentageRange[0],
          maxPercentage: searchForm.percentageRange[1]
        }),
      ...(searchForm.startDate && { startDate: new Date(searchForm.startDate).getTime() }),
      ...(searchForm.endDate && { endDate: new Date(searchForm.endDate).getTime() }),
      ...(searchForm.sortBy && { sortBy: searchForm.sortBy })
    };
    
    const res = await getAllDiscounts(params);
    tableData.value = res.items;
    total.value = res.totalItems;
    calculateStatistics(res.items);
  } catch (e) {
    message.error("Không thể tải dữ liệu đợt giảm giá");
  } finally {
    loading.value = false;
  }
}

onMounted(fetchDiscounts);

function openModal(mode: "add" | "edit", row?: DiscountResponse) {
  modalMode.value = mode;
  modalTab.value = 'info'; 
  productTab.value = 'applied';


  if (mode === "edit" && row) {
    modalRow.value = row;
    formData.discountName = row.discountName;
    formData.discountCode = row.discountCode;
    formData.percentage = row.percentage;
    formData.startDate = row.startTime || Date.now();
    formData.endDate = row.endTime || Date.now() + 24 * 60 * 60 * 1000;
    formData.description = row.description || "";
  } else {
    modalRow.value = null;
    formData.discountName = "";
    formData.discountCode = "";
    formData.percentage = 1;
    formData.startDate = Date.now();
    formData.endDate = Date.now() + 24 * 60 * 60 * 1000;
    formData.description = "";
    generateCode();
  }
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  modalTab.value = 'info';
  productTab.value = 'applied';
  appliedProducts.value = []
  unappliedProducts.value = []
  appliedSelectedKeys.value = []
  unappliedSelectedKeys.value = []
  appliedSearchKeyword.value = ''
  unappliedSearchKeyword.value = ''
  appliedCurrentPage.value = 1
  unappliedCurrentPage.value = 1
}

async function saveDiscount() {
  if (!formData.discountName || !formData.discountCode || !formData.percentage) {
    message.warning("Vui lòng nhập đầy đủ thông tin bắt buộc");
    return;
  }

  if (formData.startDate >= formData.endDate) {
    message.warning("Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc");
    return;
  }

  try {
    if (modalMode.value === "add") {
      const result = await createDiscount(formData);
      message.success("Thêm đợt giảm giá thành công");
      if (result?.data?.id) {
        modalRow.value = { 
          ...formData, 
          id: result.data.id,
          startTime: formData.startDate,
          endTime: formData.endDate,
          createdDate: Date.now()
        } as DiscountResponse;
        modalTab.value = 'products';
        message.info('Bạn có thể quản lý sản phẩm áp dụng discount ở tab bên cạnh')
      }
    } else if (modalMode.value === "edit" && modalRow.value) {
      await updateDiscount(modalRow.value.id, formData);
      message.success("Cập nhật đợt giảm giá thành công");
      modalRow.value = { ...modalRow.value, ...formData };
    }
    fetchDiscounts();
  } catch (e: any) {
    message.error(e?.message || "Có lỗi xảy ra khi lưu đợt giảm giá");
  }
}

async function handleDelete(id: string) {
  try {
    await deleteDiscount(id);
    message.success("Xóa đợt giảm giá thành công");
    fetchDiscounts();
  } catch {
    message.error("Xóa thất bại");
  }
}

async function handleDeleteSelected() {
  if (checkedRowKeys.value.length === 0) {
    message.warning("Chưa chọn đợt giảm giá nào");
    return;
  }
  try {
    await Promise.all(
      checkedRowKeys.value.map((id) => deleteDiscount(id.toString()))
    );
    message.success("Đã xóa các đợt giảm giá đã chọn");
    checkedRowKeys.value = [];
    fetchDiscounts();
  } catch {
    message.error("Xóa hàng loạt thất bại");
  }
}

async function handleStart(row: DiscountResponse) {

if (hasActiveDiscount()) {
  message.error('Không thể bắt đầu đợt giảm giá khi đang có đợt giảm giá khác đang diễn ra')
  return
}
  try {
    await startDiscount(row.id);
    message.success(`Bắt đầu sớm đợt giảm giá "${row.discountName}" thành công`);
    fetchDiscounts();
  } catch (e: any) {
    message.error(e?.message || "Có lỗi xảy ra khi bắt đầu đợt giảm giá");
  }
}

async function handleDeactivate(row: DiscountResponse) {
  try {
    await deactivateDiscount(row.id);
    message.success(`Kết thúc sớm đợt giảm giá "${row.discountName}" thành công`);
    fetchDiscounts();
  } catch (e: any) {
    message.error(e?.message || "Có lỗi xảy ra khi kết thúc đợt giảm giá");
  }
}

function refreshTable() {
  fetchDiscounts();
  message.success("Đã làm mới dữ liệu");
}

const appliedColumns: DataTableColumns<AppliedProductResponse> = [
  { type: 'selection' as const },
  {
    title: 'Mã sản phẩm',
    key: 'productCode',
    width: 120,
    render(row) {
      return h('strong', row.productCode)
    }
  },
  {
    title: 'Tên sản phẩm',
    key: 'productName',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: 'Phần trăm giảm',
    key: 'percentageDiscount',
    width: 120,
    render(row) {
      return h(NTag, {
        type: 'success',
        size: 'small'
      }, {
        default: () => `${row.percentageDiscount}%`
      })
    }
  },
  {
    title: 'Thời gian áp dụng',
    key: 'timeRange',
    width: 200,
    render(row) {
      return h('div', { style: 'font-size: 12px; line-height: 1.4' }, [
        h('div', `Từ: ${new Date(row.startTime).toLocaleDateString('vi-VN')}`),
        h('div', `Đến: ${new Date(row.endTime).toLocaleDateString('vi-VN')}`)
      ])
    }
  }
]

const unappliedColumns: DataTableColumns<ProductDetailResponse> = [
  { type: 'selection' },
  {
    title: 'Mã sản phẩm',
    key: 'productCode',
    width: 120,
    render(row) {
      return h('strong', row.productCode)
    }
  },
  {
    title: 'Tên sản phẩm',
    key: 'productName',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: 'Giá gốc',
    key: 'price',
    width: 120,
    render(row) {
      return formatPrice(row.price)
    }
  },
  {
    title: 'Giá sau giảm',
    key: 'salePrice',
    width: 120,
    render(row) {
      const salePrice = Math.round(row.price * (100 - formData.percentage) / 100)
      return h('span', { style: 'color: #f56565; font-weight: 600' }, formatPrice(salePrice))
    }
  },
  {
    title: 'Cấu hình',
    key: 'specs',
    width: 250,
    render(row) {
      const specs = []
      if (row.colorName) specs.push(`Màu: ${row.colorName}`)
      if (row.ramName) specs.push(`RAM: ${row.ramName}`)
      if (row.hardDriveName) specs.push(`Ổ cứng: ${row.hardDriveName}`)
      if (row.gpuName) specs.push(`GPU: ${row.gpuName}`)
      if (row.cpuName) specs.push(`CPU: ${row.cpuName}`)
      
      return h('div', { style: 'font-size: 12px' }, 
        specs.length > 0 ? specs.join(' • ') : '-'
      )
    }
  }
]


const columns: DataTableColumns<DiscountResponse> = [
  { type: "selection" as const },
  {
    title: "Mã",
    key: "discountCode",
    width: 120,
    render(row) {
      return h('strong', row.discountCode)
    }
  },
  {
    title: "Tên đợt giảm giá",
    key: "discountName",
    width: 200,
    ellipsis: {
      tooltip: true
    }
  },
  {
    title: "Phần trăm giảm giá",
    key: "percentage",
    width: 150,
    render(row) {
      return `${row.percentage}%`
    }
  },
  {
    title: "Thời gian áp dụng",
    key: "timeRange",
    width: 250,
    render(row) {
      return h('div', { style: 'line-height: 1.4; font-size: 12px;' }, [
        h('div', `Bắt đầu: ${formatDateTime(row.startTime || 0)}`),
        h('div', `Kết thúc: ${formatDateTime(row.endTime || 0)}`)
      ])
    }
  },
  {
    title: "Trạng thái",
    key: "status",
    width: 120,
    render(row) {
      return h(NTag, {
        type: getStatusType(row),
        size: 'small'
      }, {
        default: () => getStatus(row)
      })
    }
  },
  {
    title: "Mô tả",
    key: "description",
    width: 200,
    ellipsis: {
      tooltip: true
    },
    render(row) {
      return row.description || '-'
    }
  },
  {
    title: "Thao tác",
    key: "actions",
    width: 180,
    render(row: DiscountResponse) {
      const status = getStatus(row)
      const actions = [
        // Edit button
        h(NButton, {
          size: "small",
          quaternary: true,
          circle: true,
          onClick: () => openModal("edit", row)
        }, {
          default: () => h(Icon, { icon: "carbon:edit", width: "18" })
        })
      ]
      if (status === 'Sắp diễn ra') {
        actions.push(
          h(NButton, {
            size: "small",
            quaternary: true,
            circle: true,
            type: "primary",
            onClick: () => handleStart(row)
          }, {
            default: () => h(Icon, { icon: "carbon:play", width: "18" })
          })
        )
      }
      if (status === 'Đang diễn ra') {
        actions.push(
          h(NButton, {
            size: "small",
            quaternary: true,
            circle: true,
            type: "warning",
            onClick: () => handleDeactivate(row)
          }, {
            default: () => h(Icon, { icon: "carbon:pause", width: "18" })
          })
        )
      }

      if(status==='Sắp diễn ra'){
        actions.push(
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row.id),
          positiveText: "Xác nhận",
          negativeText: "Hủy"
        }, {
          trigger: () => h(NButton, {
            size: "small",
            quaternary: true,
            circle: true,
            type: "error"
          }, {
            default: () => h(Icon, { icon: "carbon:trash-can", width: "18" })
          }),
          default: () => `Bạn có chắc muốn xóa đợt giảm giá "${row.discountName}"?`
        })
      )
      }

      return h(NSpace, actions)
    }
  }
];


function handlePageChange(page: number) {
  currentPage.value = page;
  fetchDiscounts();
}

watch(() => modalTab.value, (newTab) => {
  if (newTab === 'products' && modalRow.value?.id) {
    fetchAppliedProducts()
    fetchUnappliedProducts()
  }
})


watch(() => formData.percentage, () => {
  if (modalTab.value === 'products' && productTab.value === 'unapplied') {
    unappliedProducts.value = [...unappliedProducts.value]
  }
})
</script>