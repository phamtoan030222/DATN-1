<script setup lang="ts">
import {
  CardOutline,
  CashOutline,
  LocationOutline,
  StorefrontOutline,
  TicketOutline,
} from '@vicons/ionicons5'
import {
  NButton,
  NCard,
  NDivider,
  NGi,
  NGrid,
  NIcon,
  NInput,
  NRadio,
  NRadioGroup,
  NSelect,
  useMessage,
} from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

// Import Store & API
import { CUSTOMER_CART_ID, CUSTOMER_CART_ITEM, USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import type { CartItemResponse } from '@/service/api/client/banhang.api'
import { createOrder, GetGioHang, getMaGiamGia, getProductDetailCart } from '@/service/api/client/banhang.api'
import { getDefaultCustomerAddress } from '@/service/api/client/customer/address.api'
import { localStorageAction } from '@/utils'
import { CartStore } from '@/utils/cartStore'

const router = useRouter()
const message = useMessage()
const processing = ref(false)

// Data
const cartItems = ref<CartItemResponse[]>([])
const userInfo = ref<any>(null) // Dùng any tạm nếu chưa import interface UserInformation

// Form Info
const deliveryType = ref<'GIAO_HANG' | 'TAI_QUAY'>('GIAO_HANG')
const paymentMethod = ref('0')

const customerInfo = ref({
  ten: '',
  sdt: '',
  diaChi: '',
  ghiChu: '',
})

const STORE_ADDRESS = 'Siêu thị My Laptop - 123 Nguyễn Văn Linh, Hải Châu, Đà Nẵng'

function hydrateCustomerInfoFromProfile() {
  const u = userInfo.value
  if (!u)
    return

  if (!customerInfo.value.ten)
    customerInfo.value.ten = u.fullName || ''
  if (!customerInfo.value.sdt)
    customerInfo.value.sdt = u.phone || ''
}

const cartId = ref<string | null>()

// --- ĐỊA CHỈ (Combobox) ---
interface Option {
  label: string
  value: number
  codename: string
}

const provinceOptions = ref<Option[]>([])
const wardOptions = ref<Option[]>([])

const selectedProvinceCode = ref<number | null>(null)
const selectedWardCode = ref<number | null>(null)
const addressDetail = ref('')

const selectedProvinceName = computed(() => provinceOptions.value.find(o => o.value === selectedProvinceCode.value)?.label || '')
const selectedWardName = computed(() => wardOptions.value.find(o => o.value === selectedWardCode.value)?.label || '')

const fullShippingAddress = computed(() => {
  const parts = [
    addressDetail.value?.trim(),
    selectedWardName.value,
    selectedProvinceName.value,
  ].filter(Boolean)
  return parts.join(', ')
})

// Load Tỉnh/Thành từ API
async function loadProvinces() {
  try {
    const res = await axios.get('https://provinces.open-api.vn/api/v2/p/')
    provinceOptions.value = (res.data || []).map((p: any) => ({
      label: p.name,
      value: p.code,
      codename: p.codename,
    }))
  }
  catch (error) {
    console.error(error)
  }
}

// Load Phường/Xã từ API
async function loadWardsByProvince(provinceCode: number) {
  try {
    const res = await axios.get(`https://provinces.open-api.vn/api/v2/p/${provinceCode}?depth=2`)
    const wards = res.data?.wards || []

    wardOptions.value = wards.map((w: any) => ({
      label: w.name,
      value: w.code,
      codename: w.codename,
    }))
  }
  catch (error) {
    console.error(error)
  }
}

// Xử lý sự kiện người dùng tự chọn đổi Tỉnh/Thành
async function handleProvinceChange(code: number | null) {
  selectedWardCode.value = null // Reset Xã
  wardOptions.value = [] // Clear list Xã cũ
  if (code) {
    await loadWardsByProvince(code)
  }
}

// Auto-fill từ Database
async function tryFillDefaultAddressFromDB() {
  const customerId = userInfo.value?.userId
  if (!customerId)
    return

  try {
    const res: any = await getDefaultCustomerAddress(customerId)
    const adr = res?.data || res?.result || res
    if (!adr)
      return

    // Tùy thuộc cấu trúc Entity trả về từ BE, map đúng field name
    // Dựa vào query SQL của bạn: có thể là provinceCity hoặc province, addressDetail hoặc detail
    const dbProvince = adr.provinceCity || adr.province
    const dbWard = adr.wardCommune
    const dbDetail = adr.addressDetail || adr.detail

    if (dbProvince) {
      const matchedProvince = provinceOptions.value.find(p => p.codename === dbProvince)
      if (matchedProvince) {
        selectedProvinceCode.value = matchedProvince.value
        await loadWardsByProvince(matchedProvince.value)

        if (dbWard) {
          const matchedWard = wardOptions.value.find(w => w.codename === dbWard)
          if (matchedWard) {
            selectedWardCode.value = matchedWard.value
          }
        }
      }
    }

    if (dbDetail && !addressDetail.value) {
      addressDetail.value = dbDetail
    }

    if (deliveryType.value === 'GIAO_HANG') {
      customerInfo.value.diaChi = fullShippingAddress.value
    }
  }
  catch (e) {
    console.warn('Không lấy được địa chỉ mặc định:', e)
  }
}

// Đồng bộ diaChi trong payload
watch(fullShippingAddress, (val) => {
  if (deliveryType.value === 'GIAO_HANG') {
    customerInfo.value.diaChi = val
  }
})

watch(selectedProvinceCode, handleProvinceChange)

// Voucher
const selectedVoucher = ref<string | null>(null)
const availableVouchers = ref<any[]>([])
const discountAmount = ref(0)

onMounted(async () => {
  userInfo.value = localStorageAction.get(USER_INFO_STORAGE_KEY)
  hydrateCustomerInfoFromProfile()
  await loadProvinces()
  await tryFillDefaultAddressFromDB()
  loadCart()
})

watch(userInfo, () => {
  hydrateCustomerInfoFromProfile()
})

watch(deliveryType, (type) => {
  if (type === 'TAI_QUAY') {
    customerInfo.value.diaChi = STORE_ADDRESS
  }
  else {
    customerInfo.value.diaChi = fullShippingAddress.value
  }
}, { immediate: true })

async function loadCart() {
  cartId.value = localStorageAction.get(CUSTOMER_CART_ID)
  if (cartId.value) {
    const res = await GetGioHang(cartId.value as string)
    cartItems.value = res.data
  }
  else {
    const cartItem = localStorageAction.get(CUSTOMER_CART_ITEM)
    if (!cartItem)
      return

    const res = await getProductDetailCart(Object.keys(cartItem))
    cartItems.value = res.data.map(productDetail => ({
      ...productDetail,
      id: '',
      productDetailId: productDetail.id,
      quantity: cartItem[productDetail.id],
    })) || []
  }

  if (cartItems.value.length === 0) {
    message.warning('Giỏ hàng trống')
    router.push('/cart')
  }
  else {
    loadVouchers()
  }
}

const subTotal = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.price * item.quantity), 0)
})

const shippingFee = computed(() => deliveryType.value === 'GIAO_HANG' ? 30000 : 0)

const finalTotal = computed(() => {
  const total = subTotal.value + shippingFee.value - discountAmount.value
  return total > 0 ? total : 0
})

const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)

async function loadVouchers() {
  if (subTotal.value <= 0)
    return

  try {
    const res: any = await getMaGiamGia({
      invoiceId: '',
      tongTien: subTotal.value,
      customerId: userInfo?.value?.userId || null,
    })

    const data = res.data || res
    if (data && data.voucherApDung) {
      availableVouchers.value = data.voucherApDung
      if (availableVouchers.value.length > 0 && !selectedVoucher.value) {
        handleSelectVoucher(availableVouchers.value[0].voucherId)
      }
    }
  }
  catch (e) {
    console.error('Lỗi load voucher:', e)
  }
}

function handleSelectVoucher(voucherId: string | null) {
  selectedVoucher.value = voucherId
  if (!voucherId) {
    discountAmount.value = 0
    return
  }

  const voucher = availableVouchers.value.find(v => v.voucherId === voucherId)
  if (voucher) {
    discountAmount.value = voucher.giamGiaThucTe || 0
    message.success(`Đã áp dụng mã: ${voucher.code}`)
  }
}

watch(subTotal, () => {
  loadVouchers()
})

async function handleCheckout() {
  if (!customerInfo.value.ten || !customerInfo.value.sdt) {
    return message.warning('Vui lòng nhập Họ tên và Số điện thoại')
  }
  if (deliveryType.value === 'GIAO_HANG') {
    if (!selectedProvinceCode.value || !selectedWardCode.value || !addressDetail.value.trim())
      return message.warning('Vui lòng chọn đủ Tỉnh/Thành - Phường/Xã và nhập Số nhà, tên đường')
  }

  processing.value = true
  try {
    const productPayload = cartItems.value.map(item => ({
      productDetailId: item.productDetailId,
      quantity: item.quantity,
      price: item.price,
    }))

    const payload = {
      ten: customerInfo.value.ten,
      sdt: customerInfo.value.sdt,
      diaChi: customerInfo.value.diaChi,
      ghiChu: customerInfo.value.ghiChu,

      tongTien: finalTotal.value,
      tienHang: subTotal.value,
      tienShip: shippingFee.value,
      giamGia: discountAmount.value,

      phuongThucThanhToan: paymentMethod.value,
      loaiHoaDon: deliveryType.value,
      idPGG: selectedVoucher.value || null,

      products: productPayload,
    }

    const res: any = await createOrder(payload)

    if (res.status === 200 || res.status === 'OK' || res.data) {
      message.success('Đặt hàng thành công!')
      CartStore.clearCart()
      router.push('/order-success')
    }

    if (!cartId.value) {
      localStorageAction.remove(CUSTOMER_CART_ID)
    }
    else {
      message.error(res.message || 'Đặt hàng thất bại')
    }
  }
  catch (error: any) {
    console.error(error)
    const msg = error.response?.data?.message || 'Có lỗi xảy ra'
    message.error(msg)
  }
  finally {
    processing.value = false
  }
}
</script>

<template>
  <div class="py-8 font-sans">
    <div class="container mx-auto px-4 max-w-6xl">
      <h1 class="text-2xl font-bold mb-6 text-gray-800 border-l-4 border-green-600 pl-3">
        Thanh toán đơn hàng
      </h1>

      <NGrid x-gap="24" cols="1 l:3" responsive="screen">
        <NGi span="2">
          <div class="space-y-6">
            <NCard title="1. Thông tin nhận hàng" size="small" class="shadow-sm">
              <div class="flex gap-4 mb-4">
                <div
                  class="flex-1 p-3 border rounded cursor-pointer flex items-center justify-center gap-2 transition-all"
                  :class="deliveryType === 'GIAO_HANG' ? 'border-green-600 bg-green-50 text-green-700 font-bold ring-1 ring-green-600' : 'hover:bg-gray-50'"
                  @click="deliveryType = 'GIAO_HANG'"
                >
                  <NIcon>
                    <LocationOutline />
                  </NIcon> Giao tận nơi
                </div>
                <div
                  class="flex-1 p-3 border rounded cursor-pointer flex items-center justify-center gap-2 transition-all"
                  :class="deliveryType === 'TAI_QUAY' ? 'border-blue-500 bg-blue-50 text-blue-700 font-bold ring-1 ring-blue-500' : 'hover:bg-gray-50'"
                  @click="deliveryType = 'TAI_QUAY'"
                >
                  <NIcon>
                    <StorefrontOutline />
                  </NIcon> Nhận tại cửa hàng
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <NInput v-model:value="customerInfo.ten" placeholder="Họ và tên người nhận (*)" />
                <NInput v-model:value="customerInfo.sdt" placeholder="Số điện thoại (*)" />

                <!-- ĐỊA CHỈ -->
                <template v-if="deliveryType === 'GIAO_HANG'">
                  <NSelect
                    v-model:value="selectedProvinceCode" :options="provinceOptions" filterable
                    placeholder="Chọn Tỉnh/Thành phố (*)"
                  />
                  <NSelect
                    v-model:value="selectedWardCode" :options="wardOptions" filterable
                    placeholder="Chọn Phường/Xã (*)" :disabled="!selectedProvinceCode"
                  />
                  <NInput v-model:value="addressDetail" placeholder="Số nhà, tên đường... (*)" />

                  <!-- field diaChi vẫn được giữ để submit payload, nhưng lấy từ fullShippingAddress -->
                  <NInput
                    v-model:value="customerInfo.diaChi" placeholder="Địa chỉ đầy đủ" readonly
                    class="md:col-span-2"
                  />
                </template>

                <template v-else>
                  <div class="md:col-span-2">
                    <div class="text-sm text-gray-600 mb-2">
                      Địa chỉ cửa hàng nhận tại quầy
                    </div>
                    <div class="p-3 rounded border bg-gray-50 text-gray-800">
                      {{ STORE_ADDRESS }}
                    </div>
                  </div>
                </template>

                <NInput
                  v-model:value="customerInfo.ghiChu" type="textarea" placeholder="Ghi chú đơn hàng"
                  class="md:col-span-2"
                />
              </div>
            </NCard>

            <NCard title="2. Phương thức thanh toán" size="small" class="shadow-sm">
              <NRadioGroup v-model:value="paymentMethod" name="payment" class="w-full">
                <div class="space-y-3">
                  <div class="border rounded p-3 cursor-pointer hover:bg-gray-50" @click="paymentMethod = '0'">
                    <NRadio value="0" class="w-full">
                      <div class="flex items-center gap-2">
                        <NIcon color="#16a34a">
                          <CashOutline />
                        </NIcon> Thanh toán khi nhận hàng (COD)
                      </div>
                    </NRadio>
                  </div>
                  <div class="border rounded p-3 cursor-pointer hover:bg-gray-50" @click="paymentMethod = '1'">
                    <NRadio value="1" class="w-full">
                      <div class="flex items-center gap-2">
                        <NIcon color="#2563eb">
                          <CardOutline />
                        </NIcon> Chuyển khoản ngân hàng
                      </div>
                    </NRadio>
                  </div>
                </div>
              </NRadioGroup>
            </NCard>
          </div>
        </NGi>

        <NGi>
          <div class="bg-white p-5 rounded-lg shadow-md border sticky top-4">
            <h3 class="font-bold text-lg mb-4 pb-2 border-b">
              Đơn hàng ({{ cartItems.length }} sản phẩm)
            </h3>

            <div class="space-y-3 mb-4 max-h-[300px] overflow-y-auto pr-1">
              <div
                v-for="item in cartItems" :key="item.productDetailId"
                class="flex justify-between text-sm py-2 border-b border-dashed"
              >
                <div class="flex-1 pr-2">
                  <div class="font-medium line-clamp-2">
                    {{ item.name }} {{ item.cpu }} {{ item.ram }} {{ item.hardDrive }}
                  </div>
                  <div class="text-gray-500 text-xs mt-1">
                    SL: <strong>x{{ item.quantity }}</strong>
                  </div>
                </div>
                <div class="text-right">
                  <div class="font-bold text-gray-800">
                    {{ formatCurrency(item.price * item.quantity) }}
                  </div>
                </div>
              </div>
            </div>

            <NDivider />

            <div class="mb-4">
              <div class="text-sm font-medium mb-1 flex items-center gap-1 text-gray-700">
                <NIcon color="#d03050">
                  <TicketOutline />
                </NIcon> Mã ưu đãi
              </div>
              <NSelect
                v-model:value="selectedVoucher" :options="availableVouchers" label-field="code"
                value-field="voucherId" placeholder="Chọn mã giảm giá" clearable
                :render-label="(option: any) => `${option.code} - Giảm ${formatCurrency(option.giamGiaThucTe)}`"
                @update:value="handleSelectVoucher"
              />
            </div>

            <div class="space-y-2 text-sm text-gray-600 bg-gray-50 p-3 rounded">
              <div class="flex justify-between">
                <span>Tạm tính:</span><span class="font-medium">{{ formatCurrency(subTotal) }}</span>
              </div>
              <div class="flex justify-between">
                <span>Phí vận chuyển:</span><span class="font-medium">{{ formatCurrency(shippingFee) }}</span>
              </div>
              <div v-if="discountAmount > 0" class="flex justify-between text-green-600 font-bold">
                <span>Voucher giảm:</span><span>-{{ formatCurrency(discountAmount) }}</span>
              </div>
            </div>

            <div class="flex justify-between items-center mt-4 pt-4 border-t">
              <div>
                <div class="font-bold text-lg text-gray-800">
                  Tổng cộng
                </div>
                <div class="text-xs text-gray-500 font-normal">
                  (Đã bao gồm VAT)
                </div>
              </div>
              <span class="font-bold text-2xl text-green-600">{{ formatCurrency(finalTotal) }}</span>
            </div>

            <NButton
              block type="primary" color="#049d14" size="large"
              class="font-bold h-12 text-lg mt-4 shadow-lg shadow-green-200" :loading="processing"
              @click="handleCheckout"
            >
              ĐẶT HÀNG NGAY
            </NButton>
          </div>
        </NGi>
      </NGrid>
    </div>
  </div>
</template>
