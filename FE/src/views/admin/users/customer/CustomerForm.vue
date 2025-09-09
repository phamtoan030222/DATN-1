<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInst, FormItemRule, SelectOption, UploadFileInfo } from 'naive-ui'
import { NBadge, NButton, NIcon, NPopconfirm, NSpace, NSwitch, NUpload, useMessage } from 'naive-ui'
import type { Customer } from '@/service/api/admin/users/customer/customer'
import type { Address } from '@/service/api/admin/users/customer/address'
import { Icon } from '@iconify/vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createCustomer,
  getCustomerById,
  updateCustomer,
  uploadAvatar,
} from '@/service/api/admin/users/customer/customer'
import {
  createAddress,
  deleteAddress as deleteAddressApi,
  getAddressesByCustomer,
  setDefaultAddress as setDefaultAddressApi,
  updateAddress,
} from '@/service/api/admin/users/customer/address'

const GENDER = {
  MALE: 'true',
  FEMALE: 'false',
} as const

const GENDER_OPTIONS: SelectOption[] = [
  { label: 'Nam', value: GENDER.MALE },
  { label: 'Nữ', value: GENDER.FEMALE },
]

const FORM_RULES = {
  customerName: [
    {
      validator(_rule: FormItemRule, value: string) {
        if (!value?.trim()) {
          return new Error('Tên khách hàng không được để trống')
        }

        const trimmedValue = value.trim()
        if (trimmedValue.length < 2) {
          return new Error('Tên khách hàng phải có ít nhất 2 ký tự')
        }
        if (trimmedValue.length > 100) {
          return new Error('Tên khách hàng không được vượt quá 100 ký tự')
        }

        const nameRegex = /^[a-zA-ZÀ-ỹ\s0-9]+$/
        if (!nameRegex.test(trimmedValue)) {
          return new Error('Tên khách hàng không được chứa ký tự đặc biệt')
        }

        if (!/[a-zA-ZÀ-ỹ0-9]/.test(trimmedValue)) {
          return new Error('Tên khách hàng không hợp lệ')
        }

        return true
      },
      trigger: ['blur', 'input'],
    },
  ],

  customerPhone: [
    {
      validator(_rule: FormItemRule, value: string) {
        if (!value?.trim()) {
          return new Error('Số điện thoại không được để trống')
        }

        const cleanPhone = value.replace(/[\s\-()+]/g, '')
        const phoneRegex = /^(03|05|07|08|09)\d{8,9}$/
        if (!phoneRegex.test(cleanPhone)) {
          return new Error('Số điện thoại không hợp lệ. Định dạng: 03x, 05x, 07x, 08x, 09x theo sau 8-9 số')
        }

        return true
      },
      trigger: ['blur', 'input'],
    },
  ],

  customerGender: {
    required: true,
    message: 'Vui lòng chọn giới tính',
    trigger: 'change',
  },

  customerEmail: [
    {
      required: true,
      validator(_rule: FormItemRule, value: string) {
        if (!value?.trim()) {
          return new Error('Email không được để trống')
        }

        const trimmedValue = value.trim()

        if (trimmedValue.length > 255) {
          return new Error('Email không được vượt quá 255 ký tự')
        }

        const emailRegex = /^[^\s@]+@[^\s@][^\s.@]*\.[^\s@]+$/
        if (!emailRegex.test(trimmedValue)) {
          return new Error('Email không hợp lệ')
        }

        const detailedEmailRegex = /^[\w.%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i
        if (!detailedEmailRegex.test(trimmedValue)) {
          return new Error('Email không hợp lệ')
        }

        return true
      },
      trigger: ['blur', 'input'],
    },
  ],

  customerBirthdayStr: [
    {
      validator(_rule: FormItemRule, value: string | null) {
        if (!value) {
          return new Error('Ngày sinh không được để trống')
        }

        const birthDate = new Date(value)
        const today = new Date()

        if (isNaN(birthDate.getTime())) {
          return new Error('Ngày sinh không hợp lệ')
        }

        if (birthDate >= today) {
          return new Error('Ngày sinh phải là ngày trong quá khứ')
        }

        const age = today.getFullYear() - birthDate.getFullYear()
        if (age > 150) {
          return new Error('Ngày sinh không hợp lý')
        }

        const minYear = today.getFullYear() - 150
        if (birthDate.getFullYear() < minYear) {
          return new Error('Ngày sinh không hợp lý')
        }

        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
}

const ADDRESS_FORM_RULES = {
  provinceCity: {
    required: true,
    validator: (_rule: FormItemRule, value: number | null) => {
      if (!value)
        return new Error('Vui lòng chọn tỉnh/thành phố')
      return true
    },
    trigger: ['change', 'blur'],
  },
  district: {
    required: true,
    validator: (_rule: FormItemRule, value: number | null) => {
      if (!value)
        return new Error('Vui lòng chọn quận/huyện')
      return true
    },
    trigger: ['change', 'blur'],
  },
  wardCommune: {
    required: true,
    validator: (_rule: FormItemRule, value: number | null) => {
      if (!value)
        return new Error('Vui lòng chọn phường/xã')
      return true
    },
    trigger: ['change', 'blur'],
  },
  addressDetail: {
    required: true,
    message: 'Vui lòng nhập địa chỉ chi tiết',
    trigger: 'blur',
  },
}

interface Ward {
  name: string
  name_with_type: string
  code: number
  division_type: string
  codename: string
  district_code: number
}

interface District {
  name: string
  name_with_type: string
  code: number
  division_type: string
  codename: string
  province_code: number
  wards: Ward[]
}

interface Province {
  name: string
  name_with_type: string
  code: number
  division_type: string
  codename: string
  districts: District[]
}

interface CustomerForm {
  customerName: string
  customerPhone: string
  customerEmail: string
  customerGender: string | null
  customerBirthdayStr: string | null
  customerAvatar: string
}

interface ExtendedAddress extends Omit<Address, 'provinceCity' | 'district' | 'wardCommune'> {
  provinceCity?: string | null
  district?: string | null
  wardCommune?: string | null
  ward?: string
}

const message = useMessage()
const router = useRouter()
const route = useRoute()

const customerId = computed(() => route.params.id as string | undefined)
const isEditMode = computed(() => !!customerId.value)

const formRef = ref<FormInst | null>(null)
const addressFormsRef = ref<FormInst[]>([])
const uploadRef = ref()

const loading = ref(false)
const submitting = ref(false)
const addressSubmitting = ref(false)
const addressDataLoading = ref(false)

const addressData = ref<Province[]>([])

const form = reactive<CustomerForm>({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  customerGender: GENDER.MALE,
  customerBirthdayStr: null,
  customerAvatar: '',
})

const avatarState = reactive({
  file: null as File | null,
  uploading: false,
  preview: '' as string,
  url: '' as string,
})

const addresses = ref<ExtendedAddress[]>([])

const avatarSrc = computed(() =>
  avatarState.preview || form.customerAvatar || 'https://www.svgrepo.com/show/452030/avatar-default.svg',
)

const submitButtonText = computed(() =>
  isEditMode.value ? 'Cập nhật' : 'Thêm mới',
)

const provinceOptions = computed(() =>
  addressData.value.map(province => ({
    label: province.name_with_type || province.name,
    value: province.codename,
  })),
)

function cleanAndValidateFormData() {
  if (form.customerName) {
    form.customerName = form.customerName.trim()
      .replace(/\s+/g, ' ')
  }

  if (form.customerPhone) {
    form.customerPhone = form.customerPhone.replace(/[\s\-()+]/g, '')
  }

  if (form.customerEmail) {
    form.customerEmail = form.customerEmail.trim().toLowerCase()
  }
}

function buildCustomerPayload(): Customer {
  cleanAndValidateFormData()

  return {
    customerName: form.customerName?.trim() || '',
    customerPhone: form.customerPhone?.replace(/[\s\-()+]/g, '') || '',
    customerEmail: form.customerEmail?.trim().toLowerCase() || '',
    customerGender: form.customerGender === GENDER.MALE,
    customerBirthday: form.customerBirthdayStr
      ? new Date(form.customerBirthdayStr).getTime()
      : undefined,
    customerAvatar: form.customerAvatar || undefined,
  }
}

async function onSubmit() {
  if (avatarState.uploading && !avatarState.url) {
    return message.warning('Ảnh đang tải lên, vui lòng đợi xíu nhé!')
  }

  if (form.customerAvatar && /^(blob:|data:)/.test(form.customerAvatar)) {
    return message.error('Ảnh chưa tải xong. Vui lòng thử lại!')
  }

  if (!formRef.value)
    return

  try {
    cleanAndValidateFormData()

    await formRef.value.validate()

    if (!form.customerName?.trim()) {
      return message.error('Tên khách hàng không được để trống')
    }

    if (!form.customerPhone?.replace(/[\s\-()+]/g, '')) {
      return message.error('Số điện thoại không được để trống')
    }

    if (!form.customerGender) {
      return message.error('Vui lòng chọn giới tính')
    }

    if (!form.customerBirthdayStr) {
      return message.error('Ngày sinh không được để trống')
    }

    submitting.value = true

    const payload = buildCustomerPayload()

    if (isEditMode.value) {
      await updateCustomer(customerId.value, payload)
      message.success('Cập nhật khách hàng thành công!')
    }
    else {
      const result = await createCustomer(payload)
      if (addresses.value.length > 0 && result.data?.data?.id) {
        const newCustomerId = result.data.data.id
        await saveAllAddresses(newCustomerId)
      }
      message.success('Thêm khách hàng thành công!')
    }

    goBack()
  }
  catch (error: any) {
    console.error('Lỗi khi lưu khách hàng:', error)

    if (error?.response?.status === 400) {
      const errorMessage = error.response?.data?.message || 'Dữ liệu không hợp lệ'
      message.error(errorMessage)
    }
    else {
      message.error('Có lỗi xảy ra khi lưu khách hàng!')
    }
  }
  finally {
    submitting.value = false
  }
}

async function loadAddressData() {
  if (addressData.value.length > 0)
    return

  try {
    addressDataLoading.value = true
    const response = await fetch('https://provinces.open-api.vn/api/?depth=3')

    if (!response.ok)
      throw new Error('Failed to fetch address data')

    const data = await response.json()
    addressData.value = data
  }
  catch (error) {
    console.error('Lỗi khi tải dữ liệu địa chỉ:', error)
    message.error('Không thể tải dữ liệu địa chỉ! Sử dụng dữ liệu dự phòng.')

    addressData.value = [
      {
        name: 'Hà Nội',
        codename: 'ha_noi',
        code: 1,
        division_type: 'thành phố trung ương',
        phone_code: 24,
        districts: [
          {
            name: 'Quận Ba Đình',
            codename: 'quan_ba_dinh',
            code: 1,
            division_type: 'quận',
            province_code: 1,
            wards: [
              {
                name: 'Phường Phúc Xá',
                codename: 'phuong_phuc_xa',
                code: 1,
                division_type: 'phường',
                district_code: 1,
              },
            ],
          },
        ],
      },
      {
        name: 'Hồ Chí Minh',
        codename: 'ho_chi_minh',
        code: 79,
        division_type: 'thành phố trung ương',
        phone_code: 28,
        districts: [
          {
            name: 'Quận 1',
            codename: 'quan_1',
            code: 760,
            division_type: 'quận',
            province_code: 79,
            wards: [
              {
                name: 'Phường Bến Nghé',
                codename: 'phuong_ben_nghe',
                code: 26734,
                division_type: 'phường',
                district_code: 760,
              },
            ],
          },
        ],
      },
    ]
  }
  finally {
    addressDataLoading.value = false
  }
}

function getDistrictOptions(provinceCodename?: string): SelectOption[] {
  if (!provinceCodename)
    return []
  const province = addressData.value.find(p => p.codename === provinceCodename)
  return province?.districts.map(district => ({
    label: district.name_with_type || district.name,
    value: district.codename,
  })) || []
}

function getWardOptions(provinceCodename?: string, districtCodename?: string): SelectOption[] {
  if (!provinceCodename || !districtCodename)
    return []
  const province = addressData.value.find(p => p.codename === provinceCodename)
  const district = province?.districts.find(d => d.codename === districtCodename)
  return district?.wards.map(ward => ({
    label: ward.name_with_type || ward.name,
    value: ward.codename,
  })) || []
}

function onProvinceChange(address: ExtendedAddress, value: number | null) {
  address.district = null
  address.wardCommune = null
}

function onDistrictChange(address: ExtendedAddress, value: number | null) {
  address.wardCommune = null
}

function onDefaultChange(address: ExtendedAddress, isDefault: boolean) {
  if (isDefault) {
    addresses.value.forEach((addr) => {
      if (addr !== address) {
        addr.isDefault = false
      }
    })
  }
}

function formatDate(timestamp?: number): string {
  if (!timestamp)
    return '-'
  const d = new Date(timestamp)
  return d.toLocaleDateString('vi-VN')
}

function resetForm() {
  form.customerName = ''
  form.customerPhone = ''
  form.customerEmail = ''
  form.customerGender = GENDER.MALE
  form.customerBirthdayStr = null
  form.customerAvatar = ''
  resetAvatar()
}

function resetAvatar() {
  if (avatarState.preview) {
    URL.revokeObjectURL(avatarState.preview)
  }
  avatarState.file = null
  avatarState.preview = ''
  avatarState.url = ''
  avatarState.uploading = false
  uploadRef.value?.clear?.()
}

function addNewAddress() {
  const newAddress: ExtendedAddress = {
    provinceCity: null,
    district: null,
    wardCommune: null,
    addressDetail: '',
    isDefault: addresses.value.length === 0,
  }
  addresses.value.push(newAddress)
}

function goBack() {
  router.push('/users/customer')
}

function triggerUpload() {
  uploadRef.value?.$el?.querySelector('input[type="file"]')?.click()
}

async function handleUploadChange({ file }: { file: UploadFileInfo }) {
  const rawFile = file?.file as File | undefined
  if (!rawFile || file.status === 'removed')
    return

  avatarState.preview = URL.createObjectURL(rawFile)
  avatarState.uploading = true
  const stop = message.loading('Đang tải ảnh lên...', 0)

  try {
    const { url } = await uploadAvatar(rawFile)
    if (!url)
      throw new Error('Server không trả về URL ảnh')

    form.customerAvatar = url
    avatarState.url = url
  }
  catch (e: any) {
    console.error('Upload error:', e)
    message.error(e?.message || 'Upload thất bại')
    form.customerAvatar = ''
    avatarState.url = ''
  }
  finally {
    stop()
    avatarState.uploading = false
  }
}

async function loadCustomer() {
  if (!isEditMode.value || !customerId.value)
    return

  try {
    loading.value = true
    const id = customerId.value

    const response = await getCustomerById(id)
    const customer: Customer = response.data.data

    if (!customer)
      throw new Error('Không tìm thấy khách hàng')

    form.customerName = customer.customerName || ''
    form.customerPhone = customer.customerPhone || ''
    form.customerEmail = customer.customerEmail || ''
    form.customerGender = customer.customerGender ? GENDER.MALE : GENDER.FEMALE
    form.customerBirthdayStr = customer.customerBirthday
      ? new Date(customer.customerBirthday).toISOString().slice(0, 10)
      : null
    form.customerAvatar = customer.customerAvatar || ''

    await loadAddresses()
  }
  catch (error) {
    console.error('Lỗi khi tải thông tin khách hàng:', error)
    message.error('Không thể tải thông tin khách hàng!')
    goBack()
  }
  finally {
    loading.value = false
  }
}

async function loadAddresses() {
  if (!isEditMode.value)
    return

  try {
    const response = await getAddressesByCustomer(customerId.value)
    addresses.value = response.data.data || []
  }
  catch (error) {
    console.error('Lỗi khi tải địa chỉ:', error)
    addresses.value = []
  }
}

async function saveAddress(address: ExtendedAddress, index: number) {
  try {
    addressSubmitting.value = true

    const payload: Address = {
      provinceCity: address.provinceCity,
      district: address.district,
      wardCommune: address.wardCommune,
      addressDetail: address.addressDetail,
      isDefault: address.isDefault,
    }

    if (address.id) {
      await updateAddress(customerId.value, address.id, payload)
      message.success('Cập nhật địa chỉ thành công!')
    }
    else {
      const result = await createAddress(customerId.value, payload)
      if (result.data?.data?.id) {
        address.id = result.data.data.id
      }
      message.success('Thêm địa chỉ thành công!')
    }

    await loadAddresses()
  }
  catch (error) {
    console.error('Lỗi khi lưu địa chỉ:', error)
    message.error('Có lỗi xảy ra khi lưu địa chỉ!')
  }
  finally {
    addressSubmitting.value = false
  }
}

async function saveAllAddresses(customerIdParam: string) {
  const promises = addresses.value
    .filter(addr => addr.provinceCity && addr.wardCommune && addr.addressDetail)
    .map(async (address) => {
      const payload: Address = {
        provinceCity: address.provinceCity,
        district: address.district,
        wardCommune: address.wardCommune,
        addressDetail: address.addressDetail,
        isDefault: address.isDefault,
      }

      return createAddress(customerIdParam, payload)
    })

  await Promise.all(promises)
}

async function setDefaultAddress(address: ExtendedAddress) {
  if (!address.id)
    return

  try {
    await setDefaultAddressApi(customerId.value, address.id)
    message.success('Đã đặt làm địa chỉ mặc định!')
    await loadAddresses()
  }
  catch (error) {
    console.error('Lỗi khi đặt địa chỉ mặc định:', error)
    message.error('Có lỗi khi đặt địa chỉ mặc định!')
  }
}

async function handleDeleteAddress(address: ExtendedAddress, index: number) {
  try {
    if (address.id && isEditMode.value) {
      await deleteAddressApi(customerId.value, address.id)
      message.success('Đã xóa địa chỉ!')
    }

    addresses.value.splice(index, 1)

    if (address.isDefault && addresses.value.length > 0) {
      addresses.value[0].isDefault = true
    }
  }
  catch (error) {
    console.error('Lỗi khi xóa địa chỉ:', error)
    message.error('Có lỗi khi xóa địa chỉ!')
  }
}

onMounted(async () => {
  await loadAddressData()

  if (isEditMode.value) {
    await loadCustomer()
  }
  else {
    resetForm()
    addNewAddress()
  }
})
</script>

<template>
  <div class="p-6">
    <!-- Header Section -->
    <n-card>
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NButton text @click="goBack">
            <NIcon size="20">
              <Icon icon="icon-park-outline:left" />
            </NIcon>
          </NButton>
          <NIcon size="24">
            <Icon icon="carbon:user-profile" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            {{ isEditMode ? 'Sửa khách hàng' : 'Thêm khách hàng' }}
          </span>
        </NSpace>
        <span>{{ isEditMode ? 'Cập nhật thông tin khách hàng' : 'Tạo mới khách hàng trong hệ thống' }}</span>
      </NSpace>
    </n-card>

    <!-- Main Form Card -->
    <NCard title="Thông tin khách hàng" class="mt-4">
      <NForm
        ref="formRef" :rules="FORM_RULES" label-placement="left" :label-width="140" :model="form"
        :show-require-mark="false" class="text-base"
      >
        <div class="grid grid-cols-12 gap-6">
          <!-- Avatar Upload - Col 1 -->
          <div class="col-span-12 md:col-span-4 flex flex-col items-center justify-start">
            <div class="relative mb-4">
              <img
                :src="avatarSrc"
                class="w-32 h-32 rounded-xl object-cover cursor-pointer border-2 border-gray-300 hover:border-blue-400 transition-colors shadow-md"
                alt="Avatar" @click="triggerUpload"
              >

              <div
                class="absolute inset-0 bg-black bg-opacity-50 rounded-xl flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity cursor-pointer"
                @click="triggerUpload"
              >
                <div class="text-center text-white">
                  <Icon icon="mdi:camera" class="text-2xl mb-1" />
                  <div class="text-sm font-medium">
                    Đổi ảnh
                  </div>
                </div>
              </div>
            </div>

            <NUpload
              ref="uploadRef" :max="1" accept="image/*" :default-upload="false" :on-change="handleUploadChange"
              :show-file-list="false" style="display: none;"
            />

            <NButton secondary :loading="avatarState.uploading" class="rounded-lg" @click="triggerUpload">
              <template #icon>
                <NIcon>
                  <Icon icon="mdi:upload" />
                </NIcon>
              </template>
              Chọn ảnh đại diện
            </NButton>

            <span class="text-sm text-gray-500 text-center mt-2">
              Khuyến nghị: 300x300px, dưới 2MB
            </span>
          </div>

          <!-- Form Fields Column 2 -->
          <div class="col-span-12 md:col-span-4 space-y-4">
            <NFormItem path="customerName" class="text-base">
              <template #label>
                <span class="text-base font-medium">Họ và tên <span class="text-red-500">*</span></span>
              </template>
              <NInput
                v-model:value="form.customerName" placeholder="Nhập họ và tên khách hàng"
                class="text-base" size="large"
              />
            </NFormItem>

            <NFormItem class="text-base">
              <template #label>
                <span class="text-base font-medium">Ngày sinh <span class="text-red-500">*</span></span>
              </template>
              <NDatePicker
                v-model:formatted-value="form.customerBirthdayStr" value-format="yyyy-MM-dd" type="date"
                class="w-full text-base" :default-value="null" clearable size="large"
              />
            </NFormItem>

            <NFormItem path="customerPhone" class="text-base">
              <template #label>
                <span class="text-base font-medium">Số điện thoại <span class="text-red-500">*</span></span>
              </template>
              <NInput
                v-model:value="form.customerPhone" placeholder="Nhập số điện thoại"
                class="text-base" size="large"
              />
            </NFormItem>
          </div>

          <!-- Form Fields Column 3 -->
          <div class="col-span-12 md:col-span-4 space-y-4">
            <NFormItem path="customerGender" class="text-base">
              <template #label>
                <span class="text-base font-medium">Giới tính <span class="text-red-500">*</span></span>
              </template>
              <NSelect
                v-model:value="form.customerGender" :options="GENDER_OPTIONS" placeholder="Chọn giới tính"
                class="text-base" size="large"
              />
            </NFormItem>

            <NFormItem path="customerEmail" class="text-base">
              <template #label>
                <span class="text-base font-medium">Email <span class="text-red-500">*</span></span>
              </template>
              <NInput
                v-model:value="form.customerEmail" placeholder="Nhập địa chỉ email"
                class="text-base" size="large"
              />
            </NFormItem>
          </div>
        </div>
      </NForm>
    </NCard>

    <!-- Address Management Card -->
    <NCard title="Quản lý địa chỉ" class="mt-4">
      <div class="space-y-4">
        <!-- Existing Addresses -->
        <div v-if="addresses.length > 0" class="space-y-3">
          <div
            v-for="(address, index) in addresses" :key="address.id || index"
            class="border rounded-xl p-4 hover:bg-gray-50 transition-colors shadow-sm"
            :class="{ 'border-blue-300 bg-blue-50': address.isDefault }"
          >
            <NForm
              ref="addressFormsRef" :rules="ADDRESS_FORM_RULES" label-placement="left" :label-width="140"
              :model="address" :show-require-mark="false" class="text-base"
            >
              <div class="grid grid-cols-12 gap-4">
                <div class="col-span-12">
                  <div class="flex items-center justify-between mb-4">
                    <span class="font-semibold text-lg">Địa chỉ {{ index + 1 }}</span>
                    <NSpace size="small">
                      <NBadge v-if="address.isDefault" type="success" text="Mặc định" />

                      <NPopconfirm v-if="isEditMode" @positive-click="handleDeleteAddress(address, index)">
                        <template #trigger>
                          <NButton text type="error" size="small" class="rounded-lg">
                            <NIcon>
                              <Icon icon="mdi:delete" />
                            </NIcon>
                          </NButton>
                        </template>
                        Bạn có chắc chắn muốn xóa địa chỉ này?
                      </NPopconfirm>
                    </NSpace>
                  </div>
                </div>

                <div class="col-span-4">
                  <NFormItem path="provinceCity" class="text-base">
                    <template #label>
                      <span class="text-base font-medium">Tỉnh/Thành phố <span class="text-red-500">*</span></span>
                    </template>
                    <NSelect
                      v-model:value="address.provinceCity" :options="provinceOptions"
                      placeholder="Chọn tỉnh/thành phố" filterable clearable
                      :loading="addressDataLoading" size="large" @update:value="onProvinceChange(address, $event)"
                    />
                  </NFormItem>
                </div>

                <div class="col-span-4">
                  <NFormItem path="district" class="text-base">
                    <template #label>
                      <span class="text-base font-medium">Quận/Huyện <span class="text-red-500">*</span></span>
                    </template>
                    <NSelect
                      v-model:value="address.district" :options="getDistrictOptions(address.provinceCity)"
                      placeholder="Chọn quận/huyện" filterable clearable
                      :disabled="!address.provinceCity || addressDataLoading" size="large" @update:value="onDistrictChange(address, $event)"
                    />
                  </NFormItem>
                </div>

                <div class="col-span-4">
                  <NFormItem path="wardCommune" class="text-base">
                    <template #label>
                      <span class="text-base font-medium">Phường/Xã <span class="text-red-500">*</span></span>
                    </template>
                    <NSelect
                      v-model:value="address.wardCommune"
                      :options="getWardOptions(address.provinceCity, address.district)" placeholder="Chọn phường/xã"
                      filterable clearable :disabled="!address.district || addressDataLoading" size="large"
                    />
                  </NFormItem>
                </div>

                <div class="col-span-8">
                  <NFormItem path="addressDetail" class="text-base">
                    <template #label>
                      <span class="text-base font-medium">Địa chỉ chi tiết <span class="text-red-500">*</span></span>
                    </template>
                    <NInput
                      v-model:value="address.addressDetail" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }"
                      placeholder="Nhập số nhà, tên đường..." size="large"
                    />
                  </NFormItem>
                </div>

                <div class="col-span-4">
                  <NFormItem label="Địa chỉ mặc định" class="text-base">
                    <NSwitch v-model:value="address.isDefault" size="large" @update:value="onDefaultChange(address, $event)">
                      <template #checked />
                      <template #unchecked />
                    </NSwitch>
                  </NFormItem>
                </div>
              </div>

              <!-- Save Address Button -->
              <div v-if="isEditMode" class="flex justify-end mt-4 pt-4 border-t">
                <NButton
                  type="primary" :loading="addressSubmitting" size="large"
                  class="rounded-lg" @click="saveAddress(address, index)"
                >
                  Lưu địa chỉ
                </NButton>
              </div>
            </NForm>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="text-center py-8 text-gray-500">
          <Icon icon="mdi:map-marker-off" class="text-4xl mb-2" />
          <p class="text-lg">
            Chưa có địa chỉ nào
          </p>
          <p class="text-base">
            Nhấn "Thêm địa chỉ" bên dưới để tạo địa chỉ đầu tiên
          </p>
        </div>

        <!-- Add New Address Button - Moved to left -->
        <div class="flex justify-start pt-4 border-t">
          <NButton type="primary" secondary size="large" class="rounded-lg" @click="addNewAddress">
            <template #icon>
              <NIcon>
                <Icon icon="mdi:plus" />
              </NIcon>
            </template>
            Thêm địa chỉ
          </NButton>
        </div>
      </div>
    </NCard>

    <!-- Action Buttons -->
    <div class="flex justify-center gap-4 mt-8 pt-4">
      <NPopconfirm :show-icon="false" @positive-click="onSubmit">
        <template #trigger>
          <NButton type="primary" :loading="submitting" size="large" class="rounded-lg px-8">
            {{ submitButtonText }}
          </NButton>
        </template>
        <div class="max-w-xs">
          <p class="font-semibold mb-2 text-base">
            Xác nhận {{ isEditMode ? 'cập nhật' : 'thêm mới' }}
          </p>
          <p class="text-sm text-gray-600">
            Bạn có chắc chắn muốn {{ isEditMode ? 'cập nhật thông tin' : 'tạo' }} khách hàng
            <strong>"{{ form.customerName }}"</strong> không?
          </p>
        </div>
      </NPopconfirm>
    </div>
  </div>
</template>
