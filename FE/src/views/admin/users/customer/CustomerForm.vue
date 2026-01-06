<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInst, FormItemRule, SelectOption, UploadFileInfo } from 'naive-ui'
import {
  createDiscreteApi,
  NButton,
  NCard,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NPopconfirm,
  NSelect,
  NSwitch,
  NUpload,
} from 'naive-ui'
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
  updateAddress,
} from '@/service/api/admin/users/customer/address'

// --- KHỞI TẠO API ---
const { message, notification } = createDiscreteApi(['message', 'notification'])

// --- STATE ---
const isAvatarUploading = ref(false)
const uploadKey = ref(0)

const GENDER = {
  MALE: 'true',
  FEMALE: 'false',
} as const

const GENDER_OPTIONS: SelectOption[] = [
  { label: 'Nam', value: GENDER.MALE },
  { label: 'Nữ', value: GENDER.FEMALE },
]

// --- VALIDATION RULES ---
const FORM_RULES = {
  customerName: [
    {
      required: true,
      validator(_rule: FormItemRule, value: string) {
        if (!value?.trim())
          return new Error('Tên khách hàng không được để trống')
        const trimmed = value.trim()
        if (trimmed.length < 2 || trimmed.length > 100)
          return new Error('Tên từ 2-100 ký tự')
        if (!/^[a-zA-ZÀ-ỹ\s0-9]+$/.test(trimmed))
          return new Error('Tên chứa ký tự đặc biệt')
        return true
      },
      trigger: ['blur', 'input'],
    },
  ],
  customerPhone: [
    {
      required: true,
      validator(_rule: FormItemRule, value: string) {
        if (!value?.trim())
          return new Error('SĐT không được để trống')
        if (!/^(03|05|07|08|09)\d{8,9}$/.test(value.replace(/[\s\-()+]/g, '')))
          return new Error('Số điện thoại không hợp lệ')
        return true
      },
      trigger: ['blur', 'input'],
    },
  ],
  customerGender: { required: true, message: 'Vui lòng chọn giới tính', trigger: 'change' },
  customerEmail: [
    {
      required: true,
      validator(_rule: FormItemRule, value: string) {
        if (!value?.trim())
          return new Error('Email không được để trống')
        if (!/^[\w.-]+@([\w-]+\.)+[\w-]{2,4}$/.test(value.trim()))
          return new Error('Email không đúng định dạng')
        return true
      },
      trigger: ['blur', 'input'],
    },
  ],
  customerBirthdayStr: [
    {
      required: true,
      validator(_rule: FormItemRule, value: string | null) {
        if (!value)
          return new Error('Chưa chọn ngày sinh')
        const birthDate = new Date(value)
        if (birthDate >= new Date())
          return new Error('Ngày sinh phải trong quá khứ')
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
}

const ADDRESS_FORM_RULES = {
  provinceCity: { required: true, message: 'Chọn Tỉnh/Thành phố', trigger: ['change'] },
  district: { required: true, message: 'Chọn Quận/Huyện', trigger: ['change'] },
  wardCommune: { required: true, message: 'Chọn Phường/Xã', trigger: ['change'] },
  addressDetail: { required: true, message: 'Nhập địa chỉ chi tiết', trigger: 'blur' },
}

// Interfaces
interface Ward { name: string, name_with_type: string, code: number, division_type: string, codename: string, district_code: number }
interface District { name: string, name_with_type: string, code: number, division_type: string, codename: string, province_code: number, wards: Ward[] }
interface Province { name: string, name_with_type: string, code: number, division_type: string, codename: string, districts: District[] }
interface CustomerForm { customerName: string, customerPhone: string, customerEmail: string, customerGender: string | null, customerBirthdayStr: string | null, customerAvatar: string }
interface ExtendedAddress extends Omit<Address, 'provinceCity' | 'district' | 'wardCommune'> { provinceCity?: string | null, district?: string | null, wardCommune?: string | null, ward?: string }

const router = useRouter()
const route = useRoute()
const customerId = computed(() => route.params.id as string | undefined)
const isEditMode = computed(() => !!customerId.value)

const formRef = ref<FormInst | null>(null)
const addressFormsRef = ref<FormInst[]>([])

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
  preview: '' as string,
  url: '' as string,
})

const addresses = ref<ExtendedAddress[]>([])

const avatarSrc = computed(() =>
  avatarState.preview || form.customerAvatar || 'https://www.svgrepo.com/show/452030/avatar-default.svg',
)

const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật' : 'Thêm mới')

const provinceOptions = computed(() =>
  addressData.value.map(province => ({ label: province.name_with_type || province.name, value: province.codename })),
)

// Helper Functions
function cleanAndValidateFormData() {
  if (form.customerName)
    form.customerName = form.customerName.trim().replace(/\s+/g, ' ')
  if (form.customerPhone)
    form.customerPhone = form.customerPhone.replace(/[\s\-()+]/g, '')
  if (form.customerEmail)
    form.customerEmail = form.customerEmail.trim().toLowerCase()
}

function buildCustomerPayload(): Customer {
  cleanAndValidateFormData()
  return {
    customerName: form.customerName?.trim() || '',
    customerPhone: form.customerPhone?.replace(/[\s\-()+]/g, '') || '',
    customerEmail: form.customerEmail?.trim().toLowerCase() || '',
    customerGender: form.customerGender === GENDER.MALE,
    customerBirthday: form.customerBirthdayStr ? new Date(form.customerBirthdayStr).getTime() : undefined,
    customerAvatar: form.customerAvatar || undefined,
  }
}

// Upload Logic
async function handleUploadChange({ file }: { file: UploadFileInfo }) {
  const rawFile = file?.file as File | undefined
  if (!rawFile || file.status === 'removed')
    return

  if (rawFile.size > 5 * 1024 * 1024) {
    uploadKey.value++
    return notification.error({ title: 'Lỗi', content: 'Ảnh quá lớn (Max 5MB)', duration: 3000 })
  }

  if (avatarState.preview)
    URL.revokeObjectURL(avatarState.preview)
  avatarState.preview = URL.createObjectURL(rawFile)

  isAvatarUploading.value = true
  let loadingMsg: any = null

  try {
    loadingMsg = message.loading('Đang tải ảnh lên...', { duration: 0 })
    const { url } = await uploadAvatar(rawFile)
    if (!url)
      throw new Error('Không nhận được URL ảnh')

    form.customerAvatar = url
    avatarState.url = url
    notification.success({ title: 'Thành công', content: 'Tải ảnh thành công', duration: 2000 })
  }
  catch (e: any) {
    console.error('Upload error:', e)
    notification.error({ title: 'Lỗi', content: 'Tải ảnh thất bại', duration: 3000 })
  }
  finally {
    isAvatarUploading.value = false
    if (loadingMsg)
      loadingMsg.destroy()
    uploadKey.value++
  }
}

function triggerUpload() {
  const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
  if (fileInput)
    fileInput.click()
}

// Submit Logic
async function onSubmit() {
  if (isAvatarUploading.value) {
    return notification.warning({ title: 'Đợi chút', content: 'Đang tải ảnh lên...', duration: 3000 })
  }

  if (!formRef.value)
    return

  try {
    cleanAndValidateFormData()
    await formRef.value.validate()

    submitting.value = true
    const payload = buildCustomerPayload()

    if (isEditMode.value) {
      await updateCustomer(customerId.value, payload)
      notification.success({ title: 'Thành công', content: 'Cập nhật thành công!', duration: 3000 })
    }
    else {
      const result = await createCustomer(payload)
      if (addresses.value.length > 0 && result.data?.data?.id) {
        await saveAllAddresses(result.data.data.id)
      }
      notification.success({ title: 'Thành công', content: 'Thêm mới thành công!', duration: 3000 })
    }
    goBack()
  }
  catch (errors: any) {
    if (Array.isArray(errors)) {
      notification.warning({ title: 'Dữ liệu không hợp lệ', content: errors[0][0].message, duration: 4000 })
    }
    else if (errors?.response) {
      const msg = errors.response?.data?.message || 'Có lỗi xảy ra'
      notification.error({ title: 'Lỗi hệ thống', content: msg, duration: 4000 })
    }
    else {
      console.error(errors)
    }
  }
  finally {
    submitting.value = false
  }
}

// Address Functions
async function loadAddressData() {
  if (addressData.value.length > 0)
    return
  try {
    addressDataLoading.value = true
    const res = await fetch('https://provinces.open-api.vn/api/?depth=3')
    if (!res.ok)
      throw new Error()
    addressData.value = await res.json()
  }
  catch {
    addressData.value = [{ name: 'Hà Nội', codename: 'ha_noi', code: 1, division_type: 'tp', phone_code: 24, districts: [] }]
  }
  finally { addressDataLoading.value = false }
}

function getDistrictOptions(p?: string) { return addressData.value.find(x => x.codename === p)?.districts.map(d => ({ label: d.name, value: d.codename })) || [] }
function getWardOptions(p?: string, d?: string) { return addressData.value.find(x => x.codename === p)?.districts.find(x => x.codename === d)?.wards.map(w => ({ label: w.name, value: w.codename })) || [] }
function onProvinceChange(a: ExtendedAddress, v: any) { a.district = null; a.wardCommune = null }
function onDistrictChange(a: ExtendedAddress, v: any) { a.wardCommune = null }
function onDefaultChange(a: ExtendedAddress, v: boolean) {
  if (v) {
    addresses.value.forEach((x) => {
      if (x !== a)
        x.isDefault = false
    })
  }
}
function resetForm() {
  Object.assign(form, { customerName: '', customerPhone: '', customerEmail: '', customerGender: GENDER.MALE, customerBirthdayStr: null, customerAvatar: '' })
  if (avatarState.preview)
    URL.revokeObjectURL(avatarState.preview)
  avatarState.preview = ''; avatarState.url = ''
}
function addNewAddress() { addresses.value.push({ provinceCity: null, district: null, wardCommune: null, addressDetail: '', isDefault: addresses.value.length === 0 }) }
function goBack() { router.push('/users/customer') }

async function loadCustomer() {
  if (!isEditMode.value || !customerId.value)
    return
  try {
    loading.value = true
    const res = await getCustomerById(customerId.value)
    const data = res.data.data
    Object.assign(form, {
      customerName: data.customerName,
      customerPhone: data.customerPhone,
      customerEmail: data.customerEmail,
      customerGender: data.customerGender ? GENDER.MALE : GENDER.FEMALE,
      customerBirthdayStr: data.customerBirthday ? new Date(data.customerBirthday).toISOString().slice(0, 10) : null,
      customerAvatar: data.customerAvatar || '',
    })
    await loadAddresses()
  }
  catch (e) { message.error('Lỗi tải dữ liệu'); goBack() }
  finally { loading.value = false }
}

async function loadAddresses() {
  if (!isEditMode.value)
    return
  try {
    const response = await getAddressesByCustomer(customerId.value!)
    let data = response.data.data || []
    const defaultItems = data.filter((a: any) => a.isDefault)
    if (defaultItems.length > 1) {
      let foundFirst = false
      data = data.map((addr: any) => {
        if (addr.isDefault) {
          if (!foundFirst) { foundFirst = true; return addr }
          else { return { ...addr, isDefault: false } }
        }
        return addr
      })
    }
    addresses.value = data
  }
  catch (error) {
    console.error('Lỗi khi tải địa chỉ:', error)
    addresses.value = []
  }
}

async function saveAddress(address: ExtendedAddress, index: number) {
  try {
    const currentForm = addressFormsRef.value[index]
    if (currentForm)
      await currentForm.validate()

    addressSubmitting.value = true
    const payload: Address = {
      provinceCity: address.provinceCity!,
      district: address.district!,
      wardCommune: address.wardCommune!,
      addressDetail: address.addressDetail!,
      isDefault: address.isDefault,
    }

    if (address.id) {
      await updateAddress(customerId.value!, address.id, payload)
    }
    else {
      const res = await createAddress(customerId.value!, payload)
      if (res.data?.data?.id)
        address.id = res.data.data.id
    }

    if (address.isDefault && address.id) {
      const otherAddresses = addresses.value.filter(a => a !== address && a.id)
      await Promise.all(otherAddresses.map((otherAddr) => {
        return updateAddress(customerId.value!, otherAddr.id!, {
          provinceCity: otherAddr.provinceCity!,
          district: otherAddr.district!,
          wardCommune: otherAddr.wardCommune!,
          addressDetail: otherAddr.addressDetail!,
          isDefault: false,
        })
      }))
    }
    notification.success({ title: 'Thành công', content: 'Lưu địa chỉ thành công!', duration: 2000 })
    await loadAddresses()
  }
  catch (errors: any) {
    if (Array.isArray(errors))
      notification.warning({ title: 'Lỗi địa chỉ', content: errors[0][0].message, duration: 3000 })
    else notification.error({ title: 'Lỗi', content: 'Không lưu được địa chỉ', duration: 3000 })
  }
  finally { addressSubmitting.value = false }
}

async function handleDeleteAddress(address: ExtendedAddress, index: number) {
  try {
    if (address.id && isEditMode.value) {
      await deleteAddressApi(customerId.value!, address.id)
      notification.success({ title: 'Đã xóa', content: 'Xóa địa chỉ thành công', duration: 2000 })
    }
    addresses.value.splice(index, 1)
    if (address.isDefault && addresses.value.length > 0)
      addresses.value[0].isDefault = true
  }
  catch { notification.error({ title: 'Lỗi', content: 'Không xóa được địa chỉ' }) }
}

async function saveAllAddresses(id: string) {
  const validAddresses = addresses.value.filter(a => a.provinceCity && a.wardCommune && a.addressDetail)
  await Promise.all(validAddresses.map(a => createAddress(id, {
    provinceCity: a.provinceCity!,
    district: a.district!,
    wardCommune: a.wardCommune!,
    addressDetail: a.addressDetail!,
    isDefault: a.isDefault,
  })))
}

onMounted(async () => {
  await loadAddressData()
  if (isEditMode.value) { await loadCustomer() }
  else { resetForm(); addNewAddress() }
})
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <NButton circle secondary type="default" size="small" @click="goBack">
          <template #icon>
            <NIcon><Icon icon="mdi:arrow-left" /></NIcon>
          </template>
        </NButton>
        <div class="flex items-center gap-2">
          <NIcon size="24" class="text-green-600">
            <Icon icon="carbon:user-profile" />
          </NIcon>
          <span class="font-bold text-xl text-gray-800">{{ isEditMode ? 'Sửa khách hàng' : 'Thêm khách hàng' }}</span>
        </div>
      </div>
    </div>

    <NCard title="Thông tin chung" class="shadow-sm rounded-lg border border-gray-100" content-style="padding: 24px;">
      <NForm
        ref="formRef"
        :rules="FORM_RULES"
        :model="form"
        :show-feedback="false"
        label-placement="top"
        class="text-base"
      >
        <div class="grid grid-cols-12 gap-8">
          <div class="col-span-12 md:col-span-3 flex flex-col items-center">
            <div class="relative group">
              <img
                :src="avatarSrc"
                class="w-40 h-40 rounded-full object-cover border-4 border-gray-100 shadow-md group-hover:border-green-100 transition-all cursor-pointer"
                alt="Avatar"
                @click="triggerUpload"
              >
              <div
                class="absolute inset-0 bg-black bg-opacity-40 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                @click="triggerUpload"
              >
                <div class="text-center text-white">
                  <Icon icon="mdi:camera" class="text-3xl mb-1" />
                  <div class="text-xs font-medium">
                    Đổi ảnh
                  </div>
                </div>
              </div>
            </div>

            <div class="mt-4 flex flex-col items-center">
              <NUpload
                ref="uploadRef"
                :key="uploadKey"
                :max="1"
                accept="image/*"
                :default-upload="false"
                :on-change="handleUploadChange"
                :show-file-list="false"
                style="display: none;"
              />
              <NButton secondary size="small" :loading="isAvatarUploading" @click="triggerUpload">
                Chọn ảnh
              </NButton>
              <span class="text-xs text-gray-400 mt-2">Dưới 2MB (300x300px)</span>
            </div>
          </div>

          <div class="col-span-12 md:col-span-9">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
              <NFormItem path="customerName" label="Họ và tên">
                <NInput v-model:value="form.customerName" placeholder="Nhập họ và tên" size="large" />
              </NFormItem>

              <NFormItem path="customerPhone" label="Số điện thoại">
                <NInput v-model:value="form.customerPhone" placeholder="Nhập số điện thoại" size="large" />
              </NFormItem>

              <NFormItem path="customerEmail" label="Email">
                <NInput v-model:value="form.customerEmail" placeholder="example@email.com" size="large" />
              </NFormItem>

              <NFormItem label="Ngày sinh" path="customerBirthdayStr">
                <NDatePicker
                  v-model:formatted-value="form.customerBirthdayStr"
                  value-format="yyyy-MM-dd"
                  type="date"
                  class="w-full"
                  size="large"
                  placeholder="Nhập ngày, tháng, năm sinh"
                />
              </NFormItem>

              <NFormItem path="customerGender" label="Giới tính">
                <NSelect
                  v-model:value="form.customerGender"
                  :options="GENDER_OPTIONS"
                  size="large"
                  placeholder="Chọn giới tính"
                />
              </NFormItem>
            </div>
          </div>
        </div>
      </NForm>
    </NCard>

    <NCard title="Danh sách địa chỉ" class="mt-6 shadow-sm rounded-lg border border-gray-100">
      <div class="space-y-4">
        <div v-if="addresses.length > 0" class="space-y-4">
          <div
            v-for="(address, index) in addresses" :key="address.id || index"
            class="border border-gray-200 rounded-lg p-5 bg-gray-50 relative hover:shadow-md transition-shadow"
            :class="{ 'ring-2 ring-green-500 ring-opacity-50 bg-green-50': address.isDefault }"
          >
            <div v-if="address.isDefault" class="absolute top-0 right-0 bg-green-600 text-white text-xs px-3 py-1 rounded-bl-lg rounded-tr-lg font-medium shadow-sm">
              Mặc định
            </div>

            <div v-if="isEditMode" class="absolute top-4 right-4" :class="{ 'top-8': address.isDefault }">
              <NPopconfirm
                positive-text="Đồng ý"
                negative-text="Quay lại"
                @positive-click="handleDeleteAddress(address, index)"
              >
                <template #trigger>
                  <NButton text type="error" class="hover:bg-red-100 p-1 rounded">
                    <NIcon size="20">
                      <Icon icon="mdi:trash-can-outline" />
                    </NIcon>
                  </NButton>
                </template>
                Bạn chắc chắn muốn xóa địa chỉ này?
              </NPopconfirm>
            </div>

            <NForm
              ref="addressFormsRef"
              :rules="ADDRESS_FORM_RULES"
              :model="address"
              :show-feedback="false"
              label-placement="top"
              class="text-base"
            >
              <div class="grid grid-cols-12 gap-4">
                <div class="col-span-12 font-semibold text-gray-700 mb-2 flex items-center gap-2">
                  <Icon icon="mdi:map-marker" class="text-gray-400" /> Địa chỉ {{ index + 1 }}
                </div>

                <div class="col-span-12 md:col-span-4">
                  <NFormItem path="provinceCity" label="Tỉnh/Thành phố">
                    <NSelect
                      v-model:value="address.provinceCity"
                      :options="provinceOptions"
                      filterable clearable
                      :loading="addressDataLoading"
                      placeholder="Chọn Tỉnh/Thành phố"
                      @update:value="onProvinceChange(address, $event)"
                    />
                  </NFormItem>
                </div>
                <div class="col-span-12 md:col-span-4">
                  <NFormItem path="district" label="Quận/Huyện">
                    <NSelect
                      v-model:value="address.district"
                      :options="getDistrictOptions(address.provinceCity)"
                      filterable clearable
                      :disabled="!address.provinceCity"
                      placeholder="Chọn Quận/Huyện"
                      @update:value="onDistrictChange(address, $event)"
                    />
                  </NFormItem>
                </div>
                <div class="col-span-12 md:col-span-4">
                  <NFormItem path="wardCommune" label="Phường/Xã">
                    <NSelect
                      v-model:value="address.wardCommune"
                      :options="getWardOptions(address.provinceCity, address.district)"
                      filterable clearable
                      :disabled="!address.district"
                      placeholder="Chọn Phường/Xã"
                    />
                  </NFormItem>
                </div>
                <div class="col-span-12">
                  <NFormItem path="addressDetail" label="Địa chỉ chi tiết">
                    <NInput v-model:value="address.addressDetail" type="textarea" :autosize="{ minRows: 2, maxRows: 2 }" placeholder="Số nhà, tên đường..." />
                  </NFormItem>
                </div>

                <div class="col-span-12 flex justify-between items-end border-t pt-3 mt-1">
                  <NFormItem label="Đặt làm mặc định" class="mb-0">
                    <NSwitch v-model:value="address.isDefault" @update:value="onDefaultChange(address, $event)" />
                  </NFormItem>

                  <div v-if="isEditMode">
                    <NButton type="primary" size="small" ghost :loading="addressSubmitting" @click="saveAddress(address, index)">
                      <template #icon>
                        <NIcon><Icon icon="mdi:content-save" /></NIcon>
                      </template>
                      Lưu thay đổi
                    </NButton>
                  </div>
                </div>
              </div>
            </NForm>
          </div>
        </div>

        <div v-else class="text-center py-10 bg-gray-50 rounded-lg border border-dashed border-gray-300">
          <Icon icon="mdi:map-marker-off" class="text-4xl text-gray-300 mb-2" />
          <p class="text-gray-500">
            Chưa có địa chỉ nào được thêm
          </p>
        </div>

        <div class="flex justify-center pt-2">
          <NButton type="primary" secondary dashed class="w-full md:w-auto" @click="addNewAddress">
            <template #icon>
              <NIcon><Icon icon="mdi:plus" /></NIcon>
            </template>
            Thêm địa chỉ mới
          </NButton>
        </div>
      </div>
    </NCard>

    <div class="flex justify-end gap-4 mt-8 pt-4 border-t sticky bottom-0 bg-white z-10 pb-4">
      <NButton size="large" @click="goBack">
        Hủy bỏ
      </NButton>
      <NPopconfirm
        :show-icon="false"
        positive-text="Đồng ý"
        negative-text="Quay lại"
        @positive-click="onSubmit"
      >
        <template #trigger>
          <NButton type="primary" :loading="submitting" size="large" class="px-8 shadow-md">
            {{ submitButtonText }}
          </NButton>
        </template>
        <div class="max-w-xs">
          <p class="font-semibold mb-2 text-base">
            Xác nhận lưu
          </p>
          <p class="text-sm text-gray-600">
            Bạn có chắc chắn muốn lưu thông tin khách hàng <strong>{{ form.customerName }}</strong>?
          </p>
        </div>
      </NPopconfirm>
    </div>
  </div>
</template>
