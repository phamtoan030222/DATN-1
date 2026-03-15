<script setup lang="ts">
import {
  AddOutline,
  CashOutline,
  CheckmarkCircleOutline,
  CloseOutline,
  LocationOutline,
  StorefrontOutline,
  TicketOutline,
} from '@vicons/ionicons5'
import axios from 'axios'
import type { FormInst } from 'naive-ui'
import {
  NAlert,
  NButton,
  NCard,
  NConfigProvider,
  NDivider,
  NEmpty,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NIcon,
  NImage,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

// Import Store & API
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { createOrder, createVNPayPayment, getMaGiamGia, getQuantityProdudtDetail, getShippingFeeClient } from '@/service/api/client/banhang.api'

import {
  createAddress,
  deleteAddress,
  getAddressesByCustomer,
  setDefaultAddress,
  updateAddress,
} from '@/service/api/client/customer/address.api'

import { useCartStore } from '@/store/app/cart'
import { localStorageAction } from '@/utils'

const themeOverrides = {
  common: {
    primaryColor: '#16a34a',
    primaryColorHover: '#15803d',
    primaryColorPressed: '#166534',
    primaryColorSuppl: '#16a34a',
  },
  Input: {
    primaryColor: '#16a34a',
    primaryColorHover: '#15803d',
    primaryColorPressed: '#166534',
    primaryColorSuppl: '#16a34a',
    boxShadowFocus: '0 0 0 2px rgba(22, 163, 74, 0.2)',
  },
  Select: {
    peers: {
      InternalSelection: {
        borderHover: '1px solid #16a34a',
        borderFocus: '1px solid #16a34a',
        boxShadowFocus: '0 0 0 2px rgba(22, 163, 74, 0.2)',
      },
    },
  },
}

const router = useRouter()
const message = useMessage()
const processing = ref(false)

const { cartId, cartItems, cartItemBuyNow } = storeToRefs(useCartStore())
const { removeCart } = useCartStore()

const selectedIdsRaw = localStorage.getItem('SELECTED_CART_ITEMS')
const selectedItemIds = selectedIdsRaw ? JSON.parse(selectedIdsRaw) : []

const cartItemsRef = computed(() => {
  if (cartItemBuyNow.value) {
    return [cartItemBuyNow.value]
  }
  return cartItems.value.filter(item => selectedItemIds.includes(item.productDetailId))
})

const totalCheckoutQuantity = computed(() => {
  return cartItemsRef.value.reduce((total, item) => total + (item.quantity || 0), 0)
})

const userInfo = ref<any>(null)

const STORE_ADDRESS = 'Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội'
const deliveryType = ref<'GIAO_HANG' | 'TAI_QUAY'>('GIAO_HANG')
const paymentMethod = ref('0')
const isOpenModalSelectVouchers = ref(false)

const checkoutFormRef = ref<FormInst | null>(null)
const checkoutForm = reactive({
  ten: '',
  sdt: '',
  email: '',
  ghiChu: '',
  provinceName: null as string | null,
  wardName: null as string | null,
  addressDetail: '',
})

const checkoutRules = computed(() => {
  const rules: any = {
    ten: { required: true, message: 'Vui lòng nhập họ và tên', trigger: ['blur', 'input'] },
    sdt: [
      { required: true, message: 'Vui lòng nhập số điện thoại', trigger: ['blur', 'input'] },
      { pattern: /^(03|05|07|08|09)\d{8,9}$/, message: 'Số điện thoại không hợp lệ', trigger: ['blur', 'input'] },
    ],
    email: [
      { required: true, message: 'Vui lòng nhập email', trigger: ['blur', 'input'] },
      { type: 'email', message: 'Email không hợp lệ', trigger: ['blur', 'input'] },
    ],
  }

  if (deliveryType.value === 'GIAO_HANG' && !userInfo.value) {
    rules.provinceName = { required: true, message: 'Vui lòng chọn Tỉnh/Thành phố', trigger: 'change' }
    rules.wardName = { required: true, message: 'Vui lòng chọn Phường/Xã', trigger: 'change' }
    rules.addressDetail = { required: true, message: 'Vui lòng nhập số nhà, tên đường', trigger: ['blur', 'input'] }
  }
  return rules
})

function hydrateCustomerInfoFromProfile() {
  if (userInfo.value) {
    if (!checkoutForm.ten)
      checkoutForm.ten = userInfo.value.fullName || ''
    if (!checkoutForm.sdt)
      checkoutForm.sdt = userInfo.value.phone || ''
    if (!checkoutForm.email)
      checkoutForm.email = userInfo.value.email || ''
  }
}

interface Option { label: string, value: string, code?: number }
const provinceOptions = ref<Option[]>([])

async function loadProvinces() {
  try {
    const res = await axios.get('https://provinces.open-api.vn/api/v2/p/')
    provinceOptions.value = (res.data || []).map((p: any) => ({ label: p.name, value: p.name, code: p.code }))
  }
  catch (e) { console.error(e) }
}

async function fetchWardsFromAPI(provName: string): Promise<Option[]> {
  const p = provinceOptions.value.find(x => x.value === provName)
  if (!p || !p.code)
    return []

  try {
    const res = await axios.get(`https://provinces.open-api.vn/api/v2/p/${p.code}?depth=2`)
    const allWards: Option[] = []

    if (res.data.wards && Array.isArray(res.data.wards)) {
      res.data.wards.forEach((w: any) => {
        if (w && w.name)
          allWards.push({ label: w.name.trim(), value: w.name.trim() })
      })
    }
    else if (res.data.districts && Array.isArray(res.data.districts)) {
      res.data.districts.forEach((d: any) => {
        if (d.wards && Array.isArray(d.wards)) {
          d.wards.forEach((w: any) => {
            allWards.push({ label: `${w.name} (${d.name})`, value: w.name.trim() })
          })
        }
      })
    }
    return allWards
  }
  catch (e) {
    console.error(e)
    return []
  }
}

const guestWardOptions = ref<Option[]>([])
async function onGuestProvinceChange(val: string | null) {
  checkoutForm.wardName = null
  guestWardOptions.value = []
  shippingFee.value = 0 // reset khi đổi tỉnh
  if (val) {
    guestWardOptions.value = await fetchWardsFromAPI(val)
  }
}

const guestFullAddress = computed(() => {
  return [checkoutForm.addressDetail?.trim(), checkoutForm.wardName, checkoutForm.provinceName].filter(Boolean).join(', ')
})

const showAddressModal = ref(false)
const showAddAddressForm = ref(false)
const isFetchingAddresses = ref(false)
const isSavingAddress = ref(false)
const myAddresses = ref<any[]>([])

const selectedAddressId = ref<string | null>(null)
const editingAddressId = ref<string | null>(null)

function formatFullAddress(addr: any) {
  if (!addr)
    return ''
  const p = addr.provinceCity || addr.province || ''
  const w = addr.wardCommune || addr.ward || ''
  const d = addr.addressDetail || addr.detail || ''
  return [d, w, p].filter(Boolean).join(', ')
}

const addressFormRef = ref<FormInst | null>(null)
const newAddress = reactive({
  provinceName: null as string | null,
  wardName: null as string | null,
  detail: '',
  isDefault: false,
})
const modalWardOptions = ref<Option[]>([])

const addressRules = {
  provinceName: { required: true, message: 'Chọn Tỉnh/Thành phố', trigger: 'change' },
  wardName: { required: true, message: 'Chọn Phường/Xã', trigger: 'change' },
  detail: { required: true, message: 'Nhập số nhà, tên đường', trigger: ['blur', 'input'] },
}

async function onModalProvinceChange(val: string | null) {
  newAddress.wardName = null
  modalWardOptions.value = []
  if (val) {
    modalWardOptions.value = await fetchWardsFromAPI(val)
  }
}

function extractArrayFromResponse(res: any): any[] {
  if (!res)
    return []
  if (Array.isArray(res))
    return res
  if (res.data && Array.isArray(res.data))
    return res.data
  if (res.data?.data && Array.isArray(res.data.data))
    return res.data.data
  if (res.data?.content && Array.isArray(res.data.content))
    return res.data.content
  if (res.content && Array.isArray(res.content))
    return res.content
  if (res.items && Array.isArray(res.items))
    return res.items
  return []
}

async function fetchMyAddresses() {
  const customerId = userInfo.value?.userId
  if (!customerId)
    return
  isFetchingAddresses.value = true
  try {
    const res: any = await getAddressesByCustomer(customerId)
    const list = extractArrayFromResponse(res)
    myAddresses.value = list

    if (!selectedAddressId.value && myAddresses.value.length > 0) {
      const defaultAddr = myAddresses.value.find((a: any) => String(a.status) === '1' || a.isDefault === true || a.isDefault === 1)
      if (defaultAddr) {
        selectedAddressId.value = defaultAddr.id
      }
      else {
        selectedAddressId.value = myAddresses.value[0].id
      }
    }
  }
  catch (error) {
    console.error('Lỗi khi fetch địa chỉ:', error)
  }
  finally {
    isFetchingAddresses.value = false
  }
}

async function openAddressModal() {
  if (!userInfo.value)
    return message.info('Vui lòng đăng nhập để quản lý sổ địa chỉ.')
  showAddressModal.value = true
  showAddAddressForm.value = false
  await fetchMyAddresses()
}

function selectAddressFromModal(addr: any) {
  selectedAddressId.value = addr.id
  showAddressModal.value = false
}

function handleOpenAddForm() {
  editingAddressId.value = null
  Object.assign(newAddress, { provinceName: null, wardName: null, detail: '', isDefault: false })
  showAddAddressForm.value = true
}

async function handleEditAddress(addr: any) {
  editingAddressId.value = addr.id
  const pName = addr.provinceCity || addr.province || null
  const wName = addr.wardCommune || addr.ward || null

  newAddress.provinceName = pName

  if (pName) {
    if (wName)
      modalWardOptions.value = [{ label: wName, value: wName }]
    newAddress.wardName = wName
    fetchWardsFromAPI(pName).then((options) => {
      modalWardOptions.value = options
    })
  }
  else {
    newAddress.wardName = null
  }

  newAddress.detail = addr.addressDetail || addr.detail || ''
  newAddress.isDefault = String(addr.status) === '1' || addr.isDefault === true || addr.isDefault === 1
  showAddAddressForm.value = true
}

async function saveNewAddress() {
  try {
    await addressFormRef.value?.validate()
  }
  catch {
    return
  }

  isSavingAddress.value = true
  try {
    const payload = {
      customerId: userInfo.value.userId,
      provinceCity: newAddress.provinceName,
      district: '',
      wardCommune: newAddress.wardName,
      addressDetail: newAddress.detail,
      isDefault: newAddress.isDefault,
      status: newAddress.isDefault ? 1 : 0,
    }

    if (editingAddressId.value) {
      await updateAddress(userInfo.value.userId, editingAddressId.value, payload)
      message.success('Cập nhật địa chỉ thành công')
    }
    else {
      await createAddress(userInfo.value.userId, payload)
      message.success('Thêm địa chỉ thành công')
    }

    showAddAddressForm.value = false
    await fetchMyAddresses()
  }
  catch (error: any) {
    message.error(error.response?.data?.message || 'Lỗi lưu thông tin địa chỉ')
  }
  finally {
    isSavingAddress.value = false
  }
}

async function handleDeleteAddress(id: string) {
  try {
    await deleteAddress(userInfo.value.userId, id)
    message.success('Đã xóa địa chỉ')
    if (selectedAddressId.value === id)
      selectedAddressId.value = null
    await fetchMyAddresses()
  }
  catch (error) { message.error('Không thể xóa địa chỉ này') }
}

async function handleSetDefault(addrId: string) {
  try {
    await setDefaultAddress(userInfo.value.userId, addrId)
    message.success('Đã thiết lập địa chỉ mặc định')
    await fetchMyAddresses()
  }
  catch (error) { message.error('Lỗi khi cài đặt mặc định') }
}

const selectedVoucher = ref<string | null>(null)
const availableVouchers = ref<any[]>([])

const discountAmount = computed(() => {
  if (!selectedVoucher.value)
    return 0

  const voucher = availableVouchers.value.find(v => v.voucherId === selectedVoucher.value)
  if (!voucher)
    return 0

  if (voucher.typeVoucher !== 'PERCENTAGE' && voucher.discountValue > 100) {
    return voucher.discountValue
  }

  // eslint-disable-next-line ts/no-use-before-define
  let calculatedDiscount = subTotal.value * (voucher.discountValue / 100)

  if (voucher.maxValue && voucher.maxValue > 0 && calculatedDiscount > voucher.maxValue) {
    calculatedDiscount = voucher.maxValue
  }

  return calculatedDiscount
})

onMounted(async () => {
  userInfo.value = localStorageAction.get(USER_INFO_STORAGE_KEY)
  hydrateCustomerInfoFromProfile()
  await loadProvinces()
  if (userInfo.value) {
    await fetchMyAddresses()
  }
  loadVouchers()
})

watch(userInfo, () => hydrateCustomerInfoFromProfile())

const subTotal = computed(() => cartItemsRef.value.reduce((t, i) => t + (i.percentage ? (i.price * (1 - i.percentage / 100)) * i.quantity : i.price * i.quantity), 0))
const shippingFee = ref(0)
const isCalculatingShip = ref(false)

const isFreeShipping = computed(() =>
  deliveryType.value === 'GIAO_HANG' && subTotal.value >= 20000000,
)
const finalTotal = computed(() => {
  const total = subTotal.value + shippingFee.value - discountAmount.value
  return total > 0 ? total : 0
})

const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)

async function loadVouchers() {
  if (subTotal.value <= 0)
    return
  try {
    const res: any = await getMaGiamGia({ invoiceId: '', tongTien: subTotal.value, customerId: userInfo?.value?.userId || null })
    const data = res.data || res
    if (data && data.voucherApDung) {
      availableVouchers.value = data.voucherApDung
      if (availableVouchers.value.length > 0 && !selectedVoucher.value) {
        handleSelectVoucher(availableVouchers.value[0].voucherId)
      }
    }
  }
  catch (e) { console.error('Lỗi load voucher:', e) }
}

async function calculateShippingFee() {
  // Không giao hàng hoặc miễn phí ship
  if (deliveryType.value !== 'GIAO_HANG') {
    shippingFee.value = 0
    return
  }

  if (isFreeShipping.value) {
    shippingFee.value = 0
    return
  }

  // Lấy tên tỉnh/phường
  let provinceName = ''
  let wardName = ''

  if (userInfo.value) {
    // Khách đã đăng nhập - lấy từ địa chỉ đã chọn
    const selected = myAddresses.value.find(a => a.id === selectedAddressId.value)
    if (!selected)
      return
    provinceName = selected.provinceCity || selected.province || ''
    wardName = selected.wardCommune || selected.ward || ''
  }
  else {
    // Khách vãng lai - lấy từ form
    if (!checkoutForm.provinceName || !checkoutForm.wardName)
      return
    provinceName = checkoutForm.provinceName
    wardName = checkoutForm.wardName
  }

  if (!provinceName || !wardName)
    return

  try {
    isCalculatingShip.value = true
    const result = await getShippingFeeClient({
      provinceName,
      wardName,
      address: checkoutForm.addressDetail || '',
      weight: cartItemsRef.value.length * 2000, // 2kg/laptop
      orderValue: subTotal.value,
    })

    if (result.success) {
      shippingFee.value = result.fee
      message.success(`Phí vận chuyển: ${formatCurrency(result.fee)}`)
    }
    else {
      shippingFee.value = 30000 // fallback mặc định
    }
  }
  catch (e) {
    shippingFee.value = 30000 // fallback nếu lỗi
  }
  finally {
    isCalculatingShip.value = false
  }
}

function handleSelectVoucher(voucherId: string | null) {
  selectedVoucher.value = voucherId
  if (voucherId) {
    const voucher = availableVouchers.value.find(v => v.voucherId === voucherId)
    if (voucher) {
      message.success(`Đã áp dụng mã: ${voucher.code}`)
    }
  }
}

watch(subTotal, (total) => {
  if (total <= 0) {
    availableVouchers.value = []
    selectedVoucher.value = null
    return
  }
  loadVouchers()
})

// Watch khi khách vãng lai chọn phường/xã
watch(() => checkoutForm.wardName, async (val) => {
  if (val && deliveryType.value === 'GIAO_HANG') {
    await calculateShippingFee()
  }
})

// Watch khi khách đăng nhập đổi địa chỉ
watch(selectedAddressId, async (val) => {
  if (val && deliveryType.value === 'GIAO_HANG') {
    await calculateShippingFee()
  }
})

// Watch khi đổi hình thức giao hàng
watch(deliveryType, async (val) => {
  if (val === 'GIAO_HANG') {
    await calculateShippingFee()
  }
  else {
    shippingFee.value = 0
  }
})

// Watch miễn phí ship
watch(isFreeShipping, (val) => {
  if (val)
    shippingFee.value = 0
})

async function handleCheckout() {
  try {
    await checkoutFormRef.value?.validate()
  }
  catch (errors) {
    message.error('Vui lòng kiểm tra lại thông tin nhận hàng')
    return
  }

  let finalAddressStr = STORE_ADDRESS

  if (deliveryType.value === 'GIAO_HANG') {
    if (userInfo.value) {
      const selected = myAddresses.value.find(a => a.id === selectedAddressId.value)
      if (!selected) {
        message.warning('Vui lòng thêm và chọn địa chỉ giao hàng trong sổ địa chỉ')
        openAddressModal()
        return
      }
      finalAddressStr = formatFullAddress(selected)
    }
    else {
      finalAddressStr = guestFullAddress.value
    }
  }

  processing.value = true
  try {
    const MAX_TOTAL_AMOUNT = 500000000
    if (finalTotal.value > MAX_TOTAL_AMOUNT) {
      message.error(`Tổng thanh toán vượt quá ${formatCurrency(MAX_TOTAL_AMOUNT)}!`)
      processing.value = false
      return
    }

    // Kiểm tra tồn kho
    const productIds = cartItemsRef.value.map(item => item.productDetailId)
    const stockRes = await getQuantityProdudtDetail(productIds)
    const stockData = stockRes.data || []
    const outOfStockErrors: { name: string, stock: number, selected: number }[] = []

    for (const cartItem of cartItemsRef.value) {
      const currentStockItem = stockData.find((s: any) =>
        String(s.id) === String(cartItem.productDetailId)
        || String(s.productDetailId) === String(cartItem.productDetailId)
        || String(s.idProductDetail) === String(cartItem.productDetailId),
      )
      const availableStock = currentStockItem ? (currentStockItem.quantity || 0) : 0
      if (cartItem.quantity > availableStock) {
        outOfStockErrors.push({
          name: `${cartItem.name} ${cartItem.cpu} ${cartItem.ram} ${cartItem.hardDrive}`,
          stock: availableStock,
          selected: cartItem.quantity,
        })
      }
    }

    if (outOfStockErrors.length > 0) {
      message.error(
        () => h('div', { class: 'text-left min-w-[300px]' }, [
          h('div', { class: 'font-bold text-red-600 text-[15px] mb-3' }, `Có ${outOfStockErrors.length} sản phẩm không đủ số lượng:`),
          h('ul', { class: 'space-y-2' }, outOfStockErrors.map(err =>
            h('li', { class: 'bg-red-50 p-2.5 rounded-md border border-red-200' }, [
              h('div', { class: 'font-semibold text-gray-800 text-sm' }, err.name),
              h('div', { class: 'text-[13px] text-gray-600 flex justify-between' }, [
                h('span', null, ['Kho còn: ', h('span', { class: 'font-bold text-red-600' }, err.stock)]),
                h('span', null, ['Bạn chọn: ', h('span', { class: 'font-bold' }, err.selected)]),
              ]),
            ]),
          )),
        ]),
        { duration: 4000 },
      )
      processing.value = false
      return
    }

    // Build payload
    const productPayload = cartItemsRef.value.map(item => ({
      productDetailId: item.productDetailId,
      quantity: item.quantity,
      price: item.percentage ? item.price * (1 - item.percentage / 100) : item.price,
    }))

    const payload = {
      ten: checkoutForm.ten.trim(),
      sdt: checkoutForm.sdt.trim(),
      diaChi: finalAddressStr.trim(),
      ghiChu: checkoutForm.ghiChu ? checkoutForm.ghiChu.trim() : '',
      tongTien: finalTotal.value,
      tienHang: subTotal.value,
      tienShip: shippingFee.value,
      email: checkoutForm.email.trim(),
      giamGia: discountAmount.value,
      phuongThucThanhToan: paymentMethod.value,
      loaiHoaDon: deliveryType.value,
      idPGG: selectedVoucher.value || null,
      products: productPayload,
      cartId: cartId.value || null,
    }

    // Tạo đơn hàng trước
    const res: any = await createOrder(payload)

    if (!(res.status === 200 || res.status === 'OK' || res.data)) {
      message.error('Tạo đơn hàng thất bại!')
      return
    }

    const createdOrder = res.data?.data

    // ===== XỬ LÝ THEO PHƯƠNG THỨC THANH TOÁN =====
    if (paymentMethod.value === '2') {
      // VNPAY → redirect sang trang thanh toán VNPAY
      message.loading('Đang tạo liên kết thanh toán VNPAY...')

      const vnpayRes = await createVNPayPayment({
        orderId: createdOrder?.id || '',
        code: createdOrder?.code || '',
        totalAmountAfterDecrease: finalTotal.value,
        tienHang: subTotal.value,
        tienShip: shippingFee.value,
        idPGG: selectedVoucher.value || null,
        orderType: '250000',
        language: 'vn',
        customerName: checkoutForm.ten.trim(),
        customerPhone: checkoutForm.sdt.trim(),
        customerAddress: finalAddressStr.trim(),
      })

      if (vnpayRes.code === '00' && vnpayRes.paymentUrl) {
        // Xóa giỏ hàng
        if (cartItemBuyNow.value) {
          await removeCart(cartItemBuyNow.value.productDetailId, { buyNow: true })
        }
        else {
          const removeRequests = cartItemsRef.value.map(item => removeCart(item.productDetailId))
          await Promise.all(removeRequests)
          localStorage.removeItem('SELECTED_CART_ITEMS')
        }

        // Lưu mã đơn để dùng sau khi return
        localStorage.setItem('PENDING_ORDER_CODE', createdOrder?.code || '')

        // Redirect sang VNPAY
        window.location.href = vnpayRes.paymentUrl
      }
      else {
        message.error(vnpayRes.message || 'Tạo thanh toán VNPAY thất bại!')
      }
    }
    else {
      // COD, Momo, VietQR → đặt hàng thành công bình thường
      message.success('Đặt hàng thành công!')
      router.push({
        name: 'OrderSuccess',
        query: { 'ma-hoa-don': createdOrder?.code || '' },
      })

      if (cartItemBuyNow.value) {
        await removeCart(cartItemBuyNow.value.productDetailId, { buyNow: true })
      }
      else {
        const removeRequests = cartItemsRef.value.map(item => removeCart(item.productDetailId))
        await Promise.all(removeRequests)
        localStorage.removeItem('SELECTED_CART_ITEMS')
      }
    }
  }
  catch (error: any) {
    message.error(error.response?.data?.message || 'Có lỗi xảy ra trong quá trình đặt hàng')
  }
  finally {
    processing.value = false
  }
}

function formatCurrencyInput(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function parseCurrency(value: string) {
  if (!value)
    return 0
  let str = String(value).replace(/[^0-9nghìtrieu]/g, '').trim().toLowerCase()
  let number = Number.parseInt(str.replace(/\D/g, '')) || 0
  if (str.includes('nghìn'))
    number *= 1000
  else if (str.includes('triệu'))
    number *= 1000000
  return number
}

function handleCloseVoucherModal() {
  isOpenModalSelectVouchers.value = false
}

function handleOpenVoucherModal() {
  if (availableVouchers.value.length === 0) {
    message.info('Không có mã giảm giá nào áp dụng được cho đơn hàng này')
    return
  }
  isOpenModalSelectVouchers.value = true
}

function handleSelectVoucherInModal(voucherId: string) {
  handleSelectVoucher(voucherId)
  isOpenModalSelectVouchers.value = false
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <div class="py-8 font-sans m-0 bg-white min-h-screen">
      <div class="py-8 font-sans m-0 bg-white min-h-screen">
        <div class="container mx-auto px-4 lg:px-8 max-w-[1440px]">
          <h1 class="text-2xl font-bold mb-6 text-gray-800 border-l-4 border-green-600 pl-3">
            Thanh toán đơn hàng
          </h1>

          <NGrid x-gap="24" cols="1 l:3" responsive="screen">
            <NGi span="2">
              <div class="space-y-6">
                <NCard title="1. Hình thức nhận hàng" size="small" class="shadow-sm rounded-xl">
                  <div class="flex gap-4">
                    <div
                      class="flex-1 p-3 border rounded-lg cursor-pointer flex items-center justify-center gap-2 transition-all"
                      :class="deliveryType === 'GIAO_HANG' ? 'border-green-600 bg-green-50 text-green-700 font-bold ring-1 ring-green-600' : 'hover:bg-green-100 border-gray-200'"
                      @click="deliveryType = 'GIAO_HANG'"
                    >
                      <NIcon size="20">
                        <LocationOutline />
                      </NIcon> Giao tận nơi
                    </div>
                    <div
                      class="flex-1 p-3 border rounded-lg cursor-pointer flex items-center justify-center gap-2 transition-all"
                      :class="deliveryType === 'TAI_QUAY' ? 'border-blue-500 bg-blue-50 text-blue-700 font-bold ring-1 ring-blue-500' : 'hover:bg-blue-100 border-gray-200'"
                      @click="deliveryType = 'TAI_QUAY'"
                    >
                      <NIcon size="20">
                        <StorefrontOutline />
                      </NIcon> Nhận tại cửa hàng
                    </div>
                  </div>
                </NCard>

                <NCard title="2. Thông tin người nhận" size="small" class="shadow-sm rounded-xl">
                  <NForm ref="checkoutFormRef" :model="checkoutForm" :rules="checkoutRules" label-placement="top">
                    <div class="text-sm font-bold text-gray-700 mb-2 mt-2">
                      THÔNG TIN LIÊN HỆ
                    </div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
                      <NFormItem path="ten" label="Họ và tên người nhận">
                        <NInput v-model:value="checkoutForm.ten" placeholder="Nhập họ và tên" size="large" />
                      </NFormItem>
                      <NFormItem path="sdt" label="Số điện thoại">
                        <NInput v-model:value="checkoutForm.sdt" placeholder="Nhập số điện thoại" size="large" />
                      </NFormItem>
                      <NFormItem path="email" label="Email" class="md:col-span-2">
                        <NInput v-model:value="checkoutForm.email" placeholder="Nhập email" size="large" />
                      </NFormItem>
                    </div>

                    <NDivider style="margin: 0 0 16px 0;" />

                    <div class="flex justify-between items-center mb-3">
                      <div class="text-sm font-bold text-gray-700">
                        ĐỊA CHỈ NHẬN HÀNG
                      </div>
                      <NButton
                        v-if="deliveryType === 'GIAO_HANG' && userInfo" class="text-blue-600 hover:text-green"
                        text size="small" @click="openAddressModal"
                      >
                        <template #icon>
                          <NIcon size="16">
                            <LocationOutline />
                          </NIcon>
                        </template>
                        Sổ địa chỉ của bạn
                      </NButton>
                    </div>

                    <template v-if="deliveryType === 'GIAO_HANG'">
                      <template v-if="userInfo">
                        <div
                          v-if="selectedAddressId"
                          class="bg-green-50/50 p-4 border border-green-200 rounded-lg relative"
                        >
                          <NTag
                            v-if="String(myAddresses.find(a => a.id === selectedAddressId)?.status) === '1' || myAddresses.find(a => a.id === selectedAddressId)?.isDefault"
                            type="success" size="small" class="mb-2"
                          >
                            Mặc định
                          </NTag>
                          <div class="text-gray-800 font-medium leading-relaxed">
                            {{ formatFullAddress(myAddresses.find(a => a.id === selectedAddressId)) }}
                          </div>
                        </div>
                        <div v-else class="text-sm text-orange-600 bg-orange-50 p-3 rounded border border-orange-200">
                          Vui lòng chọn địa chỉ giao hàng từ <span
                            class="font-bold cursor-pointer underline"
                            @click="openAddressModal"
                          >Sổ địa chỉ</span>.
                        </div>
                      </template>

                      <template v-else>
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
                          <NFormItem path="provinceName" label="Tỉnh/Thành phố">
                            <NSelect
                              v-model:value="checkoutForm.provinceName" :options="provinceOptions" filterable
                              placeholder="Chọn Tỉnh/Thành" size="large" @update:value="onGuestProvinceChange"
                            />
                          </NFormItem>
                          <NFormItem path="wardName" label="Phường/Xã/Thị trấn">
                            <NSelect
                              v-model:value="checkoutForm.wardName" :options="guestWardOptions" filterable
                              placeholder="Chọn Phường/Xã" :disabled="!checkoutForm.provinceName" size="large"
                            />
                          </NFormItem>
                          <div class="md:col-span-2">
                            <NFormItem path="addressDetail" label="Địa chỉ chi tiết">
                              <NInput
                                v-model:value="checkoutForm.addressDetail"
                                placeholder="Nhập Số nhà, tên đường, tòa nhà..." size="large"
                              />
                            </NFormItem>
                          </div>
                        </div>
                      </template>
                    </template>

                    <template v-else>
                      <div class="p-4 rounded-lg border border-blue-200 bg-blue-50 text-blue-800 font-medium">
                        Nhận máy trực tiếp tại: {{ STORE_ADDRESS }}
                      </div>
                    </template>

                    <NDivider style="margin: 4px 0 16px 0;" />

                    <NFormItem path="ghiChu" label="Ghi chú thêm (Tùy chọn)">
                      <NInput
                        v-model:value="checkoutForm.ghiChu" type="textarea"
                        placeholder="Giao hàng trong giờ hành chính..." :autosize="{ minRows: 2, maxRows: 4 }"
                      />
                    </NFormItem>
                  </NForm>
                </NCard>

                <NCard title="3. Phương thức thanh toán" size="small" class="shadow-sm rounded-xl">
                  <NRadioGroup v-model:value="paymentMethod" name="payment" class="w-full">
                    <div class="space-y-3">
                      <div
                        class="border border-gray-200 rounded-lg p-4 cursor-pointer hover:bg-green-50 transition-colors"
                        :class="{ 'ring-1 ring-green-500 border-green-500 bg-green-50/30': paymentMethod === '0' }"
                        @click="paymentMethod = '0'"
                      >
                        <NRadio value="0" class="w-full">
                          <div class="flex items-center gap-3 font-medium text-gray-800">
                            <NIcon color="#16a34a" size="24">
                              <CashOutline />
                            </NIcon>
                            Thanh toán khi nhận hàng (COD)
                          </div>
                        </NRadio>
                      </div>

                      <div
                        class="border border-gray-200 rounded-lg p-4 cursor-pointer hover:bg-green-50 transition-colors"
                        :class="{ 'ring-1 ring-green-500 border-green-500 bg-green-50/30': paymentMethod === '1' }"
                        @click="paymentMethod = '1'"
                      >
                        <NRadio value="1" class="w-full">
                          <div class="flex items-center gap-3 font-medium text-gray-800">
                            <NImage width="25" src="../../../../../images/momo.png" />
                            Momo
                          </div>
                        </NRadio>
                      </div>

                      <div
                        class="border border-gray-200 rounded-lg p-4 cursor-pointer hover:bg-green-50 transition-colors"
                        :class="{ 'ring-1 ring-green-500 border-green-500 bg-green-50/30': paymentMethod === '2' }"
                        @click="paymentMethod = '2'"
                      >
                        <NRadio value="2" class="w-full">
                          <div class="flex items-center gap-3 font-medium text-gray-800">
                            <NImage width="25" src="../../../../../images/vnpay.png" />
                            VNPAY
                          </div>
                        </NRadio>
                      </div>

                      <div
                        class="border border-gray-200 rounded-lg p-4 cursor-pointer hover:bg-green-50 transition-colors"
                        :class="{ 'ring-1 ring-green-500 border-green-500 bg-green-50/30': paymentMethod === '3' }"
                        @click="paymentMethod = '3'"
                      >
                        <NRadio value="3" class="w-full">
                          <div class="flex items-center gap-3 font-medium text-gray-800">
                            <NImage width="40" src="../../../../../images/vietqr.png" />
                            VietQR
                          </div>
                        </NRadio>
                      </div>
                    </div>
                  </NRadioGroup>
                </NCard>
              </div>
            </NGi>

            <NGi>
              <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 sticky top-4">
                <h3 class="font-bold text-lg mb-4 pb-3 border-b text-gray-800">
                  Đơn hàng ({{ totalCheckoutQuantity }} sản phẩm)
                </h3>

                <div class="space-y-1 mb-4 max-h-[350px] overflow-y-auto pr-2 custom-scrollbar">
                  <div
                    v-for="item in cartItemsRef" :key="item.productDetailId"
                    class="py-4 border-b border-dashed border-gray-200 last:border-0 group"
                  >
                    <div class="flex justify-between items-start mb-2">
                      <div class="font-semibold text-gray-800 leading-snug pr-4 text-[15px]">
                        {{ item.name }}
                      </div>
                      <button
                        class="w-6 h-6 flex items-center justify-center shrink-0 rounded-full text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                        @click.stop="removeCart(item.productDetailId, { buyNow: !!cartItemBuyNow })"
                      >
                        <NIcon size="20">
                          <CloseOutline />
                        </NIcon>
                      </button>
                    </div>

                    <div class="flex justify-between items-end">
                      <div class="text-gray-500 text-[13px] px-2 py-1 rounded">
                        Số lượng: <strong class="text-gray-800 text-[14px]">x{{ item.quantity }}</strong>
                      </div>

                      <div class="text-right flex flex-col items-end">
                        <div class="font-bold text-red-600 text-[16px]">
                          {{ formatCurrency((item.percentage && item.percentage > 0 ? item.price * (1 - item.percentage
                            / 100)
                            : item.price) * item.quantity) }}
                        </div>
                        <div v-if="(item.percentage ?? 0) > 0" class="text-[12px] text-gray-500 line-through mt-0.5">
                          {{ formatCurrency((item.price ?? 0) * item.quantity) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="mb-5 mt-4 p-4 bg-green-50 rounded-lg border border-green-200">
                  <div class="text-sm font-semibold mb-2 flex items-center gap-2 text-green-800">
                    <NIcon color="#16a34a" size="18">
                      <TicketOutline />
                    </NIcon> Khuyến mãi
                  </div>
                  <NButton class="w-full font-medium" type="success" dashed @click="handleOpenVoucherModal">
                    {{ selectedVoucher
                      ? `${availableVouchers.find(v => v.voucherId === selectedVoucher)?.code || ''} - Giảm
                    ${formatCurrency(availableVouchers.find(v => v.voucherId === selectedVoucher)?.giamGiaThucTe || 0)}`
                      : 'Chọn mã giảm giá'
                    }}
                  </NButton>
                </div>

                <div class="space-y-3 text-[15px] text-gray-600">
                  <div class="flex justify-between">
                    <span>Tạm tính:</span><span class="font-medium text-gray-800">{{ formatCurrency(subTotal) }}</span>
                  </div>
                  <div class="flex justify-between items-center">
                    <span>Phí vận chuyển:</span>
                    <div class="flex items-center gap-2">
                      <!-- Logo GHTK bên trái -->
                      <img
                        src="https://cdn.haitrieu.com/wp-content/uploads/2022/05/Logo-GHTK-H.png"
                        alt="GHTK"
                        style="height: 18px; width: auto; object-fit: contain; margin-right: 25px;"
                      >

                      <!-- Đang tính phí -->
                      <NSpin v-if="isCalculatingShip" size="small" />

                      <!-- Miễn phí ship -->
                      <span v-else-if="isFreeShipping" class="font-medium text-green-600">
                        Miễn phí
                      </span>

                      <!-- Hiển thị phí tính được -->
                      <span v-else-if="shippingFee > 0" class="font-medium text-gray-800">
                        {{ formatCurrency(shippingFee) }}
                      </span>

                      <!-- Chưa chọn địa chỉ - cho nhập tay -->
                      <NInputNumber
                        v-else
                        v-model:value="shippingFee"
                        :min="0"
                        :formatter="formatCurrencyInput"
                        :parser="parseCurrency"
                        size="small"
                        style="width: 130px"
                        placeholder="Nhập phí ship"
                        :show-button="false"
                      />
                    </div>
                  </div>

                  <NAlert v-if="isFreeShipping" type="success" size="small" show-icon style="margin-top: 8px;">
                    Miễn phí vận chuyển (Đơn hàng trên 20.000.000đ)
                  </NAlert>
                  <div v-if="discountAmount > 0" class="flex justify-between text-green-600 font-bold">
                    <span>Voucher giảm:</span><span>-{{ formatCurrency(discountAmount) }}</span>
                  </div>
                </div>

                <div class="mt-5 pt-4 border-t border-gray-200">
                  <div class="flex justify-between items-end">
                    <div>
                      <div class="font-bold text-lg text-gray-800">
                        Tổng thanh toán
                      </div>
                      <div class="text-xs text-gray-400 font-normal mt-0.5">
                        (Đã bao gồm VAT)
                      </div>
                    </div>
                    <span class="font-black text-2xl text-red-600">{{ formatCurrency(finalTotal) }}</span>
                  </div>
                </div>

                <NPopconfirm
                  :positive-button-props="{ type: 'success' }" positive-text="Xác nhận" negative-text="Hủy"
                  @positive-click="handleCheckout"
                >
                  <template #trigger>
                    <NButton
                      block type="success" size="large"
                      class="font-bold h-12 text-lg mt-6 shadow-md hover:-translate-y-0.5 transition-transform"
                      :loading="processing" :disabled="cartItemsRef.length === 0 || processing"
                    >
                      Đặt hàng ngay
                    </NButton>
                  </template>
                  Bạn chắc chắn muốn thao tác
                </NPopconfirm>
              </div>
            </NGi>
          </NGrid>
        </div>

        <NModal :show="isOpenModalSelectVouchers" @mask-click="handleCloseVoucherModal">
          <NCard
            style="width: 850px; max-width: 95vw;"
            title="Chọn phiếu giảm giá"
            :bordered="false"
            size="huge"
            role="dialog"
            aria-modal="true"
          >
            <template #header-extra>
              <NButton text @click="handleCloseVoucherModal">
                <NIcon size="24">
                  <CloseOutline />
                </NIcon>
              </NButton>
            </template>

            <div :style="{ maxHeight: '500px', overflowY: 'auto' }" class="custom-scrollbar pr-2 py-2">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div
                  v-for="voucher in availableVouchers"
                  :key="voucher.voucherId"
                  class="voucher-ticket relative flex border rounded-md shadow-sm cursor-pointer transition-all duration-200 hover:shadow-md hover:-translate-y-0.5"
                  :class="
                    selectedVoucher === voucher.voucherId
                      ? 'border-[#16a34a] ring-1 ring-[#16a34a] bg-[#f0fdf4] is-active'
                      : 'border-gray-200 bg-white'
                  "
                  @click="handleSelectVoucherInModal(voucher.voucherId)"
                >
                  <div class="flex-1 p-3 pl-4 relative flex flex-col justify-center rounded-l-md">
                    <div
                      v-if="selectedVoucher === voucher.voucherId"
                      class="absolute bottom-2 right-2 text-[#16a34a] bg-white rounded-full flex items-center justify-center shadow-sm z-20"
                    >
                      <NIcon size="26">
                        <CheckmarkCircleOutline />
                      </NIcon>
                    </div>

                    <div
                      class="absolute top-0 left-0 bg-[#16a34a] text-white text-[10px] font-bold px-2 py-0.5 rounded-br-md rounded-tl-md z-10 shadow-sm"
                    >
                      {{ voucher.code }}
                    </div>

                    <div class="mt-4">
                      <h4 class="text-[#16a34a] font-bold text-[15px] truncate mb-1.5 pr-8">
                        {{
                          voucher.ten
                            || (voucher.typeVoucher === 'PERCENTAGE'
                              ? `Giảm giá ${voucher.discountValue}%`
                              : `Giảm ${formatCurrency(voucher.discountValue)}`)
                        }}
                      </h4>

                      <div class="text-[12px] text-gray-500 leading-relaxed pr-8">
                        <div
                          v-if="voucher.typeVoucher === 'PERCENTAGE' && voucher.maxValue && voucher.maxValue > 0"
                          class="font-medium text-gray-700"
                        >
                          Giảm tối đa: {{ formatCurrency(voucher.maxValue) }}
                        </div>
                        <div>Đơn tối thiểu: {{ formatCurrency(voucher.dieuKien || 0) }}</div>
                      </div>
                    </div>
                  </div>

                  <div class="w-[135px] shrink-0 bg-[#00AA00] flex flex-col items-center justify-center text-white relative px-2 rounded-r-md">
                    <div
                      class="voucher-split absolute left-0 top-0 bottom-0 w-[4px] -ml-[2px] border-l-[4px] border-dashed border-white"
                    >
                      <span class="cutout-top" />
                      <span class="cutout-bottom" />
                    </div>

                    <div class="text-[17px] font-bold flex items-baseline justify-center flex-wrap text-center leading-none">
                      <template v-if="voucher.typeVoucher === 'PERCENTAGE'">
                        {{ voucher.discountValue }}<span class="text-sm ml-0.5">%</span>
                      </template>
                      <template v-else>
                        {{ formatCurrency(voucher.discountValue).replace('₫', '').trim() }}
                        <span class="text-sm ml-0.5">đ</span>
                      </template>
                    </div>

                    <div class="text-[10px] uppercase font-bold mt-1.5 tracking-wider opacity-90">
                      SALE OFF
                    </div>
                  </div>
                </div>
              </div>

              <div
                v-if="availableVouchers.length === 0"
                class="text-center text-gray-400 py-12 flex flex-col items-center bg-gray-50 rounded-lg"
              >
                <NIcon size="48" color="#d1d5db">
                  <TicketOutline />
                </NIcon>
                <span class="mt-3 font-medium text-gray-500">Không có mã giảm giá nào phù hợp</span>
              </div>
            </div>
          </NCard>
        </NModal>

        <NModal
          v-model:show="showAddressModal" preset="card" title="Sổ Địa Chỉ Của Bạn" class="text-blue-600"
          style="width: 650px" size="huge"
        >
          <NSpin :show="isFetchingAddresses">
            <div v-if="!showAddAddressForm">
              <div class="flex justify-end mb-4">
                <NButton type="success" dashed @click="handleOpenAddForm">
                  <template #icon>
                    <NIcon>
                      <AddOutline />
                    </NIcon>
                  </template>
                  Thêm địa chỉ mới
                </NButton>
              </div>

              <div v-if="myAddresses.length === 0" class="py-10 text-center">
                <NEmpty description="Bạn chưa có địa chỉ nào lưu sẵn." />
              </div>

              <div v-else class="space-y-4 max-h-[450px] overflow-y-auto pr-2 custom-scrollbar">
                <div
                  v-for="addr in myAddresses" :key="addr.id"
                  class="border p-4 rounded-lg transition-all relative group bg-white"
                  :class="{ 'border-green-500 bg-green-50/30 ring-1 ring-green-200': selectedAddressId === addr.id, 'border-gray-200': selectedAddressId !== addr.id }"
                >
                  <div class="flex justify-between items-start">
                    <div class="flex-1 pr-4">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="font-bold text-gray-800 text-[15px]">{{ userInfo?.fullName }}</span>
                        <span class="text-gray-400">|</span>
                        <span class="text-gray-600">{{ userInfo?.phone }}</span>
                        <NTag
                          v-if="String(addr.status) === '1' || addr.isDefault" type="success" size="small"
                          class="ml-2"
                        >
                          Mặc định
                        </NTag>
                      </div>
                      <div class="text-gray-600 text-sm mt-2 leading-relaxed">
                        {{ formatFullAddress(addr) }}
                      </div>
                    </div>

                    <div class="flex flex-col items-end gap-3 shrink-0">
                      <div class="flex items-center gap-2 text-blue-600">
                        <span
                          class="cursor-pointer hover:underline text-sm font-medium"
                          @click="handleEditAddress(addr)"
                        >Sửa</span>
                        <span v-if="!(String(addr.status) === '1' || addr.isDefault)" class="text-gray-300">|</span>
                        <NPopconfirm
                          v-if="!(String(addr.status) === '1' || addr.isDefault)" positive-text="Xóa"
                          negative-text="Hủy" @positive-click="handleDeleteAddress(addr.id)"
                        >
                          <template #trigger>
                            <span class="cursor-pointer text-red-500 hover:underline text-sm font-medium">Xóa</span>
                          </template>
                          Xóa địa chỉ này khỏi sổ của bạn?
                        </NPopconfirm>
                      </div>

                      <NButton
                        v-if="selectedAddressId !== addr.id" class="text-blue-600" ghost size="small"
                        @click="selectAddressFromModal(addr)"
                      >
                        Giao đến địa chỉ này
                      </NButton>

                      <div
                        v-else
                        class="text-green-600 font-bold flex items-center gap-1 text-sm bg-green-100 px-2 py-1 rounded"
                      >
                        <NIcon size="16">
                          <CheckmarkCircleOutline />
                        </NIcon> Đang chọn
                      </div>

                      <NButton
                        v-if="!(String(addr.status) === '1' || addr.isDefault)" size="tiny" text
                        class="mt-1 opacity-0 group-hover:opacity-100 transition-opacity text-blue-600"
                        @click="handleSetDefault(addr.id)"
                      >
                        Thiết lập mặc định
                      </NButton>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-else>
              <h3 class="font-bold text-gray-800 mb-4 border-l-4 border-green-500 pl-2">
                {{ editingAddressId ? 'Cập Nhật Địa Chỉ' : 'Thêm Địa Chỉ Mới' }}
              </h3>

              <NForm ref="addressFormRef" :model="newAddress" :rules="addressRules" label-placement="top">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
                  <NFormItem path="provinceName" label="Tỉnh/Thành phố">
                    <NSelect
                      v-model:value="newAddress.provinceName" :options="provinceOptions" filterable
                      placeholder="Chọn Tỉnh/Thành" @update:value="onModalProvinceChange"
                    />
                  </NFormItem>
                  <NFormItem path="wardName" label="Phường/Xã/Thị trấn">
                    <NSelect
                      v-model:value="newAddress.wardName" :options="modalWardOptions" filterable
                      placeholder="Chọn Phường/Xã" :disabled="!newAddress.provinceName"
                    />
                  </NFormItem>
                </div>

                <NFormItem path="detail" label="Địa chỉ chi tiết">
                  <NInput v-model:value="newAddress.detail" placeholder="Số nhà, Tên đường, Tòa nhà..." />
                </NFormItem>

                <div class=" text-white relative px-2">
                  <div class="flex justify-end gap-3 pt-4 border-t border-gray-100 mt-6">
                    <NButton @click="showAddAddressForm = false">
                      Trở lại
                    </NButton>

                    <NPopconfirm
                      :positive-button-props="{ type: 'success' }" positive-text="Xác nhận"
                      negative-text="Hủy" @positive-click="saveNewAddress"
                    >
                      <template #trigger>
                        <NButton type="success" :loading="isSavingAddress">
                          Lưu địa chỉ
                        </NButton>
                      </template>
                      Bạn có chắc chắn muốn lưu thông tin địa chỉ này không?
                    </NPopconfirm>
                  </div>
                </div>
              </NForm>
            </div>
          </NSpin>
        </NModal>
      </div>
    </div>
  </NConfigProvider>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  border-radius: 4px;
}

/* --- VẾT CẮT & ĐƯỜNG VIỀN UỐN LƯỢN --- */

.cutout-top,
.cutout-bottom {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 16px;
  background-color: #ffffff; /* Cập nhật lại màu này nếu Nền Modal của bạn có màu khác nhé */
  border-radius: 50%;
  z-index: 20;
}

.cutout-top::after,
.cutout-bottom::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
}

/* ========================================= */
/* TRẠNG THÁI MẶC ĐỊNH (CHƯA CHỌN)           */
/* ========================================= */

.cutout-top { top: -9px; }
.cutout-bottom { bottom: -9px; }

.cutout-top::after {
  clip-path: inset(50% 0 0 0);
}
.cutout-bottom::after {
  clip-path: inset(0 0 50% 0);
}

/* ========================================= */
/* TRẠNG THÁI KHI ĐƯỢC CHỌN (IS-ACTIVE)      */
/* ========================================= */

.is-active .cutout-top { top: -10px; }
.is-active .cutout-bottom { bottom: -10px; }

.is-active .cutout-top::after,
.is-active .cutout-bottom::after {
  border: 2px solid #16a34a;
}
</style>
