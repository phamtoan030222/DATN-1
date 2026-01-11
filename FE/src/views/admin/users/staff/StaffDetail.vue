<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createDiscreteApi,
  NAvatar,
  NButton,
  NCard,
  NDatePicker,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NModal,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSpin,
  NTooltip,
} from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode'

// --- API IMPORTS ---
import { createStaff, getStaffById, updateStaff } from '@/service/api/admin/users/staff.api'
import type { CreateStaffRequest } from '@/service/api/admin/users/staff.api'
import { getCommunes, getDistricts, getProvinces } from '@/service/api/admin/geo.api'
import { uploadAvatar } from '@/service/api/admin/users/customer/customer'

// --- CONFIG & STATE ---
const { message, dialog } = createDiscreteApi(['message', 'dialog'])
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const isUpdating = ref(false)
const isAvatarUploading = ref(false)

// Ref cho Input File ẩn (Giải quyết triệt để lỗi click)
const fileInputRef = ref<HTMLInputElement | null>(null)

// Camera
const showScanner = ref(false)
const html5QrCode = ref<Html5Qrcode | null>(null)
const isScanning = ref(false)

const staffId = computed(() => route.params.id as string | undefined)
const isEditMode = computed(() => !!staffId.value)
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật' : 'Thêm mới')

const formData = reactive<CreateStaffRequest>({
  fullName: '',
  role: null as any,
  birthday: null as any,
  citizenIdentifyCard: '',
  hometown: '',
  avatarUrl: '',
  phone: '',
  email: '',
  gender: true,
  provinceCode: null as any,
  districtCode: null as any,
  communeCode: null as any,
})

// --- RULES ---
const rules: FormRules = {
  fullName: [{ required: true, message: 'Nhập họ tên', trigger: ['blur', 'input'] }],
  role: [{ required: true, message: 'Chọn chức vụ', trigger: ['blur', 'change'] }],
  phone: [{ pattern: /^0[3|5789]\d{8}$/, message: 'SĐT không hợp lệ', trigger: ['blur', 'input'] }],
  email: [{ type: 'email', message: 'Email sai định dạng', trigger: ['blur', 'input'] }],
  citizenIdentifyCard: [{ required: true, message: 'Nhập số CCCD', trigger: ['blur', 'input'] }],
  birthday: [{ required: true, type: 'number', message: 'Chọn ngày sinh', trigger: ['blur', 'change'] }],
  provinceCode: [{ required: true, type: 'number', message: 'Chọn Tỉnh/TP', trigger: ['change'] }],
  districtCode: [{ required: true, type: 'number', message: 'Chọn Quận/Huyện', trigger: ['change'] }],
  communeCode: [{ required: true, type: 'number', message: 'Chọn Phường/Xã', trigger: ['change'] }],
  hometown: [{ required: true, message: 'Nhập địa chỉ cụ thể', trigger: ['blur', 'input'] }],
}

const provinces = ref<{ label: string, value: number }[]>([])
const districts = ref<{ label: string, value: number }[]>([])
const communes = ref<{ label: string, value: number }[]>([])

// --- GEO ---
async function loadProvinces() {
  const data = await getProvinces()
  provinces.value = data.map((p: any) => ({ label: p.name, value: Number(p.code) }))
}

watch(() => formData.provinceCode, async (newCode) => {
  if (isUpdating.value)
    return
  formData.districtCode = null; formData.communeCode = null; districts.value = []; communes.value = []
  if (newCode) {
    const data = await getDistricts(String(newCode))
    districts.value = data.map((d: any) => ({ label: d.name, value: Number(d.code) }))
  }
})

watch(() => formData.districtCode, async (newCode) => {
  if (isUpdating.value)
    return
  formData.communeCode = null; communes.value = []
  if (newCode) {
    const data = await getCommunes(String(newCode))
    communes.value = data.map((c: any) => ({ label: c.name, value: Number(c.code) }))
  }
})

// --- HELPER ---
function normalizeString(str: string) {
  if (!str)
    return ''
  return str.toLowerCase().normalize('NFD').replace(/[\u0300-\u036F]/g, '').replace(/đ/g, 'd').replace(/tinh|thanh pho|tp|quan|huyen|thi xa|phuong|xa|th/g, ' ').replace(/[^a-z0-9\s]/g, '').trim().replace(/\s+/g, ' ')
}

// --- SCAN QR ---
async function onScanSuccess(decodedText: string) {
  try {
    stopScanner(); showScanner.value = false; isUpdating.value = true; formRef.value?.restoreValidation()
    const parts = decodedText.split('|')
    if (parts.length < 6) { message.warning('QR sai định dạng'); isUpdating.value = false; return }
    const [cccd, , fullName, dobStr, genderStr, address] = parts

    formData.citizenIdentifyCard = cccd || ''
    formData.fullName = fullName || ''
    if (genderStr)
      formData.gender = genderStr.toLowerCase() === 'nam'
    if (dobStr && dobStr.length === 8) {
      const d = Number.parseInt(dobStr.substring(0, 2)); const m = Number.parseInt(dobStr.substring(2, 4)) - 1; const y = Number.parseInt(dobStr.substring(4, 8))
      formData.birthday = new Date(y, m, d).getTime()
    }
    if (address) {
      formData.hometown = address
      const addrParts = address.split(',').map(s => s.trim())
      const pName = addrParts[addrParts.length - 1] || ''
      const dName = addrParts.length > 1 ? addrParts[addrParts.length - 2] : ''
      const cName = addrParts.length > 2 ? addrParts[addrParts.length - 3] : ''
      const foundProv = provinces.value.find(p => normalizeString(p.label).includes(normalizeString(pName)))
      if (foundProv) {
        formData.provinceCode = foundProv.value
        const dListRaw = await getDistricts(String(foundProv.value))
        districts.value = dListRaw.map((d: any) => ({ label: d.name, value: Number(d.code) }))
        const foundDist = districts.value.find(d => normalizeString(d.label).includes(normalizeString(dName)))
        if (foundDist) {
          formData.districtCode = foundDist.value
          const cListRaw = await getCommunes(String(foundDist.value))
          communes.value = cListRaw.map((c: any) => ({ label: c.name, value: Number(c.code) }))
          const foundComm = communes.value.find(c => normalizeString(c.label).includes(normalizeString(cName)))
          if (foundComm)
            formData.communeCode = foundComm.value
        }
      }
    }
    message.success('Đã điền thông tin!')
  }
  catch (e) { message.error('Lỗi xử lý') }
  finally { setTimeout(() => { isUpdating.value = false }, 500) }
}

async function startScanner() {
  showScanner.value = true; isScanning.value = true; await nextTick()
  html5QrCode.value = new Html5Qrcode('reader')
  html5QrCode.value.start({ facingMode: 'environment' }, { fps: 10, qrbox: { width: 300, height: 300 } }, onScanSuccess, () => {}).catch(() => { message.error('Cấp quyền Camera!'); showScanner.value = false })
}
function stopScanner() {
  if (html5QrCode.value?.isScanning)
    html5QrCode.value.stop().then(() => { html5QrCode.value?.clear(); isScanning.value = false })
}
onBeforeUnmount(() => stopScanner())

// --- UPLOAD LOGIC (NATIVE INPUT) ---
// Hàm kích hoạt input ẩn
function triggerSelectImage() {
  fileInputRef.value?.click()
}

// Xử lý sự kiện change của input
async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0)
    return

  const file = input.files[0]
  if (!file.type.startsWith('image/')) {
    message.error('Vui lòng chỉ chọn file ảnh!')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('Ảnh quá lớn (Max 5MB)')
    return
  }

  try {
    isAvatarUploading.value = true
    const { url } = await uploadAvatar(file)
    if (url) {
      formData.avatarUrl = url
      message.success('Tải ảnh thành công')
    }
  }
  catch (e) {
    message.error('Lỗi tải ảnh lên server')
  }
  finally {
    isAvatarUploading.value = false
    // Reset input để có thể chọn lại cùng 1 file nếu muốn
    input.value = ''
  }
}

// --- CRUD ---
async function loadStaffDetail() {
  if (!isEditMode.value || !staffId.value)
    return
  try {
    const res = await getStaffById(staffId.value)
    const data = res?.data || res
    isUpdating.value = true
    Object.assign(formData, {
      fullName: data.name || data.fullName,
      role: data.account?.roleConstant || data.role,
      phone: data.phone,
      email: data.email,
      citizenIdentifyCard: data.citizenIdentifyCard,
      hometown: data.hometown,
      gender: data.gender ?? true,
      avatarUrl: data.avatarUrl || '',
      birthday: data.birthday ? new Date(data.birthday).getTime() : null,
      provinceCode: data.provinceCode ? Number(data.provinceCode) : null,
      districtCode: data.districtCode ? Number(data.districtCode) : null,
      communeCode: data.communeCode ? Number(data.communeCode) : null,
    })
    if (formData.provinceCode) {
      const d = await getDistricts(String(formData.provinceCode)); districts.value = d.map((x: any) => ({ label: x.name, value: Number(x.code) }))
      if (formData.districtCode) {
        const c = await getCommunes(String(formData.districtCode)); communes.value = c.map((x: any) => ({ label: x.name, value: Number(x.code) }))
      }
    }
  }
  catch { router.push('/users/staff') }
  finally { setTimeout(() => isUpdating.value = false, 300) }
}

async function handleValidateAndSubmit() {
  if (!formRef.value)
    return
  await formRef.value.validate((e) => {
    if (!e)
      dialog.warning({ title: 'Xác nhận', content: 'Lưu thông tin?', positiveText: 'Lưu', negativeText: 'Hủy', onPositiveClick: processSubmit })
    else message.error('Kiểm tra lại dữ liệu')
  })
}

async function processSubmit() {
  submitting.value = true
  try {
    const payload = { ...formData, fullName: formData.fullName.trim() }
    isEditMode.value ? await updateStaff(staffId.value!, payload) : await createStaff(payload)
    message.success('Thành công!'); router.push('/users/staff')
  }
  catch (e: any) { message.error(e.response?.data?.message || 'Lỗi') }
  finally { submitting.value = false }
}

onMounted(async () => {
  await loadProvinces(); if (isEditMode.value)
    await loadStaffDetail()
})
</script>

<template>
  <div class="p-4 md:p-6 bg-gray-50/50 min-h-screen">
    <div class="flex items-center gap-3 mb-4">
      <NButton circle secondary @click="router.push('/users/staff')">
        <template #icon>
          <NIcon><Icon icon="mdi:arrow-left" /></NIcon>
        </template>
      </NButton>
      <span class="font-bold text-xl text-gray-800">{{ isEditMode ? 'Cập nhật nhân viên' : 'Thêm nhân viên mới' }}</span>
    </div>

    <NCard class="shadow-sm rounded-xl border border-gray-100" content-style="padding: 24px;">
      <NForm ref="formRef" :model="formData" :rules="rules" label-placement="top" size="medium" require-mark-placement="right-hanging">
        <NGrid :x-gap="32" :y-gap="16" cols="1 768:12">
          <NGridItem span="1 768:3">
            <div class="w-full flex flex-col items-center">
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                class="hidden"
                style="display: none"
                @change="onFileSelected"
              >

              <div class="cursor-pointer transition-opacity hover:opacity-90" @click="triggerSelectImage">
                <NSpin :show="isAvatarUploading">
                  <NAvatar
                    :size="160"
                    round
                    class="bg-gray-100 flex items-center justify-center"
                    :src="formData.avatarUrl || undefined"
                    object-fit="cover"
                  >
                    <template #default>
                      <NIcon v-if="!formData.avatarUrl" size="100" class="text-gray-300">
                        <Icon icon="carbon:user-avatar-filled-alt" />
                      </NIcon>
                    </template>
                  </NAvatar>
                </NSpin>
              </div>

              <div class="mt-4 flex flex-col items-center gap-2">
                <NButton secondary size="small" :disabled="isAvatarUploading" @click="triggerSelectImage">
                  Chọn ảnh
                </NButton>

                <span class="text-xs text-gray-400">Max 5MB</span>

                <NButton v-if="formData.avatarUrl" text type="error" size="tiny" @click="formData.avatarUrl = ''">
                  Gỡ ảnh
                </NButton>
              </div>
            </div>
          </NGridItem>

          <NGridItem span="1 768:9">
            <div class="mb-4">
              <NFormItem label="Số CCCD/CMND" path="citizenIdentifyCard">
                <NInput v-model:value="formData.citizenIdentifyCard" placeholder="Nhập số CCCD..." size="large" clearable>
                  <template #suffix>
                    <NTooltip trigger="hover">
                      <template #trigger>
                        <NButton text type="primary" class="mr-1" @click="startScanner">
                          <Icon icon="mdi:qrcode-scan" width="24" class="text-green-600" />
                        </NButton>
                      </template>
                      Quét mã QR để điền nhanh
                    </NTooltip>
                  </template>
                </NInput>
              </NFormItem>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
              <div class="md:col-span-5">
                <NFormItem label="Họ và tên" path="fullName">
                  <NInput v-model:value="formData.fullName" placeholder="Nhập họ tên" size="large" />
                </NFormItem>
              </div>
              <div class="md:col-span-4">
                <NFormItem label="Ngày sinh" path="birthday">
                  <NDatePicker v-model:value="formData.birthday" type="date" class="w-full" size="large" format="dd/MM/yyyy" placeholder="Chọn ngày sinh" />
                </NFormItem>
              </div>
              <div class="md:col-span-3">
                <NFormItem label="Giới tính">
                  <NRadioGroup v-model:value="formData.gender">
                    <NSpace>
                      <NRadio :value="true">
                        Nam
                      </NRadio><NRadio :value="false">
                        Nữ
                      </NRadio>
                    </NSpace>
                  </NRadioGroup>
                </NFormItem>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-12 gap-4 mt-2">
              <div class="md:col-span-4">
                <NFormItem label="Số điện thoại" path="phone">
                  <NInput v-model:value="formData.phone" placeholder="Nhập SĐT" size="large" />
                </NFormItem>
              </div>
              <div class="md:col-span-4">
                <NFormItem label="Email" path="email">
                  <NInput v-model:value="formData.email" placeholder="Nhập email" size="large" />
                </NFormItem>
              </div>
              <div class="md:col-span-4">
                <NFormItem label="Chức vụ" path="role">
                  <NSelect v-model:value="formData.role" :options="[{ label: 'Quản lý', value: 'QUAN_LY' }, { label: 'Nhân viên', value: 'NHAN_VIEN' }]" placeholder="Chọn chức vụ" size="large" />
                </NFormItem>
              </div>
            </div>

            <div class="mt-6 border-t pt-4 bg-gray-50/50 p-4 rounded-lg">
              <h3 class="font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <Icon icon="carbon:location" /> Địa chỉ thường trú
              </h3>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div class="md:col-span-3 mb-2">
                  <NFormItem label="Địa chỉ cụ thể (Từ CCCD)" path="hometown">
                    <NInput v-model:value="formData.hometown" placeholder="Số nhà, tên đường, thôn xóm..." />
                  </NFormItem>
                </div>
                <NFormItem label="Tỉnh/Thành" path="provinceCode">
                  <NSelect v-model:value="formData.provinceCode" :options="provinces" filterable placeholder="Chọn Tỉnh/TP" />
                </NFormItem>
                <NFormItem label="Quận/Huyện" path="districtCode">
                  <NSelect v-model:value="formData.districtCode" :options="districts" filterable placeholder="Chọn Quận/Huyện" :disabled="!formData.provinceCode" />
                </NFormItem>
                <NFormItem label="Phường/Xã" path="communeCode">
                  <NSelect v-model:value="formData.communeCode" :options="communes" filterable placeholder="Chọn Phường/Xã" :disabled="!formData.districtCode" />
                </NFormItem>
              </div>
            </div>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <div class="sticky bottom-0 bg-white/90 border-t p-4 mt-8 flex justify-end gap-3 z-20">
      <NButton size="large" @click="router.push('/users/staff')">
        Hủy bỏ
      </NButton>
      <NButton type="primary" :loading="submitting" size="large" @click="handleValidateAndSubmit">
        {{ submitButtonText }}
      </NButton>
    </div>

    <NModal v-model:show="showScanner" :mask-closable="false" preset="card" title="Quét CCCD" style="width: 600px; max-width: 95%;">
      <div class="flex flex-col items-center">
        <div id="reader" style="width: 100%; min-height: 400px; border-radius: 12px; overflow: hidden; background: #000;" />
        <p class="mt-4 text-gray-600 text-center">
          Đưa mã QR vào khung hình.<br><span class="text-xs text-gray-400">Giữ yên để lấy nét.</span>
        </p>
        <NButton class="mt-4" type="error" secondary @click="stopScanner; showScanner = false">
          Đóng Camera
        </NButton>
      </div>
    </NModal>
  </div>
</template>
