<template>
  <div class="main-layout">
    <div class="left-column">
      <div class="top-header">
        <div class="search-and-create-section">
          <n-button type="primary" @click="createInvoice" class="btn-create-new-invoice">
            <template #icon>
              <n-icon>
                <AddCircleOutline />
              </n-icon>
            </template>
            Tạo hóa đơn mới
          </n-button>
        </div>
      </div>

      <n-card class="card" size="small">
        <template #header>
          <n-text type="primary" strong>Hóa đơn chờ</n-text>
        </template>
        <div class="pending-invoices-container">
          <n-scrollbar x-scrollable>
            <div class="pending-invoices-wrapper">
              <n-space :wrap="false">
                <div
                  v-for="tab in tabs"
                  :key="tab.id"
                  :class="['pending-invoice-card', { active: activeTab === tab.id }]"
                  @click="clickkActiveTab(tab.id, tab.idHD, tab.loaiHoaDon)"
                >
                  <div class="invoice-header">
                    <n-text strong>{{ tab.ma }}</n-text>
                    <n-popconfirm
                      @positive-click="() => huy(tab.idHD)"
                      positive-text="Đồng ý"
                      negative-text="Hủy"
                    >
                      <template #trigger>
                        <n-button text type="error" size="tiny" class="delete-invoice-btn">
                          <n-icon>
                            <TrashOutline />
                          </n-icon>
                        </n-button>
                      </template>
                      <n-text depth="3">Bạn có chắc chắn muốn hủy hóa đơn này?</n-text>
                    </n-popconfirm>
                  </div>
                  <n-space vertical :size="4">
                    <n-tag type="warning" size="small" round>Chờ xử lý</n-tag>
                    <n-text depth="3">{{ tab.soLuong || 0 }} sản phẩm</n-text>
                  </n-space>
                </div>
              </n-space>
            </div>
          </n-scrollbar>
        </div>
      </n-card>

      <n-card class="card" size="small">
        <template #header>
          <n-text type="primary" strong>Giỏ hàng</n-text>
        </template>
        <template #header-extra>
          <n-space>
            <n-button type="primary" size="small" @click="openProductSelectionModal">
              Chọn sản phẩm
            </n-button>
            <n-button type="primary" size="small" @click="openQrModal">
              <template #icon>
                <n-icon>
                  <QrCodeOutline />
                </n-icon>
              </template>
              Quét QR
            </n-button>
          </n-space>
        </template>
        <div v-if="state.gioHang.length > 0">
          <n-data-table
            :columns="columnsGiohang"
            :data="state.gioHang"
            :max-height="280"
            size="small"
            :pagination="{ pageSize: 5 }"
          />
        </div>
        <n-empty v-else description="Không có sản phẩm nào trong giỏ hàng" size="small" />
      </n-card>

      <n-card v-if="isDeliveryEnabled" class="card" size="small">
        <template #header>
          <n-text type="primary" strong>Thông tin người nhận</n-text>
        </template>
        <n-form ref="deliveryForm" :model="deliveryInfo" label-placement="left" :label-width="140">
          <n-grid :cols="12" :x-gap="12" :y-gap="12">
            <n-form-item-gi :span="6" path="tenNguoiNhan" label="Tên người nhận" required>
              <n-input
                v-model:value="deliveryInfo.tenNguoiNhan"
                placeholder="Nhập tên người nhận"
                clearable
              />
            </n-form-item-gi>
            
            <n-form-item-gi :span="6" path="sdtNguoiNhan" label="Số điện thoại" required>
              <n-input
                v-model:value="deliveryInfo.sdtNguoiNhan"
                placeholder="Nhập số điện thoại"
                clearable
              />
            </n-form-item-gi>
            
            <n-form-item-gi :span="4" path="tinhThanhPho" label="Tỉnh/Thành phố" required>
              <n-select
                v-model:value="deliveryInfo.tinhThanhPho"
                placeholder="Chọn tỉnh/thành phố"
                :options="provinces"
                @update:value="onProvinceChange"
                filterable
                clearable
              />
            </n-form-item-gi>
            
            <n-form-item-gi :span="4" path="quanHuyen" label="Quận/Huyện" required>
              <n-select
                v-model:value="deliveryInfo.quanHuyen"
                placeholder="Chọn quận/huyện"
                :options="districts"
                @update:value="onDistrictChange"
                filterable
                clearable
              />
            </n-form-item-gi>
            
            <n-form-item-gi :span="4" path="phuongXa" label="Phường/Xã" required>
              <n-select
                v-model:value="deliveryInfo.phuongXa"
                placeholder="Chọn phường/xã"
                :options="wards"
                @update:value="onWardChange"
                filterable
                clearable
              />
            </n-form-item-gi>
            
            <n-form-item-gi :span="12" path="diaChiCuThe" label="Địa chỉ cụ thể" required>
              <n-input
                v-model:value="deliveryInfo.diaChiCuThe"
                placeholder="Nhập số nhà, tên đường..."
                clearable
              />
            </n-form-item-gi>
            
            <n-form-item-gi :span="12" path="ghiChu" label="Ghi chú">
              <n-input
                v-model:value="deliveryInfo.ghiChu"
                type="textarea"
                placeholder="Nhập ghi chú (nếu có)"
                :rows="3"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>
      </n-card>
    </div>

    <div class="right-column">
      <n-card class="card" size="small">
        <template #header>
          <n-text type="primary" strong>Khách hàng</n-text>
        </template>
        <div v-if="state.detailKhachHang">
          <n-space vertical :size="16">
            <div>
              <n-text depth="3">Tên khách hàng</n-text>
              <n-input :value="state.detailKhachHang.ten" readonly class="mt-1" />
            </div>
            <div>
              <n-text depth="3">Số điện thoại</n-text>
              <n-input :value="state.detailKhachHang.sdt" readonly class="mt-1" />
            </div>
          </n-space>
        </div>
        <div v-else>
          <n-space vertical :size="16">
            <div>
              <n-text depth="3">Tên khách hàng</n-text>
              <n-input v-model:value="newCustomer.ten" placeholder="Tên khách hàng" class="mt-1" />
            </div>
            <div>
              <n-text depth="3">Số điện thoại</n-text>
              <n-input v-model:value="newCustomer.sdt" placeholder="Số điện thoại" class="mt-1" />
            </div>
          </n-space>
        </div>
        <template #footer>
          <n-space>
            <n-button type="primary" @click="showKhachHangModal = true" secondary>
              Chọn khách hàng
            </n-button>
            <n-button type="primary" @click="addCustomer" :loading="addCustomerLoading" secondary>
              Thêm khách hàng
            </n-button>
          </n-space>
        </template>
      </n-card>

      <n-card class="card" size="small">
        <template #header>
          <n-space justify="space-between" align="center" style="width: 100%">
            <n-text type="primary" strong>Thông tin đơn hàng</n-text>
            <n-space align="center">
              <n-text depth="3">Giao hàng</n-text>
              <n-switch
                v-model:value="isDeliveryEnabled"
                @update:value="giaoHang"
                size="small"
              />
            </n-space>
          </n-space>
        </template>
        <n-space vertical :size="16">
          <div>
            <n-text depth="3">Mã giảm giá</n-text>
            <n-space align="center" class="mt-1">
              <n-input
                v-model:value="selectedDiscountCode"
                placeholder="Chọn mã giảm giá"
                readonly
                style="flex: 1"
              />
              <n-button type="primary" @click="showDiscountModal = true" size="small">
                Chọn mã
              </n-button>
            </n-space>
            <n-text v-if="betterDiscountMessage" type="error" depth="3" class="mt-1">
              {{ betterDiscountMessage }}
            </n-text>
            <n-text v-if="selectedDiscount" type="success" depth="3" class="mt-1">
              Giảm: {{ formatCurrency(giamGia) }}
            </n-text>
          </div>

          <n-divider style="margin: 8px 0" />

          <n-space vertical :size="12">
            <n-space justify="space-between">
              <n-text depth="3">Tổng tiền hàng:</n-text>
              <n-text strong>{{ formatCurrency(tienHang) }}</n-text>
            </n-space>
            
            <n-space justify="space-between">
              <n-text depth="3">Giảm giá:</n-text>
              <n-text type="success">-{{ formatCurrency(giamGia) }}</n-text>
            </n-space>
            
            <template v-if="isDeliveryEnabled">
              <n-space justify="space-between" align="center">
                <n-text depth="3">Phí vận chuyển:</n-text>
                <n-space align="center">
                  <n-input-number
                    v-model:value="shippingFee"
                    :min="0"
                    :formatter="formatCurrencyInput"
                    :parser="parseCurrency"
                    :disabled="isFreeShipping"
                    size="small"
                    style="width: 120px"
                  />
                  <img src="/images/ghn-logo.webp" alt="GHN Logo" style="height: 20px" />
                </n-space>
              </n-space>
              <n-alert v-if="isFreeShipping" type="success" size="small" show-icon>
                Miễn phí vận chuyển (Đơn hàng trên 5,000,000 VND)
              </n-alert>
            </template>

            <n-divider style="margin: 8px 0" />

            <n-space justify="space-between">
              <n-text strong size="large">Tổng tiền:</n-text>
              <n-text strong type="primary" size="large">{{ formatCurrency(tongTien) }}</n-text>
            </n-space>
          </n-space>

          <n-divider style="margin: 12px 0" />

          <n-space vertical :size="8">
            <n-text depth="3">Phương thức thanh toán:</n-text>
            <n-space>
              <n-button
                :type="state.currentPaymentMethod === '0' ? 'primary' : 'default'"
                size="small"
                @click="handlePaymentMethod('0')"
                secondary
              >
                Tiền mặt
              </n-button>
              <n-button
                :type="state.currentPaymentMethod === '1' ? 'primary' : 'default'"
                size="small"
                @click="handlePaymentMethod('1')"
                secondary
              >
                Chuyển khoản
              </n-button>
            </n-space>
          </n-space>

          <n-popconfirm
            @positive-click="xacNhan(0)"
            positive-text="Đồng ý"
            negative-text="Hủy"
          >
            <template #trigger>
              <n-button type="primary" size="large" style="width: 100%; margin-top: 8px">
                Xác nhận thanh toán
              </n-button>
            </template>
            <n-text depth="3">Bạn có chắc chắn muốn xác nhận thanh toán hóa đơn này?</n-text>
          </n-popconfirm>
        </n-space>
      </n-card>
    </div>

    <!-- Modals -->
    <n-modal
      v-model:show="showProductModal"
      preset="dialog"
      title="Chọn sản phẩm"
      style="width: 90%; max-width: 1400px"
      :mask-closable="false"
    >
      <n-card size="small" :bordered="false">
        <template #header>
          <n-space vertical :size="12">
            <n-grid :cols="12" :x-gap="12" :y-gap="12">
              <n-gi :span="12">
                <n-input
                  v-model:value="localSearchQuery"
                  placeholder="Tìm kiếm sản phẩm..."
                  clearable
                />
              </n-gi>
              <n-gi :span="6">
                <n-select
                  v-model:value="localColor"
                  :options="ColorOptions"
                  placeholder="Màu sắc"
                  clearable
                />
              </n-gi>
              <n-gi :span="6">
                <n-select
                  v-model:value="localCPU"
                  :options="CpuOptions"
                  placeholder="CPU"
                  clearable
                />
              </n-gi>
              <n-gi :span="6">
                <n-select
                  v-model:value="localGPU"
                  :options="GpuOptions"
                  placeholder="GPU"
                  clearable
                />
              </n-gi>
              <n-gi :span="6">
                <n-select
                  v-model:value="localRAM"
                  :options="RamOptions"
                  placeholder="RAM"
                  clearable
                />
              </n-gi>
              <n-gi :span="6">
                <n-select
                  v-model:value="localHardDrive"
                  :options="HardDriveOptions"
                  placeholder="Ổ cứng"
                  clearable
                />
              </n-gi>
              <n-gi :span="6">
                <n-select
                  v-model:value="localSelectedMaterial"
                  :options="MaterialOptions"
                  placeholder="Chất liệu"
                  clearable
                />
              </n-gi>
              <n-gi :span="3">
                <n-input-number
                  v-model:value="localMinPrice"
                  placeholder="Giá từ"
                  :min="0"
                  style="width: 100%"
                />
              </n-gi>
              <n-gi :span="3">
                <n-input-number
                  v-model:value="localMaxPrice"
                  placeholder="Đến"
                  :min="0"
                  style="width: 100%"
                />
              </n-gi>
              <n-gi :span="12">
                <n-button @click="resetFilters" type="default" size="small" secondary>
                  <template #icon>
                    <n-icon><ReloadOutline /></n-icon>
                  </template>
                  Đặt lại bộ lọc
                </n-button>
              </n-gi>
            </n-grid>
          </n-space>
        </template>
        <n-data-table
          :columns="columns"
          :data="stateSP.products"
          :max-height="400"
          size="small"
          :pagination="{
            page: stateSP.paginationParams.page,
            pageSize: stateSP.paginationParams.size,
            pageCount: Math.ceil(stateSP.totalItems / stateSP.paginationParams.size),
            showSizePicker: true,
            pageSizes: [10, 20, 30, 40, 50]
          }"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </n-card>
    </n-modal>

    <n-modal
      v-model:show="showKhachHangModal"
      preset="dialog"
      title="Chọn khách hàng"
      style="width: 90%; max-width: 1000px"
      :mask-closable="false"
    >
      <n-card size="small" :bordered="false">
        <n-space vertical :size="16">
          <n-input
            v-model:value="customerSearchQuery"
            placeholder="Tìm kiếm theo tên hoặc số điện thoại..."
            clearable
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
          
          <n-data-table
            :columns="columnsKhachHang"
            :data="state.khachHang"
            :max-height="400"
            size="small"
            :pagination="{
              page: state.paginationParams.page,
              pageSize: state.paginationParams.size,
              pageCount: Math.ceil(state.totalItemsKH / state.paginationParams.size),
              showSizePicker: true,
              pageSizes: [10, 20, 30, 40, 50]
            }"
            @update:page="handleCustomerPageChange"
            @update:page-size="handleCustomerPageSizeChange"
          />
        </n-space>
      </n-card>
    </n-modal>

    <n-modal
      v-model:show="isBothPaymentModalVisible"
      preset="dialog"
      title="Thanh toán kết hợp"
      positive-text="Xác nhận"
      negative-text="Hủy"
      @positive-click="confirmBothPayment"
      @negative-click="closeBothPaymentModal"
      :loading="bothPaymentLoading"
    >
      <n-space vertical :size="16">
        <n-text depth="3">Quét QR để thanh toán phần còn lại</n-text>
        <n-image
          src="/images/qr.png"
          width="200"
          height="200"
          object-fit="contain"
          preview-disabled
        />
        <n-form :model="paymentForm">
          <n-form-item label="Số tiền khách đưa (VND)" path="amountPaid">
            <n-input-number
              v-model:value="amountPaid"
              placeholder="Nhập số tiền"
              style="width: 100%"
              :min="0"
            />
          </n-form-item>
        </n-form>
        <n-space vertical :size="8">
          <n-space justify="space-between">
            <n-text depth="3">Tổng tiền khách đã trả:</n-text>
            <n-text strong>
              {{ formatCurrency(tienKhachThanhToan + (amountPaid || 0)) }}
            </n-text>
          </n-space>
          <n-space justify="space-between">
            <n-text depth="3">Tiền còn thiếu:</n-text>
            <n-text type="error" strong>
              {{ formatCurrency(Math.max(0, tongTien - (tienKhachThanhToan + (amountPaid || 0)))) }}
            </n-text>
          </n-space>
        </n-space>
      </n-space>
    </n-modal>

    <n-modal
      v-model:show="isQrVNpayModalVisible"
      preset="dialog"
      title="Quét QR thanh toán"
      positive-text="Đã thanh toán"
      @positive-click="closeQrModalVnPay"
      :mask-closable="false"
    >
      <n-space vertical align="center" :size="16">
        <n-text depth="3">Quét mã QR để thanh toán qua VNPay</n-text>
        <n-image
          src="/images/qr.png"
          width="200"
          height="200"
          object-fit="contain"
          preview-disabled
        />
      </n-space>
    </n-modal>

    <n-modal
      v-model:show="state.isModalOpen"
      preset="dialog"
      title="Chọn số lượng"
      positive-text="Xác nhận"
      negative-text="Hủy"
      @positive-click="confirmQuantity"
      @negative-click="closeModal"
      :mask-closable="false"
    >
      <n-form :model="selectedProduct">
        <n-form-item label="Số lượng">
          <n-input-number
            v-model:value="selectedProduct.soLuong"
            :min="1"
            placeholder="Nhập số lượng"
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
    </n-modal>

    <n-modal
      v-model:show="showDeliveryModal"
      preset="dialog"
      title="Áp dụng phiếu giảm giá tốt hơn"
      positive-text="Áp dụng"
      negative-text="Không"
      @positive-click="confirmQuantityP"
      @negative-click="closeModalP"
      :mask-closable="false"
    >
      <n-space vertical align="center" :size="16">
        <n-text>Đang có một phiếu giảm giá tốt hơn</n-text>
        <n-text type="primary" strong>{{ phieuNgon }}</n-text>
        <n-text depth="3">Bạn có muốn áp dụng không?</n-text>
      </n-space>
    </n-modal>

    <n-modal
      v-model:show="isQrModalVisible"
      preset="dialog"
      title="Quét mã QR sản phẩm"
      :mask-closable="false"
      @after-leave="stopQrScanning"
    >
      <n-card size="small" :bordered="false">
        <div id="reader" style="width: 100%; max-width: 400px; margin: 0 auto;"></div>
        <n-alert v-if="!hasCamera" type="error" title="Lỗi camera" class="mt-2">
          Không tìm thấy camera hoặc không có quyền truy cập camera.
        </n-alert>
      </n-card>
    </n-modal>

    <n-modal
      v-model:show="showDiscountModal"
      preset="dialog"
      title="Chọn mã giảm giá"
      style="width: 600px"
      :mask-closable="false"
    >
      <n-card size="small" :bordered="false">
        <n-list v-if="state.discountList.length > 0" class="discount-list">
          <n-list-item v-for="item in state.discountList" :key="item.id">
            <n-thing :title="item.ma" :description="item.ten">
              <template #avatar>
                <n-tag type="success" size="small">{{ formatCurrency(item.giaTriGiamThucTe) }}</n-tag>
              </template>
            </n-thing>
            <template #suffix>
              <n-button
                type="primary"
                size="small"
                @click="selectDiscount(item)"
                :disabled="!!selectedDiscount && selectedDiscount.id === item.id"
              >
                {{ selectedDiscount?.id === item.id ? 'Đã chọn' : 'Chọn' }}
              </n-button>
            </template>
          </n-list-item>
        </n-list>
        <n-empty v-else description="Không có mã giảm giá nào phù hợp" />
      </n-card>
      <template #action>
        <n-space justify="end">
          <n-button @click="resetDiscount" v-if="selectedDiscount">Bỏ chọn</n-button>
          <n-button @click="showDiscountModal = false">Đóng</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, watch, nextTick, h } from 'vue'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'
import {
  GetHoaDons,
  getCreateHoaDon,
  themSanPham,
  GetGioHang,
  xoaSP,
  themSL,
  xoaSL,
  type KhachHangResponse,
  GetKhachHang,
  themKhachHang,
  GeOneKhachHang,
  getPhuongThucThanhToan,
  type PhuongThucThanhToanResponse,
  thanhToanThanhCong,
  type PhieuGiamGiaResponse,
  getMaGiamGia,
  type ThongTinGiaoHangResponse,
  suaGiaoHang,
  huyHoaDon,
  getMaGiamGiaKoDu,
  themMoiKhachHang
} from '@/service/api/admin/banhang.api'
import type { DataTableColumns } from 'naive-ui'
import type { ADProductDetailResponse, ADProductDetailRequest } from '@/service/api/admin/product/productDetail.api'
import {getProductDetails, getColors, getCPUs, getGPUs, getRAMs, getHardDrives, getMaterials } from '@/service/api/admin/product/productDetail.api'
import { getGHNProvinces } from '@/service/api/ghn.api'
import { getGHNDistricts } from '@/service/api/ghn.api'
import { getGHNWards } from '@/service/api/ghn.api'
import { getAvailableServices } from '@/service/api/ghn.api'
import type { AvailableServiceRequest } from '@/service/api/ghn.api'
import type { ShippingFeeRequest } from '@/service/api/ghn.api'
import { calculateFee } from '@/service/api/ghn.api'
import { Html5Qrcode } from 'html5-qrcode'
import { debounce } from 'lodash';
import { localStorageAction } from '@/utils/storage';
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'

// Naive UI Icons
import {
  QrCodeOutline,
  ReloadOutline,
  AddCircleOutline,
  TrashOutline,
  SearchOutline
} from '@vicons/ionicons5'

// Naive UI components
import {
  NDataTable,
  NModal,
  NInput,
  NSelect,
  NCard,
  NSwitch,
  NPopconfirm,
  NTooltip,
  NList,
  NListItem,
  NThing,
  NEmpty,
  NSpace,
  NForm,
  NFormItem,
  NInputNumber,
  NButton,
  NTag,
  NImage,
  NIcon,
  NText,
  NDivider,
  NAlert,
  NGrid,
  NGi,
  NScrollbar,
  NFormItemGi
} from 'naive-ui'

// Local filter variables
const localSearchQuery = ref('');
const localColor = ref<string | null>(null);
const localCPU = ref<string | null>(null);
const localGPU = ref<string | null>(null);
const localRAM = ref<string | null>(null);
const localHardDrive = ref<string | null>(null);
const localSelectedMaterial = ref<string | null>(null);
const localMinPrice = ref<number>(0);
const localMaxPrice = ref<number>(0);

const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)
const ColorOptions = ref<{ label: string; value: string }[]>([]);
const CpuOptions = ref<{ label: string; value: string }[]>([]);
const GpuOptions = ref<{ label: string; value: string }[]>([]);
const RamOptions = ref<{ label: string; value: string }[]>([]);
const HardDriveOptions = ref<{ label: string; value: string }[]>([]);
const MaterialOptions = ref<{ label: string; value: string }[]>([]);

const isBothPaymentModalVisible = ref(false)
const amountPaid = ref(0)
const bothPaymentLoading = ref(false)
const soTien = ref(0)
const tienKhachThanhToan = ref(0)
const tienThieu = ref(0)
const tongTien = ref(0)
const tongTienTruocGiam = ref(0)
const giamGia = ref(0)
const tienHang = ref(0)
const idSP = ref('')
const idHDS = ref('')
const tabs = ref<Array<{ id: number, idHD: string, ma: string, soLuong: number, products: any[], loaiHoaDon: string }>>([])
const activeTab = ref(0)
const idPGG = ref('')
let nextTabId = 1
const loaiHD = ref('')
const showDiscountModal = ref(false)
const discountList = ref<PhieuGiamGiaResponse[]>([])
const selectedDiscount = ref<PhieuGiamGiaResponse | null>(null)
const selectedDiscountCode = ref<string>('')
const isDeliveryEnabled = ref(false)
const showDeliveryModal = ref(false)
const deliveryInfo = reactive({
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: null as string | null,
  quanHuyen: null as string | null,
  phuongXa: null as string | null,
  diaChiCuThe: '',
  ghiChu: '',
})
const currentDeliveryInfo = ref<ThongTinGiaoHangResponse | null>(null)
const customerSearchQuery = ref('')
const betterDiscountMessage = ref('')
const deliveryInfoByInvoice = reactive<{ [key: string]: any }>({})
// GHN specific states
const provinces = ref<Array<{ value: string, label: string, code: string }>>([])
const districts = ref<Array<{ value: string, label: string, code: string }>>([])
const wards = ref<Array<{ value: string, label: string, code: string }>>([])
const shippingFee = ref(0)
const provinceCode = ref<number | null>(null)
const districtCode = ref<number | null>(null)
const wardCode = ref<string | null>(null)
const FROM_DISTRICT_ID = 3440
const FROM_WARD_CODE = '13010'
const isBestDiscountApplied = ref(false)
const phieuNgon = ref('')
const GHN_API_TOKEN = '72f634c6-58a2-11f0-8a1e-1e10d8df3c04'
const GHN_SHOP_ID = 5872469

// Main reactive state object
const state = reactive({
  searchQuery: '',
  idSP: '',
  searchStatus: null as number | null,
  isModalOpen: false,
  isModaThanhToanlOpen: false,
  isModalChangeStatus: false,
  selectedProductId: null as string | null,
  khachHang: [] as KhachHangResponse[],
  thanhToan: [],
  discountList: [] as PhieuGiamGiaResponse[],
  phuongThuThanhToan: [] as PhuongThucThanhToanResponse[],
  tongTien: null as { tongTien: number } | null,
  detailKhachHang: null as KhachHangResponse | null,
  products: [] as ADProductDetailResponse[],
  gioHang: [] as any[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0,
  totalItemsKH: 0,
  selectedPaymentMethod: '' as string,
  currentPaymentMethod: '0'
})

const stateSP = reactive({
  searchQuery: '',
  searchStatus: null as number | null,
  selectedMaterial: null as string | null,
  isModalOpen: false,
  isModalChangeStatus: false,
  selectedProductId: null as string | null,
  products: [] as ADProductDetailResponse[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0
})

const debouncedFetchCustomers = debounce(async () => {
  await fetchCustomers()
}, 300)

const fetchCustomers = async () => {
  try {
    const params = {
      page: state.paginationParams.page,
      size: state.paginationParams.size,
      q: customerSearchQuery.value.trim(),
    }
    const response = await GetKhachHang(params)
    state.khachHang = response.data?.data || []
    state.totalItemsKH = response.totalElements || 0
  } catch (error) {
    console.error('Failed to fetch customers:', error)
    state.khachHang = []
    state.totalItemsKH = 0
  }
}

const debouncedFetchProducts = debounce(async () => {
  stateSP.searchQuery = localSearchQuery.value
  stateSP.selectedMaterial = localSelectedMaterial.value
  await fetchProducts()
}, 300)

watch(localSearchQuery, () => {
  debouncedFetchProducts()
})

watch([localColor, localCPU, localGPU, localRAM, localHardDrive, localSelectedMaterial, localMinPrice, localMaxPrice], () => {
  debouncedFetchProducts()
})

const resetFilters = () => {
  localSearchQuery.value = ''
  localColor.value = null
  localCPU.value = null
  localGPU.value = null
  localRAM.value = null
  localHardDrive.value = null
  localSelectedMaterial.value = null
  localMinPrice.value = 0
  localMaxPrice.value = 0
  stateSP.searchQuery = ''
  stateSP.selectedMaterial = null
  fetchProducts()
}

const handleColorChange = () => {
  debouncedFetchProducts()
}

const handleCPUChange = () => {
  debouncedFetchProducts()
}

const handleGPUChange = () => {
  debouncedFetchProducts()
}

const handleRAMChange = () => {
  debouncedFetchProducts()
}

const handleHardDriveChange = () => {
  debouncedFetchProducts()
}

const parseCurrency = (value: string) => {
  if (!value) return 0
  let str = String(value).replace(/[^0-9nghìntrieu]/g, '').trim().toLowerCase()
  let number = parseInt(str.replace(/[^0-9]/g, '')) || 0

  if (str.includes('nghìn')) {
    number *= 1000
  } else if (str.includes('triệu')) {
    number *= 1000000
  }

  return number
}

const formatCurrencyInput = (value: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

const fetchProvinces = async () => {
  try {
    const response = await getGHNProvinces(GHN_API_TOKEN)
    provinces.value = response.map((item: any) => ({
      value: String(item.ProvinceID),
      label: item.ProvinceName,
      code: String(item.ProvinceID),
    }))
  } catch (error) {
    console.error('Failed to fetch provinces:', error)
    provinces.value = []
  }
}

const fetchDistricts = async (provinceId: number) => {
  try {
    const response = await getGHNDistricts(provinceId, GHN_API_TOKEN)
    districts.value = response.map((item: any) => ({
      value: String(item.DistrictID),
      label: item.DistrictName,
      code: String(item.DistrictID),
    }))
  } catch (error) {
    console.error('Failed to fetch districts:', error)
    toast.error('Không thể tải danh sách Quận/Huyện.')
    districts.value = []
  }
}

const fetchWards = async (districtId: number) => {
  try {
    const response = await getGHNWards(districtId, GHN_API_TOKEN)
    wards.value = response.map((item: any) => ({
      value: item.WardCode,
      label: item.WardName,
      code: item.WardCode,
    }))
  } catch (error) {
    console.error('Failed to fetch wards:', error)
    toast.error('Không thể tải danh sách Phường/Xã.')
    wards.value = []
  }
}

const fetchColor = async () => {
  try {
    const { data } = await getColors()
    ColorOptions.value = data.map((c: any) => ({ label: c.ten, value: c.id }))
  } catch (e) {
    console.error('Error fetching colors:', e)
  }
}

const fetchCPU = async () => {
  try {
    const { data } = await getCPUs()
    CpuOptions.value = data.map((c: any) => ({ label: c.ten, value: c.id }))
  } catch (e) {
    console.error('Error fetching CPU:', e)
  }
}

const fetchGPU = async () => {
  try {
    const { data } = await getGPUs()
    GpuOptions.value = data.map((g: any) => ({ label: g.ten, value: g.id }))
  } catch (e) {
    console.error('Error fetching GPU:', e)
  }
}

const fetchRAM = async () => {
  try {
    const { data } = await getRAMs()
    RamOptions.value = data.map((r: any) => ({ label: r.ten, value: r.id }))
  } catch (e) {
    console.error('Error fetching RAM:', e)
  }
}

const fetchHardDrive = async () => {
  try {
    const { data } = await getHardDrives()
    HardDriveOptions.value = data.map((h: any) => ({ label: h.ten, value: h.id }))
  } catch (e) {
    console.error('Error fetching Hard Drive:', e)
  }
}

const fetchMaterial = async () => {
  try {
    const { data } = await getMaterials()
    MaterialOptions.value = data.map((m: any) => ({ label: m.ten, value: m.id }))
  } catch (e) {
    console.error('Error fetching Material:', e)
  }
}

const checkFromDistrictAndWard = async () => {
  try {
    const response = await getGHNDistricts(null, GHN_API_TOKEN)
    const districtExists = response.data.some((d: any) => d.DistrictID === FROM_DISTRICT_ID)
    if (!districtExists) {
      console.error(`FROM_DISTRICT_ID ${FROM_DISTRICT_ID} không hợp lệ!`)
      toast.error('Mã quận/huyện nguồn không hợp lệ.')
      return false
    }
    const wardResponse = await getGHNWards(FROM_DISTRICT_ID, GHN_API_TOKEN)
    const wardExists = wardResponse.data.some((w: any) => w.WardCode === FROM_WARD_CODE)
    if (!wardExists) {
      console.error(`FROM_WARD_CODE ${FROM_WARD_CODE} không hợp lệ!`)
      toast.error('Mã phường/xã nguồn không hợp lệ.')
      return false
    }
    return true
  } catch (error) {
    console.error('Lỗi khi kiểm tra mã quận/huyện hoặc phường/xã:', error)
    return false
  }
}

onMounted(async () => {
  await fetchColor()
  await fetchCPU()
  await fetchGPU()
  await fetchRAM()
  await fetchHardDrive()
  await fetchMaterial()
  await fetchHoaDon()
  await checkFromDistrictAndWard()
  await fetchProvinces()
  setDefaultPaymentMethod()
})

const onProvinceChange = async (value: string) => {
  deliveryInfo.tinhThanhPho = value
  const selectedProvince = provinces.value.find(p => p.value === value)
  provinceCode.value = selectedProvince ? parseInt(selectedProvince.code) : null
  if (provinceCode.value) {
    await fetchDistricts(provinceCode.value)
    deliveryInfo.quanHuyen = undefined
    deliveryInfo.phuongXa = undefined
    districtCode.value = null
    wardCode.value = null
    await calculateShippingFee()
  } else {
    districts.value = []
    wards.value = []
    shippingFee.value = 0
    calculateTotalAmounts()
  }

  deliveryInfoByInvoice[idHDS.value] = {
    tenNguoiNhan: deliveryInfo.tenNguoiNhan,
    sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
    diaChiCuThe: deliveryInfo.diaChiCuThe,
    tinhThanhPho: deliveryInfo.tinhThanhPho,
    quanHuyen: deliveryInfo.quanHuyen,
    phuongXa: deliveryInfo.phuongXa,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    shippingFee: shippingFee.value,
  }
}

const onDistrictChange = async (value: string) => {
  deliveryInfo.quanHuyen = value
  const selectedDistrict = districts.value.find(d => d.value === value)
  districtCode.value = selectedDistrict ? parseInt(selectedDistrict.code) : null
  if (districtCode.value) {
    await fetchWards(districtCode.value)
    deliveryInfo.phuongXa = undefined
    wardCode.value = null
    await calculateShippingFee()
  } else {
    wards.value = []
    shippingFee.value = 0
    calculateTotalAmounts()
  }

  deliveryInfoByInvoice[idHDS.value] = {
    tenNguoiNhan: deliveryInfo.tenNguoiNhan,
    sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
    diaChiCuThe: deliveryInfo.diaChiCuThe,
    tinhThanhPho: deliveryInfo.tinhThanhPho,
    quanHuyen: deliveryInfo.quanHuyen,
    phuongXa: deliveryInfo.phuongXa,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    shippingFee: shippingFee.value,
  }
}

const onWardChange = async (value: string) => {
  deliveryInfo.phuongXa = value
  const selectedWard = wards.value.find(w => w.value === value)
  wardCode.value = selectedWard ? selectedWard.code : null
  await calculateShippingFee()

  deliveryInfoByInvoice[idHDS.value] = {
    tenNguoiNhan: deliveryInfo.tenNguoiNhan,
    sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
    diaChiCuThe: deliveryInfo.diaChiCuThe,
    tinhThanhPho: deliveryInfo.tinhThanhPho,
    quanHuyen: deliveryInfo.quanHuyen,
    phuongXa: deliveryInfo.phuongXa,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    shippingFee: shippingFee.value,
  }
}

const newCustomer = reactive({
  ten: '',
  sdt: ''
})

const isFreeShipping = ref(false)

watch(tienHang, () => {
  calculateTotalAmounts()
  if (isDeliveryEnabled.value && provinceCode.value && districtCode.value && wardCode.value) {
    calculateShippingFee()
  }
})

watch(tienHang, async (newTienHang) => {
  if (idHDS.value) {
    await fetchDiscounts(idHDS.value)
  }
})

const calculateShippingFee = async () => {
  if (!isDeliveryEnabled.value || !idHDS.value || !provinceCode.value || !districtCode.value || !wardCode.value || tienHang.value <= 0) {
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()
    return
  }

  if (tienHang.value > 5000000) {
    isFreeShipping.value = true
    shippingFee.value = 0
    toast.success('Đơn hàng trên 5,000,000 VND, miễn phí vận chuyển!')
    calculateTotalAmounts()
    return
  }

  try {
    const availableServicesRequestBody: AvailableServiceRequest = {
      shop_id: GHN_SHOP_ID,
      from_district: FROM_DISTRICT_ID,
      to_district: districtCode.value,
    }
    const availableServicesResponse = await getAvailableServices(GHN_API_TOKEN, availableServicesRequestBody)

    if (!availableServicesResponse.data || !availableServicesResponse.data.length) {
      shippingFee.value = 0
      isFreeShipping.value = false
      toast.warn('Không tìm thấy dịch vụ vận chuyển phù hợp.')
      calculateTotalAmounts()
      return
    }

    const selectedServiceId = availableServicesResponse.data[0].service_id
    const requestBody: ShippingFeeRequest = {
      myRequest: {
        FromDistrictID: FROM_DISTRICT_ID,
        FromWardCode: FROM_WARD_CODE,
        ServiceID: selectedServiceId,
        ToDistrictID: districtCode.value,
        ToWardCode: wardCode.value,
        Height: 15,
        Length: 15,
        Weight: 500,
        Width: 15,
        InsuranceValue: tienHang.value,
        Coupon: null,
        PickShift: null,
      },
    }

    const response = await calculateFee(requestBody, GHN_API_TOKEN, GHN_SHOP_ID)
    shippingFee.value = response.data.total || 0
    isFreeShipping.value = false
    calculateTotalAmounts()
  } catch (error) {
    console.error('Failed to calculate shipping fee:', error)
    shippingFee.value = 0
    isFreeShipping.value = false
    toast.error('Không thể tính phí vận chuyển.')
    calculateTotalAmounts()
  }
}

const confirmBothPayment = async () => {
  bothPaymentLoading.value = true
  try {
    if (!idHDS.value) throw new Error('Không có hóa đơn được chọn!')
    if (amountPaid.value <= 0) throw new Error('Vui lòng nhập số tiền hợp lệ!')
    const remainingAmount = tongTien.value - tienKhachThanhToan.value - amountPaid.value
    if (remainingAmount > 0) {
      toast.info(`Còn ${formatCurrency(remainingAmount)} cần thanh toán qua QR.`)
    } else {
      toast.success('Thanh toán đủ!')
    }

    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('tongTien', amountPaid.value.toString())
    formData.append('phuongThuc', 'Cả hai')
    await themPTTT(formData)

    toast.success('Xác nhận thanh toán cả hai phương thức thành công!')
    await clickkActiveTab(activeTab.value, idHDS.value, loaiHD.value)
    closeBothPaymentModal()
  } catch (error: any) {
    console.error('Error in confirmBothPayment:', error)
    toast.error(error.message || 'Xác nhận thanh toán thất bại!')
  } finally {
    bothPaymentLoading.value = false
  }
}

watch([deliveryInfo.tinhThanhPho, deliveryInfo.quanHuyen, deliveryInfo.phuongXa], () => {
  localStorage.setItem('deliveryInfoByInvoice', JSON.stringify(deliveryInfoByInvoice))
})

watch(selectedDiscount, () => {
  localStorage.setItem('selectedDiscount', JSON.stringify(selectedDiscount.value))
  localStorage.setItem('isBestDiscountApplied', JSON.stringify(isBestDiscountApplied.value))
})

const fetchDiscounts = async (idHD: string) => {
  try {
    if (!idHD) {
      console.warn('Missing idHD:', idHD)
      resetDiscount()
      return
    }

    const params = {
      idHD: tongTienTruocGiam.value.toString(),
      idKH: state.detailKhachHang?.id || '',
    }

    const response = await getMaGiamGia(params)
    state.discountList = Array.isArray(response.data) ? response.data : []

    const paramsFake = {
      idHD: idHD,
      idKH: state.detailKhachHang?.id || '',
      tongTien: tienHang.value,
    }
    const responseFake = await getMaGiamGiaKoDu(paramsFake)

    if (responseFake && responseFake.betterVoucher && responseFake.betterVoucher.amountNeeded !== undefined) {
      betterDiscountMessage.value = `Mua thêm ${formatCurrency(responseFake.betterVoucher.amountNeeded)} để nhận phiếu giảm giá tốt hơn`
    } else {
      betterDiscountMessage.value = ''
    }

    const storedDiscount = localStorage.getItem('selectedDiscount')
    if (storedDiscount) {
      let parsedDiscount
      try {
        parsedDiscount = JSON.parse(storedDiscount)
      } catch (e) {
        console.error('Failed to parse stored discount:', e)
        parsedDiscount = null
      }

      if (parsedDiscount && (parsedDiscount.id || parsedDiscount.ma)) {
        const matchedDiscount = state.discountList.find((d) =>
          d.id === parsedDiscount.id || d.ma === parsedDiscount.ma
        )
        if (matchedDiscount) {
          selectedDiscount.value = matchedDiscount
          selectedDiscountCode.value = matchedDiscount.ma
          giamGia.value = matchedDiscount.giaTriGiamThucTe || 0
          isBestDiscountApplied.value = phieuNgon.value === matchedDiscount.ma
        } else {
          applyBestDiscount()
        }
      } else {
        applyBestDiscount()
      }
    } else if (state.discountList.length > 0) {
      applyBestDiscount()
    } else {
      resetDiscount()
    }

    localStorage.setItem('selectedDiscount', JSON.stringify(selectedDiscount.value))
    localStorage.setItem('isBestDiscountApplied', JSON.stringify(isBestDiscountApplied.value))
    calculateTotalAmounts()
  } catch (error) {
    state.discountList = []
    resetDiscount()
  }
}

const applyBestDiscount = () => {
  if (state.discountList.length > 0) {
    const bestDiscount = state.discountList.reduce((best, current) =>
      (best.giaTriGiamThucTe || 0) > (current.giaTriGiamThucTe || 0) ? best : current
    )
    selectedDiscount.value = bestDiscount
    selectedDiscountCode.value = bestDiscount.ma
    phieuNgon.value = bestDiscount.ma
    giamGia.value = bestDiscount.giaTriGiamThucTe || 0
    isBestDiscountApplied.value = true
  } else {
    selectedDiscount.value = null
    selectedDiscountCode.value = ''
    giamGia.value = 0
    isBestDiscountApplied.value = false
  }
  calculateTotalAmounts()
  showDiscountModal.value = false
}

const giaoHang = async (isDeliveryEnableds: boolean) => {
  isDeliveryEnabled.value = isDeliveryEnableds

  if (isDeliveryEnabled.value && state.detailKhachHang) {
    deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
    deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
    deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''

    if (!provinces.value.length) {
      await fetchProvinces()
    }

    const tinhThanhPhoId = state.detailKhachHang.tinh
    const quanHuyenId = state.detailKhachHang.huyen
    const phuongXaId = state.detailKhachHang.xa

    if (tinhThanhPhoId) {
      const selectedProvince = provinces.value.find(p => p.code === tinhThanhPhoId.toString())
      if (selectedProvince) {
        deliveryInfo.tinhThanhPho = selectedProvince.value
        provinceCode.value = parseInt(tinhThanhPhoId)
        await fetchDistricts(provinceCode.value)
      }
    }

    if (quanHuyenId && provinceCode.value) {
      const selectedDistrict = districts.value.find(d => d.code === quanHuyenId.toString())
      if (selectedDistrict) {
        deliveryInfo.quanHuyen = selectedDistrict.value
        districtCode.value = parseInt(quanHuyenId)
        await fetchWards(districtCode.value)
      }
    }

    if (phuongXaId && districtCode.value) {
      const selectedWard = wards.value.find(w => w.code === phuongXaId)
      if (selectedWard) {
        deliveryInfo.phuongXa = selectedWard.value
        wardCode.value = phuongXaId
      }
    }

    await calculateShippingFee()

    deliveryInfoByInvoice[idHDS.value] = {
      tenNguoiNhan: deliveryInfo.tenNguoiNhan,
      sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
      diaChiCuThe: deliveryInfo.diaChiCuThe,
      tinhThanhPho: deliveryInfo.tinhThanhPho,
      quanHuyen: deliveryInfo.quanHuyen,
      phuongXa: deliveryInfo.phuongXa,
      provinceCode: provinceCode.value,
      districtCode: districtCode.value,
      wardCode: wardCode.value,
      shippingFee: shippingFee.value,
    }
  } else {
    Object.assign(deliveryInfo, {
      tenNguoiNhan: state.detailKhachHang?.ten || '',
      sdtNguoiNhan: state.detailKhachHang?.sdt || '',
      diaChiCuThe: state.detailKhachHang?.diaChi || '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()

    deliveryInfoByInvoice[idHDS.value] = {
      tenNguoiNhan: state.detailKhachHang?.ten || '',
      sdtNguoiNhan: state.detailKhachHang?.sdt || '',
      diaChiCuThe: state.detailKhachHang?.diaChi || '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
      provinceCode: null,
      districtCode: null,
      wardCode: null,
      shippingFee: 0,
    }
  }

  await suaGiaoHang(idHDS.value)
  await capNhatDanhSach()
}

const selectDiscount = (discount: PhieuGiamGiaResponse) => {
  selectedDiscount.value = discount
  selectedDiscountCode.value = discount.ma
  giamGia.value = discount.giaTriGiamThucTe || 0
  toast.success(`Đã chọn phiếu giảm giá: ${discount.ma}`)
  calculateTotalAmounts()
  if (phieuNgon.value === discount.ma) {
    isBestDiscountApplied.value = true
  } else {
    isBestDiscountApplied.value = false
  }
  showDiscountModal.value = false
}

const handlePageChange = (page: number) => {
  stateSP.paginationParams.page = page
  fetchProducts()
}

const handlePageSizeChange = (pageSize: number) => {
  stateSP.paginationParams.size = pageSize
  stateSP.paginationParams.page = 1
  fetchProducts()
}

const handleCustomerPageChange = (page: number) => {
  state.paginationParams.page = page
  fetchCustomers()
}

const handleCustomerPageSizeChange = (pageSize: number) => {
  state.paginationParams.size = pageSize
  state.paginationParams.page = 1
  fetchCustomers()
}

const closeBothPaymentModal = () => {
  isBothPaymentModalVisible.value = false
  amountPaid.value = 0
  bothPaymentLoading.value = false
}

const themPTTT = async (formData: FormData) => {
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
  } catch (error) {
    console.error('Failed to add payment method:', error)
    throw error
  }
}

const handlePaymentMethod = async (method: string) => {
  if (!idHDS.value) {
    toast.error('Vui lòng chọn hoặc tạo hóa đơn trước khi chọn phương thức thanh toán!')
    return
  }

  state.currentPaymentMethod = method

  try {
    if (method === '0') {
      toast.success('Đã chọn phương thức thanh toán Tiền mặt.')
    } else if (method === '1') {
      toast.success('Đã chọn phương thức thanh toán chuyển khoản.')
      openQrModalVNPay()
    } else if (method === '2') {
      toast.success('Đã chọn phương thức thanh toán vừa chuyển khoản vừa tiền mặt.')
      openQrModalVNPayCaHai()
    }
  } catch (error: any) {
    console.error('Error in handlePaymentMethod:', error)
    toast.error('Có l��i khi chọn phương thức thanh toán!')
  }
}

const updatePaymentStatus = async () => {
  if (idHDS.value) {
    const responsePTTT = await getPhuongThucThanhToan(idHDS.value)
    state.phuongThuThanhToan = responsePTTT
    let totalPaid = 0
    state.phuongThuThanhToan.forEach((item) => {
      totalPaid += item.tongTien
    })
    tienKhachThanhToan.value = totalPaid
    tienThieu.value = (tongTien.value || 0) - tienKhachThanhToan.value
  }
}

const fetchHoaDon = async () => {
  try {
    await fetchProducts()
    const response = await GetHoaDons()

    if (response && Array.isArray(response)) {
      tabs.value = response.map((invoice, index) => ({
        id: index + 1,
        idHD: invoice.id,
        ma: invoice.ma,
        soLuong: invoice.soLuong,
        loaiHoaDon: invoice.loaiHoaDon,
        products: invoice.data?.products || [],
      }))

      if (tabs.value.length > 0) {
        activeTab.value = tabs.value[0].id
        idHDS.value = tabs.value[0].idHD
        loaiHD.value = tabs.value[0].loaiHoaDon

        await clickkActiveTab(tabs.value[0].id, tabs.value[0].idHD, tabs.value[0].loaiHoaDon)
      } else {
        resetDiscount()
        currentDeliveryInfo.value = null
        isDeliveryEnabled.value = false
        Object.assign(deliveryInfo, {
          tenNguoiNhan: '',
          sdtNguoiNhan: '',
          diaChiCuThe: '',
          tinhThanhPho: undefined,
          quanHuyen: undefined,
          phuongXa: undefined,
        })
        provinceCode.value = null
        districtCode.value = null
        wardCode.value = null
        shippingFee.value = 0
        isFreeShipping.value = false
        state.gioHang = []
        state.detailKhachHang = null
        state.currentPaymentMethod = '0'
        tongTien.value = 0
        tongTienTruocGiam.value = 0
        giamGia.value = 0
        tienHang.value = 0
        tienKhachThanhToan.value = 0
        tienThieu.value = 0
      }
    }
  } catch (error) {
    console.error('Failed to fetch invoices:', error)
    toast.error('Lấy danh sách hóa đơn thất bại!')
    resetDiscount()
    currentDeliveryInfo.value = null
    isDeliveryEnabled.value = false
    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    isFreeShipping.value = false
  }
}

const resetDiscount = () => {
  state.discountList = []
  selectedDiscount.value = null
  selectedDiscountCode.value = ''
  giamGia.value = 0
  calculateTotalAmounts()
}

const clickkActiveTab = async (id: number, hd: string, loaiHoaDon: string) => {
  idHDS.value = hd
  activeTab.value = id
  loaiHD.value = loaiHoaDon
  isBestDiscountApplied.value = false

  try {
    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    isFreeShipping.value = false

    const response = await GetGioHang(hd)
    state.gioHang = response

    const responseKH = await GeOneKhachHang(hd)
    state.detailKhachHang = responseKH.id ? responseKH : null

    isDeliveryEnabled.value = loaiHoaDon === 'GIAO_HANG'

    if (isDeliveryEnabled.value && state.detailKhachHang) {
      deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
      deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
      deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''

      const tinhThanhPhoId = state.detailKhachHang.tinh
      const quanHuyenId = state.detailKhachHang.huyen
      const phuongXaId = state.detailKhachHang.xa

      if (!provinces.value.length) {
        await fetchProvinces()
      }

      if (tinhThanhPhoId) {
        const selectedProvince = provinces.value.find(p => p.code === tinhThanhPhoId.toString())
        if (selectedProvince) {
          deliveryInfo.tinhThanhPho = selectedProvince.value
          provinceCode.value = parseInt(tinhThanhPhoId)
          await fetchDistricts(provinceCode.value)
        }
      }

      if (quanHuyenId && provinceCode.value) {
        const selectedDistrict = districts.value.find(d => d.code === quanHuyenId.toString())
        if (selectedDistrict) {
          deliveryInfo.quanHuyen = selectedDistrict.value
          districtCode.value = parseInt(quanHuyenId)
          await fetchWards(districtCode.value)
        }
      }

      if (phuongXaId && districtCode.value) {
        const selectedWard = wards.value.find(w => w.code === phuongXaId)
        if (selectedWard) {
          deliveryInfo.phuongXa = selectedWard.value
          wardCode.value = phuongXaId
        }
      }

      if (deliveryInfoByInvoice[hd]) {
        Object.assign(deliveryInfo, {
          tenNguoiNhan: deliveryInfoByInvoice[hd].tenNguoiNhan,
          sdtNguoiNhan: deliveryInfoByInvoice[hd].sdtNguoiNhan,
          diaChiCuThe: deliveryInfoByInvoice[hd].diaChiCuThe,
          tinhThanhPho: deliveryInfoByInvoice[hd].tinhThanhPho,
          quanHuyen: deliveryInfoByInvoice[hd].quanHuyen,
          phuongXa: deliveryInfoByInvoice[hd].phuongXa,
        })
        provinceCode.value = deliveryInfoByInvoice[hd].provinceCode
        districtCode.value = deliveryInfoByInvoice[hd].districtCode
        wardCode.value = deliveryInfoByInvoice[hd].wardCode
        shippingFee.value = deliveryInfoByInvoice[hd].shippingFee
      }

      await calculateShippingFee()
    } else if (state.detailKhachHang) {
      deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
      deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
      deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''
    }

    deliveryInfoByInvoice[hd] = {
      tenNguoiNhan: deliveryInfo.tenNguoiNhan,
      sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
      diaChiCuThe: deliveryInfo.diaChiCuThe,
      tinhThanhPho: deliveryInfo.tinhThanhPho,
      quanHuyen: deliveryInfo.quanHuyen,
      phuongXa: deliveryInfo.phuongXa,
      provinceCode: provinceCode.value,
      districtCode: districtCode.value,
      wardCode: wardCode.value,
      shippingFee: shippingFee.value,
    }

    calculateTotalAmounts()
    await fetchDiscounts(hd)
    await updatePaymentStatus()
  } catch (error) {
    console.error('Failed to switch invoice:', error)
    toast.error('Chuyển hóa đơn thất bại!')
    resetDiscount()
    isDeliveryEnabled.value = false
    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()
  }
}

watch(isDeliveryEnabled, async (newValue) => {
  if (!newValue) {
    currentDeliveryInfo.value = null
    Object.assign(deliveryInfo, { tenNguoiNhan: '', sdtNguoiNhan: '', diaChiGiaoHang: '', tinhThanhPho: undefined, quanHuyen: undefined, phuongXa: undefined, diaChiCuThe: '' })
    shippingFee.value = 0
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    calculateTotalAmounts()
  } else {
    await fetchProvinces()
  }
}, { immediate: true })

const columnsKhachHang: DataTableColumns<KhachHangResponse> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => h(NText, { depth: 3 }, () => `${index + 1}`)
  },
  {
    title: 'Tên khách hàng',
    key: 'ten',
    width: 150,
    ellipsis: true
  },
  {
    title: 'Số điện thoại',
    key: 'sdt',
    width: 130,
    align: 'center'
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 90,
    align: 'center',
    render: (row) => h(
      NButton,
      {
        type: 'primary',
        size: 'small',
        secondary: true,
        onClick: () => selectKhachHang(row.id)
      },
      { default: () => 'Chọn' }
    )
  }
]

const columnsGiohang: DataTableColumns<any> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => h(NText, { depth: 3 }, () => `${index + 1}`)
  },
  {
    title: 'Ảnh',
    key: 'anh',
    width: 80,
    align: 'center',
    render: (row) => h(NImage, {
      width: 40,
      height: 40,
      src: row.urlImage || row.anh,
      objectFit: 'cover',
      style: { 'border-radius': '4px' }
    })
  },
  {
    title: 'Tên sản phẩm',
    key: 'name',
    width: 150,
    ellipsis: true,
    render: (row) => row.name || row.ten
  },
  {
    title: 'Số lượng',
    key: 'soLuong',
    width: 120,
    align: 'center',
    render: (row) => h(NSpace, { justify: 'center', align: 'center', size: 4 }, () => [
      h(NButton, {
        size: 'tiny',
        circle: true,
        type: 'default',
        onClick: () => decreaseQuantity(row.idHDCT, row.id)
      }, { default: () => '-' }),
      h(NText, () => row.soLuong),
      h(NButton, {
        size: 'tiny',
        circle: true,
        type: 'default',
        onClick: () => increaseQuantity(row.idHDCT, row.id)
      }, { default: () => '+' })
    ])
  },
  {
    title: 'Giá bán',
    key: 'price',
    width: 110,
    align: 'center',
    render: (row) => formatCurrency(row.price || row.giaBan)
  },
  {
    title: 'CPU',
    key: 'cpu',
    width: 100,
    align: 'center',
    ellipsis: true
  },
  {
    title: 'GPU',
    key: 'gpu',
    width: 100,
    align: 'center',
    ellipsis: true
  },
  {
    title: 'RAM',
    key: 'ram',
    width: 80,
    align: 'center'
  },
  {
    title: 'Ổ cứng',
    key: 'hardDrive',
    width: 100,
    align: 'center'
  },
  {
    title: 'Màu sắc',
    key: 'color',
    width: 80,
    align: 'center'
  },
  {
    title: 'Tổng tiền',
    key: 'total',
    width: 100,
    align: 'center',
    render: (row) => h(NText, { type: 'primary', strong: true }, () =>
      formatCurrency((row.price || row.giaBan) * row.soLuong)
    )
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 80,
    align: 'center',
    render: (row) => h(NTooltip, null, {
      trigger: () => h(NButton, {
        type: 'error',
        size: 'small',
        text: true,
        onClick: () => deleteProduc(row.id, row.idHDCT)
      }, {
        icon: () => h(NIcon, null, () => h(TrashOutline))
      }),
      default: () => 'Xóa sản phẩm'
    })
  }
]

const columns: DataTableColumns<ADProductDetailResponse> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => h(NText, { depth: 3 }, () => `${index + 1}`)
  },
  {
    title: 'Ảnh',
    key: 'urlImage',
    width: 80,
    align: 'center',
    render: (row) => h(NImage, {
      width: 40,
      height: 40,
      src: row.urlImage,
      objectFit: 'cover',
      style: { 'border-radius': '4px' }
    })
  },
  {
    title: 'Mã',
    key: 'code',
    width: 100,
    ellipsis: true
  },
  {
    title: 'Tên',
    key: 'name',
    width: 150,
    ellipsis: true
  },
  {
    title: 'Số lượng',
    key: 'quantity',
    width: 90,
    align: 'center'
  },
  {
    title: 'Giá bán',
    key: 'price',
    width: 110,
    align: 'center',
    render: (row) => formatCurrency(row.price)
  },
  {
    title: 'CPU',
    key: 'cpu',
    width: 120,
    ellipsis: true
  },
  {
    title: 'GPU',
    key: 'gpu',
    width: 120,
    ellipsis: true
  },
  {
    title: 'RAM',
    key: 'ram',
    width: 90,
    ellipsis: true
  },
  {
    title: 'Ổ cứng',
    key: 'hardDrive',
    width: 100,
    ellipsis: true
  },
  {
    title: 'Màu sắc',
    key: 'color',
    width: 90,
    ellipsis: true
  },
  {
    title: 'Chất liệu',
    key: 'material',
    width: 100,
    ellipsis: true
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: (row) => h(NTag, {
      type: row.status === 'ACTIVE' ? 'success' : 'error',
      size: 'small',
      round: true
    }, () => row.status === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động')
  },
  {
    title: 'Hành động',
    key: 'operation',
    width: 90,
    align: 'center',
    render: (row) => h(NTooltip, null, {
      trigger: () => h(NButton, {
        type: 'primary',
        size: 'small',
        secondary: true,
        onClick: () => selectProduct(row.id),
        disabled: row.status !== 'ACTIVE' || row.quantity <= 0
      }, { default: () => 'Chọn' }),
      default: () => row.status !== 'ACTIVE' ? 'Sản phẩm không hoạt động' :
               row.quantity <= 0 ? 'Hết hàng' : 'Chọn sản phẩm'
    })
  }
]

const increaseQuantity = async (idHDCT: any, idSPS: any) => {
  try {
    const formData = new FormData();
    formData.append('idSP', idSPS);
    formData.append('idHDCT', idHDCT);
    formData.append('idHD', idHDS.value);
    const res = await themSL(formData);

    if (res.message.includes("thay đổi giá từ")) {
      toast.warning(res.message);
      return;
    }

    if (res.message === 'Số lượng sản phẩm thêm vào nhiều hơn số lượng trong kho') {
      toast.error(res.message);
      return;
    }

    const response = await GetGioHang(idHDS.value);
    state.gioHang = response;

    calculateTotalAmounts();
    await fetchDiscounts(idHDS.value);
    capNhatDanhSach();
  } catch (error) {
    console.error('Failed to increase quantity:', error);
    toast.error('Tăng số lượng thất bại!');
    const updatedProduct = state.gioHang.find((item) => item.id === idSPS);
    if (updatedProduct) {
      updatedProduct.soLuong = Math.max(0, updatedProduct.soLuong - 1);
      calculateTotalAmounts();
    }
  }
};

const huy = async (idHD: string) => {
  try {
    const formData = new FormData()
    formData.append('idNV', idNV.userId)
    formData.append('idHD', idHD)

    const res = await huyHoaDon(formData)

    toast.success(res.message)

    await fetchProducts()
    await capNhatDanhSach()

    const indexToRemove = tabs.value.findIndex((tab) => tab.idHD === idHDS.value)
    if (indexToRemove !== -1) {
      tabs.value.splice(indexToRemove, 1)
    }

    idHDS.value = ''
    activeTab.value = 0
    state.gioHang = []
    state.detailKhachHang = null
    state.phuongThuThanhToan = []
    state.tongTien = null
    tongTien.value = 0
    tongTienTruocGiam.value = 0
    giamGia.value = 0
    tienHang.value = 0
    soTien.value = 0
    tienKhachThanhToan.value = 0
    tienThieu.value = 0
    state.currentPaymentMethod = '0'
    resetDiscount()
    isDeliveryEnabled.value = false
    currentDeliveryInfo.value = null
    Object.assign(deliveryInfo, { tenNguoiNhan: '', sdtNguoiNhan: '', diaChiGiaoHang: '', tinhThanhPho: undefined, quanHuyen: undefined, phuongXa: undefined, diaChiCuThe: '' });
    shippingFee.value = 0;
    provinceCode.value = null;
    districtCode.value = null;
    wardCode.value = null;

  } catch (error: any) {
    if (error?.response?.data?.message) {
      toast.error(error.response.data.message)
    } else {
      toast.error('Có lỗi xảy ra khi xác nhận thanh toán!')
      console.error('Lỗi khi xác nhận thanh toán:', error)
    }
  }
}

const xacNhan = async (check: number) => {
  if (!idHDS.value) {
    toast.error('Vui lòng chọn một hóa đơn để xác nhận thanh toán!')
    console.error('Lỗi: idHDS.value là null khi xác nhận thanh toán.')
    return
  }

  if (isDeliveryEnabled.value) {
    if (!deliveryInfo.tenNguoiNhan || !deliveryInfo.sdtNguoiNhan || !deliveryInfo.diaChiCuThe ||
      !deliveryInfo.tinhThanhPho || !deliveryInfo.quanHuyen || !deliveryInfo.phuongXa) {
      toast.error('Vui lòng nhập đầy đủ thông tin giao hàng!');
      return;
    }
    if (shippingFee.value < 0 && tienHang.value > 0) {
      toast.error('Vui làm kiểm tra lại tiền giao hàng');
      return;
    }
  }

  const selectedProvince = provinces.value.find((p) => p.code === deliveryInfo.tinhThanhPho);
  const selectedDistrict = districts.value.find((d) => d.code === deliveryInfo.quanHuyen);
  const selectedWard = wards.value.find((w) => w.code === deliveryInfo.phuongXa);

  try {
    const formData = new FormData()
    formData.append('idNV', idNV.userId)
    formData.append('tienHang', tienHang.value.toString())
    formData.append('idHD', idHDS.value)
    formData.append('tongTien', tongTien.value.toString())
    formData.append('ten', deliveryInfo.tenNguoiNhan);
    formData.append('sdt', deliveryInfo.sdtNguoiNhan);
    formData.append('diaChi', `${deliveryInfo.diaChiCuThe}, ${selectedWard?.label}, ${selectedDistrict?.label}, ${selectedProvince?.label}`);
    formData.append('tienShip', shippingFee.value.toString());
    formData.append('giamGia', giamGia.value.toString());
    formData.append('phuongThucThanhToan', state.currentPaymentMethod);
    formData.append('check', check.toString());
    if (selectedDiscount.value?.id) {
      formData.append('idPGG', selectedDiscount.value.id)
    }

    formData.append('isDeliveryEnabled', isDeliveryEnabled.value.toString())

    const res = await thanhToanThanhCong(formData)

    if (res.message != null) {
      if (res.message.startsWith("Số")) {
        toast.error(res.message);
        return;
      }

      if (res.message.startsWith("Phiếu")) {
        toast.error(res.message);
        await fetchDiscounts(idHDS.value)
        return;
      }

      if (res.message.startsWith("Đã")) {
        showDeliveryModal.value = true;
        await fetchDiscounts(idHDS.value)
        return;
      }
    }

    if (isDeliveryEnabled.value == true) {
      toast.success('Giao hàng thành công!')
    } else {
      toast.success('Thanh toán thành công!')
    }

    await fetchProducts()
    await capNhatDanhSach()

    const indexToRemove = tabs.value.findIndex((tab) => tab.idHD === idHDS.value)
    if (indexToRemove !== -1) {
      tabs.value.splice(indexToRemove, 1)
    }

    idHDS.value = ''
    activeTab.value = 0
    state.gioHang = []
    state.detailKhachHang = null
    state.phuongThuThanhToan = []
    state.tongTien = null
    tongTien.value = 0
    tongTienTruocGiam.value = 0
    giamGia.value = 0
    tienHang.value = 0
    soTien.value = 0
    tienKhachThanhToan.value = 0
    tienThieu.value = 0
    state.currentPaymentMethod = '0'
    resetDiscount()
    isDeliveryEnabled.value = false
    currentDeliveryInfo.value = null
    Object.assign(deliveryInfo, { tenNguoiNhan: '', sdtNguoiNhan: '', diaChiGiaoHang: '', tinhThanhPho: undefined, quanHuyen: undefined, phuongXa: undefined, diaChiCuThe: '' });
    shippingFee.value = 0;
    provinceCode.value = null;
    districtCode.value = null;
    wardCode.value = null;

  } catch (error: any) {
    if (error?.response?.data?.message) {
      toast.error(error.response.data.message)
    } else {
      toast.error('Có lỗi xảy ra khi xác nhận thanh toán!')
      console.error('Lỗi khi xác nhận thanh toán:', error)
    }
  }
}

const decreaseQuantity = async (idHDCT: any, idSPS: any) => {
  try {
    const formData = new FormData();
    formData.append('idSP', idSPS);
    formData.append('idHDCT', idHDCT);
    await xoaSL(formData);

    const response = await GetGioHang(idHDS.value);
    state.gioHang = response;

    calculateTotalAmounts();
    await fetchDiscounts(idHDS.value);
    capNhatDanhSach();

  } catch (error) {
    console.error('Failed to decrease quantity:', error);
    toast.error('Giảm số lượng thất bại!');
    const updatedProduct = state.gioHang.find((item) => item.id === idSPS);
    if (updatedProduct) {
      updatedProduct.soLuong = Math.min(updatedProduct.soLuong + 1, 1);
      calculateTotalAmounts();
    }
  }
};

watch(
  () => tienHang.value,
  async () => {
    if (idHDS.value) {
      await fetchDiscounts(idHDS.value)
    }
  }
)

watch(
  () => [deliveryInfo.tinhThanhPho, deliveryInfo.quanHuyen, deliveryInfo.phuongXa],
  async () => {
    if (isDeliveryEnabled.value) {
      await calculateShippingFee();
    }
  }
);

const openProductSelectionModal = async () => {
  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước khi chọn sản phẩm!')
    return
  }
  await fetchProducts()
  showProductModal.value = true
}

async function createInvoice() {
  if (tabs.value.length >= 10) {
    toast.warning('Chỉ được tạo tối đa 10 hóa đơn!', { autoClose: 3000 });
    return;
  }

  try {
    const formData = new FormData();
    formData.append('idNV', idNV.userId);
    const newInvoice = await getCreateHoaDon(formData);

    const newTabId = nextTabId++;
    tabs.value.push({
      id: newTabId,
      idHD: newInvoice.data.id,
      ma: newInvoice.data.ma,
      soLuong: 0,
      loaiHoaDon: newInvoice.loaiHoaDon || 'OFFLINE',
      products: [],
    });
    idHDS.value = newInvoice.data.id;
    activeTab.value = newTabId;
    loaiHD.value = newInvoice.loaiHoaDon || 'OFFLINE';

    state.gioHang = [];
    state.detailKhachHang = null;
    resetDiscount();
    isDeliveryEnabled.value = false;
    currentDeliveryInfo.value = null;
    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    });
    provinceCode.value = null;
    districtCode.value = null;
    wardCode.value = null;
    shippingFee.value = 0;
    isFreeShipping.value = false;
    state.currentPaymentMethod = '0';
    tienHang.value = 0;
    tongTien.value = 0;
    tongTienTruocGiam.value = 0;
    tienKhachThanhToan.value = 0;
    tienThieu.value = 0;

    await clickkActiveTab(newTabId, newInvoice.data.id, newInvoice.data.loaiHoaDon || 'OFFLINE');
    toast.success('Tạo hóa đơn thành công!');
  } catch (error) {
    console.error('Failed to create invoice:', error);
    toast.error('Tạo hóa đơn thất bại!');
  }
}

const closeModal = () => {
  state.isModalOpen = false
}

const closeModalP = () => {
  showDeliveryModal.value = false
  xacNhan(0)
}

const showKhachHangModal = ref(false)
const showProductModal = ref(false)
const selectedProduct = ref({ soLuong: 0 })

const setDefaultPaymentMethod = () => {
  state.currentPaymentMethod = '0'
}

const selectKhachHang = async (getIdKH: any) => {
  try {
    const formData = new FormData();
    formData.append('idHD', idHDS.value);
    formData.append('idKH', getIdKH);
    await themKhachHang(formData);
    const responseKH = await GeOneKhachHang(idHDS.value);
    state.detailKhachHang = responseKH;

    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    });
    provinceCode.value = null;
    districtCode.value = null;
    wardCode.value = null;
    shippingFee.value = 0;

    if (state.detailKhachHang) {
      deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || '';
      deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || '';
      deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || '';

      if (loaiHD.value === 'GIAO_HANG') {
        const tinhThanhPhoId = state.detailKhachHang.tinh;
        const quanHuyenId = state.detailKhachHang.huyen;
        const phuongXaId = state.detailKhachHang.xa;

        if (!provinces.value.length) {
          await fetchProvinces();
        }

        if (tinhThanhPhoId) {
          const selectedProvince = provinces.value.find(p => p.code === tinhThanhPhoId.toString());
          if (selectedProvince) {
            deliveryInfo.tinhThanhPho = selectedProvince.value;
            provinceCode.value = parseInt(tinhThanhPhoId);
            await fetchDistricts(provinceCode.value);
          }
        }

        if (quanHuyenId && provinceCode.value) {
          const selectedDistrict = districts.value.find(d => d.code === quanHuyenId.toString());
          if (selectedDistrict) {
            deliveryInfo.quanHuyen = selectedDistrict.value;
            districtCode.value = parseInt(quanHuyenId);
            await fetchWards(districtCode.value);
          }
        }

        if (phuongXaId && districtCode.value) {
          const selectedWard = wards.value.find(w => w.code === phuongXaId);
          if (selectedWard) {
            deliveryInfo.phuongXa = selectedWard.value;
            wardCode.value = phuongXaId;
          }
        }

        if (tinhThanhPhoId && quanHuyenId && phuongXaId) {
          isDeliveryEnabled.value = true;
          await calculateShippingFee();
        } else {
          isDeliveryEnabled.value = false;
          toast.info('Thông tin địa chỉ khách hàng không đầy đủ, vui lòng nhập thủ công.');
        }
      } else {
        isDeliveryEnabled.value = false;
      }

      deliveryInfoByInvoice[idHDS.value] = {
        tenNguoiNhan: deliveryInfo.tenNguoiNhan,
        sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
        diaChiCuThe: deliveryInfo.diaChiCuThe,
        tinhThanhPho: deliveryInfo.tinhThanhPho,
        quanHuyen: deliveryInfo.quanHuyen,
        phuongXa: deliveryInfo.phuongXa,
        provinceCode: provinceCode.value,
        districtCode: districtCode.value,
        wardCode: wardCode.value,
        shippingFee: shippingFee.value,
      };
    }

    await fetchDiscounts(idHDS.value);
    showKhachHangModal.value = false;
    toast.success('Chọn khách hàng thành công!');
  } catch (error) {
    console.error('Failed to select customer:', error);
    toast.error('Chọn khách hàng thất bại!');
  }
};

const deleteProduc = async (idSPS: any, idHDCT: string) => {
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    await xoaSP(formData)
    betterDiscountMessage.value = ''
    state.gioHang = state.gioHang.filter((item) => item.id !== idSPS)
    calculateTotalAmounts()

    await fetchDiscounts(idHDS.value)
    const response = await GetGioHang(idHDS.value)
    state.gioHang = response
    toast.success('Xóa sản phẩm thành công!')
    await capNhatDanhSach()
  } catch (error) {
    console.error('Failed to delete product:', error)
    toast.error('Xóa sản phẩm thất bại!')
  }
}

const fetchProducts = async () => {
  try {
    const params: ADProductDetailRequest = {
      page: stateSP.paginationParams.page,
      size: stateSP.paginationParams.size,
      q: localSearchQuery.value || undefined,
      idColor: localColor.value || undefined,
      idCPU: localCPU.value || undefined,
      idGPU: localGPU.value || undefined,
      idRAM: localRAM.value || undefined,
      idHardDrive: localHardDrive.value || undefined,
      idMaterial: localSelectedMaterial.value || undefined,
      minPrice: localMinPrice.value || 0,
      maxPrice: localMaxPrice.value || 0
    };
    const response = await getProductDetails(params);
    stateSP.products = response.data?.data || [];
    state.products = response.data?.data || [];
    stateSP.totalItems = response.data?.totalElements || 0;
    state.totalItems = response.data?.totalElements || 0;
  } catch (error) {
    console.error('Failed to fetch products:', error);
    toast.error('Lấy danh sách sản phẩm thất bại!');
  }
};

watch(customerSearchQuery, () => {
  state.paginationParams.page = 1;
  debouncedFetchCustomers();
});

const confirmQuantity = async () => {
  try {
    if (selectedProduct.value.soLuong <= 0) {
      toast.error('Số lượng không hợp lệ!')
      return
    }
    const formData = new FormData()
    formData.append('soLuong', selectedProduct.value.soLuong.toString())
    formData.append('idHD', idHDS.value)
    formData.append('idSP', idSP.value)

    const res = await themSanPham(formData)

    if (res.message === 'Số lượng sản phẩm thêm vào nhiều hơn số lượng trong kho') {
      toast.error(res.message)
      return
    }

    state.isModalOpen = false

    if (res.message.includes("thay đổi giá từ")) {
      toast.warning(res.message);
      setTimeout(() => {
        toast.success("Thêm thành công");
      }, 5000);
    } else {
      toast.success(res.message)
    }

    const response = await GetGioHang(idHDS.value)
    state.gioHang = response

    calculateTotalAmounts()

    await fetchDiscounts(idHDS.value)

    await fetchProducts()
    await capNhatDanhSach()
  } catch (error: any) {
    if (error?.response?.data?.message) {
      toast.error(error.response.data.message)
    } else {
      toast.error('Thêm sản phẩm vào giỏ hàng thất bại!')
    }
  }
}

const confirmQuantityP = async () => {
  showDeliveryModal.value = false
  applyBestDiscount();
  xacNhan(0)
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

const calculateTotalAmounts = () => {
  tienHang.value = state.gioHang.reduce((sum, item) => sum + (item.price || item.giaBan) * item.soLuong, 0)
  giamGia.value = selectedDiscount.value ? selectedDiscount.value.giaTriGiamThucTe || 0 : 0
  tongTienTruocGiam.value = tienHang.value
  let currentTotal = tienHang.value - giamGia.value;
  if (currentTotal < 0) currentTotal = 0;

  if (isDeliveryEnabled.value) {
    currentTotal += shippingFee.value;
  }
  tongTien.value = currentTotal;
  tienThieu.value = tongTien.value - tienKhachThanhToan.value;
}

watch(() => state.gioHang, calculateTotalAmounts, { deep: true })
watch(giamGia, calculateTotalAmounts);
watch(shippingFee, calculateTotalAmounts);

const capNhatDanhSach = async () => {
  const response = await GetHoaDons()
  if (response && Array.isArray(response)) {
    tabs.value = response.map((invoice, index) => ({
      id: index + 1,
      idHD: invoice.id,
      ma: invoice.ma,
      soLuong: invoice.soLuong,
      loaiHoaDon: invoice.loaiHoaDon,
      products: invoice.data?.products || []
    }))
  }
}

const isQrVNpayModalVisible = ref(false)

const openQrModalVNPayCaHai = () => {
  isBothPaymentModalVisible.value = true;
  amountPaid.value = 0;
};

const openQrModalVNPay = () => {
  isQrVNpayModalVisible.value = true;
};

const closeQrModalVnPay = () => {
  isQrVNpayModalVisible.value = false
}

const isQrModalVisible = ref(false)
const qrData = ref('')
const hasCamera = ref(true);

let html5QrCode: Html5Qrcode

const openQrModal = () => {
  isQrModalVisible.value = true
  nextTick(() => {
    startQrScanning()
  })
}

function selectProduct(idSPS: any) {
  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước khi thêm sản phẩm!');
    return;
  }
  idSP.value = idSPS
  selectedProduct.value.soLuong = 1
  state.isModalOpen = true
  showProductModal.value = false
}

const startQrScanning = () => {
  const qrRegionId = 'reader'
  const qrRegionElement = document.getElementById(qrRegionId)
  if (!qrRegionElement) {
    console.error("Không tìm thấy phần tử với id 'reader'")
    return
  }

  html5QrCode = new Html5Qrcode(qrRegionId)

  Html5Qrcode.getCameras()
    .then((cameras: { id: string; label: string }[]) => {
      if (cameras && cameras.length) {
        hasCamera.value = true;
        const cameraId = cameras[0].id
        html5QrCode.start(
          cameraId,
          { fps: 10, qrbox: 250 },
          (qrCodeMessage: string) => {
            qrData.value = qrCodeMessage
            selectProduct(qrCodeMessage)
            html5QrCode.stop()
            closeQrModal()
          },
          (errorMessage) => {
            console.warn('Lỗi đọc QR: ', errorMessage)
          }
        )
      } else {
        console.warn('Không tìm thấy camera nào!')
        hasCamera.value = false;
        toast.error('Không tìm thấy camera hoặc không có quyền truy cập camera!');
      }
    })
    .catch((error: any) => {
      console.error('Lỗi khi lấy camera: ', error)
      hasCamera.value = false;
      toast.error('Lỗi khi truy cập camera: ' + error.message);
    })
}

const closeQrModal = () => {
  isQrModalVisible.value = false
  stopQrScanning()
}

const stopQrScanning = () => {
  html5QrCode.stop().catch((err) => console.error('Không thể dừng scanner:', err))
}

const addCustomerLoading = ref(false);

const addCustomer = async () => {
  try {
    if (!newCustomer.ten || !newCustomer.sdt) {
      toast.error('Vui lòng nhập đầy đủ thông tin khách hàng!')
      return
    }

    const phoneRegex = /^[0-9]{10}$/;
    if (!phoneRegex.test(newCustomer.sdt)) {
      toast.error('Số điện thoại phải là 10 chữ số!');
      return;
    }

    if (!idHDS.value) {
      toast.error('Vui lòng chọn hoặc tạo hóa đơn trước khi thêm khách hàng!');
      return;
    }

    addCustomerLoading.value = true;

    const formData = new FormData();
    formData.append('ten', newCustomer.ten);
    formData.append('sdt', newCustomer.sdt);

    const response = await themMoiKhachHang(formData);
    const newCustomerId = response.data.id;

    const selectFormData = new FormData();
    selectFormData.append('idHD', idHDS.value);
    selectFormData.append('idKH', newCustomerId);
    await themKhachHang(selectFormData);

    const responseKH = await GeOneKhachHang(idHDS.value);
    state.detailKhachHang = responseKH.id ? responseKH : null;

    newCustomer.ten = '';
    newCustomer.sdt = '';

    toast.success('Thêm và chọn khách hàng thành công!');
  } catch (error) {
    console.error('Failed to add customer:', error);
    toast.error('Thêm khách hàng thất bại!');
  } finally {
    addCustomerLoading.value = false;
  }
}

onMounted(async () => {
  const storedDiscount = localStorage.getItem('selectedDiscount');
  const storedBestDiscount = localStorage.getItem('isBestDiscountApplied');
  if (storedDiscount) {
    selectedDiscount.value = JSON.parse(storedDiscount);
  }
  if (storedBestDiscount) {
    isBestDiscountApplied.value = JSON.parse(storedBestDiscount);
  }
  const storedDeliveryInfo = localStorage.getItem('deliveryInfoByInvoice');
  if (storedDeliveryInfo) {
    Object.assign(deliveryInfoByInvoice, JSON.parse(storedDeliveryInfo));
  }
  await fetchCustomers();
  await fetchHoaDon()
  if (idHDS.value) {
    await fetchDiscounts(idHDS.value);
  }
  setDefaultPaymentMethod()
  await fetchProvinces()
})
</script>

<style scoped>
.main-layout {
  display: flex;
  gap: 16px;
  padding: 16px;
  height: calc(100vh - 64px);
  background: #f5f5f5;
}

.left-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.right-column {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.top-header {
  margin-bottom: 8px;
}

.pending-invoices-container {
  width: 100%;
}

.pending-invoices-wrapper {
  width: 100%;
}

.pending-invoice-card {
  min-width: 200px;
  padding: 12px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pending-invoice-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.pending-invoice-card.active {
  border-color: #1890ff;
  background: #f0f9ff;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.delete-invoice-btn {
  margin-left: 4px;
}

.discount-list {
  max-height: 400px;
  overflow-y: auto;
}

.mt-1 {
  margin-top: 4px;
}

@media (max-width: 1200px) {
  .main-layout {
    flex-direction: column;
    height: auto;
  }
  
  .right-column {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .main-layout {
    padding: 12px;
    gap: 12px;
  }
  
  .pending-invoice-card {
    min-width: 180px;
  }
}
</style>