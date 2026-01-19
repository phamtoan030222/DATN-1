<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInst, UploadFileInfo } from 'naive-ui'
import {
  createDiscreteApi,
  NButton,
  NCard,
  NDatePicker,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NUpload,
} from 'naive-ui'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'

// Import API
import type { Customer } from '@/service/api/admin/users/customer/customer'
import type { Address } from '@/service/api/admin/users/customer/address'
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
const { message, dialog } = createDiscreteApi(['message', 'dialog'])

// --- STATE ---
const isAvatarUploading = ref(false)
const uploadKey = ref(0)
const GENDER = { MALE: 'true', FEMALE: 'false' } as const

// --- VALIDATION RULES ---
const FORM_RULES = {
  customerName: [
    { required: true, message: 'Tên khách hàng không được để trống', trigger: ['blur', 'input'] },
    { min: 2, max: 100, message: 'Tên từ 2-100 ký tự', trigger: ['blur', 'input'] },
  ],
  customerPhone: [
    { required: true, message: 'SĐT không được để trống', trigger: ['blur', 'input'] },
    { pattern: /^(03|05|07|08|09)\d{8,9}$/, message: 'Số điện thoại không hợp lệ', trigger: ['blur', 'input'] },
  ],
  customerGender: { required: true, message: 'Vui lòng chọn giới tính', trigger: 'change' },
  customerEmail: [
    { required: true, message: 'Email không được để trống', trigger: ['blur', 'input'] },
    { type: 'email', message: 'Email không đúng định dạng', trigger: ['blur', 'input'] },
  ],
  customerBirthdayStr: { required: true, message: 'Chưa chọn ngày sinh', trigger: ['blur', 'change'] },
}

// Validation cho địa chỉ (Đã bỏ district bắt buộc vì API v2 không có)
const ADDRESS_FORM_RULES = {
  provinceCity: { required: true, message: 'Chọn Tỉnh/Thành phố', trigger: ['change'] },
  wardCommune: { required: true, message: 'Chọn Phường/Xã', trigger: ['change'] },
  addressDetail: { required: true, message: 'Nhập địa chỉ chi tiết', trigger: 'blur' },
}

// Interfaces (Cập nhật cho API v2)
interface Province {
  name: string
  name_with_type: string
  code: number
  codename: string
  wards: any[] // API v2: Tỉnh chứa trực tiếp Wards
}

interface CustomerForm { customerName: string, customerPhone: string, customerEmail: string, customerGender: string | null, customerBirthdayStr: string | null, customerAvatar: string }
interface ExtendedAddress extends Omit<Address, 'provinceCity' | 'district' | 'wardCommune'> { provinceCity?: string | null, district?: string | null, wardCommune?: string | null, ward?: string }

const router = useRouter()
const route = useRoute()
const customerId = computed(() => route.params.id as string | undefined)
const isEditMode = computed(() => !!customerId.value)

const formRef = ref<FormInst | null>(null)
const addressFormsRef = ref<FormInst[]>([])

const submitting = ref(false)
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

// --- UPLOAD LOGIC ---
async function handleUploadChange({ file }: { file: UploadFileInfo }) {
  const rawFile = file?.file as File | undefined
  if (!rawFile || file.status === 'removed')
    return
  if (rawFile.size > 5 * 1024 * 1024) {
    uploadKey.value++
    return message.error('Lỗi: Ảnh quá lớn (Max 5MB)')
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
    message.success('Tải ảnh thành công')
  }
  catch (e: any) {
    message.error('Tải ảnh thất bại')
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

// --- ADDRESS LOGIC (Dành riêng cho API v2) ---

// 1. Load danh sách Tỉnh
async function loadAddressData() {
  if (addressData.value.length > 0)
    return
  try {
    addressDataLoading.value = true
    // API v2: Lấy danh sách tỉnh
    const res = await fetch('https://provinces.open-api.vn/api/v2/p/?depth=1')
    if (!res.ok)
      throw new Error()
    addressData.value = await res.json()
  }
  catch {
    addressData.value = [{ name: 'Hà Nội', codename: 'ha_noi', code: 1, wards: [] } as any]
  }
  finally {
    addressDataLoading.value = false
  }
}

// 2. Logic khi chọn Tỉnh -> Tải luôn Wards (API v2 bỏ qua District)
async function onProvinceChange(a: ExtendedAddress, codename: any) {
  a.wardCommune = null // Reset xã
  a.district = null // Reset huyện (dù không dùng)

  if (!codename)
    return

  const index = addressData.value.findIndex(x => x.codename === codename)
  if (index === -1)
    return
  const province = addressData.value[index]

  // Nếu chưa có wards thì gọi API tải về
  if (!province.wards || province.wards.length === 0) {
    try {
      addressDataLoading.value = true
      // API v2: depth=2 lấy luôn wards của tỉnh
      const res = await fetch(`https://provinces.open-api.vn/api/v2/p/${province.code}?depth=2`)
      const data = await res.json()

      // Cập nhật lại dữ liệu
      addressData.value[index] = { ...province, wards: data.wards || [] }
      addressData.value = [...addressData.value] // Trigger reactivity
    }
    catch (e) {
      message.error('Lỗi tải danh sách Phường/Xã')
    }
    finally {
      addressDataLoading.value = false
    }
  }
}

// 3. Hàm lấy Options cho Phường/Xã (Lấy trực tiếp từ Tỉnh)
function getWardOptions(provinceCodename?: string) {
  if (!provinceCodename)
    return []
  const province = addressData.value.find(x => x.codename === provinceCodename)
  // Map trực tiếp từ province.wards (thêm ?. để an toàn)
  return province?.wards?.map((w: any) => ({
    label: w.name,
    value: w.codename,
  })) || []
}

// === LOGIC Toggle Mặc định ===
function onDefaultChange(targetAddress: ExtendedAddress, val: boolean) {
  if (val) {
    addresses.value.forEach((addr) => {
      addr.isDefault = (addr === targetAddress)
    })
  }
  else {
    targetAddress.isDefault = true
    message.warning('Vui lòng chọn địa chỉ khác làm mặc định để thay thế!')
  }
}

function addNewAddress() {
  addresses.value.push({ provinceCity: null, district: null, wardCommune: null, addressDetail: '', isDefault: addresses.value.length === 0 })
}

async function handleDeleteAddress(address: ExtendedAddress, index: number) {
  try {
    if (address.id && isEditMode.value) {
      await deleteAddressApi(customerId.value!, address.id)
      message.success('Xóa địa chỉ thành công')
    }
    addresses.value.splice(index, 1)
    if (address.isDefault && addresses.value.length > 0) {
      addresses.value[0].isDefault = true
    }
  }
  catch { message.error('Lỗi: Không xóa được địa chỉ') }
}

// --- SUBMIT LOGIC ---
async function handleValidateAndSubmit() {
  if (isAvatarUploading.value)
    return message.warning('Vui lòng đợi ảnh tải lên hoàn tất')
  if (!formRef.value)
    return

  try {
    cleanAndValidateFormData()
    await formRef.value.validate()
    if (addresses.value.length > 0) {
      for (const addrForm of addressFormsRef.value) {
        if (addrForm)
          await addrForm.validate()
      }
    }
  }
  catch (errors) {
    return message.error('Vui lòng kiểm tra lại thông tin nhập liệu')
  }

  const actionText = isEditMode.value ? 'cập nhật' : 'thêm mới'
  dialog.warning({
    title: 'Xác nhận',
    content: `Bạn có chắc chắn muốn ${actionText} thông tin này?`,
    positiveText: 'Đồng ý',
    negativeText: 'Hủy',
    positiveButtonProps: { color: '#16a34a', textColor: '#ffffff', style: { fontWeight: 'bold' } },
    onPositiveClick: () => processSubmit(),
  })
}

async function processSubmit() {
  submitting.value = true
  try {
    const payload = buildCustomerPayload()
    let currentCustId = customerId.value

    if (isEditMode.value) {
      await updateCustomer(currentCustId!, payload)
    }
    else {
      const result = await createCustomer(payload)
      if (result.data?.data?.id) {
        currentCustId = result.data.data.id
      }
      else {
        throw new Error('Không lấy được ID khách hàng mới')
      }
    }

    if (addresses.value.length > 0 && currentCustId) {
      await syncAddressesToBackend(currentCustId)
    }

    message.success(isEditMode.value ? 'Cập nhật thành công!' : 'Thêm mới thành công!')
    goBack()
  }
  catch (errors: any) {
    console.error(errors)
    const msg = errors.response?.data?.message || 'Có lỗi xảy ra'
    message.error(msg)
  }
  finally {
    submitting.value = false
  }
}

async function syncAddressesToBackend(custId: string) {
  const sortedAddresses = [...addresses.value].sort((a, b) => {
    return (a.isDefault === b.isDefault) ? 0 : a.isDefault ? 1 : -1
  })

  for (const addr of sortedAddresses) {
    const payload: Address = {
      provinceCity: addr.provinceCity!,
      district: addr.district || '', // Gửi chuỗi rỗng nếu không có district (để tránh lỗi backend nếu cần)
      wardCommune: addr.wardCommune!,
      addressDetail: addr.addressDetail!,
      isDefault: addr.isDefault,
    }

    if (addr.id) {
      await updateAddress(custId, addr.id, payload)
    }
    else {
      await createAddress(custId, payload)
    }
  }
}

function resetForm() {
  Object.assign(form, { customerName: '', customerPhone: '', customerEmail: '', customerGender: GENDER.MALE, customerBirthdayStr: null, customerAvatar: '' })
  if (avatarState.preview)
    URL.revokeObjectURL(avatarState.preview)
  avatarState.preview = ''; avatarState.url = ''
}
function goBack() { router.push('/users/customer') }

async function loadCustomer() {
  if (!isEditMode.value || !customerId.value)
    return
  try {
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
  catch (error) { addresses.value = [] }
}

onMounted(async () => {
  await loadAddressData()
  if (isEditMode.value) { await loadCustomer() }
  else { resetForm(); addNewAddress() }
})
</script>

<template>
  <div class="p-4 md:p-6 bg-gray-50/50 min-h-screen">
    <div class="flex flex-col md:flex-row items-start md:items-center justify-between mb-4 gap-3">
      <div class="flex items-center gap-3">
        <NButton circle secondary type="default" size="medium" @click="goBack">
          <template #icon>
            <NIcon><Icon icon="mdi:arrow-left" /></NIcon>
          </template>
        </NButton>
        <div class="flex items-center gap-2">
          <NIcon size="28" class="text-green-600">
            <Icon icon="carbon:user-profile" />
          </NIcon>
          <span class="font-bold text-xl md:text-2xl text-gray-800">{{ isEditMode ? 'Sửa khách hàng' : 'Thêm khách hàng' }}</span>
        </div>
      </div>
    </div>

    <NCard title="Thông tin chung" class="shadow-sm rounded-xl border border-gray-100" content-style="padding: 24px;">
      <NForm ref="formRef" :rules="FORM_RULES" :model="form" :show-feedback="true" label-placement="top" class="text-base">
        <NGrid x-gap="24" y-gap="16" cols="1 768:12">
          <NGridItem span="1 768:3">
            <div class="flex flex-col items-center">
              <div class="relative group">
                <img :src="avatarSrc" class="w-32 h-32 md:w-40 md:h-40 rounded-full object-cover border-4 border-white shadow-lg cursor-pointer" @click="triggerUpload">
                <div class="absolute inset-0 bg-black bg-opacity-40 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer" @click="triggerUpload">
                  <div class="text-center text-white">
                    <Icon icon="mdi:camera" class="text-3xl mb-1" /><div class="text-xs font-medium">
                      Đổi ảnh
                    </div>
                  </div>
                </div>
              </div>
              <div class="mt-4 flex flex-col items-center">
                <NUpload ref="uploadRef" :key="uploadKey" :max="1" accept="image/*" :default-upload="false" :on-change="handleUploadChange" :show-file-list="false" style="display: none;" />
                <NButton secondary size="small" :loading="isAvatarUploading" @click="triggerUpload">
                  Chọn ảnh
                </NButton>
                <span class="text-xs text-gray-400 mt-2">Max 5MB</span>
              </div>
            </div>
          </NGridItem>

          <NGridItem span="1 768:9">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-2">
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
                <NDatePicker v-model:formatted-value="form.customerBirthdayStr" value-format="yyyy-MM-dd" type="date" class="w-full" size="large" placeholder="Chọn ngày sinh" />
              </NFormItem>

              <NFormItem path="customerGender" label="Giới tính">
                <NRadioGroup v-model:value="form.customerGender" name="genderGroup" size="large">
                  <NSpace>
                    <NRadio :value="GENDER.MALE">
                      Nam
                    </NRadio>
                    <NRadio :value="GENDER.FEMALE">
                      Nữ
                    </NRadio>
                  </NSpace>
                </NRadioGroup>
              </NFormItem>
            </div>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <NCard title="Danh sách địa chỉ" class="mt-6 shadow-sm rounded-xl border border-gray-100">
      <div class="space-y-6">
        <template v-if="addresses.length > 0">
          <div
            v-for="(address, index) in addresses" :key="address.id || index"
            class="border border-gray-200 rounded-xl p-4 md:p-6 bg-white relative transition-all hover:shadow-md"
            :class="{ 'ring-2 ring-green-500 ring-opacity-50 bg-green-50/30': address.isDefault }"
          >
            <div v-if="address.isDefault" class="absolute top-0 right-0 bg-green-600 text-white text-[10px] md:text-xs px-3 py-1 rounded-bl-lg rounded-tr-lg font-bold shadow-sm z-10">
              Mặc định
            </div>

            <div class="absolute top-3 right-3 z-10" :class="{ 'top-8': address.isDefault }">
              <NPopconfirm positive-text="Xóa" negative-text="Hủy" @positive-click="handleDeleteAddress(address, index)">
                <template #trigger>
                  <NButton text type="error" class="hover:bg-red-50 p-1 rounded-full transition-colors">
                    <NIcon size="20">
                      <Icon icon="mdi:trash-can-outline" />
                    </NIcon>
                  </NButton>
                </template>
                Bạn chắc chắn muốn xóa địa chỉ này?
              </NPopconfirm>
            </div>

            <NForm
              :ref="(el) => { if (el) addressFormsRef[index] = el as FormInst }"
              :rules="ADDRESS_FORM_RULES"
              :model="address"
              :show-feedback="false"
              label-placement="top"
            >
              <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
                <div class="md:col-span-12 font-bold text-gray-700 mb-1 flex items-center gap-2 border-b pb-2">
                  <Icon icon="mdi:map-marker-radius" class="text-green-600 text-xl" />
                  <span>Địa chỉ {{ index + 1 }}</span>
                </div>

                <div class="md:col-span-6">
                  <NFormItem path="provinceCity" label="Tỉnh/Thành phố">
                    <NSelect v-model:value="address.provinceCity" :options="provinceOptions" filterable clearable :loading="addressDataLoading" placeholder="Chọn Tỉnh/Thành phố" @update:value="onProvinceChange(address, $event)" />
                  </NFormItem>
                </div>

                <div class="md:col-span-6">
                  <NFormItem path="wardCommune" label="Phường/Xã/Thị trấn">
                    <NSelect v-model:value="address.wardCommune" :options="getWardOptions(address.provinceCity)" filterable clearable :disabled="!address.provinceCity" placeholder="Chọn Phường/Xã" />
                  </NFormItem>
                </div>

                <div class="md:col-span-12">
                  <NFormItem path="addressDetail" label="Địa chỉ chi tiết">
                    <NInput v-model:value="address.addressDetail" type="textarea" :autosize="{ minRows: 2, maxRows: 3 }" placeholder="Số nhà, tên đường..." />
                  </NFormItem>
                </div>

                <div class="md:col-span-12 border-t pt-3 mt-1">
                  <div class="flex items-center gap-3">
                    <span class="text-gray-600 font-medium">Đặt làm mặc định:</span>
                    <NSwitch
                      :value="address.isDefault"
                      :disabled="address.isDefault"
                      @update:value="(val) => onDefaultChange(address, val)"
                    />
                    <span v-if="address.isDefault" class="text-xs text-gray-400 italic">(Bật địa chỉ khác để thay thế)</span>
                  </div>
                </div>
              </div>
            </NForm>
          </div>
        </template>

        <div v-else class="text-center py-12 bg-white rounded-xl border-2 border-dashed border-gray-200">
          <Icon icon="carbon:location-heart-filled" class="text-5xl text-gray-200 mb-3 mx-auto" />
          <p class="text-gray-500 font-medium">
            Chưa có địa chỉ nào
          </p>
          <p class="text-gray-400 text-sm">
            Vui lòng thêm địa chỉ giao hàng
          </p>
        </div>

        <div class="flex justify-center pt-2">
          <NButton type="primary" dashed class="w-full md:w-auto px-8" size="medium" @click="addNewAddress">
            <template #icon>
              <NIcon><Icon icon="mdi:plus" /></NIcon>
            </template>
            Thêm địa chỉ mới
          </NButton>
        </div>
      </div>
    </NCard>

    <div class="sticky bottom-0 bg-white/90 backdrop-blur-sm border-t p-4 mt-8 -mx-4 md:-mx-6 z-20 flex justify-end gap-3 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
      <NButton size="large" @click="goBack">
        Hủy bỏ
      </NButton>
      <NButton type="primary" :loading="submitting" size="large" class="px-8 shadow-lg shadow-green-500/30" @click="handleValidateAndSubmit">
        <template #icon>
          <Icon icon="carbon:save" />
        </template>
        {{ submitButtonText }}
      </NButton>
    </div>
  </div>
</template>
