<template>
  <div class="container mx-auto px-5">

    <div class="flex mt-5" v-if="route.query.q">
      <n-button type="success" secondary @click="() => router.push({ name: 'Orders' })">
        <n-icon :size="20">
          <ArrowBackOutline />
        </n-icon>
      </n-button>
      <div class="mx-2 w-1 bg-[#049d14] rounded m-r-2"></div>
      <h2 class="text-2xl font-bold">
        Trở về danh sách đơn hàng
      </h2>
    </div>

    <!-- Search Card -->
    <div class="flex gap-2 flex-1 items-end">
      <n-form-item path="searchInvoiceCode" class="flex-1 mb-0">
        <n-input v-model:value="searchInvoiceCode" placeholder="Nhập mã hóa đơn để tìm kiếm" clearable />
      </n-form-item>
      <n-form-item>
        <n-button type="success" @click="handleSearch">
          <template #icon>
            <n-icon>
              <SearchOutline />
            </n-icon>
          </template>
          Tìm kiếm
        </n-button>
      </n-form-item>
    </div>

    <!-- Progress Timeline -->
    <NCard v-if="invoice" class="" :bordered="false">
      <template #header>
        <n-space justify="space-between" class="m-y-2">
          <!-- Header with invoice code -->
          <div v-if="invoice" class="mb-4 p-2 flex">
            <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
            <h2 class="text-2xl font-bold">
              Tiến độ đơn hàng -
              <div class="inline-block rounded">
                {{ invoice.code }}
              </div>
            </h2>
          </div>

          <n-space>
            <NButton type="error" block ghost @click="openCancelModal"
              v-if="+(invoice.invoiceStatus) === 0 && invoice.typePayment == 'TIEN_MAT' && isLoggedIn"
              :style="{ maxWidth: '150px' }">
              <template #icon>
                <n-icon>
                  <CloseCircleOutline />
                </n-icon>
              </template>
              Hủy đơn hàng
            </NButton>
            <NButton type="info" secondary @click="isOpenHistoryModel = true">
              <template #icon>
                <NIcon>
                  <ListOutline />
                </NIcon>
              </template> Chi tiết
            </NButton>
          </n-space>
        </n-space>
      </template>

      <div class="relative">
        <div v-if="!isCancelled">
          <div class="pt-2 pb-0 w-full">
            <div class="relative mx-auto flex justify-between items-start transition-all duration-500"
              :class="getTimelineContainerWidth(currentVisibleSteps.length)">
              <template v-for="(step, index) in currentVisibleSteps" :key="`TrackingView-Step-${index}`">
                <div class="relative flex flex-col items-center flex-1">

                  <!-- LINE -->
                  <div v-if="index < currentVisibleSteps.length - 1" class="absolute h-[6px] z-0 left-[50%] w-full"
                    style="top:45px" :class="isStepCompleted(step.key) ? 'bg-blue-500' : 'bg-gray-200'" />

                  <!-- ICON -->
                  <div class="h-24 flex items-center justify-center relative z-10 w-full">

                    <div
                      class="rounded-full flex items-center justify-center transition-all duration-300 relative hover:scale-110"
                      :class="getDynamicIconSizeClass(step.key)">
                      <n-icon :size="getDynamicIconSize(step.key)">
                        <component :is="step.icon" />
                      </n-icon>

                      <!-- completed check -->
                      <div v-if="isStepCompleted(step.key)"
                        class="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center border-2 border-white shadow">
                        <n-icon size="14" color="white">
                          <CheckmarkCircleOutline />
                        </n-icon>
                      </div>

                    </div>

                  </div>

                  <!-- TEXT -->
                  <div class="text-center mt-2 px-2">

                    <p class="text-sm font-semibold mb-1" :class="getStepTextClass(step.key)">
                      {{ step.title }}
                    </p>

                    <p v-if="historiesStatusInvoice.length > 0" class="text-xs text-gray-500">
                      {{
                        isStepCompleted(step.key)
                          ? formatTime(
                            historiesStatusInvoice.find(h => h.trangThai === +(step.key))?.thoiGian
                          )
                          : ''
                      }}
                    </p>

                  </div>

                </div>
              </template>
            </div>
          </div>
        </div>
        <div v-else>
          <div class="flex flex-col items-center">
            <div
              class="w-15 h-15 rounded-full border-6 bg-white flex items-center justify-center mb-3 relative transition-all duration-300 hover:scale-110 text-red-500">
              <n-icon size="30" class="text-red-500 ">
                <CloseOutline />
              </n-icon>
            </div>

            <!-- Step label -->
            <div class="text-center">
              <p class="text-sm font-semibold mb-1 text-red-500">
                Đã hủy
              </p>
              <p v-if="historiesStatusInvoice.length > 0" class="text-xs text-red-500">
                {{formatTime(historiesStatusInvoice.find(h => h.trangThai == 5)?.thoiGian ?? '0')}}
              </p>
            </div>
          </div>
        </div>

        <!-- customer info panel -->
        <div class="flex gap-5">
          <div class="flex-1">
            <div v-if="invoice" class="mb-4 p-2 flex m-t-[50px] justify-between">
              <div class="flex">
                <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
                <h2 class="text-2xl font-bold">
                  Thông tin người nhận hàng
                </h2>
              </div>
              <div class="" v-if="invoice.invoiceStatus === 0 && isLoggedIn">
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <n-button :bordered="false" circle size="large" secondary type="success"
                      @click="isOpenCustomerForm = true">
                      <n-icon size="20">
                        <PencilOutline />
                      </n-icon>
                    </n-button>
                  </template>
                  Cập nhật thông tin người nhận
                </n-tooltip>
              </div>
            </div>
            <div class="p-4 rounded-lg flex flex-col gap-3">
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Khách hàng:</span>
                <span>{{ invoice.nameReceiver }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">SĐT:</span>
                <span>{{ invoice.phoneReceiver }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Email:</span>
                <span>{{ invoice.email }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Ngày đặt:</span>
                <span>{{ convertTimeMillis(invoice.createDate) }}</span>
              </div>
              <div v-if="invoice.description" class="text-sm pb-2">
                <div class="border-b border-gray-300/30 p-2 flex-1">
                  <span class="font-medium mr-2">Ghi chú:</span>
                  <span>{{ invoice.description }}</span>
                </div>
              </div>
              <div class="text-sm">
                <div class="border-b border-gray-300/30 p-2 flex-1">
                  <span class="font-medium mr-2">Địa chỉ:</span>
                  <span class="">{{ invoice.addressReceiver }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="flex-1">
            <div v-if="invoice" class="mb-4 p-2 flex m-t-[50px]">
              <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
              <h2 class="text-2xl font-bold">
                Trạng thái thanh toán
              </h2>
            </div>
            <div class="p-4 rounded-lg flex flex-col gap-3">
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Phương thức thanh toán:</span>
                <span>{{ convertTextFromTypePayment(invoice.typePayment) }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Trạng thái thanh toán:</span>
                <n-tag :type="statusPayment">
                  {{ convertTextFromTrangThaiThanhToan(invoice.trangThaiThanhToan) }}
                </n-tag>
              </div>
            </div>
          </div>
        </div>

        <div v-if="invoice" class="mb-4 p-2 flex m-t-[50px] justify-between">
          <div class="flex">
            <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
            <h2 class="text-2xl font-bold">
              Sản phẩm đã đặt
            </h2>
          </div>
          <div v-if="invoice.invoiceStatus == 0 && isLoggedIn">
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button :bordered="false" circle size="large" secondary type="success"
                  @click="isEditQuantityProduct = true">
                  <n-icon size="20">
                    <PencilOutline />
                  </n-icon>
                </n-button>
              </template>
              Cập nhật số lượng sản phẩm
            </n-tooltip>
          </div>
        </div>

        <div v-if="isEditQuantityProduct" class="ml-5">
          <span class="italic font-semibold">
            <span class="text-red-500">* Chú ý: </span>
            Mỗi sản phẩm chỉ được thêm tối đa 5 sản phẩm
          </span>
        </div>

        <!-- product list -->
        <div v-if="invoiceDetails.length > 0" class="space-y-4">
          <div v-for="(detail, index) in invoiceDetails" :key="detail.id"
            class="flex items-center border-b border-gray-300 p-4">
            <img :src="detail.urlImage" alt="product" class="w-30 h-30 object-contain rounded" />
            <div class="flex-1 ml-4">
              <div class="text-base font-medium">
                <span>{{ detail.nameProductDetail }} </span>
              </div>
              <div class="flex flex-wrap text-xs text-gray-500 mt-1 gap-2">
                <span v-if="detail.color" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.color }}</span>
                <span v-if="detail.ram" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.ram }}</span>
                <span v-if="detail.hardDrive" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.hardDrive }}</span>
                <span v-if="detail.cpu" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.cpu }}</span>
                <span v-if="detail.gpu" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.gpu }}</span>
                <span v-if="detail.material" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.material }}</span>
              </div>
            </div>
            <div class="mr-3 flex-2">
              <div class="flex gap-2 items-center w-400px">
                <span class="mr-3">Số lượng:</span>
                <button class="qty-btn" v-if="isEditQuantityProduct"
                  :disabled="updateProductDetails[index].quantity <= 1 || updateProductDetails[index].itNotEdit"
                  @click.stop="handleUpdateQuantity(updateProductDetails[index].idInvoiceDetail, updateProductDetails[index].quantity - 1)">
                  <NIcon size="14">
                    <RemoveOutline />
                  </NIcon>
                </button>
                <n-input-number v-model:value="updateProductDetails[index].quantity" :min="1" :show-button="false"
                  size="small" :bordered="isEditQuantityProduct" style="width: 100px" placeholder="Nhập số lượng"
                  :max="5"
                  :input-props="{ style: 'border:none; box-shadow:none; background:transparent; font-weight:600; font-size:15px; padding:0 4px; text-align: center;' }"
                  :readonly="!isEditQuantityProduct || updateProductDetails[index].itNotEdit" />
                <button v-if="isEditQuantityProduct" class="qty-btn"
                  :disabled="updateProductDetails[index].quantity >= 5 || updateProductDetails[index].itNotEdit"
                  @click.stop="handleUpdateQuantity(updateProductDetails[index].idInvoiceDetail, updateProductDetails[index].quantity + 1)">
                  <NIcon size="14">
                    <AddOutline />
                  </NIcon>
                </button>
              </div>
              <div v-if="updateProductDetails[index].isAddByChange || updateProductDetails[index].isChangedByCustomer"
                class="text-yellow-500 text-12px italic">
                <span class="">Giá sản phẩm tằng từ {{index > 0 && formatCurrency(
                  _.slice(invoiceDetails, 0, index)
                    .findLast(it => it.idProductDetail === updateProductDetails[index].idProductDetail)?.price ?? 0)}}
                  lên
                  {{ formatCurrency(updateProductDetails[index].price) }}</span>
              </div>
            </div>
            <div class="text-lg font-semibold text-red-600">
              {{ formatCurrency(updateProductDetails[index].totalAmount) }}
            </div>
            <div v-if="isEditQuantityProduct" class="ml-5">
              <n-button :bordered="false" type="success" circle secondary
                @click="handleUpdateQuantity(updateProductDetails[index].idInvoiceDetail, 0)"
                :disabled="updateProductDetails.length === 1">
                <n-icon>
                  <TrashOutline />
                </n-icon>
              </n-button>
            </div>
          </div>
        </div>

        <!-- totals summary -->
        <div v-if="invoice && !isEditQuantityProduct" class="mt-6 p-4 ">
          <div class="max-w-xs ml-auto text-sm space-y-3">
            <div class="flex justify-between">
              <span>Tạm tính:</span>
              <span>{{ formatCurrency(invoice.totalAmountAfterDecrease) }}</span>
            </div>
            <div v-if="discount != 0" class="flex justify-between text-red-600">
              <span>Giảm giá:</span>
              <span>-{{ formatCurrency(discount) }}</span>
            </div>
            <div v-if="shippingFee != 0" class="flex justify-between">
              <span>Phí vận chuyển:</span>
              <span>{{ formatCurrency(shippingFee) }}</span>
            </div>
            <div class="flex justify-between font-bold text-lg border-t border-gray-300 p-2">
              <span>Tổng cộng:</span>
              <span class="text-red-600">{{ formatCurrency(invoice.totalAmount) }}</span>
            </div>
          </div>
        </div>

        <n-space class="mt-20px" v-if="isEditQuantityProduct" justify="end">
          <n-button @click="handleClickCancelSaveEditQuantity">Hủy</n-button>
          <n-popconfirm :positive-button-props="{
            type: 'success'
          }" :positive-text="'Xác nhận'" :negative-text="'Hủy'" @positive-click="handleClickSaveEditQuantity"
            @negative-click="handleClickCancelSaveEditQuantity">
            <template #trigger>
              <n-button type="success">Xác nhận</n-button>
            </template>
            Bạn chắc chắn muốn thao tác ?
          </n-popconfirm>
        </n-space>
      </div>
    </NCard>

    <NCard v-if="!invoice" class="shadow-sm border-0 rounded-xl no-print bg-red-50" content-class="p-6">
      <p class="text-red-600 font-semibold">Không có thông tin chi tiết về đơn hàng. Vui lòng liên hệ bộ phận chăm sóc
        khách
        hàng để biết thêm chi tiết.</p>
    </NCard>

    <NModal v-model:show="isOpenModalCancelInvoice" preset="card" class="w-30%" :bordered="false" size="huge"
      content-style="padding-top: 0;">
      <template #header>
        <div class="flex items-center gap-3 pt-2">
          <div>
            <h3 class="text-lg font-bold text-gray-900 leading-tight">
              Hủy đơn hàng
            </h3>
            <p class="text-xs text-gray-500 font-normal mt-0.5">
              Mã đơn: <span class="font-mono text-gray-700 font-semibold">{{ invoice?.code }}</span>
            </p>
          </div>
        </div>
      </template>

      <div class="py-4">
        <div class="space-y-2">
          <label class="text-sm font-semibold text-gray-700 flex items-center gap-1">
            Lý do hủy đơn
            <span class="text-red-500 font-bold">*</span>
          </label>
          <NInput v-model:value="statusNote" type="textarea" placeholder="Bắt buộc nhập lý do hủy đơn hàng..." :rows="3"
            status="error" class="rounded-lg" />
          <p v-if="statusNote.trim() === ''" class="text-xs text-red-500 mt-1">
            Vui lòng nhập lí do
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3 justify-end pt-2">
          <NButton size="large" class="rounded-lg font-medium" @click="isOpenModalCancelInvoice = false">
            Hủy bỏ
          </NButton>
          <n-popconfirm :positive-button-props="{
            type: 'success'
          }" :positive-text="'Xác nhận'" :negative-text="'Hủy'" @positive-click="handleClickCancelInvoice">
            <template #trigger>
              <NButton type="success" size="large"
                class="rounded-lg font-bold shadow-md hover:-translate-y-0.5 transition-transform"
                :disabled="statusNote.length == 0">
                Xác hủy đơn hàng
              </NButton>
            </template>
            Bạn chắc chắn muốn thao tác ?
          </n-popconfirm>
        </div>
      </template>
    </NModal>

    <!-- ==================== MODAL: LỊCH SỬ ĐƠN HÀNG ==================== -->
    <NModal v-model:show="isOpenHistoryModel" preset="card" title="Lịch sử đơn hàng"
      style="width: 1000px; max-width: 95vw; border-radius: 12px;" class="no-print shadow-2xl" :bordered="false"
      size="huge">
      <div class="w-full mt-2">
        <div
          class="grid grid-cols-12 gap-4 pb-4 border-b-2 border-gray-100 text-[13px] font-bold text-gray-500 uppercase tracking-wider">
          <div class="col-span-3 pl-4">
            Trạng thái
          </div>
          <div class="col-span-3">
            Thời gian
          </div>
          <div class="col-span-3">
            Người xác nhận
          </div>
          <div class="col-span-3 pr-4">
            Ghi chú
          </div>
        </div>
        <div class="divide-y divide-gray-100 max-h-[60vh] overflow-y-auto pr-2 mt-2">
          <div v-for="(item, index) in historiesStatusInvoice" :key="`HISTORY-INVOICE-${index}`"
            class="grid grid-cols-12 gap-4 py-4 items-center hover:bg-gray-50/70 transition-colors rounded-lg">
            <div class="col-span-3 pl-4 flex items-center gap-3">
              <div class="w-8 h-8 rounded-full flex items-center justify-center"
                :class="getStepCircleBg(item.trangThai)">
                <NIcon size="18" :class="getStepIconClass(item.trangThai.toString())">
                  <component :is="getStatusIcon(item.trangThai)" />
                </NIcon>
              </div>
              <span class="font-bold text-gray-800 text-[15px]">{{ getStatusText(item.trangThai) }}</span>
            </div>
            <div class="col-span-3 text-[14px] text-gray-600 font-medium tracking-wide">
              {{ formatTime(item.thoiGian) }}
            </div>
            <div class="col-span-3 text-[14.5px] text-gray-800 font-semibold">
              {{ item.nameStaff || 'Hệ thống tự động' }}
            </div>
            <div class="col-span-3 pr-4 text-[14px] text-gray-600 leading-relaxed">
              {{ item.note || '---' }}
            </div>
          </div>
          <div v-if="historiesStatusInvoice.length === 0"
            class="py-16 text-center text-gray-400 flex flex-col items-center">
            <NIcon size="48" class="mb-3 opacity-30">
              <TimeOutline />
            </NIcon>
            <p class="text-lg">
              Chưa có dữ liệu lịch sử cập nhật.
            </p>
          </div>
        </div>
      </div>
    </NModal>

    <!-- ==================== MODAL: CHỈNH SỬA KHÁCH HÀNG ==================== -->
    <NModal v-model:show="isOpenCustomerForm" preset="card" title="Chỉnh sửa thông tin khách hàng"
      style="width: 500px; max-width: 95vw; border-radius: 12px" :bordered="false" class="no-print shadow-xl">
      <NForm ref="customerFormRef" :model="customerForm" :rules="customerFormRules" label-placement="top">
        <NFormItem label="Họ và tên" path="tenKhachHang">
          <NInput v-model:value="customerForm.tenKhachHang" placeholder="Nhập họ và tên khách hàng" size="large" />
        </NFormItem>

        <NFormItem label="Số điện thoại" path="sdtKH">
          <NInput v-model:value="customerForm.sdtKH" placeholder="Nhập số điện thoại" size="large" />
        </NFormItem>

        <NFormItem label="Email" path="email">
          <NInput v-model:value="customerForm.email" placeholder="Nhập email (không bắt buộc)" size="large" />
        </NFormItem>

        <NFormItem label="Địa chỉ" path="diaChi">
          <NInput v-model:value="customerForm.diaChi" placeholder="Nhập địa chỉ giao hàng" size="large" type="textarea"
            :rows="3" />
        </NFormItem>
      </NForm>

      <template #footer>
        <div class="flex justify-end gap-3">
          <NButton size="large" @click="isOpenCustomerForm = false">
            Hủy bỏ
          </NButton>
          <NPopconfirm positive-text="Xác nhận" :positive-button-props="{ type: 'success' }" negative-text="Hủy bỏ"
            @positive-click="handleClickSaveCustomerForm">
            <template #trigger>
              <NButton type="success" size="large" :loading="isSavingCustomer">
                Lưu thông tin
              </NButton>
            </template>

            <template #icon>
              <NIcon color="#18a058">
                <CheckmarkCircleOutline />
              </NIcon>
            </template>

            Bạn có chắc muốn cập nhật thông tin khách hàng?
          </NPopconfirm>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
// import { useRouter, useRoute } from 'vue-router'
import {
  FormInst,
  FormRules,
  NButton,
  NCard,
  NFormItem,
  NIcon,
  NInput,
  NSpace
} from 'naive-ui'

// Icons
import { ClientInvoiceDetailResponse, ClientInvoiceDetailsResponse, getHistoryStatusInvoice, getInvoiceById, getInvoiceDetails, LichSuTrangThaiHoaDonResponse, putInvoiceCancel, putInvoiceDetail, putReceiver } from '@/service/api/invoice.api'
import {
  AddOutline,
  ArrowBackOutline,
  CartOutline,
  CheckmarkCircleOutline,
  CheckmarkDoneOutline,
  CloseCircleOutline,
  CloseOutline,
  ListOutline,
  PencilOutline,
  RemoveOutline,
  SearchOutline,
  TimeOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { getProductDetailById } from '@/service/api/client/product/productDetail.api'
import _ from 'lodash'
import { useAuthStore } from '@/store'
import { router } from '@/router'

interface CustomerForm {
  tenKhachHang: string
  sdtKH: string
  email: string
  diaChi: string
}

type UpdateProductDetailType = {
  idProductDetail: string
  idInvoiceDetail: string
  quantity: number
  price: number
  giaGoc: number
  totalAmount: number,
  itNotEdit: boolean
  isAddByChange?: boolean
  isChangedByCustomer?: boolean
}

// const router = useRouter()
const route = useRoute()
const notification = useNotification()
const dialog = useDialog()


// State
const invoice = ref<ClientInvoiceDetailResponse | null>(null)
const searchInvoiceCode = ref('')
const statusNote = ref('')
const isOpenModalCancelInvoice = ref(false)

const historiesStatusInvoice = ref<LichSuTrangThaiHoaDonResponse[]>([])
const invoiceDetails = ref<ClientInvoiceDetailsResponse[]>([])
const updateProductDetails = ref<UpdateProductDetailType[]>([])
const removeProductDetails = ref<UpdateProductDetailType[]>([])
// Lấy dữ liệu tổng hợp từ danh sách sản phẩm
const currentStatus = computed(() => {
  return invoice.value?.invoiceStatus ?? 0
})

const customerForm = reactive<CustomerForm>({
  tenKhachHang: '',
  sdtKH: '',
  email: '',
  diaChi: '',
})

const { userInfoDatn } = storeToRefs(useAuthStore())

const customerFormRef = ref<FormInst | null>(null)

const isOpenHistoryModel = ref(false)
const isOpenCustomerForm = ref(false)
const isEditQuantityProduct = ref(false)

const isLoggedIn = computed(() => userInfoDatn.value?.userId)

// pricing computations
const subtotal = computed(() => invoice.value?.totalAmount ?? 0)
const discount = computed(() => {
  if (!invoice.value) return 0
  return invoice.value?.totalAmountAfterDecrease - (invoice.value.totalAmount ?? subtotal.value) + invoice.value.shippingFee
})
const shippingFee = computed(() => invoice.value?.shippingFee ?? 0)

// Computed values
const isCancelled = computed(() => currentStatus.value === 5)

const currentVisibleSteps = computed(() => {
  const steps = filteredSteps.value
  const currentIndex = steps.findIndex(step => step.key === currentStatus.value.toString())
  if (currentIndex === -1)
    return steps // fallback: hiện tất cả nếu không tìm thấy
  return steps.filter((step, index) => index <= currentIndex || (isCancelled.value && step.key === '5'))
})

const filteredSteps = computed(() => {
  return TIMELINE_STEPS.filter(step => step.key !== '5')
})

const TIMELINE_STEPS = [
  { key: '0', title: 'Chờ xác nhận', icon: TimeOutline, color: 'yellow' },
  { key: '1', title: 'Đã xác nhận', icon: CheckmarkCircleOutline, color: 'blue' },
  { key: '2', title: 'Chờ giao hàng', icon: CartOutline, color: 'purple' },
  { key: '3', title: 'Đang giao hàng', icon: CheckmarkCircleOutline, color: 'info' },
  { key: '4', title: 'Hoàn thành', icon: CheckmarkDoneOutline, color: 'green' },
  { key: '5', title: 'Đã hủy', icon: CloseCircleOutline, color: 'red' },
]

// ==================== Constants ====================
const STATUS_MAP: Record<number, { label: string, type: string, icon: any }> = {
  0: { label: 'Chờ xác nhận', type: 'warning', icon: TimeOutline },
  1: { label: 'Đã xác nhận', type: 'info', icon: CheckmarkCircleOutline },
  2: { label: 'Chờ giao hàng', type: 'default', icon: CartOutline },
  3: { label: 'Đang giao hàng', type: 'info', icon: CheckmarkCircleOutline },
  4: { label: 'Hoàn thành', type: 'success', icon: CheckmarkDoneOutline },
  5: { label: 'Đã hủy', type: 'error', icon: CloseCircleOutline },
}

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

function getStepCircleBg(status: string | number | undefined): string {
  const map: Record<number, string> = { 0: 'bg-yellow-100', 1: 'bg-blue-100', 2: 'bg-purple-100', 3: 'bg-blue-100', 4: 'bg-green-100', 5: 'bg-red-100' }
  return map[Number(status)] || 'bg-gray-100'
}

function getStatusIcon(status: string | number | undefined): any { return STATUS_MAP[Number(status)]?.icon || TimeOutline }

function getStatusText(status: string | number | undefined): string { return STATUS_MAP[Number(status)]?.label || 'Không xác định' }

// convert Java LocalDateTime string to "HH:mm:ss dd/mm/yyyy"
function formatTime(datetime: string | undefined | null): string {
  if (!datetime) return ''
  const d = new Date(datetime)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(
    d.getMonth() + 1
  )}/${d.getFullYear()}`
}

// convert milliseconds since epoch to "HH:mm:ss dd/mm/yyyy"
function convertTimeMillis(ms: number | undefined | null): string {
  if (ms == null) return ''
  const d = new Date(ms)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(
    d.getMonth() + 1
  )}/${d.getFullYear()}`
}

// Timeline functions
const isStepCompleted = (stepKey: string): boolean => {
  const stepIndex = parseInt(stepKey)
  return stepIndex <= currentStatus.value && stepKey !== '5'
}

const getStepIconClass = (stepKey: string): string => {
  if (isStepCompleted(stepKey)) {
    return 'text-green-500'
  } else {
    return 'text-gray-400'
  }
}

const getStepTextClass = (stepKey: string): string => {
  if (isStepCompleted(stepKey)) {
    return 'text-blue-600'
  } else {
    return 'text-gray-500'
  }
}


// Actions

const handleSearch = (): void => {
  if (!searchInvoiceCode.value.trim()) {
    notification.warning({ content: 'Vui lòng nhập mã hóa đơn', duration: 3000 })
    return
  }

  fetchInvoice()
}

const openCancelModal = (): void => {
  isOpenModalCancelInvoice.value = true
}

const fetchInvoice = async (query?: string): Promise<void> => {
  try {
    const response = await getInvoiceById(query ?? searchInvoiceCode.value)

    invoice.value = response.data
    // load history statuses and details concurrently
    if (invoice.value?.id) {
      try {
        const [histRes, detailRes] = await Promise.all([
          getHistoryStatusInvoice(invoice.value.id),
          getInvoiceDetails([invoice.value.id])
        ])
        historiesStatusInvoice.value = histRes.data || []
        invoiceDetails.value = _.sortBy(detailRes.data.filter(it => it.idInvoice === invoice.value?.id), 'createdDate') || []

        updateProductDetails.value = convertInvoiceDetailToUpdateProductDetail(invoiceDetails.value)

        customerForm.tenKhachHang = invoice.value.nameReceiver;
        customerForm.email = invoice.value.email;
        customerForm.sdtKH = invoice.value.phoneReceiver;
        customerForm.diaChi = invoice.value.addressReceiver;
      } catch (e) {
        console.error('Error fetching invoice meta:', e)
        historiesStatusInvoice.value = []
        invoiceDetails.value = []
      }
    }
  } catch (error: any) {
    invoice.value = null
    historiesStatusInvoice.value = []
  }
}

const convertInvoiceDetailToUpdateProductDetail = (invoiceDetails: ClientInvoiceDetailsResponse[]): UpdateProductDetailType[] => {
  return invoiceDetails.map((it, index) => {
    const isChangedByCustomer = _.slice(invoiceDetails, 0, index)
      .filter(invoiceDetail => invoiceDetail.idProductDetail === it.idProductDetail).length > 0

    const isLastInvoiceDetail = _.findLast(invoiceDetails, invoiceDetail => invoiceDetail.idProductDetail === it.idProductDetail)?.id === it.id

    return {
      idInvoiceDetail: it.id,
      idProductDetail: it.idProductDetail,
      giaGoc: it.giaGoc,
      price: it.price,
      quantity: it.quantity,
      itNotEdit: !isLastInvoiceDetail,
      totalAmount: it.totalAmount,
      isChangedByCustomer
    }
  })
}

function convertTextFromTypePayment(payment: string): string {
  switch (payment) {
    case 'TIEN_MAT':
      return 'Tiền mặt'
    case 'VNPAY':
      return 'VNPay'
    case 'CHUYEN_KHOAN':
      return 'Chuyển khoản'
    case 'THE_TIN_DUNG':
      return 'Thẻ tín dụng/Thẻ ghi nợ'
    case 'VI_DIEN_TU':
      return 'Ví điện tử'
    case 'TIEN_MAT_CHUYEN_KHOAN':
      return 'Tiền mặt + Chuyển khoản'
    default:
      return ''
  }
}

function convertTextFromTrangThaiThanhToan(status: string): string {
  switch (status) {
    case 'CHUA_THANH_TOAN':
      return 'Chưa thanh toán'
    case 'CHO_THANH_TOAN_VNPAY':
      return 'Chờ thanh toán VNPay'
    case 'DA_THANH_TOAN':
      return 'Đã thanh toán'
    case 'THANH_TOAN_MOT_PHAN':
      return 'Thanh toán một phần'
    case 'THANH_TOAN_THAT_BAI':
      return 'Thanh toán thất bại'
    default:
      return ''
  }
}

const statusPayment = computed(() => {
  switch (invoice.value?.trangThaiThanhToan) {
    case 'CHUA_THANH_TOAN':
      return 'error'
    case 'CHO_THANH_TOAN_VNPAY':
      return 'warning'
    case 'DA_THANH_TOAN':
      return 'success'
    case 'THANH_TOAN_MOT_PHAN':
      return 'success'
    case 'THANH_TOAN_THAT_BAI':
      return 'error'
    default:
      return 'success'
  }
})

const handleClickCancelInvoice = async () => {
  try {
    if (statusNote.value.length === 0) return;

    const res = await putInvoiceCancel({
      id: invoice.value?.id as string,
      note: statusNote.value
    })

    notification.success({ content: 'Hủy hóa đơn thành công', duration: 3000 })
    isOpenModalCancelInvoice.value = false
    fetchInvoice(route.query.q as string)
  } catch (e) {
    notification.error({ content: 'Hủy hóa đơn thất bại. Vui lòng thử lại', duration: 3000 })
  }
}

function getTimelineContainerWidth(total: number) {
  if (total === 1) return 'max-w-sm'
  if (total === 2) return 'max-w-2xl'
  if (total === 3) return 'max-w-4xl'
  if (total === 4) return 'max-w-6xl'
  return 'w-full'
}

function getDynamicIconSizeClass(stepKey: string) {

  if (currentStatus.value.toString() === stepKey)
    return 'w-24 h-24 border-[6px] border-blue-500 bg-blue-50 text-blue-600 shadow-lg'

  if (isStepCompleted(stepKey))
    return 'w-20 h-20 border-[5px] border-blue-500 bg-blue-50 text-blue-600 shadow-md'

  return 'w-20 h-20 border-[5px] border-gray-300 bg-white text-gray-400'
}

function getDynamicIconSize(stepKey: string) {
  return currentStatus.value.toString() === stepKey ? 48 : 40
}

const customerFormRules: FormRules = {
  tenKhachHang: [{ required: true, message: 'Vui lòng nhập họ và tên', trigger: ['blur', 'input'] }],
  sdtKH: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: ['blur', 'input'] },
    { pattern: /(84|0[3|5789])+(\d{8})\b/, message: 'Số điện thoại không hợp lệ', trigger: ['blur', 'input'] },
  ],
  email: [{ type: 'email', message: 'Email không hợp lệ', trigger: ['blur', 'input'] }],
}

const isSavingCustomer = ref(false)
const handleClickSaveCustomerForm = async () => {
  try { await customerFormRef.value?.validate(); } catch (e) { return; }
  try {
    isSavingCustomer.value = true;


    if (!invoice.value?.id) return;

    await putReceiver(invoice.value.id, customerForm)

    notification.success({ content: 'Cập nhật thông tin người nhật thành công', duration: 3000 })

    fetchInvoice(route.query.q as string)

    isOpenCustomerForm.value = false
  } catch {
    notification.error({ content: 'Cập nhật thông tin người nhật thất bại', duration: 3000 })
  }
  finally {
    isSavingCustomer.value = false
  }
}

const handleClickSaveEditQuantity = async () => {
  if (!invoice.value) return;
  try {
    const totalAmount = updateProductDetails.value.reduce((total, it) => total + it.totalAmount, 0);
    await putInvoiceDetail(invoice.value?.id, {
      totalAmount,
      updateProductDetails: _.concat(updateProductDetails.value, removeProductDetails.value)
        .filter((it, index) => (index < invoiceDetails.value.length && it.quantity !== invoiceDetails.value[index].quantity) || it.isAddByChange || it.quantity === 0)
        .map(it => ({
          ..._.pick(it, ['giaGoc', 'idProductDetail', 'idInvoiceDetail', 'quantity', 'price', 'totalAmount'])
        }))
    })
  } catch (e) {
    notification.error({ content: 'Thay đổi số lượng sản phẩm thất bại', duration: 3000 })
  } finally {
    isEditQuantityProduct.value = false
    fetchInvoice(route.query.q as string)
  }
}

const handleClickCancelSaveEditQuantity = async () => {
  isEditQuantityProduct.value = false
  fetchInvoice(route.query.q as string)
}

const handleUpdateQuantity = async (idInvoiceDetail: string, quantity: number) => {
  const item = updateProductDetails.value.find(it => it.idInvoiceDetail === idInvoiceDetail)
  const invoiceDetail = invoiceDetails.value.find(it => it.id === idInvoiceDetail)
  if (!item || !invoiceDetail) return;

  if (quantity === 0) {
    const indexInvoiceDetail = invoiceDetails.value.findIndex(it => it.id === idInvoiceDetail);

    if (indexInvoiceDetail !== -1) {
      invoiceDetails.value.splice(indexInvoiceDetail, 1);
      updateProductDetails.value.splice(indexInvoiceDetail, 1);

      item.quantity = 0;
      removeProductDetails.value.push(item);

      const lastProduct = _.findLast(updateProductDetails.value, it => it.idProductDetail === item.idProductDetail)

      if (lastProduct) {
        lastProduct.itNotEdit = false;
      }
    }
    return;
  }

  if (item.quantity > quantity) item.quantity = quantity;
  if (item.quantity < quantity) {
    const productDetailRes = await getProductDetailById(item.idProductDetail);

    const productDetail = productDetailRes.data
    const salePrice = productDetail.percentage ? (
      productDetail.percentage < 100 ? productDetail.price * (100 - productDetail.percentage) / 100 : productDetail.price - productDetail.percentage
    ) : productDetail.price;
    if (salePrice > invoiceDetail.price) {
      dialog.warning({
        title: 'Giá của sản phẩm thay đổi',
        content: `Giá của sản phẩm đã tăng từ ${formatCurrency(invoiceDetail.price)} lên ${formatCurrency(salePrice)}`,
        positiveText: 'Xác nhận',
        negativeText: 'Hủy',
        draggable: true,
        onPositiveClick: () => {
          const quantityNewInvoiceDetaiy = quantity - invoiceDetail.quantity;
          updateProductDetails.value.push({
            idInvoiceDetail: '',
            idProductDetail: productDetail.id ?? item.idProductDetail,
            giaGoc: productDetail.price,
            price: salePrice,
            totalAmount: salePrice * quantityNewInvoiceDetaiy,
            quantity: quantityNewInvoiceDetaiy,
            isAddByChange: true,
            itNotEdit: false,
          })

          item.itNotEdit = true;

          const indexInvoiceDetail = invoiceDetails.value.findIndex(it => it.id === idInvoiceDetail);
          invoiceDetails.value.splice(indexInvoiceDetail + 1, 0, {
            ...invoiceDetails.value[indexInvoiceDetail],
            id: '',
            giaGoc: productDetail.price,
            price: salePrice,
            totalAmount: salePrice * quantityNewInvoiceDetaiy,
            quantity: quantityNewInvoiceDetaiy,
          })
        },
      })
    } else {
      item.quantity = quantity
    }
  }

  item.totalAmount = item.price * item.quantity
}

// Lifecycle
onMounted(() => {
  fetchInvoice(route.query.q as string)
})
</script>

<style scoped>
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

.qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border: none;
  cursor: pointer;
  color: #475569;
  transition: all 0.2s;
}

.qty-btn:hover:not(:disabled) {
  background: #18a058;
  color: white;
}

.qty-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
