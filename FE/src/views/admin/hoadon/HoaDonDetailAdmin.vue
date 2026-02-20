<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <!-- Header - Ẩn khi in -->
    <div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 mb-6 no-print">
      <div>
        <h1 class="text-2xl lg:text-3xl font-bold text-gray-900">Hóa đơn #{{ invoiceCode }}</h1>
        <div class="flex flex-wrap items-center gap-2 mt-2">
          <NTag :type="getStatusTagType(currentStatus)" size="medium" round>
            <template #icon>
              <n-icon :component="getStatusIcon(currentStatus)" />
            </template>
            {{ getStatusText(currentStatus) }}
          </NTag>

          <NTag type="info" size="small" round>
            {{ getInvoiceTypeText(hoaDonData?.loaiHoaDon) }}
          </NTag>

          <NTag type="default" size="small">
            {{ formatDateTime(hoaDonData?.ngayTao) }}
          </NTag>
        </div>
      </div>

      <div class="flex flex-wrap gap-2">
        <NButton type="primary" @click="handlePrint" :loading="printLoading">
          <template #icon>
            <n-icon>
              <PrintOutline />
            </n-icon>
          </template>
          In hóa đơn
        </NButton>

        <NButton type="default" @click="handleBack">
          <template #icon>
            <n-icon>
              <ArrowBackOutline />
            </n-icon>
          </template>
          Quay lại
        </NButton>
      </div>
    </div>

    <!-- Nội dung hóa đơn - Ẩn đi, chỉ dùng để tạo PDF -->
    <div id="invoice-content" class="hidden">
      <!-- Toàn bộ nội dung hóa đơn được copy từ phần in ấn -->
      <div class="invoice-paper bg-white text-black p-8 md:p-12">
        <!-- Header hóa đơn -->
        <div class="flex justify-between items-start mb-10">
          <div>
            <div class="flex items-center gap-2 mb-2">
              <img src="@/assets/svg-icons/logo.svg" class="w-8 h-8" alt="logo">
              <span class="text-2xl font-bold tracking-widest text-gray-900">My Laptop</span>
            </div>
            <div class="text-xs text-gray-500 space-y-1">
              <p>123 Đại Cồ Việt, Hai Bà Trưng, Hà Nội</p>
              <p>Website: mylaptop.vn | Hotline: 1900.8888</p>
            </div>
          </div>
          <div class="text-right">
            <h2 class="text-xl font-bold uppercase text-gray-800">
              Hóa Đơn
            </h2>
            <p class="text-sm font-bold text-gray-600 mt-1">
              Mã: {{ hoaDonData?.maHoaDon || invoiceCode }}
            </p>
            <p class="text-sm text-gray-500">
              Ngày: {{ formatDateTime(hoaDonData?.ngayTao) }}
            </p>
          </div>
        </div>

        <NDivider class="border-dashed my-6" />

        <!-- Thông tin khách hàng và đơn hàng -->
        <div class="grid grid-cols-2 gap-8 mb-8 text-sm">
          <div>
            <h3 class="font-bold text-gray-400 uppercase text-[15px] mb-3 tracking-wider">
              Khách hàng
            </h3>
            <p class="font-bold text-gray-900 text-base mb-1">
              {{ hoaDonData?.tenKhachHang2 || hoaDonData?.tenKhachHang || 'Khách lẻ' }}
            </p>
            <p class="text-gray-600 mb-1">
              {{ hoaDonData?.sdtKH2 || hoaDonData?.sdtKH || 'Chưa cập nhật' }}
            </p>
            <p class="text-gray-600 leading-relaxed">
              {{ formatAddressCustomer(hoaDonData?.diaChi) || formatAddressCustomer(hoaDonData?.diaChi2) || 'Không có địa chỉ' }}
            </p>
          </div>

          <div class="text-right">
            <h3 class="font-bold text-gray-400 uppercase text-[15px] mb-3 tracking-wider">
              Thông tin đơn hàng
            </h3>
            <div class="space-y-1.5 text-gray-600">
              <p>Nhân viên: <span class="text-gray-900 font-medium">{{ hoaDonData?.tenNhanVien || 'Không xác định' }}</span></p>
              <p>Vận chuyển: <span class="text-gray-900 font-medium">{{ getShippingMethodText(hoaDonData?.phuongThucVanChuyen) }}</span></p>
              <p>Thanh toán: <span class="text-gray-900 font-medium">{{ getPaymentMethodText(hoaDonData?.phuongThucThanhToan) }}</span></p>
            </div>
          </div>
        </div>

        <!-- Bảng sản phẩm -->
        <table class="w-full text-sm mb-6">
          <thead>
            <tr class="bg-gray-50 border-y border-gray-200 text-gray-500 text-xs uppercase">
              <th class="py-3 text-left pl-2 font-semibold">
                Sản phẩm
              </th>
              <th class="py-3 text-center font-semibold w-16">
                SL
              </th>
              <th class="py-3 text-right font-semibold w-28">
                Đơn giá
              </th>
              <th class="py-3 text-right font-semibold w-32 pr-2">
                Thành tiền
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="item in printProducts" :key="item.id + (item.imeiCode || '')">
              <td class="py-4 pl-2 align-top">
                <p class="font-bold text-gray-800 text-[15px]">
                  {{ item.tenSanPham }}
                </p>
                <p class="text-[11px] text-gray-500 mt-1 italic">
                  <span v-if="item.thuongHieu">{{ item.thuongHieu }} | </span>
                  <span v-if="item.mauSac">{{ item.mauSac }} | </span>
                  <span v-if="item.imeiCode">Serial: {{ item.imeiCode }}</span>
                  <span v-else-if="!item.imeiCode && item.danhSachImei && parseIMEIList(item.danhSachImei).length === 0">Không có Serial</span>
                </p>
              </td>
              <td class="py-4 text-center align-top text-gray-600 font-medium">
                {{ item.soLuongImei || item.soLuong }}
              </td>
              <td class="py-4 text-right align-top text-gray-600">
                {{ formatCurrency(item.giaBanImei || item.giaBan) }}
              </td>
              <td class="py-4 text-right align-top font-bold text-gray-900 pr-2">
                {{ formatCurrency(item.tongTienImei || item.tongTien) }}
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Tổng hợp thanh toán -->
        <div class="flex justify-end">
          <div class="w-2/3 md:w-1/2 space-y-2 text-right">
            <div class="flex justify-between text-sm text-gray-600">
              <span>Tổng tiền hàng:</span>
              <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
            </div>

            <div v-if="hoaDonData?.phiVanChuyen && hoaDonData.phiVanChuyen > 0" class="flex justify-between text-sm text-gray-600">
              <span>Phí vận chuyển:</span>
              <span>+ {{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>

            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData.giaTriVoucher > 0" class="py-2 my-1 border-y border-dashed border-gray-100">
              <div class="flex justify-between text-sm text-green-600 mb-1">
                <span>Ưu đãi ({{ hoaDonData?.tenVoucher || 'Voucher' }}):</span>
                <span>- {{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
              </div>
              <div v-if="hoaDonData?.maVoucher" class="flex justify-between text-xs text-gray-400 italic">
                <span>Mã áp dụng:</span>
                <span>{{ hoaDonData.maVoucher }}</span>
              </div>
            </div>

            <div class="flex justify-between text-sm text-gray-600">
              <span>Thuế (VAT 0%):</span>
              <span>{{ formatCurrency(0) }}</span>
            </div>

            <NDivider class="my-3 bg-gray-800" />

            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800 uppercase text-sm">Tổng thanh toán:</span>
              <span class="text-2xl font-extrabold text-indigo-700">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>

            <div v-if="hoaDonData?.duNo && hoaDonData.duNo > 0" class="flex justify-between text-sm text-orange-600 mt-2">
              <span>Còn nợ:</span>
              <span class="font-bold">{{ formatCurrency(hoaDonData.duNo) }}</span>
            </div>
          </div>
        </div>

        <!-- Ghi chú -->
        <div v-if="hoaDonData?.ghiChu" class="mt-6 p-4 bg-gray-50 rounded-lg">
          <p class="text-sm text-gray-600"><span class="font-bold">Ghi chú:</span> {{ hoaDonData.ghiChu }}</p>
        </div>

        <!-- Footer -->
        <div class="mt-16 pt-8 text-center border-t border-gray-100">
          <p class="font-bold text-gray-800">
            Cảm ơn quý khách đã tin tưởng My Laptop Store!
          </p>
          <p class="text-[10px] text-gray-400 mt-1">
            Sản phẩm được bảo hành chính hãng. Vui lòng giữ lại hóa đơn này để bảo hành.
          </p>
          <p class="text-[10px] text-gray-400 mt-1">
            Hóa đơn được tạo lúc {{ formatTime(hoaDonData?.ngayTao) }}
          </p>
        </div>
      </div>
    </div>

    <!-- Progress Timeline -->
    <NCard class="shadow-sm border-0 rounded-xl no-print" content-class="p-6">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900">Tiến trình đơn hàng</h3>
          <span class="text-sm text-gray-500">Cập nhật: {{ formatDateTime(hoaDonData?.thoiGianCapNhatCuoi ||
            hoaDonData?.ngayTao) }}</span>
        </div>

        <NButton type="error" block ghost @click="openCancelModal" :disabled="isCancelled"
          :style="{ maxWidth: '150px', marginTop: '10px' }">
          <template #icon>
            <n-icon>
              <CloseCircleOutline />
            </n-icon>
          </template>
          Hủy đơn hàng
        </NButton>
      </template>

      <div class="relative">
        <!-- Progress bar (ẩn khi loaiHoaDon = 0) -->
        <template v-if="hoaDonData?.loaiHoaDon !== '0'">
          <div class="absolute top-5 left-0 right-0 h-1.5 bg-gray-200 rounded-full z-0"></div>
          <div
            class="absolute top-5 left-0 h-1.5 bg-blue-500 rounded-full z-10"
            :style="{ width: progressWidth }">
          </div>
        </template>

        <!-- Steps -->
        <div class="relative flex justify-between z-20">
          <div
            v-for="(step, index) in filteredSteps"
            :key="step.key"
            class="flex flex-col items-center flex-1"
            :class="{ 'cursor-pointer': isStepSelectable(step.key) }"
            @click="handleStepClick(step.key)"
          >
            <!-- Step circle -->
            <div
              class="w-10 h-10 rounded-full border-4 bg-white flex items-center justify-center mb-3 relative transition-all duration-300 hover:scale-110"
              :class="getStepCircleClass(step.key)"
            >
              <n-icon size="18" :class="getStepIconClass(step.key)">
                <component :is="step.icon" />
              </n-icon>

              <!-- Completed check -->
              <div
                v-if="isStepCompleted(step.key)"
                class="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center border-2 border-white shadow"
              >
                <n-icon size="14" color="white">
                  <CheckmarkCircleOutline />
                </n-icon>
              </div>
            </div>

            <!-- Step label -->
            <div class="text-center">
              <p class="text-sm font-semibold mb-1" :class="getStepTextClass(step.key)">
                {{ step.title }}
              </p>
              <p class="text-xs text-gray-500 min-h-[20px]">
                {{ getStepTime(step.key) || 'Đang chờ' }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Current status info -->
      <div class="mt-8 pt-6 border-t border-gray-100">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center">
              <n-icon size="24" color="#3b82f6">
                <component :is="getCurrentStepIcon()" />
              </n-icon>
            </div>
            <div>
              <p class="text-sm text-gray-500">Trạng thái hiện tại</p>
              <p class="font-semibold" :class="getCurrentStatusTextClass()">
                {{ getStatusText(currentStatus) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-green-50 flex items-center justify-center">
              <n-icon size="24" color="#10b981">
                <CashOutline />
              </n-icon>
            </div>
            <div>
              <p class="text-sm text-gray-500">Tổng tiền</p>
              <p class="font-semibold text-red-600">
                {{ formatCurrency(hoaDonData?.tongTienSauGiam) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-purple-50 flex items-center justify-center">
              <n-icon size="24" color="#8b5cf6">
                <CubeOutline />
              </n-icon>
            </div>
            <div>
              <p class="text-sm text-gray-500">Số lượng sản phẩm</p>
              <p class="font-semibold text-gray-900">
                {{ totalQuantity }} sản phẩm
              </p>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <!-- Main Information Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 no-print">
      <!-- Customer Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <n-icon size="20" color="#4b5563">
                <PersonOutline />
              </n-icon>
              <h3 class="text-lg font-semibold text-gray-900">Thông tin khách hàng</h3>
            </div>
            <n-button type="primary" tertiary size="small" @click="openCustomerEditModal" class="!text-sm">
              <template #icon>
                <n-icon>
                  <CreateOutline />
                </n-icon>
              </template>
              Chỉnh sửa
            </n-button>
          </div>
        </template>

        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-14 h-14 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <n-icon size="24" color="#3b82f6">
                <PersonCircleOutline />
              </n-icon>
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-gray-900 truncate">
                {{ hoaDonData?.tenKhachHang2 || hoaDonData?.tenKhachHang || 'Khách lẻ' }}
              </h4>
              <div class="flex flex-wrap gap-2 mt-2">
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <n-icon size="14">
                    <CallOutline />
                  </n-icon>
                  {{ hoaDonData?.sdtKH2 || hoaDonData?.sdtKH || 'Chưa cập nhật' }}
                </span>
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <n-icon size="14">
                    <MailOutline />
                  </n-icon>
                  {{ hoaDonData?.email2 || hoaDonData?.email || 'Chưa có email' }}
                </span>
              </div>
            </div>
          </div>

          <div class="space-y-3 pt-4 border-t border-gray-100">
            <div>
              <p class="text-sm text-gray-600 mb-1">Địa chỉ giao hàng:</p>
              <p class="text-sm font-medium text-gray-900 bg-gray-50 p-3 rounded">
                {{ formatAddressCustomer(hoaDonData?.diaChi) || formatAddressCustomer(hoaDonData?.diaChi2) || 'Không có địa chỉ' }}
              </p>
            </div>
          </div>
        </div>
      </NCard>

      <!-- Modal chỉnh sửa thông tin khách hàng -->
      <n-modal v-model:show="showCustomerModal" preset="card" title="Chỉnh sửa thông tin khách hàng" :bordered="false"
        class="!w-full !max-w-2xl" :segmented="{
          content: 'soft',
          footer: 'soft'
        }">

        <div class="space-y-4">
          <!-- Form thông tin khách hàng -->
          <n-form ref="customerFormRef" :model="customerForm" :rules="customerFormRules" label-placement="left"
            label-width="140" require-mark-placement="right-hanging" size="medium">
            <n-grid :cols="2" :x-gap="24">
              <n-gi>
                <n-form-item label="Họ và tên" path="tenKhachHang">
                  <n-input v-model:value="customerForm.tenKhachHang" placeholder="Nhập họ và tên" clearable />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="Số điện thoại" path="sdtKH">
                  <n-input v-model:value="customerForm.sdtKH" placeholder="Nhập số điện thoại" clearable />
                </n-form-item>
              </n-gi>
              <n-gi :span="2">
                <n-form-item label="Email" path="email">
                  <n-input v-model:value="customerForm.email" placeholder="Nhập email" clearable />
                </n-form-item>
              </n-gi>
              <n-gi :span="2">
                <n-form-item label="Địa chỉ" path="diaChi">
                  <n-input v-model:value="customerForm.diaChi" type="textarea" placeholder="Nhập địa chỉ đầy đủ"
                    :autosize="{
                      minRows: 2,
                      maxRows: 4
                    }" />
                </n-form-item>
              </n-gi>
            </n-grid>
          </n-form>
        </div>

        <template #footer>
          <div class="flex justify-end gap-3">
            <n-button @click="showCustomerModal = false">
              Hủy
            </n-button>
            <n-button type="primary" @click="saveCustomerInfo" :loading="isSavingCustomer">
              Lưu thông tin
            </n-button>
          </div>
        </template>
      </n-modal>

      <!-- Order Summary -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563">
              <ReceiptOutline />
            </n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Tóm tắt đơn hàng</h3>
          </div>
        </template>

        <div class="space-y-3">
          <div class="grid grid-cols-2 gap-3">
            <div class="bg-gray-50 p-3 rounded-lg">
              <p class="text-xs text-gray-500">Tổng sản phẩm</p>
              <div v-if="productCount > 0">
                <p class="text-lg font-bold text-gray-900">{{ productCount }} sản phẩm</p>
                <p class="text-xs text-gray-500 mt-1">{{ imeiProductsCount }} IMEI</p>
              </div>
              <div v-else>
                <p class="text-lg font-bold text-gray-900 text-red-500">Không có sản phẩm</p>
              </div>
            </div>
            <div class="bg-gray-50 p-3 rounded-lg">
              <p class="text-xs text-gray-500">Tổng số lượng</p>
              <p class="text-lg font-bold text-gray-900">{{ totalQuantity }}</p>
            </div>
          </div>

          <div class="space-y-2 pt-4">
            <div class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Tổng tiền hàng:</span>
              <span class="font-semibold">{{ formatCurrency(totalAmount) }}</span>
            </div>

            <div v-if="hoaDonData?.phiVanChuyen && hoaDonData.phiVanChuyen > 0"
              class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-semibold">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>

            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData.giaTriVoucher > 0"
              class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Giảm giá voucher:</span>
              <span class="font-semibold text-green-600">-{{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
            </div>

            <div class="flex justify-between items-center pt-4">
              <span class="text-lg font-bold text-gray-900">TỔNG CỘNG:</span>
              <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>

          <!-- Voucher Information -->
          <div v-if="hoaDonData?.maVoucher" class="mt-4 p-3 bg-green-50 rounded-lg border border-green-100">
            <div class="flex items-center gap-2 mb-1">
              <n-icon size="16" color="#10b981">
                <PricetagOutline />
              </n-icon>
              <span class="text-sm font-medium text-green-800">Voucher áp dụng</span>
            </div>
            <p class="text-green-700 font-medium">{{ hoaDonData.maVoucher }} - {{ hoaDonData.tenVoucher }}</p>
            <p class="text-xs text-green-600 mt-1">Giảm: {{ formatCurrency(hoaDonData.giaTriVoucher) }}</p>
          </div>
        </div>
      </NCard>

      <!-- Payment Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563">
              <CardOutline />
            </n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Thông tin thanh toán</h3>
          </div>
        </template>

        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
                <n-icon size="20" color="#3b82f6">
                  <CashOutline />
                </n-icon>
              </div>
              <div>
                <p class="font-medium">{{ getPaymentMethodText(hoaDonData?.phuongThucThanhToan) }}</p>
                <p class="text-xs text-gray-500">Phương thức</p>
              </div>
            </div>
            <NTag :type="getPaymentStatusTagType()" size="small">
              {{ getPaymentStatusText() }}
            </NTag>
          </div>

          <div class="space-y-3 pt-4 border-t border-gray-100">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Tổng tiền thanh toán:</span>
              <span class="font-bold text-green-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>

            <div v-if="hoaDonData?.duNo && hoaDonData.duNo > 0" class="flex justify-between items-center">
              <span class="text-gray-600">Còn nợ:</span>
              <span class="font-bold text-orange-600">{{ formatCurrency(hoaDonData.duNo) }}</span>
            </div>

            <div v-if="hoaDonData?.hoanPhi && hoaDonData.hoanPhi > 0" class="flex justify-between items-center">
              <span class="text-gray-600">Hoàn phí:</span>
              <span class="font-bold text-blue-600">{{ formatCurrency(hoaDonData.hoanPhi) }}</span>
            </div>

            <div class="pt-4">
              <p class="text-sm text-gray-600 mb-2">Thời gian thanh toán:</p>
              <div class="flex items-center gap-2 text-sm">
                <n-icon size="16" color="#6b7280">
                  <TimeOutline />
                </n-icon>
                <span>{{ formatDateTime(hoaDonData?.ngayTao) }}</span>
              </div>
            </div>
          </div>
        </div>
      </NCard>
    </div>

    <!-- Products Table -->
    <NCard class="shadow-sm border-0 rounded-xl overflow-hidden no-print">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563">
              <CubeOutline />
            </n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Danh sách sản phẩm</h3>
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm text-gray-500">{{ productCount }} sản phẩm</span>
            <span class="text-sm text-gray-500">{{ totalQuantity }} số lượng</span>
            <NTag v-if="imeiProductsCount > 0" type="info" size="small">
              {{ imeiProductsCount }} Serial
            </NTag>
          </div>
        </div>
      </template>

      <div class="overflow-x-auto">
        <n-data-table 
          :columns="productColumns" 
          :data="displayProducts" 
          :pagination="false" 
          striped
          class="min-w-full" 
          :row-class-name="rowClassName" 
        />
      </div>

      <!-- Summary -->
      <div class="border-t border-gray-200">
        <div class="p-6">
          <div class="max-w-md ml-auto space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Tạm tính:</span>
              <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
            </div>

            <div v-if="hoaDonData?.phiVanChuyen && hoaDonData.phiVanChuyen > 0"
              class="flex justify-between items-center">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-medium">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>

            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData.giaTriVoucher > 0"
              class="flex justify-between items-center">
              <span class="text-gray-600">Giảm giá voucher:</span>
              <span class="font-medium text-green-600">-{{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
            </div>

            <div class="flex justify-between items-center pt-3 border-t border-gray-200">
              <span class="text-lg font-bold text-gray-900">Tổng cộng:</span>
              <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <!-- Notes & Actions -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 no-print">
      <!-- System Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563">
              <InformationCircleOutline />
            </n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Thông tin hệ thống</h3>
          </div>
        </template>

        <div class="space-y-3 text-sm">
          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Mã hóa đơn:</span>
            <span class="font-medium font-mono">{{ hoaDonData?.maHoaDon }}</span>
          </div>

          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Loại hóa đơn:</span>
            <NTag size="small" :type="getInvoiceTypeTagType(hoaDonData?.loaiHoaDon)">
              {{ getInvoiceTypeText(hoaDonData?.loaiHoaDon) }}
            </NTag>
          </div>

          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Ngày tạo:</span>
            <span class="font-medium">{{ formatDateTime(hoaDonData?.ngayTao) }}</span>
          </div>

          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Trạng thái:</span>
            <NTag :type="getStatusTagType(currentStatus)" size="small">
              {{ getStatusText(currentStatus) }}
            </NTag>
          </div>

          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Phí vận chuyển:</span>
            <span class="font-medium">{{ formatCurrency(hoaDonData?.phiVanChuyen || 0) }}</span>
          </div>

          <div v-if="hoaDonData?.thoiGianCapNhatCuoi" class="flex justify-between items-center py-1">
            <span class="text-gray-600">Cập nhật lần cuối:</span>
            <span class="font-medium">{{ formatDateTimeFromString(hoaDonData.thoiGianCapNhatCuoi) }}</span>
          </div>
        </div>
      </NCard>
    </div>

    <!-- Status Update Modal -->
    <NModal v-model:show="showStatusModal" preset="dialog" title="Cập nhật trạng thái hóa đơn" class="no-print">
      <template #header>
        <div class="flex items-center gap-2">
          <n-icon size="20" color="#3b82f6">
            <RefreshOutline />
          </n-icon>
          <span class="font-semibold">Cập nhật trạng thái</span>
        </div>
      </template>

      <div class="space-y-4 py-4">
        <div class="p-3 bg-blue-50 rounded-lg">
          <p class="text-sm text-gray-600">Mã hóa đơn: <span class="font-bold">{{ hoaDonData?.maHoaDon }}</span></p>
          <div class="flex items-center gap-2 mt-2">
            <NTag :type="getStatusTagType(currentStatus)" size="small">
              {{ getStatusText(currentStatus) }}
            </NTag>
            <n-icon size="16" class="text-gray-400">
              <ArrowForwardOutline />
            </n-icon>
            <NTag :type="getStatusTagType(selectedStatus?.toString())" size="small">
              {{ getStatusText(selectedStatus?.toString()) }}
            </NTag>
          </div>
        </div>

        <NFormItem label="Chọn trạng thái mới:" required>
          <NSelect v-model:value="selectedStatus" :options="availableStatusOptions" placeholder="Chọn trạng thái"
            clearable :disabled="isCompleted || isCancelled" />
        </NFormItem>

        <NFormItem label="Ghi chú (tùy chọn):">
          <NInput v-model:value="statusNote" type="textarea" placeholder="Nhập ghi chú cho trạng thái mới..."
            :rows="3" />
        </NFormItem>

        <div v-if="selectedStatus != null" class="p-3 bg-gray-50 rounded">
          <p class="text-sm font-medium text-gray-700 mb-1">Xác nhận thay đổi:</p>
          <p class="text-sm text-gray-600">
            Chuyển từ <span class="font-bold text-yellow-600">{{ getStatusText(currentStatus) }}</span>
            sang <span class="font-bold text-green-600">{{ getStatusText(selectedStatus?.toString()) }}</span>
          </p>
        </div>
      </div>

      <template #action>
        <div class="flex gap-2 justify-end">
          <NButton @click="showStatusModal = false">Hủy</NButton>
          <NButton type="primary" @click="confirmStatusUpdate" :disabled="!selectedStatus" :loading="isUpdating">
            Xác nhận cập nhật
          </NButton>
        </div>
      </template>
    </NModal>

    <!-- Modal chọn serial -->
    <NModal v-model:show="showSerialModal" preset="dialog" title="Chọn Serial Sản Phẩm"
      style="width: 90%; max-width: 1200px" :mask-closable="false" class="no-print">
      <NCard size="small" :bordered="false">
        <template #header>
          <NSpace align="center" justify="space-between" class="serial-modal-header">
            <div>
              <NText strong>
                {{ selectedProductItem?.tenSanPham }} - {{ selectedProductItem?.maSanPham }}
              </NText>
              <NText depth="3" size="small" style="display: block; margin-top: 4px">
                Giá: {{ formatCurrency(selectedProductItem?.giaBan || 0) }}
              </NText>
            </div>
            <div style="text-align: right">
              <NText type="primary" strong>
                Cần chọn: {{ requiredQuantity }} serial
              </NText>
              <NText depth="3" size="small" style="display: block; margin-top: 4px">
                Đã chọn: {{ selectedSerialIds.length }}/{{ requiredQuantity }} serial
              </NText>
            </div>
          </NSpace>
        </template>

        <!-- Thêm loading state -->
        <div v-if="loadingSerials" style="text-align: center; padding: 40px">
          <n-spin size="large">
            <template #description>
              Đang tải danh sách serial...
            </template>
          </n-spin>
        </div>

        <!-- Data table khi có dữ liệu -->
        <NDataTable v-else :columns="serialColumns" :data="selectedSerials" :max-height="400" size="small"
          :pagination="{ pageSize: 10 }" :loading="loadingSerials" :bordered="false" />

        <template #footer>
          <NSpace justify="space-between" align="center" style="width: 100%">
            <div class="flex flex-col">
              <NText depth="3">
                Đã chọn {{ selectedSerialIds.length }} serial
              </NText>
              <NText v-if="selectedProductItem" :type="selectedSerialIds.length === requiredQuantity ? 'success' : 'warning'" size="small">
                Cần chọn đúng {{ requiredQuantity }} serial
              </NText>
            </div>
            <NSpace>
              <NButton @click="showSerialModal = false">
                Hủy
              </NButton>
              <NButton 
                type="primary" 
                :disabled="selectedSerialIds.length !== requiredQuantity || loadingSerials"
                :loading="isAddingSerials" 
                @click="addSerialsToInvoice"
              >
                Thêm {{ selectedSerialIds.length }} serial
                <span v-if="selectedProductItem">
                  ({{ selectedSerialIds.length }}/{{ requiredQuantity }})
                </span>
              </NButton>
            </NSpace>
          </NSpace>
        </template>
      </NCard>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, h, watch, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NIcon,
  NDataTable,
  NCard,
  NTag,
  NAvatar,
  NFormItem,
  NSelect,
  NInput,
  NModal,
  useMessage,
  type DataTableColumns,
  type SelectOption,
  NSpace,
  NTooltip,
  NCheckbox,
  NText,
  NSpin,
  NForm,
  NGrid,
  NGi,
  NDivider,
  type FormInst,
  type FormRules,
  useDialog
} from 'naive-ui'
import { 
  getHoaDonChiTiets, 
  changeOrderStatus, 
  type ADChangeStatusRequest, 
  parseIMEIList,
} from '@/service/api/admin/hoadon.api'
import {
  getImeiProductDetail,
  themSanPham,
} from '@/service/api/admin/banhang.api'
import type { HoaDonChiTietItem } from '@/service/api/admin/hoadon.api'
import { ADPDImeiResponse } from '@/service/api/admin/product/productDetail.api'

// Icons
import {
  TimeOutline,
  CheckmarkCircleOutline,
  CartOutline,
  CheckmarkDoneOutline,
  CloseCircleOutline,
  CashOutline,
  CardOutline,
  PersonOutline,
  PersonCircleOutline,
  CallOutline,
  MailOutline,
  ReceiptOutline,
  CubeOutline,
  PricetagOutline,
  PrintOutline,
  ArrowBackOutline,
  CreateOutline,
  RefreshOutline,
  InformationCircleOutline,
  ArrowForwardOutline,
  CloseOutline
} from '@vicons/ionicons5'
import { localStorageAction } from '@/utils/storage'
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)

// Các interface
interface CustomerForm {
  tenKhachHang: string
  sdtKH: string
  email: string
  diaChi: string
}

interface HoaDonData {
  tenKhachHang?: string
  sdtKH?: string
  email?: string
  diaChi?: string
  tenKhachHang2?: string
  sdtKH2?: string
  email2?: string
  diaChi2?: string
  tenNhanVien?: string
  phuongThucVanChuyen?: string
}

// Component setup
const props = defineProps<{
  hoaDonData?: HoaDonData
}>()

const emit = defineEmits<{
  'update:customer': [customerData: CustomerForm]
}>()

const selectedProductItem = ref<HoaDonChiTietItem | null>(null)
const selectedSerials = ref<ADPDImeiResponse[]>([])

// Refs
const showCustomerModal = ref(false)
const isSavingCustomer = ref(false)
const customerFormRef = ref<FormInst | null>(null)

// Form data
const customerForm = reactive<CustomerForm>({
  tenKhachHang: '',
  sdtKH: '',
  email: '',
  diaChi: '',
})

// Form validation rules
const customerFormRules: FormRules = {
  tenKhachHang: [
    {
      required: true,
      message: 'Vui lòng nhập họ và tên',
      trigger: ['blur', 'input']
    }
  ],
  sdtKH: [
    {
      required: true,
      message: 'Vui lòng nhập số điện thoại',
      trigger: ['blur', 'input']
    },
    {
      pattern: /(84|0[3|5|7|8|9])+([0-9]{8})\b/,
      message: 'Số điện thoại không hợp lệ',
      trigger: ['blur', 'input']
    }
  ],
  email: [
    {
      type: 'email',
      message: 'Email không hợp lệ',
      trigger: ['blur', 'input']
    }
  ]
}

// Computed values
const invoiceCode = computed(() => route.params.id as string || 'N/A')
const requiredQuantity = computed(() => selectedProductItem.value?.soLuong || 0)

// Tính số lượng serial khả dụng
const availableSerialsCount = computed(() => {
  return selectedSerials.value.filter(s => s.imeiStatus === 'AVAILABLE').length
})

const serialColumns: DataTableColumns<ADPDImeiResponse> = [
  {
    title: 'Chọn',
    key: 'select',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: selectedSerialIds.value.includes(row.id),
      disabled: row.imeiStatus !== 'AVAILABLE',
      onUpdateChecked: (checked) => {
        if (checked) {
          selectedSerialIds.value = [...selectedSerialIds.value, row.id]
        }
        else {
          selectedSerialIds.value = selectedSerialIds.value.filter(id => id !== row.id)
        }
      },
    }),
  },
  {
    title: 'Serial/IMEI',
    key: 'serialNumber',
    width: 180,
    render: row => h(NText, {
      strong: true,
      code: true,
      style: {
        fontFamily: 'monospace',
        fontSize: '12px',
      },
    }, () => row.code || '-'),
  },
  {
    title: 'Tên',
    key: 'name',
    width: 120,
    render: row => h(NText, () => row.name || '-'),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: (row) => {
      const statusConfig: Record<string, { type: any, text: string }> = {
        AVAILABLE: { type: 'success', text: 'Khả dụng' },
        SOLD: { type: 'warning', text: 'Đã bán' },
        DEFECTIVE: { type: 'error', text: 'Lỗi' },
        RESERVED: { type: 'info', text: 'Đã đặt' },
      }
      const config = statusConfig[row.imeiStatus] || {
        type: 'default',
        text: row.imeiStatus || 'Không xác định',
      }
      return h(NTag, {
        type: config.type,
        size: 'small',
        round: true,
      }, () => config.text)
    },
  },
  {
    title: 'Trạng thái SP',
    key: 'productStatus',
    width: 100,
    align: 'center',
    render: (row) => {
      const config = {
        ACTIVE: { type: 'success', text: 'Hoạt động' },
        INACTIVE: { type: 'default', text: 'Không HĐ' },
      }[row.status] || { type: 'default', text: row.status || '-' }
      return h(NTag, {
        type: config.type,
        size: 'small',
        round: true,
      }, () => config.text)
    },
  },
]

// Methods cho customer
const openCustomerEditModal = () => {
  // Copy data from hoaDonData to form
  if (props.hoaDonData) {
    Object.keys(customerForm).forEach(key => {
      const formKey = key as keyof CustomerForm
      const dataKey = key as keyof HoaDonData
      customerForm[formKey] = props.hoaDonData?.[dataKey] || ''
    })
  }
  showCustomerModal.value = true
}

const saveCustomerInfo = () => {
  customerFormRef.value?.validate((errors) => {
    if (!errors) {
      isSavingCustomer.value = true

      // Simulate API call
      setTimeout(() => {
        emit('update:customer', { ...customerForm })
        isSavingCustomer.value = false
        showCustomerModal.value = false

        // Show success message
        message.success('Cập nhật thông tin khách hàng thành công')
      }, 500)
    }
  })
}

const formatAddressCustomer = (address: string | undefined): string => {
  if (!address) return ''
  return address
}

// Watch for hoaDonData changes
watch(() => props.hoaDonData, (newData) => {
  if (newData) {
    Object.keys(customerForm).forEach(key => {
      const formKey = key as keyof CustomerForm
      const dataKey = key as keyof HoaDonData
      customerForm[formKey] = newData[dataKey] || ''
    })
  }
}, { immediate: true })

// State chính
const invoiceItems = ref<HoaDonChiTietItem[]>([])
const printLoading = ref(false)
const isLoading = ref(false)
const isUpdating = ref(false)
const showStatusModal = ref(false)
const selectedStatus = ref<number | null>(null)
const statusNote = ref('')
const showSerialModal = ref(false)
const selectedSerialIds = ref<string[]>([])
const loadingSerials = ref(false)
const isAddingSerials = ref(false)

// Lấy dữ liệu tổng hợp từ danh sách sản phẩm
const hoaDonData = computed(() => {
  if (!invoiceItems.value || invoiceItems.value.length === 0) return null
  // Lấy thông tin từ item đầu tiên
  return invoiceItems.value[0]
})

// Xác định loại hóa đơn
const isOnlineInvoice = computed(() => {
  return hoaDonData.value?.loaiHoaDon === '1' // '1' là online
})

const isCounterOrDelivery = computed(() => {
  return hoaDonData.value?.loaiHoaDon === '0' || hoaDonData.value?.loaiHoaDon === '2' // '0' tại quầy, '2' giao hàng
})

const currentStatus = computed(() => {
  return parseInt(hoaDonData.value?.trangThaiHoaDon || '0')
})

const productCount = computed(() => {
  return (invoiceItems.value || []).filter(item =>
    item?.tenSanPham && item?.soLuong != null
  ).length
})

const totalQuantity = computed(() => {
  if (!invoiceItems.value || !Array.isArray(invoiceItems.value)) {
    return 0
  }

  return invoiceItems.value.reduce((sum, item) => {
    const isProduct = item?.tenSanPham
    if (isProduct && item.soLuong != null) {
      return sum + item.soLuong
    }
    return sum
  }, 0)
})

const totalAmount = computed(() => {
  return invoiceItems.value?.reduce((sum, item) => sum + (item.tongTien || 0), 0) || 0
})

// Tính tổng số IMEI
const imeiProductsCount = computed(() => {
  if (!invoiceItems.value || !Array.isArray(invoiceItems.value)) {
    return 0
  }

  let totalImei = 0
  invoiceItems.value.forEach((item) => {
    const imeiList = parseIMEIList(item.danhSachImei)
    totalImei += imeiList.length
  })
  return totalImei
})

// Transform dữ liệu cho hóa đơn tại quầy/giao hàng: mỗi IMEI thành 1 hàng
const imeiProducts = computed(() => {
  if (!invoiceItems.value || !Array.isArray(invoiceItems.value)) {
    return []
  }

  // Chỉ xử lý cho hóa đơn tại quầy/giao hàng
  if (isOnlineInvoice.value) {
    return []
  }

  const result: any[] = []
  let stt = 1

  invoiceItems.value.forEach((invoiceItem) => {
    // Parse danh sách IMEI
    const imeiList = parseIMEIList(invoiceItem.danhSachImei)

    // Nếu có IMEI, tạo hàng cho mỗi IMEI
    if (imeiList.length > 0) {
      imeiList.forEach((imei) => {
        result.push({
          // Thông tin từ hóa đơn chi tiết
          ...invoiceItem,
          // Thông tin IMEI
          imeiId: imei.id,
          imeiCode: imei.code || imei.imeiCode,
          imeiStatus: imei.status,
          imeiStatusText: imei.statusText,
          // STT
          stt: stt++,
          // Số lượng cho mỗi IMEI là 1
          soLuongImei: 1,
          // Giá cho mỗi IMEI = tổng giá / số lượng IMEI
          giaBanImei: invoiceItem.giaBan ? invoiceItem.giaBan / imeiList.length : 0,
          // Thành tiền cho mỗi IMEI
          tongTienImei: invoiceItem.tongTien ? invoiceItem.tongTien / imeiList.length : 0,
          // Đánh dấu đây là hàng IMEI
          isImeiRow: true
        })
      })
    } else {
      // Nếu không có IMEI, giữ nguyên hàng
      result.push({
        ...invoiceItem,
        stt: stt++,
        imeiCode: null,
        imeiStatus: null,
        imeiStatusText: 'Không có IMEI',
        isImeiRow: false,
        giaBanImei: invoiceItem.giaBan,
        tongTienImei: invoiceItem.tongTien
      })
    }
  })

  return result
})

// Dữ liệu hiển thị cho bảng sản phẩm - Áp dụng logic thay thế sản phẩm bằng serial
const displayProducts = computed(() => {
  if (isCounterOrDelivery.value) {
    // Đối với hóa đơn tại quầy/giao hàng, hiển thị từng IMEI
    return imeiProducts.value
  } else {
    // Đối với hóa đơn online, hiển thị theo logic thay thế
    const result: any[] = []
    let stt = 1

    invoiceItems.value.forEach((item) => {
      // Parse danh sách IMEI
      const imeiList = parseIMEIList(item.danhSachImei)
      
      if (imeiList.length > 0) {
        // Nếu có serial, thay thế sản phẩm bằng danh sách serial
        imeiList.forEach((imei) => {
          result.push({
            ...item,
            stt: stt++,
            imeiId: imei.id,
            imeiCode: imei.code || imei.imeiCode,
            imeiStatus: imei.status,
            imeiStatusText: imei.statusText,
            soLuong: 1, // Mỗi serial là 1 sản phẩm
            tongTien: item.giaBan, // Tổng tiền = đơn giá (vì số lượng = 1)
            isImeiRow: true,
            originalProductId: item.id
          })
        })
      } else {
        // Nếu không có serial, hiển thị sản phẩm bình thường
        result.push({
          ...item,
          stt: stt++,
          imeiCode: null,
          imeiStatus: null,
          imeiStatusText: null,
          isImeiRow: false
        })
      }
    })
    
    return result
  }
})

// Dữ liệu cho in ấn
const printProducts = computed(() => {
  const result: any[] = []
  
  invoiceItems.value.forEach((item) => {
    const imeiList = parseIMEIList(item.danhSachImei)
    
    if (imeiList.length > 0) {
      // Nếu có serial, hiển thị từng serial
      imeiList.forEach((imei) => {
        result.push({
          ...item,
          imeiCode: imei.code || imei.imeiCode,
          soLuongImei: 1,
          giaBanImei: item.giaBan,
          tongTienImei: item.giaBan
        })
      })
    } else {
      // Nếu không có serial, hiển thị sản phẩm gộp
      result.push({
        ...item,
        imeiCode: null,
        soLuongImei: item.soLuong,
        giaBanImei: item.giaBan,
        tongTienImei: item.tongTien
      })
    }
  })
  
  return result
})

// Progress
const progressWidth = computed(() => {
  const currentStep = currentStatus.value
  return `${(currentStep / 4) * 100}%` // 4 là số bước tối đa (0-4)
})

const isCompleted = computed(() => currentStatus.value === 4)
const isCancelled = computed(() => currentStatus.value === 5)

const availableStatusOptions = computed<SelectOption[]>(() => {
  const current = currentStatus.value

  // Map trạng thái
  const statusMap: Record<number, { label: string; disabled: boolean }> = {
    0: { label: 'Chờ xác nhận', disabled: false },
    1: { label: 'Đã xác nhận', disabled: false },
    2: { label: 'Chờ giao hàng', disabled: false },
    3: { label: 'Đang giao hàng', disabled: false },
    4: { label: 'Hoàn thành', disabled: true },
    5: { label: 'Đã hủy', disabled: true }
  }

  // Nếu đã hoàn thành, chỉ hiển thị tất cả trạng thái đã đi qua
  if (isCompleted.value) {
    const options: SelectOption[] = []
    // Tạo array từ 0 đến 4 (hoàn thành)
    for (let i = 0; i <= 4; i++) {
      options.push({
        value: i,
        label: statusMap[i].label,
        disabled: true
      })
    }
    return options
  }

  // Nếu đã hủy, chỉ hiển thị trạng thái cuối cùng là hủy
  if (isCancelled.value) {
    return [{
      value: 5,
      label: statusMap[5].label,
      disabled: true
    }]
  }

  // Đơn đang xử lý: hiển thị các trạng thái từ hiện tại trở đi
  const options: SelectOption[] = []
  for (let i = current; i <= 5; i++) {
    const isCurrentStatus = i === current
    options.push({
      value: i,
      label: statusMap[i].label,
      disabled: isCurrentStatus // Có thể disable trạng thái hiện tại
    })
  }

  return options
})

const filteredSteps = computed(() => {
  if (hoaDonData.value?.loaiHoaDon === '0') {
    // chỉ giữ step hoàn thành (đổi key nếu của bạn khác)
    return timelineSteps.filter(step => step.key === '4')
  }
  return timelineSteps
})

// Timeline steps
const timelineSteps = [
  { key: '0', title: 'Chờ xác nhận', icon: TimeOutline, color: 'yellow' },
  { key: '1', title: 'Đã xác nhận', icon: CheckmarkCircleOutline, color: 'blue' },
  { key: '2', title: 'Chờ giao hàng', icon: CartOutline, color: 'purple' },
  { key: '3', title: 'Đang giao hàng', icon: CheckmarkCircleOutline, color: 'info' },
  { key: '4', title: 'Hoàn thành', icon: CheckmarkDoneOutline, color: 'green' },
  { key: '5', title: 'Đã hủy', icon: CloseCircleOutline, color: 'red' }
]

// Helper functions
const formatCurrency = (value: number | undefined | null): string => {
  if (value === undefined || value === null) return "0 ₫"
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value)
}

const formatDateTime = (timestamp: number | undefined): string => {
  if (!timestamp) return "N/A"
  try {
    const date = new Date(timestamp)
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    })
  } catch {
    return "N/A"
  }
}

const formatTime = (timestamp: number | undefined): string => {
  if (!timestamp) return "N/A"
  try {
    const date = new Date(timestamp)
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit"
    })
  } catch {
    return "N/A"
  }
}

const formatDateTimeFromString = (dateString: string | undefined): string => {
  if (!dateString) return "N/A"
  try {
    const date = new Date(dateString)
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    })
  } catch {
    return dateString
  }
}

const getStatusText = (status: string | number | undefined): string => {
  const statusMap: Record<string, string> = {
    '0': 'Chờ xác nhận',
    '1': 'Đã xác nhận',
    '2': 'Chờ giao hàng',
    '3': 'Đang giao hàng',
    '4': 'Hoàn thành',
    '5': 'Đã hủy'
  }

  const statusStr = typeof status === 'number' ? status.toString() : status
  return statusMap[statusStr || '0'] || 'Không xác định'
}

const getStatusIcon = (status: string | number | undefined): any => {
  const statusMap: Record<string, any> = {
    '0': TimeOutline,
    '1': CheckmarkCircleOutline,
    '2': CartOutline,
    '3': CheckmarkCircleOutline,
    '4': CheckmarkDoneOutline,
    '5': CloseCircleOutline
  }

  const statusStr = typeof status === 'number' ? status.toString() : status
  return statusMap[statusStr || '0'] || TimeOutline
}

const getStatusTagType = (status: string | number | undefined): string => {
  const typeMap: Record<string, string> = {
    '0': 'warning',
    '1': 'info',
    '2': 'default',
    '3': 'info',
    '4': 'success',
    '5': 'error'
  }

  const statusStr = typeof status === 'number' ? status.toString() : status
  return typeMap[statusStr || '0'] || 'default'
}

const getInvoiceTypeText = (type: string | undefined): string => {
  const typeMap: Record<string, string> = {
    '0': 'Tại quầy',
    '1': 'Online',
    '2': 'Giao hàng'
  }
  return typeMap[type || '0'] || 'Không xác định'
}

const getInvoiceTypeTagType = (type: string | undefined): string => {
  const typeMap: Record<string, string> = {
    '0': 'success',
    '1': 'primary',
    '2': 'info'
  }
  return typeMap[type || '0'] || 'default'
}

const getPaymentMethodText = (method: string | undefined): string => {
  if (!method) return 'Tiền mặt'
  const methodMap: Record<string, string> = {
    'CASH': 'Tiền mặt',
    'BANKING': 'Chuyển khoản',
    'CREDIT_CARD': 'Thẻ tín dụng',
    'MOMO': 'Ví MoMo',
    'ZALOPAY': 'Ví ZaloPay'
  }
  return methodMap[method] || method
}

const getShippingMethodText = (method: string | undefined): string => {

  if (hoaDonData.value?.loaiHoaDon === '0'){
    return 'Nhận tại quầy'
  }

  if (!method) return 'Giao hàng tiêu chuẩn'
  const methodMap: Record<string, string> = {
    'STANDARD': 'Giao hàng tiêu chuẩn',
    'EXPRESS': 'Giao hàng nhanh',
    'ECONOMY': 'Giao hàng tiết kiệm'
  }
  return methodMap[method] || method
}

const getPaymentStatusText = (): string => {
  if (hoaDonData.value?.duNo && hoaDonData.value.duNo > 0) {
    return 'Còn nợ'
  }
  return 'Đã thanh toán'
}

const getPaymentStatusTagType = (): string => {
  if (hoaDonData.value?.duNo && hoaDonData.value.duNo > 0) {
    return 'warning'
  }
  return 'success'
}

const getCurrentStatusTextClass = (): string => {
  const classMap: Record<string, string> = {
    '0': 'text-yellow-600',
    '1': 'text-blue-600',
    '2': 'text-purple-600',
    '3': 'text-blue-600',
    '4': 'text-green-600',
    '5': 'text-red-600'
  }

  const statusStr = currentStatus.value.toString()
  return classMap[statusStr] || 'text-gray-600'
}

const getCurrentStepIcon = (): any => {
  return getStatusIcon(currentStatus.value)
}

// Timeline functions
const isStepCompleted = (stepKey: string): boolean => {
  const stepIndex = parseInt(stepKey)
  return stepIndex < currentStatus.value && stepKey !== '5'
}

const isStepCurrent = (stepKey: string): boolean => {
  return currentStatus.value.toString() === stepKey
}

const getStepCircleClass = (stepKey: string): string => {
  if (isStepCurrent(stepKey)) {
    return 'border-blue-500 border-4'
  } else if (isStepCompleted(stepKey)) {
    return 'border-green-500 border-2'
  } else {
    return 'border-gray-300 border-2'
  }
}

const getStepIconClass = (stepKey: string): string => {
  if (isStepCurrent(stepKey)) {
    return 'text-blue-500'
  } else if (isStepCompleted(stepKey)) {
    return 'text-green-500'
  } else {
    return 'text-gray-400'
  }
}

const getStepTextClass = (stepKey: string): string => {
  if (isStepCurrent(stepKey)) {
    return 'text-blue-600'
  } else if (isStepCompleted(stepKey)) {
    return 'text-green-600'
  } else {
    return 'text-gray-500'
  }
}

const getStepTime = (stepKey: string): string | null => {
  if (!hoaDonData.value?.lichSuTrangThai) {
    return null
  }

  try {
    const lichSuArray = JSON.parse(hoaDonData.value.lichSuTrangThai)
    const targetItem = lichSuArray.find(
      (item: any) => item.trangThai?.toString() === stepKey
    )

    if (targetItem?.thoiGian) {
      const [datePart, timePart] = targetItem.thoiGian.split(' ')
      if (!datePart || !timePart) return targetItem.thoiGian

      const [year, month, day] = datePart.split('-')
      const [hours, minutes] = timePart.split(':')
      return `${day}/${month}/${year} ${hours}:${minutes}`
    }

    return null
  } catch (error) {
    console.error('Error parsing lichSuTrangThai:', error)
    return null
  }
}

const isStepSelectable = (stepKey: string): boolean => {
  if (isCancelled.value || isCompleted.value) return false
  const stepIndex = parseInt(stepKey)
  // Chỉ cho phép chọn các step tiếp theo
  return stepIndex === currentStatus.value + 1
}

// Table columns - cập nhật theo loại hóa đơn
const productColumns = computed<DataTableColumns>(() => {
  const baseColumns: DataTableColumns = [
    {
      title: "STT",
      key: "stt",
      width: 60,
      align: "center",
      render: (row) => h('span', { class: 'font-medium' }, row.stt)
    },
    {
      title: "Sản phẩm",
      key: "productInfo",
      width: 250,
      render: (row) => {
        if (!row.tenSanPham) {
          return h('div', { class: 'hidden' })
        }

        const indentClass = row.isImeiRow ? 'ml-6' : ''

        return h('div', { class: 'flex items-center gap-3' }, [
          h(NAvatar, {
            src: row.anhSanPham,
            size: row.isImeiRow ? 'small' : 'medium',
            round: false,
            class: `border ${indentClass}`,
            fallbackSrc: 'https://via.placeholder.com/40?text=No+Image'
          }),
          h('div', { class: 'flex-1 min-w-0' }, [
            h('div', {
              class: `font-semibold text-gray-900 truncate ${row.isImeiRow ? 'text-sm' : ''}`
            }, row.tenSanPham),
            h('div', { class: 'flex flex-wrap gap-1 mt-1' }, [
              row.thuongHieu && h(NTag, {
                size: 'tiny',
                type: 'info',
                bordered: false
              }, { default: () => row.thuongHieu }),
              row.mauSac && h(NTag, {
                size: 'tiny',
                type: 'default',
                bordered: false
              }, { default: () => row.mauSac }),
            ]),
            h('div', { class: 'text-xs text-gray-500 mt-1 truncate' }, row.size || '')
          ])
        ])
      }
    },
    {
      title: "Serial",
      key: "imeiCode",
      width: 150,
      align: "center",
      render: (row) => {
        if (!row.imeiCode) {
          return h('div', { class: 'text-center text-gray-400 italic text-sm' }, 'Không có Serial')
        }

        return h('div', { class: 'space-y-1' }, [
          h('div', {
            class: 'font-mono font-bold text-center text-gray-800 text-sm p-1 bg-gray-50 rounded'
          }, row.imeiCode),
        
        ])
      }
    },
    {
      title: "Đơn giá",
      key: "price",
      width: 120,
      align: "right",
      render: (row) => {
        const price = row.giaBan
        return h('div', { class: 'font-semibold' }, formatCurrency(price))
      }
    },

  ]

  // Thêm cột Thao tác cho hóa đơn online
  if (isOnlineInvoice.value) {
    baseColumns.push({
      title: 'Thao tác',
      key: 'operation',
      width: 120,
      align: 'center',
      render: (row) => {
        // Kiểm tra xem sản phẩm đã có IMEI chưa
        const imeiList = parseIMEIList(row.danhSachImei)
        const hasImei = imeiList.length > 0
        const isFullySerialized = hasImei && imeiList.length >= row.soLuong
        
        // Chỉ hiển thị nút cho hàng sản phẩm gốc, không phải hàng serial
        if (row.isImeiRow) {
          return h('span', { class: 'text-gray-400 text-sm' }, 'Serial')
        }
        
        return h(NSpace, { size: 8 }, () => [
          h(NTooltip, null, {
            trigger: () => h(NButton, {
              type: isFullySerialized ? 'success' : hasImei ? 'warning' : 'primary',
              size: 'small',
              secondary: true,
              onClick: () => selectProductForSerial(row),
              disabled: isFullySerialized
            }, { 
              default: () => {
                if (isFullySerialized) return 'Đã có Serial'
                if (hasImei) return `Đã có ${imeiList.length}/${row.soLuong} Serial`
                return 'Chọn Serial'
              }
            }),
            default: () => {
              if (row.soLuong <= 0) return 'Hết hàng'
              if (isFullySerialized) return 'Sản phẩm đã có đủ serial'
              return `Chọn serial cho sản phẩm (cần ${row.soLuong} serial)`
            },
          }),
        ])
      }
    })
  }

  return baseColumns
})

const rowClassName = (row: any) => {
  if (row.isImeiRow) {
    return 'imei-row bg-blue-50 hover:bg-blue-100'
  }
  return 'product-row hover:bg-gray-50'
}

// Hàm chọn sản phẩm để xem serial (chỉ cho hóa đơn online)
async function selectProductForSerial(productItem: HoaDonChiTietItem) {
  if (!isOnlineInvoice.value) return
  
  // Kiểm tra nếu sản phẩm đã có serial đầy đủ
  const existingImeis = parseIMEIList(productItem.danhSachImei)
  if (existingImeis.length >= productItem.soLuong) {
    dialog.info({
      title: 'Thông báo',
      content: `Sản phẩm này đã có đủ ${existingImeis.length} serial.`,
      positiveText: 'Đóng',
      closable: true
    })
    return
  }
  
  selectedProductItem.value = productItem
  loadingSerials.value = true
  selectedSerials.value = []
  selectedSerialIds.value = []

  try {
    // Gọi API để lấy danh sách serial khả dụng cho sản phẩm
    if (productItem.productDetailId) {
      const response = await getImeiProductDetail(productItem.productDetailId)
      if (response.success && response.data) {
        // Lọc chỉ lấy serial khả dụng và chưa được sử dụng trong hóa đơn này
        const availableSerials = response.data.filter(s => 
          s.imeiStatus === 'AVAILABLE' || 
          (s.imeiStatus === 'RESERVED' && existingImeis.some(imei => imei.id === s.id))
        )
        
        selectedSerials.value = availableSerials
        
        // Tự động chọn các serial đã có trong hóa đơn
        existingImeis.forEach(imei => {
          if (availableSerials.some(s => s.id === imei.id)) {
            selectedSerialIds.value.push(imei.id)
          }
        })
        
        // Kiểm tra số lượng serial khả dụng
        const remainingNeeded = productItem.soLuong - existingImeis.length
        if (availableSerials.length - existingImeis.length < remainingNeeded) {
          message.warning(`Chỉ có ${availableSerials.length - existingImeis.length} serial khả dụng, còn thiếu ${remainingNeeded - (availableSerials.length - existingImeis.length)} serial`)
        }
        
        showSerialModal.value = true
      } else {
        message.error(response.message || 'Không thể tải danh sách serial')
      }
    } else {
      message.error('Không tìm thấy thông tin sản phẩm chi tiết')
    }
  } catch (error) {
    console.error('Error fetching serials:', error)
    message.error('Đã xảy ra lỗi khi tải danh sách serial')
  } finally {
    loadingSerials.value = false
  }
}

// Hàm gọi API thêm IMEI vào hóa đơn chi tiết
async function addSerialsToInvoice() {
  if (!selectedProductItem.value || !hoaDonData.value || selectedSerialIds.value.length === 0) {
    message.error('Vui lòng chọn ít nhất một serial')
    return
  }

  // Kiểm tra số lượng serial đã chọn phải bằng số lượng sản phẩm
  const requiredQuantity = selectedProductItem.value.soLuong || 0
  if (selectedSerialIds.value.length !== requiredQuantity) {
    message.error(`Sản phẩm cần ${requiredQuantity} serial. Vui lòng chọn đủ số lượng!`)
    return
  }

  // Kiểm tra xem đã có serial nào trong hóa đơn chưa
  const existingImeis = parseIMEIList(selectedProductItem.value.danhSachImei)
  const newSerialIds = selectedSerialIds.value.filter(id => 
    !existingImeis.some(imei => imei.id === id)
  )

  if (newSerialIds.length === 0 && existingImeis.length === requiredQuantity) {
    message.info('Sản phẩm đã có đủ serial')
    showSerialModal.value = false
    return
  }

  isAddingSerials.value = true
  try {
    const requestData = {
      invoiceId: selectedProductItem.value.invoiceId,
      productDetailId: selectedProductItem.value.productDetailId,
      imeiIds: selectedSerialIds.value, // Gửi tất cả serial đã chọn
      hoaDonChiTietId: selectedProductItem.value.id
    }

    const response = await themSanPham(requestData)
    
    if (response.success) {
      message.success(`Đã thêm ${selectedSerialIds.value.length} serial vào sản phẩm`)
      showSerialModal.value = false
      
      // Refresh dữ liệu hóa đơn
      await fetchInvoiceDetails()
    } else {
      message.error(response.message || 'Thêm serial thất bại')
    }
  } catch (error: any) {
    console.error('Error adding serials:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi thêm serial')
  } finally {
    isAddingSerials.value = false
  }
}

// Actions
const handleStepClick = (stepKey: string): void => {
  if (!isStepSelectable(stepKey)) {
    return
  }
  const stepIndex = parseInt(stepKey)
  selectedStatus.value = stepIndex
  showStatusModal.value = true
}

// Hàm xử lý in hóa đơn - Mở cửa sổ mới hiển thị nội dung hóa đơn
function handlePrint() {
  if (!hoaDonData.value) return
  
  printLoading.value = true
  
  // Lấy nội dung hóa đơn từ element ẩn
  const invoiceContent = document.getElementById('invoice-content')
  if (!invoiceContent) {
    message.error('Không tìm thấy nội dung hóa đơn')
    printLoading.value = false
    return
  }

  // Tạo cửa sổ mới
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    message.error('Trình duyệt đã chặn cửa sổ popup. Vui lòng cho phép popup và thử lại.')
    printLoading.value = false
    return
  }

  // Lấy nội dung HTML từ invoiceContent
  const contentHTML = invoiceContent.innerHTML

  // Tạo CSS cho in ấn
  const printStyles = `
    <style>
      * {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
      }
      body {
        font-family: 'Roboto', 'Helvetica', 'Arial', sans-serif;
        background: white;
        color: black;
        padding: 20px;
        margin: 0;
        font-size: 12px;
        line-height: 1.4;
      }
      .invoice-paper {
        max-width: 800px;
        margin: 0 auto;
        background: white;
        color: black;
        padding: 30px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.1);
        border-radius: 8px;
      }
      .text-gray-900 { color: #111827; }
      .text-gray-800 { color: #1f2937; }
      .text-gray-600 { color: #4b5563; }
      .text-gray-500 { color: #6b7280; }
      .text-gray-400 { color: #9ca3af; }
      .text-indigo-700 { color: #4338ca; }
      .text-orange-600 { color: #ea580c; }
      .text-green-600 { color: #16a34a; }
      .bg-gray-50 { background-color: #f9fafb; }
      .bg-gray-100 { background-color: #f3f4f6; }
      .border-gray-100 { border-color: #f3f4f6; }
      .border-gray-200 { border-color: #e5e7eb; }
      .border-gray-800 { border-color: #1f2937; }
      .border-dashed { border-style: dashed; }
      .rounded-lg { border-radius: 0.5rem; }
      .font-bold { font-weight: 700; }
      .font-semibold { font-weight: 600; }
      .font-medium { font-weight: 500; }
      .uppercase { text-transform: uppercase; }
      .tracking-wider { letter-spacing: 0.05em; }
      .tracking-widest { letter-spacing: 0.1em; }
      .text-xs { font-size: 0.75rem; }
      .text-sm { font-size: 0.875rem; }
      .text-base { font-size: 1rem; }
      .text-lg { font-size: 1.125rem; }
      .text-xl { font-size: 1.25rem; }
      .text-2xl { font-size: 1.5rem; }
      .italic { font-style: italic; }
      .text-left { text-align: left; }
      .text-center { text-align: center; }
      .text-right { text-align: right; }
      .mb-1 { margin-bottom: 0.25rem; }
      .mb-2 { margin-bottom: 0.5rem; }
      .mb-3 { margin-bottom: 0.75rem; }
      .mb-4 { margin-bottom: 1rem; }
      .mb-6 { margin-bottom: 1.5rem; }
      .mb-8 { margin-bottom: 2rem; }
      .mb-10 { margin-bottom: 2.5rem; }
      .mt-1 { margin-top: 0.25rem; }
      .mt-2 { margin-top: 0.5rem; }
      .mt-4 { margin-top: 1rem; }
      .mt-6 { margin-top: 1.5rem; }
      .mt-16 { margin-top: 4rem; }
      .my-1 { margin-top: 0.25rem; margin-bottom: 0.25rem; }
      .my-3 { margin-top: 0.75rem; margin-bottom: 0.75rem; }
      .my-6 { margin-top: 1.5rem; margin-bottom: 1.5rem; }
      .p-4 { padding: 1rem; }
      .p-8 { padding: 2rem; }
      .px-4 { padding-left: 1rem; padding-right: 1rem; }
      .py-3 { padding-top: 0.75rem; padding-bottom: 0.75rem; }
      .py-4 { padding-top: 1rem; padding-bottom: 1rem; }
      .pl-2 { padding-left: 0.5rem; }
      .pr-2 { padding-right: 0.5rem; }
      .pt-8 { padding-top: 2rem; }
      .pb-8 { padding-bottom: 2rem; }
      .space-y-1 > * + * { margin-top: 0.25rem; }
      .space-y-1\.5 > * + * { margin-top: 0.375rem; }
      .space-y-2 > * + * { margin-top: 0.5rem; }
      .space-y-3 > * + * { margin-top: 0.75rem; }
      .space-y-4 > * + * { margin-top: 1rem; }
      .grid { display: grid; }
      .grid-cols-2 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .gap-8 { gap: 2rem; }
      .flex { display: flex; }
      .justify-between { justify-content: space-between; }
      .justify-end { justify-content: flex-end; }
      .items-start { align-items: flex-start; }
      .items-center { align-items: center; }
      .w-8 { width: 2rem; }
      .h-8 { height: 2rem; }
      .w-2\/3 { width: 66.666667%; }
      .w-full { width: 100%; }
      .max-w-md { max-width: 28rem; }
      table { width: 100%; border-collapse: collapse; margin-bottom: 1.5rem; }
      th { background-color: #f9fafb; border-top: 1px solid #e5e7eb; border-bottom: 1px solid #e5e7eb; color: #6b7280; font-size: 0.75rem; text-transform: uppercase; padding: 0.75rem 0.5rem; font-weight: 600; }
      td { padding: 1rem 0.5rem; border-bottom: 1px solid #f3f4f6; vertical-align: top; }
      .divide-y > * + * { border-top: 1px solid #f3f4f6; }
      .border-t { border-top: 1px solid #f3f4f6; }
      .border-b { border-bottom: 1px solid #f3f4f6; }
      .border-y { border-top: 1px solid #e5e7eb; border-bottom: 1px solid #e5e7eb; }
      hr { border: none; border-top: 1px dashed #e5e7eb; margin: 1.5rem 0; }
    </style>
  `

  // Viết nội dung vào cửa sổ mới
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>HoaDon_${hoaDonData.value?.maHoaDon || invoiceCode.value}</title>
      ${printStyles}
    </head>
    <body>
      ${contentHTML}
      <script>
        window.onload = function() {
          // Tự động in khi trang load xong
          window.print();
          // Sau khi in, vẫn giữ cửa sổ để người dùng có thể in lại nếu cần
        }
      <\/script>
    </body>
    </html>
  `)

  printWindow.document.close()
  printLoading.value = false
}

const handleBack = (): void => {
  router.push('/orders/list')
}

const handleComplete = (): void => {
  if (isCompleted.value) {
    message.warning('Đơn hàng đã hoàn thành')
    return
  }
  selectedStatus.value = 4 // Hoàn thành
  showStatusModal.value = true
}

const handleUpdateStatus = (): void => {
  showStatusModal.value = true
}

const confirmStatusUpdate = async (): Promise<void> => {
  if (selectedStatus.value === null || !hoaDonData.value?.maHoaDon) {
    message.error('Vui lòng chọn trạng thái mới')
    return
  }

  isUpdating.value = true
  try {
    const requestData: ADChangeStatusRequest = {
      maHoaDon: hoaDonData.value.maHoaDon,
      statusTrangThaiHoaDon: selectedStatus.value,
      note: statusNote.value || '',
      idNhanVien: idNV.userId
    }

    const response = await changeOrderStatus(requestData)

    if (response.success) {
      message.success('Cập nhật trạng thái thành công')

      // Refresh dữ liệu
      await fetchInvoiceDetails()

      // Reset form
      selectedStatus.value = null
      statusNote.value = ''
      showStatusModal.value = false
    } else {
      message.error(response.message || 'Cập nhật thất bại')
    }
  } catch (error: any) {
    console.error('Error updating status:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi cập nhật')
  } finally {
    isUpdating.value = false
  }
}

const handleEditInvoice = (): void => {
  message.info("Tính năng chỉnh sửa hóa đơn đang được phát triển")
}

const openCancelModal = (): void => {
  if (isCancelled.value) {
    message.warning('Đơn hàng đã bị hủy')
    return
  }

  dialog.error({
    title: 'Xác nhận hủy đơn hàng',
    content: 'Bạn có chắc chắn muốn hủy đơn hàng này? Hành động này không thể hoàn tác.',
    positiveText: 'Xác nhận hủy',
    negativeText: 'Hủy bỏ',
    positiveButtonProps: {
      type: 'error',
      ghost: false
    },
    onPositiveClick: () => {
      selectedStatus.value = 5 // Đã hủy
      showStatusModal.value = true
    }
  })
}

// Fetch invoice details
const fetchInvoiceDetails = async (): Promise<void> => {
  try {
    isLoading.value = true

    const params = {
      maHoaDon: invoiceCode.value,
      page: 0,
      size: 100
    }

    const response = await getHoaDonChiTiets(params)

    if (response.success && response.data?.content) {
      invoiceItems.value = response.data.content
      console.log('Invoice items loaded:', invoiceItems.value.length)
      console.log('Invoice type:', hoaDonData.value?.loaiHoaDon)
      
      // Log thông tin serial của từng sản phẩm
      invoiceItems.value.forEach((item, index) => {
        const imeiList = parseIMEIList(item.danhSachImei)
        console.log(`Product ${index + 1}: ${item.tenSanPham}, Quantity: ${item.soLuong}, IMEIs: ${imeiList.length}`)
        imeiList.forEach(imei => {
          console.log(`  - IMEI: ${imei.code}, Status: ${imei.status}`)
        })
      })
    } else {
      message.error(response.message || 'Không thể tải chi tiết hóa đơn')
    }
  } catch (error: any) {
    console.error('Lỗi khi fetch chi tiết hóa đơn:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi tải dữ liệu')
  } finally {
    isLoading.value = false
  }
}

// Lifecycle
onMounted(() => {
  console.log("Invoice detail page loaded")
  console.log("Invoice ID from URL:", invoiceCode.value)

  if (invoiceCode.value) {
    fetchInvoiceDetails()
  } else {
    message.error('Không tìm thấy mã hóa đơn')
    router.push('/admin/hoa-don')
  }
})
</script>

<style scoped>
/* Print styles - không còn sử dụng trực tiếp */
@media print {
  .no-print {
    display: none !important;
  }

  body {
    background: white !important;
    color: black !important;
    padding: 0 !important;
    margin: 0 !important;
    font-size: 12px;
  }

  .container {
    max-width: 100% !important;
    padding: 0 !important;
    margin: 0 !important;
  }

  #invoice-paper {
    box-shadow: none !important;
    padding: 20px !important;
    max-width: 100%;
  }

  .print-table {
    width: 100%;
    border-collapse: collapse;
  }

  .print-table th,
  .print-table td {
    padding: 8px 5px;
    border: 1px solid #ddd;
  }

  .print-table th {
    background-color: #f8f9fa;
    font-weight: bold;
  }

  /* Page breaks */
  tr {
    page-break-inside: avoid;
  }

  thead {
    display: table-header-group;
  }

  tfoot {
    display: table-footer-group;
  }
}

/* Ẩn nội dung hóa đơn gốc */
.hidden {
  display: none;
}

/* Custom scrollbar */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Row styles */
:deep(.imei-row) {
  border-left: 4px solid #3b82f6 !important;
}

:deep(.product-row) {
  border-left: 4px solid #d1d5db !important;
}
</style>