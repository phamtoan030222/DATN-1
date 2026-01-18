<script setup lang="ts">
import { ref, reactive, onMounted, watch, h } from "vue";
import { useAppStore } from "@/store";
import { DashboardOverviewResponse, statisticsApi } from "@/service/api/admin/statistics/api";
// Components
import Chart from "./components/chart.vue";
import Chart3 from "./components/chart3.vue";
import ChartProduct from "./components/ChartProduct.vue";

// Icons
import { 
  LogoUsd, CubeOutline, PeopleOutline, CartOutline, 
  TrendingUpOutline, TrendingDownOutline, BarChartOutline ,CloudDownloadOutline
} from '@vicons/ionicons5';
import { Icon } from '@iconify/vue'
import { NTag } from "naive-ui";
import { NDatePicker } from "naive-ui";

const appStore = useAppStore();



// --- STATE ---
const overviewData = ref<DashboardOverviewResponse>({
  revenueToday: 0, revenueWeek: 0, revenueMonth: 0, totalRevenue: 0,
  totalOrders: 0, successfulOrders: 0, cancelledOrders: 0, pendingOrders: 0, processingOrders: 0,
  totalProducts: 0, lowStockProducts: 0,
  totalCustomers: 0, newCustomersMonth: 0 ,
  topSellingProducts: [],
  topBrands: []
});

const growthStats = ref<any[]>([]);
const lowStockData = ref<any[]>([]);
const filterType = ref("week");      
const searchQuantity = ref(10);   
const customDateRange = ref<[number, number] | null>(null);   

// --- CONFIG TABLE ---
const pagination = reactive({
  page: 1, pageSize: 5, itemCount: 0,
  onChange: (page: number) => { 
    pagination.page = page; 
    fetchLowStock(); 
  }
});

//gửi mail
const handleExportExcel = async () => {
  try {
    window.$message.loading("Đang tạo file Excel...");
    const blob = await statisticsApi.exportRevenueExcel();
    
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `Bao_cao_doanh_thu_${new Date().toISOString().slice(0,10)}.xlsx`);
    
    document.body.appendChild(link);
    link.click();
    
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    window.$message.success("Tải xuống thành công!");
  } catch (e) {
    window.$message.error("Lỗi khi xuất file Excel.");
  }
};

const inventoryColumns = [
  { 
    title: 'STT', key: 'stt', width: 60, align: 'center', 
    render: (_: any, index: number) => (pagination.page - 1) * pagination.pageSize + index + 1 
  },
  { title: 'Tên Sản phẩm', key: 'name' },
  { title: 'Thương hiệu', key: 'brandName' },
  { 
    title: 'Số lượng', key: 'quantity', 
    render(row: any) {
      return h(NTag, { 
        type: row.quantity === 0 ? 'error' : 'warning', 
        size: 'small', bordered: false 
      }, { default: () => row.quantity });
    }
  },
  {
    title: 'Ngày nhập', key: 'createdDate',
    render(row: any) {
       return row.createdDate ? new Date(row.createdDate).toLocaleDateString('vi-VN') : '-'
    }
  }
];

// --- API CALLS ---
const fetchOverviewData = async () => {
  const res = await statisticsApi.getOverview();
  if (res) overviewData.value = res;
};

const fetchGrowthData = async () => {
  const res = await statisticsApi.getGrowthStats();
  if (res) growthStats.value = res;
};

const fetchLowStock = async () => {
  const res = await statisticsApi.getLowStockProducts(
    searchQuantity.value, 
    { page: pagination.page, size: pagination.pageSize }
  );
  lowStockData.value = res.items;
  pagination.itemCount = res.totalItems;
};

// --- WATCH & MOUNT ---
watch(searchQuantity, () => { pagination.page = 1; fetchLowStock(); });
onMounted(() => {
  fetchOverviewData();
  fetchGrowthData();
  fetchLowStock();
});

const filterOptions = [
 { label: 'Hôm nay', value: 'today' },
  { label: 'Tuần này', value: 'week' },
  { label: 'Tháng này', value: 'month' },
  { label: 'Năm nay', value: 'year' },
  { label: 'Tùy chỉnh', value: 'custom' }
];
</script>

<template>
  <n-grid :x-gap="16" :y-gap="16" :cols="12" item-responsive responsive="screen">
    
    <n-gi span="6 m:3">
      <n-card :bordered="false" content-style="padding: 16px;">
        <div class="card-header">
          <n-avatar round size="medium" color="#e7fcf3" style="color: #18a058;">
            <n-icon size="20"><logo-usd /></n-icon>
          </n-avatar>
          <div class="card-title-group">
            <div class="card-title">Doanh Thu</div>
            <div class="card-subtitle">Tổng quan doanh thu</div>
          </div>
        </div>
        <div class="card-main-value">
          <n-number-animation :from="0" :to="overviewData.revenueMonth" show-separator /> ₫
          <div class="card-note">Doanh thu tháng này</div>
        </div>
        <n-grid x-gap="12" cols="2" style="margin-bottom: 16px;">
          <n-gi>
            <div class="mini-stat-box">
              <div class="mini-stat-val"><n-number-animation :from="0" :to="overviewData.revenueToday" show-separator /> ₫</div>
              <div class="mini-stat-label">Hôm nay</div>
            </div>
          </n-gi>
          <n-gi>
            <div class="mini-stat-box">
              <div class="mini-stat-val"><n-number-animation :from="0" :to="overviewData.revenueWeek" show-separator /> ₫</div>
              <div class="mini-stat-label">Tuần này</div>
            </div>
          </n-gi>
        </n-grid>
        <div class="success-box">
           <span style="font-weight: 800;"><n-number-animation :from="0" :to="overviewData.totalRevenue" show-separator /> ₫</span>
           <div style="font-size: 11px;">Tổng doanh thu toàn thời gian</div>
        </div>
      </n-card>
    </n-gi>

    <n-gi span="6 m:3">
      <n-card :bordered="false" content-style="padding: 16px;">
        <div class="card-header">
          <n-avatar round size="medium" color="#eff6ff" style="color: #3b82f6;">
            <n-icon size="20"><cart-outline /></n-icon>
          </n-avatar>
          <div class="card-title-group">
            <div class="card-title">Đơn Hàng</div>
            <div class="card-subtitle">Tình trạng đơn hàng</div>
          </div>
        </div>
        <div class="card-main-value" style="color: #2563eb;">
          <n-number-animation :from="0" :to="overviewData.totalOrders" />
          <div class="card-note">Tổng số đơn hàng</div>
        </div>
        <div class="status-list">
          <div class="status-item"><span>Chờ xác nhận</span><span style="color: #eab308; font-weight: 700;">{{ overviewData.pendingOrders }}</span></div>
          <div class="status-item"><span>Đang xử lý</span><span style="color: #3b82f6; font-weight: 700;">{{ overviewData.processingOrders }}</span></div>
          <div class="status-item"><span>Hoàn thành</span><span style="color: #10b981; font-weight: 700;">{{ overviewData.successfulOrders }}</span></div>
          <div class="status-item"><span>Đã hủy</span><span style="color: #ef4444; font-weight: 700;">{{ overviewData.cancelledOrders }}</span></div>
        </div>
      </n-card>
    </n-gi>

   <n-gi span="6 m:3">
      <n-card :bordered="false" content-style="padding: 16px;">
        <div class="card-header">
          <n-avatar round size="medium" color="#f3e8ff" style="color: #9333ea;">
            <n-icon size="20"><cube-outline /></n-icon>
          </n-avatar>
          <div class="card-title-group">
            <div class="card-title">Sản Phẩm</div>
            <div class="card-subtitle">Quản lý kho hàng</div>
          </div>
        </div>
        
        <div class="card-main-value" style="color: #9333ea;">
          <n-number-animation :from="0" :to="overviewData.totalProducts" />
          <div class="card-note">Tổng số sản phẩm</div>
        </div>

        <div class="product-overview-section">
          
          <div class="overview-item-row">
            <span class="label">Sắp hết hàng</span>
            <span class="count-badge green">{{ overviewData.lowStockProducts }}</span>
          </div>

          <div class="section-title">Sản phẩm bán chạy</div>
          <div class="top-list">
            <div v-for="(item, index) in overviewData.topSellingProducts" :key="index" class="top-item">
              <span class="rank-badge">{{ index + 1 }}</span>
              <span class="item-name n-ellipsis" :title="item.name">{{ item.name }}</span>
              <span class="count-badge green light" >{{ item.count }}</span>
            </div>
            <div v-if="overviewData.topSellingProducts.length === 0" class="no-data">Chưa có dữ liệu</div>
          </div>

          <div class="section-title">Thương hiệu hàng đầu</div>
          <div class="top-list">
            <div v-for="(item, index) in overviewData.topBrands" :key="index" class="top-item">
              <span class="item-name n-ellipsis" :title="item.name" style="flex: 1;">
                {{ item.name }}
              </span>
              <span class="count-badge blue">
                {{ item.count }}
              </span>
            </div>
            <div v-if="overviewData.topBrands.length === 0" class="no-data">
              Chưa có dữ liệu
            </div>
          </div>

          <div class="bottom-boxes">
            <div class="summary-box green-box">
              {{ overviewData.totalProducts }}
            </div>
            <div class="summary-box orange-box">
              {{ overviewData.lowStockProducts }}
            </div>
          </div>

        </div>
      </n-card>
    </n-gi>

    <n-gi span="6 m:3">
      <n-card :bordered="false" content-style="padding: 16px;">
        <div class="card-header">
          <n-avatar round size="medium" color="#ccfbf1" style="color: #0d9488;">
            <n-icon size="20"><people-outline /></n-icon>
          </n-avatar>
          <div class="card-title-group">
            <div class="card-title">Khách Hàng</div>
            <div class="card-subtitle">Thông tin khách hàng</div>
          </div>
        </div>
        <div class="card-main-value" style="color: #0f766e;">
          <n-number-animation :from="0" :to="overviewData.totalCustomers" />
          <div class="card-note">Tổng số khách hàng</div>
        </div>
        <div style="border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px; margin-top: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 4px;">
            <span style="font-size: 13px; color: #374151; font-weight: 600;">Khách mới tháng này</span>
            <div style="background-color: #0ea5e9; color: white; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700;">
              {{ overviewData.newCustomersMonth }}
            </div>
          </div>
          <div style="font-size: 11px; color: #9ca3af;">Thành viên đăng ký mới</div>
        </div>
      </n-card>
    </n-gi>

   <n-gi span="12">
      <n-card content-style="padding: 0;">
        <div style="padding: 16px 20px 0; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
           <span style="font-weight: 700; font-size: 16px;">Thống kê chi tiết</span>
           
           <div style="display: flex; gap: 10px; align-items: center;">
            <NButton
              type="success"
               secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              @click="handleExportExcel"
            >
              <template #icon>
                <NIcon size="15">
                  <Icon icon="file-icons:microsoft-excel" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Xuất Excel
              </span>
            </NButton>
             <n-radio-group v-model:value="filterType" size="small">
                <n-radio-button 
                  v-for="opt in filterOptions" 
                  :key="opt.value" 
                  :value="opt.value" 
                  :label="opt.label" 
                />
             </n-radio-group>

             <n-date-picker 
                v-if="filterType === 'custom'"
                v-model:value="customDateRange"
                type="daterange"
                size="small"
                clearable
                style="width: 240px"
             />
           </div>
        </div>

        <n-tabs type="line" size="large" :tabs-padding="20" pane-style="padding: 20px;">
          <n-tab-pane name="Doanh thu">
            <Chart :filter-type="filterType" />
          </n-tab-pane>
          <n-tab-pane name="Đơn hàng">
            <Chart3 :filter-type="filterType" />
          </n-tab-pane>
          <n-tab-pane name="Sản phẩm bán chạy">
             <ChartProduct :filter-type="filterType" />
          </n-tab-pane>
        </n-tabs>
      </n-card>
    </n-gi>

    <n-gi span="12 m:6">
      <n-card title="Sản phẩm sắp hết hàng" :segmented="{ content: true }">
        <template #header-extra>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-size: 12px; color: #666;">Số lượng ≤</span>
            <n-input-number v-model:value="searchQuantity" size="small" style="width: 80px" :min="0" />
          </div>
        </template>
        <n-data-table :columns="inventoryColumns" :data="lowStockData" :pagination="pagination" :bordered="false" size="small" />
      </n-card>
    </n-gi>

    <n-gi span="12 m:6">
      <n-card title="Tốc độ tăng trưởng" :segmented="{ content: true }">
        <div style="display: flex; flex-direction: column; gap: 12px;">
          <div v-for="(item, index) in growthStats" :key="index" class="growth-row">
            <div class="growth-label"><n-icon :component="BarChartOutline" style="margin-right: 6px;" />{{ item.label }}</div>
            <div style="font-weight: 700; color: #fff; flex: 1; text-align: center;">{{ item.value }}</div>
            <div class="growth-percent" :style="{ color: item.trend === 'up' ? '#4ade80' : '#f87171' }">
              <n-icon :component="item.trend === 'up' ? TrendingUpOutline : TrendingDownOutline" />
              <span style="margin-left: 4px;">{{ item.percent }}</span>
            </div>
          </div>
        </div>
      </n-card>
    </n-gi>

  </n-grid>
</template>

<style scoped>
.card-header { display: flex; align-items: center; margin-bottom: 16px; }
.card-title-group { margin-left: 12px; }
.card-title { font-weight: 700; font-size: 16px; line-height: 1.2; }
.card-subtitle { font-size: 12px; color: #9ca3af; }
.card-main-value { text-align: center; margin-bottom: 24px; font-size: 28px; font-weight: 800; line-height: 1.2; color: #18a058; }
.card-note { font-size: 12px; color: #6b7280; margin-top: 4px; font-weight: normal; }
.mini-stat-box { border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px 8px; text-align: center; }
.mini-stat-val { font-weight: 700; font-size: 15px; }
.mini-stat-label { font-size: 11px; color: #6b7280; margin-top: 4px; }
.success-box { background-color: #f0fdf4; border-radius: 8px; padding: 12px; text-align: center; color: #15803d; }
.warning-box { background-color: #fafafa; border: 1px solid #f3f4f6; border-radius: 8px; padding: 12px; display: flex; justify-content: space-between; align-items: center; font-weight: 600; font-size: 13px; color: #374151; }
.badge { background-color: #ef4444; color: white; width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.status-list { display: flex; flex-direction: column; gap: 10px; }
.status-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border: 1px solid #f3f4f6; border-radius: 8px; font-size: 13px; color: #4b5563; }
.growth-row { display: flex; justify-content: space-between; align-items: center; background-color: #18181c; padding: 8px 12px; border-radius: 6px; color: white; transition: transform 0.2s; }
.growth-row:hover { transform: translateX(4px); }
.growth-label { display: flex; align-items: center; padding: 4px 8px; border-radius: 4px; font-size: 13px; font-weight: 600; color: white; min-width: 130px; }
.growth-percent { display: flex; align-items: center; font-weight: 700; font-size: 13px; background: rgba(255, 255, 255, 0.1); padding: 2px 6px; border-radius: 4px; }

.product-overview-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #374151;
  padding-bottom: 8px;
  border-bottom: 1px dashed #f3f4f6; 
}
.section-title {
  font-weight: 700;
  font-size: 13px;
  color: #111827;
  margin-bottom: -4px; 
}
.top-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.top-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #4b5563;
}

.rank-badge {
  background-color: #ffedd5;
  color: #c2410c;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 11px;
  margin-right: 10px;
  flex-shrink: 0;
}

.item-name {
  flex: 1;
  margin-right: 8px;
}

.count-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 12px;
  min-width: 24px;
  text-align: center;
}

/* Màu Badge */
.count-badge.green { background-color: #dcfce7; color: #15803d; } /* Xanh lá đậm cho Sắp hết hàng */
.count-badge.green.light { background-color: #f0fdf4; color: #22c55e; } /* Xanh lá nhạt cho Top SP */
.count-badge.blue { background-color: #eff6ff; color: #3b82f6; } /* Xanh dương cho Brand */

/* 2 Box dưới cùng */
.bottom-boxes {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.summary-box {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  text-align: center;
  font-weight: 700;
  font-size: 16px;
}
.summary-box.green-box { background-color: #ecfdf5; color: #10b981; }
.summary-box.orange-box { background-color: #fff7ed; color: #ea580c; }

.no-data {
  font-size: 12px; color: #9ca3af; font-style: italic; padding-left: 4px;
}
</style>