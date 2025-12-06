
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

    <!-- Bộ lọc -->
  <NCard title="Bộ lọc" style="margin-top: 16px">
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
  <NFormItem label="Trạng thái">
    <NSelect 
      v-model:value="searchForm.discountStatus"
      placeholder="Chọn trạng thái"
      style="width: 150px;"
      clearable
      @update:value="handleAdvancedSearch"
      :options="[
        { label: `Đang diễn ra (${statistics.active})`, value: 0 },
        { label: `Sắp diễn ra (${statistics.upcoming})`, value: 1 }, 
        { label: `Đã hết hạn (${statistics.expired})`, value: 3 }
      ]"
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
                  style="width: 160px"
                />
                <span>đến</span>
                <NInputNumber 
                  v-model:value="searchForm.percentageRange[1]"
                  placeholder="100" 
                  :min="0" 
                  :max="100"
                  @update:value="handleAdvancedSearch"
                  style="width: 160px"
                />
              </NSpace>
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="Thời gian áp dụng ">
              <input
                type="date"
                v-model="searchForm.startDate"
                @change="handleAdvancedSearch"
                placeholder="Chọn ngày bắt đầu"
                style="width: 175px; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-right: 5px ;"
              />
              <span> đến </span>
                <input
                type="date"
                v-model="searchForm.endDate"
                @change="handleAdvancedSearch"
                placeholder="Chọn ngày kết thúc"
                style="width: 175px; padding: 6px; border: 1px solid #d9d9d9; border-radius: 4px; margin-left: 5px;"
              />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NButton @click="handleReset" style="margin-top: 28px;">
            <template #icon>
              <NIcon>
                <Icon :icon="'carbon:rotate'" />
              </NIcon>
            </template>
          </NButton>
          </NGridItem>
        </NGrid>
      </NForm>
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
            @click="navigateToAddPage"
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

          <NPopconfirm @positive-click="handleSendMailSelected"
          positive-text="Xác nhận"
            negative-text="Hủy bỏ"
          >
            <template #trigger>
              <NButton type="error"  secondary circle title="Gửi mail hàng loạt">
                <NIcon size="24">
                  <Icon :icon="'icon-park-outline:align-text-top'" />
                </NIcon>
              </NButton>
            </template>
            Xác nhận gửi Mail tất cả đợt giảm giá đã chọn?
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
    NForm,
    NFormItem,
    NInput,
    NSelect,
    NInputNumber,
    NGrid,
    NGridItem,
    NPopconfirm,
    NTag,
    NPagination,
    NIcon,
    useMessage,
    DataTableColumns
  } from "naive-ui";
  import { Icon } from "@iconify/vue";
  import {
    getAllDiscounts,
    sendEmail,
    getAppliedProducts,
    getUnappliedProducts,

    type DiscountResponse,
    type ParamsGetDiscount,
    type CreateDiscountRequest,
    type ProductDetailResponse,
    type AppliedProductResponse,
    deleteDiscount,
  } from '@/service/api/admin/discount/discountApi'

import { useRouter } from 'vue-router'
const router = useRouter()

function navigateToAddPage() {
  router.push('/discounts/campaign/add')
}

function navigateToEditPage(row: DiscountResponse) {
  router.push(`/discounts/campaign/edit/${row.id}`)
}
  const message = useMessage();
  const tableData = ref<DiscountResponse[]>([]);
  const total = ref(0);
  const currentPage = ref(1);
  const pageSize = ref(10);
  const loading = ref(false);
  const searchKeyword = ref("");
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


  // ================= mó đồ =================
  const modalRow = ref<DiscountResponse | null>(null);
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


const filteredTableData = computed(() => {
  let filtered = tableData.value;
  
  if (searchForm.discountStatus !== undefined) {
    filtered = filtered.filter(item => {
      const status = getStatusValue(item);
      return status === searchForm.discountStatus;
    });
  }
  
  if (searchForm.q) {
    filtered = filtered.filter(item => 
      item.discountName.toLowerCase().includes(searchForm.q.toLowerCase())
    );
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

const getStatusValue = (item: DiscountResponse): number => {
  const now = Date.now()
  if (item.startTime && item.endTime) {
    if (now >= item.startTime && now <= item.endTime) return 0 
    if (now < item.startTime) return 1 
  }
  return 3
}

  const paginatedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return filteredTableData.value.slice(start, end)
  })

  const totalFilteredPages = computed(() => {
    return Math.ceil(filteredTableData.value.length / pageSize.value)
  })  

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


  async function handleSendMailSelected() {
    if (checkedRowKeys.value.length === 0) {
      message.warning("Chưa chọn đợt giảm giá nào");
      return;
    }

    try {
      const results = await Promise.allSettled(
        checkedRowKeys.value.map((id) => sendEmail(id.toString()))
      );
      const successCount = results.filter(r => r.status === "fulfilled").length;
      const failCount = results.filter(r => r.status === "rejected").length;
      if (failCount === 0) {
        message.success(`Đã gửi Mail thành công cho ${successCount} đợt giảm giá`);
      } else {
        message.error(
          `Gửi Mail hoàn tất. Thành công: ${successCount}, Thất bại: ${failCount}`
        );
      }
      checkedRowKeys.value = [];
      fetchDiscounts();
    } catch (error) {
      message.error("Có lỗi xảy ra khi gửi Mail");
    }
  }




  function refreshTable() {
    fetchDiscounts();
    message.success("Đã làm mới dữ liệu");
  }


  const columns: DataTableColumns<DiscountResponse> = [
    { type: "selection" as const },
     {
    title: "STT",
    key: "stt",
    width: 50,
    align: "center",
    render(_: DiscountResponse, index: number) {
      return index + 1
    }
  },
    {
      title: "Mã",
      key: "discountCode",
      width: 230,
      render(row) {
        return h('strong', row.discountCode)
      }
    },
    {
      title: "Tên đợt giảm giá",
      key: "discountName",
      width: 230,
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
      title: "Thao tác",
      key: "actions",
      width: 90,
      render(row: DiscountResponse) {
        const status = getStatus(row)
        const actions = [
          h(NButton, {
            size: "small",
            quaternary: true,
            circle: true,
            onClick: () => navigateToEditPage(row)
          }, {
            default: () => h(Icon, { icon: "carbon:edit", width: "18" })
          })

        ]

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


    async function handleDelete(id: string) {
    try {
      await deleteDiscount(id);
      message.success("Xóa đợt giảm giá thành công");
      fetchDiscounts();
    } catch {
      message.error("Xóa thất bại");
    }
  }

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