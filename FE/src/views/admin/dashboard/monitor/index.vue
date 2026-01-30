<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { statisticsApi } from "@/service/api/admin/statistics/api";
import { Icon } from '@iconify/vue';
import { useMessage } from 'naive-ui';

// Components Biểu đồ (Import file vừa sửa ở trên)
import Chart from "./components/chart.vue";
import Chart3 from "./components/chart3.vue";

const message = useMessage();
const overviewData = ref<any>({});
const filterType = ref("month"); // Mặc định tháng
const rangeDate = ref<[number, number] | null>(null);

// Map hiển thị tiêu đề Viết Hoa
const periodMap: any = {
  today: { label: 'Hôm nay' },
  week: { label: 'Tuần này' },
  month: { label: 'Tháng này' },
  year: { label: 'Năm nay' }
};

const formatMoney = (val: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
};

// Tính toán số liệu hiển thị trong bộ lọc
const currentFilterStats = computed(() => {
  // Nếu là tùy chỉnh, ta lấy số liệu của 'month' hoặc xử lý riêng nếu API hỗ trợ trả về stats custom
  // Ở đây fallback về month để tránh lỗi hiển thị khi chọn custom
  if (filterType.value === 'custom') return overviewData.value['month'] || {}; 
  
  return overviewData.value[filterType.value] || {};
});

const fetchOverviewData = async () => {
  const res = await statisticsApi.getOverview();
  if (res) overviewData.value = res;
};

const handleExportExcel = async () => {
  try {
    message.loading("Đang tạo báo cáo Excel...");
    
    // 1. Gọi API nhận về Blob (file nhị phân)
    const blobData = await statisticsApi.exportRevenueExcel();
    
    if (!blobData) {
      message.error("Không có dữ liệu để xuất!");
      return;
    }

    const url = window.URL.createObjectURL(new Blob([blobData]));
    const link = document.createElement('a');
    link.href = url;
    const fileName = `BaoCaoDoanhThu_${new Date().getTime()}.xlsx`;
    link.setAttribute('download', fileName);
        document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    
    message.success("Xuất báo cáo thành công!");
  } catch (error) {
    console.error(error);
    message.error("Có lỗi xảy ra khi xuất file.");
  }
};

onMounted(() => {
  fetchOverviewData();
});
</script>

<template>
  <n-space vertical :size="20" style="padding: 16px; background-color: #f9f9f9; min-height: 100vh;">
    
    <n-card :bordered="false" class="shadow-sm">
      <n-space vertical :size="8">
        <n-space align="center">
          <n-icon size="24" style="color: #2563eb">
            <Icon icon="carbon:carbon-ui-builder" />
          </n-icon>
          <span style="font-weight: 600; font-size: 24px">Thống Kê</span>
        </n-space>
        <span style="color: #666">Thống kê Doanh thu - Sản phẩm - Đơn hàng</span>
      </n-space>
    </n-card>

    <n-grid :x-gap="12" :cols="4" item-responsive responsive="screen">
      <n-gi v-for="(info, key) in periodMap" :key="key">
        <n-card :bordered="false" class="period-card shadow-sm">
          <div class="period-header">{{ info.label }}</div>
          <div class="period-revenue  text-green">{{ formatMoney(overviewData[key]?.revenue) }}</div>
          
          <div class="period-sub">
            Sản phẩm đã bán: {{ overviewData[key]?.soldProducts || 0 }} | Đơn hàng: {{ overviewData[key]?.totalOrders || 0 }}
          </div>
          
          <div class="period-sub" style="margin-top: 4px;">
            Hoàn thành: <span class="text-green">{{ overviewData[key]?.completed || 0 }}</span> | 
            Hủy: <span class="text-red">{{ overviewData[key]?.cancelled || 0 }}</span> | 
            Xử lý: <span class="text-orange">{{ overviewData[key]?.processing || 0 }}</span>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card :bordered="false" content-style="padding: 24px;" class="shadow-sm">
      <n-space vertical :size="16">
        <div class="filter-header-row">
          <div class="title-section">
            <div class="main-title">Bộ Lọc Tìm Kiếm</div>
            <p class="sub-title">Chọn khoảng thời gian để xem số liệu</p>
          </div>
          <n-space align="center">
            <n-radio-group v-model:value="filterType" name="filterGroup">
              <n-radio-button value="today">Hôm nay</n-radio-button>
              <n-radio-button value="week">Tuần này</n-radio-button>
              <n-radio-button value="month">Tháng này</n-radio-button>
              <n-radio-button value="year">Năm nay</n-radio-button>
              <n-radio-button value="custom">Tùy chỉnh</n-radio-button>
            </n-radio-group>

            <n-date-picker 
              v-if="filterType === 'custom'" 
              v-model:value="rangeDate" 
              type="daterange" 
              size="small"
              clearable 
            />

            <n-button type="success" @click="handleExportExcel">Xuất báo cáo</n-button>
          </n-space>
        </div>

        <n-divider style="margin: 5px 0" />

        <n-grid :cols="6" class="filter-stats-row">
          <n-gi>
            <div class="mini-label">Tổng số đơn hàng ({{ periodMap[filterType]?.label || 'Tùy chỉnh' }})</div>
            <div class="mini-value text-black">{{ currentFilterStats.totalOrders || 0 }}</div>
          </n-gi>
            <n-gi>
            <div class="mini-label">Số lượng sản phẩm({{ periodMap[filterType]?.label || 'Tùy chỉnh' }})</div>
            <div class="mini-value text-black">{{ (currentFilterStats.soldProducts) }}</div>
          </n-gi>
          <n-gi>
            <div class="mini-label">Hoàn thành({{ periodMap[filterType]?.label || 'Tùy chỉnh' }})</div>
            <div class="mini-value text-blue">{{ (currentFilterStats.completed) }}</div>
          </n-gi>
          <n-gi>
            <div class="mini-label">Đang xử lý({{ periodMap[filterType]?.label || 'Tùy chỉnh' }})</div>
            <div class="mini-value text-yellow">{{ (currentFilterStats.processing) }}</div>
          </n-gi>
          <n-gi>
            <div class="mini-label">Huỷ({{ periodMap[filterType]?.label || 'Tùy chỉnh' }})</div>
            <div class="mini-value text-red">{{ (currentFilterStats.cancelled) }}</div>
          </n-gi>
          <n-gi>
            <div class="mini-label">Tổng doanh thu ({{ periodMap[filterType]?.label || 'Tùy chỉnh' }})</div>
            <div class="mini-value text-green">{{ formatMoney(currentFilterStats.revenue) }}</div>
          </n-gi>
        </n-grid>
      </n-space>
    </n-card>

    <n-grid :x-gap="16" :cols="12">
      <n-gi span="7">
        <n-card title="Biểu Đồ Doanh Thu" :bordered="false" class="shadow-sm">
          <Chart :filter-type="filterType" :range-date="rangeDate" />
        </n-card>
      </n-gi>
      <n-gi span="5">
        <n-card title="Phân bổ trạng thái đơn hàng" :bordered="false" class="shadow-sm">
          <Chart3 :filter-type="filterType" :range-date="rangeDate" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-grid :x-gap="16" :cols="12">

      <n-gi span="6">
        <n-card title="Top sản phẩm bán chạy" :bordered="false" class="shadow-sm">
          <n-table :single-line="false" size="small">
            <thead>
              <tr>
                <th style="width: 40px">#</th>
                <th style="width: 60px">Ảnh</th>
                <th>Tên Sản Phẩm</th>
                <th>Giá</th>
                <th>Bán</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in overviewData.topSellingProducts" :key="index">
                <td>{{ index + 1 }}</td>
                <td>
                  <n-avatar round size="small" :src="item.image" fallback-src="https://via.placeholder.com/40" style="border: none;" />
                </td>
                <td class="n-ellipsis">{{ item.name }}</td>
                <td class="text-red font-bold">{{ formatMoney(item.price) }}</td>
                <td><n-tag type="success" size="small" round>{{ item.count }}</n-tag></td>
              </tr>
            </tbody>
          </n-table>
        </n-card>
      </n-gi>

       <n-gi span="6">
        <n-card title="Bảng Thống Kê Chi Tiết" :bordered="false" class="shadow-sm">
          <n-table :single-line="false" size="small" striped>
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Doanh thu</th>
                <th>Số đơn</th>
                <th>Giá trị TB</th>
                <th>Tăng trưởng</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(info, key) in periodMap" :key="key">
                <td class="font-bold">{{ info.label }}</td>
                <td class="font-bold">{{ formatMoney(overviewData[key]?.revenue) }}</td>
                <td>{{ overviewData[key]?.totalOrders || 0 }}</td>
                <td>{{ formatMoney(overviewData[key]?.revenue / (overviewData[key]?.totalOrders || 1)) }}</td>
                <td class="text-green">{{ overviewData[key]?.growthRate || '+0%' }}</td>
              </tr>
            </tbody>
          </n-table>
        </n-card>
      </n-gi>
    </n-grid>
  </n-space>
</template>

<style scoped>
.shadow-sm { box-shadow: 0 1px 3px rgba(0,0,0,0.1); border-radius: 8px; }

/* Filter styles */
.filter-header-row { display: flex; justify-content: space-between; align-items: center; }
.main-title { margin: 0; font-size: 20px; font-weight: 600;  }
.sub-title { margin: 4px 0 0 0; color: #767c82; font-size: 13px; }

.filter-stats-row .mini-label { color: #767c82; font-size: 13px; margin-bottom: 4px; }
.filter-stats-row .mini-value { font-size: 18px; font-weight: 700; }

.text-green { color: #18a058; }
.text-blue { color: #2080f0; }
.text-red { color: #d03050; }
.text-orange { color: #f0a020; }
.text-black { color: #000; }
.font-bold { font-weight: 600; }

/* Table styles - Chữ thường cho dữ liệu */
th { font-weight: 600 !important; color: #666; }
td { font-weight: 400 !important; }

/* Card styles */
.period-card { height: 160px; display: flex; flex-direction: column; justify-content: center; }
.period-header { color: #666; font-size: 14px; margin-bottom: 8px; font-weight: 600; }
.period-revenue { font-size: 22px; font-weight: 700;  }
.period-sub { font-size: 11px; color: #999; margin-top: 8px; }

.n-ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
</style>