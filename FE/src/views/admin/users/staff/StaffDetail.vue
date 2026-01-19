<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
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
import type { FormInst, FormRules, SelectOption } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { Html5Qrcode } from 'html5-qrcode'

// --- API IMPORTS ---
import { createStaff, getStaffById, updateStaff } from '@/service/api/admin/users/staff.api'
import type { CreateStaffRequest } from '@/service/api/admin/users/staff.api'
import { uploadAvatar } from '@/service/api/admin/users/customer/customer'

// --- CONFIG & STATE ---
const { message, dialog } = createDiscreteApi(['message', 'dialog'])
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const isUpdating = ref(false)
const isAvatarUploading = ref(false)

// Ref cho Input File ẩn
const fileInputRef = ref<HTMLInputElement | null>(null)

// Camera
const showScanner = ref(false)
const html5QrCode = ref<Html5Qrcode | null>(null)
const isScanning = ref(false)

// Edit Mode Check
const staffId = computed(() => route.params.id as string | undefined)
const isEditMode = computed(() => !!staffId.value)
const submitButtonText = computed(() => isEditMode.value ? 'Cập nhật' : 'Thêm mới')

// Form Data
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
  districtCode: null, // API v2 không dùng, gửi null
  communeCode: null as any,
})

// --- VALIDATION RULES (Đầy đủ & Chặt chẽ) ---
const rules: FormRules = {
  fullName: [
    { required: true, message: 'Vui lòng nhập họ và tên', trigger: ['blur', 'input'] },
    { min: 2, max: 50, message: 'Tên phải từ 2 - 50 ký tự', trigger: ['blur', 'input'] },
  ],
  citizenIdentifyCard: [
    { required: true, message: 'Vui lòng nhập số CCCD/CMND', trigger: ['blur', 'input'] },
    { pattern: /^\d{9,12}$/, message: 'CCCD phải là số (9-12 ký tự)', trigger: ['blur', 'input'] },
  ],
  role: [
    { required: true, message: 'Vui lòng chọn chức vụ', trigger: ['blur', 'change'] },
  ],
  birthday: [
    { required: true, type: 'number', message: 'Vui lòng chọn ngày sinh', trigger: ['blur', 'change'] },
  ],
  gender: [
    { required: true, type: 'boolean', message: 'Vui lòng chọn giới tính', trigger: 'change' },
  ],
  phone: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: ['blur', 'input'] },
    { pattern: /^(03|05|07|08|09)\d{8}$/, message: 'SĐT không hợp lệ (VD: 098...)', trigger: ['blur', 'input'] },
  ],
  email: [
    { required: true, message: 'Vui lòng nhập Email', trigger: ['blur', 'input'] },
    { type: 'email', message: 'Định dạng Email không đúng', trigger: ['blur', 'input'] },
  ],
  hometown: [
    { required: true, message: 'Vui lòng nhập địa chỉ cụ thể (Số nhà, đường...)', trigger: ['blur', 'input'] },
  ],
  // Địa chỉ theo API v2: Chỉ bắt buộc Tỉnh và Xã
  provinceCode: [
    { required: true, type: 'number', message: 'Vui lòng chọn Tỉnh/Thành phố', trigger: ['change', 'blur'] },
  ],
  communeCode: [
    { required: true, type: 'number', message: 'Vui lòng chọn Phường/Xã', trigger: ['change', 'blur'] },
  ],
}

// --- ADDRESS LOGIC (API V2) ---
// Interface cho dữ liệu địa lý
interface GeoUnit { label: string, value: number, codename: string, wards?: any[] }
const provinces = ref<GeoUnit[]>([])
const communes = ref<GeoUnit[]>([])
const addressLoading = ref(false)

// Cache dữ liệu gốc để tìm kiếm khi Scan QR
const rawProvincesData = ref<any[]>([])

// 1. Load danh sách Tỉnh (API v2)
async function loadProvinces() {
  try {
    const res = await fetch('https://provinces.open-api.vn/api/v2/p/?depth=1')
    if (res.ok) {
      const data = await res.json()
      rawProvincesData.value = data
      provinces.value = data.map((p: any) => ({
        label: p.name,
        value: Number(p.code),
        codename: p.codename,
      }))
    }
  }
  catch (e) { message.error('Không thể tải danh sách Tỉnh/Thành') }
}

// 2. Xử lý khi chọn Tỉnh -> Tải Phường/Xã (Bỏ qua Quận)
async function handleProvinceChange(provinceCode: number | null) {
  // Reset Xã
  formData.communeCode = null
  communes.value = []

  if (!provinceCode)
    return

  try {
    addressLoading.value = true
    // Gọi API v2 lấy chi tiết Tỉnh (bao gồm wards)
    const res = await fetch(`https://provinces.open-api.vn/api/v2/p/${provinceCode}?depth=2`)
    if (res.ok) {
      const data = await res.json()
      if (data.wards) {
        communes.value = data.wards.map((w: any) => ({
          label: w.name,
          value: Number(w.code),
          codename: w.codename,
        }))
      }
    }
  }
  catch (e) {
    message.error('Lỗi tải danh sách Phường/Xã')
  }
  finally {
    addressLoading.value = false
  }
}

// --- HELPER MỚI: Chuẩn hóa chuỗi an toàn hơn ---
function normalizeString(str: string) {
  if (!str)
    return ''
  return str.toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036F]/g, '') // Bỏ dấu
    .replace(/đ/g, 'd')
    .replace(/[.,]/g, ' ') // Thay dấu chấm, phẩy bằng khoảng trắng
    // Chỉ xóa các từ khóa hành chính khi nó là từ riêng biệt (tránh xóa nhầm tên như 'Phượng' -> 'hượng')
    .replace(/\b(tinh|thanh pho|tp|quan|huyen|thi xa|tx|phuong|xa|thi tran|tt|p|x|so|duong|pho|thon|xom)\b/g, ' ')
    .replace(/[^a-z0-9\s]/g, '') // Bỏ ký tự đặc biệt còn lại
    .replace(/\s+/g, ' ') // Gộp khoảng trắng
    .trim()
}

// --- SCAN QR LOGIC (Fix lỗi không tìm thấy xã) ---
async function onScanSuccess(decodedText: string) {
  try {
    stopScanner()
    showScanner.value = false
    isUpdating.value = true
    formRef.value?.restoreValidation()

    // 1. Phân tích chuỗi QR (Chuẩn CCCD gắn chip)
    const parts = decodedText.split('|')

    // Fallback: Nếu không phải định dạng | thì thử tìm theo keyword (đề phòng QR cũ)
    if (parts.length < 6 && !decodedText.includes('ĐC:')) {
      message.warning('Mã QR không đúng định dạng CCCD')
      isUpdating.value = false
      return
    }

    let cccd, fullName, dobStr, genderStr, address

    // Xử lý dữ liệu CCCD Chip (Format chuẩn: Số|CMND cũ|Tên|Ngày sinh|Giới tính|Địa chỉ|...)
    if (parts.length >= 6) {
      cccd = parts[0]
      fullName = parts[2]
      dobStr = parts[3]
      genderStr = parts[4]
      address = parts[5]
    }

    // --- ĐIỀN THÔNG TIN CƠ BẢN ---
    formData.citizenIdentifyCard = cccd || ''
    formData.fullName = fullName || ''
    if (genderStr)
      formData.gender = genderStr.toLowerCase() === 'nam'

    if (dobStr && dobStr.length === 8) {
      const d = Number.parseInt(dobStr.substring(0, 2))
      const m = Number.parseInt(dobStr.substring(2, 4)) - 1
      const y = Number.parseInt(dobStr.substring(4, 8))
      formData.birthday = new Date(y, m, d).getTime()
    }

    // --- XỬ LÝ ĐỊA CHỈ (QUAN TRỌNG) ---
    if (address) {
      formData.hometown = address // Điền full địa chỉ vào ô text

      // Tách chuỗi địa chỉ: "Thôn A, Xã B, Huyện C, Tỉnh D"
      // Lọc bỏ phần tử rỗng sau khi split
      const addrParts = address.split(',').map(s => s.trim()).filter(s => s)

      // 1. Tìm Tỉnh (Thường nằm cuối cùng)
      const pNameInQR = addrParts[addrParts.length - 1] || ''
      const normalizedPName = normalizeString(pNameInQR)

      const foundProv = provinces.value.find(p =>
        normalizeString(p.label).includes(normalizedPName) || normalizedPName.includes(normalizeString(p.label)),
      )

      if (foundProv) {
        formData.provinceCode = foundProv.value

        // Gọi API tải danh sách xã của tỉnh này
        const res = await fetch(`https://provinces.open-api.vn/api/v2/p/${foundProv.value}?depth=2`)
        const data = await res.json()

        const wardList = data.wards || []
        communes.value = wardList.map((w: any) => ({
          label: w.name,
          value: Number(w.code),
          codename: w.codename,
        }))

        // 2. Tìm Xã (Quét ngược từ dưới lên, bỏ qua phần tử cuối là Tỉnh)
        // Ví dụ: [Thôn A, Xã B, Huyện C] -> Thử C trước, rồi đến B
        // Vì API v2 phẳng (không có Huyện), nên vòng lặp sẽ bỏ qua C (Huyện) và khớp B (Xã)
        let foundComm = null
        const searchParts = addrParts.slice(0, -1).reverse()

        for (const part of searchParts) {
          const normalizedPart = normalizeString(part) // VD: "tan yen" hoặc "ngoc chau"

          // Tìm trong list xã xem có khớp không
          foundComm = communes.value.find((c) => {
            const normalizedCommuneAPI = normalizeString(c.label) // VD: "xa ngoc chau" -> "ngoc chau"

            // So sánh 2 chiều:
            // 1. QR chứa API (QR: "Xã Ngọc Châu A" - API: "Ngọc Châu")
            // 2. API chứa QR (QR: "Ngọc Châu" - API: "Xã Ngọc Châu")
            // 3. Khớp chính xác
            return normalizedPart === normalizedCommuneAPI
              || (normalizedPart.length > 4 && normalizedCommuneAPI.includes(normalizedPart))
              || (normalizedCommuneAPI.length > 4 && normalizedPart.includes(normalizedCommuneAPI))
          })

          if (foundComm)
            break // Tìm thấy thì dừng ngay
        }

        if (foundComm) {
          formData.communeCode = foundComm.value
        }
        else {
          // Trường hợp không tìm thấy xã, chỉ giữ lại Tỉnh
          console.warn('Không tìm thấy xã khớp:', searchParts)
        }
      }
    }
    message.success('Đã quét thông tin thành công!')
  }
  catch (e) {
    console.error(e)
    message.error('Lỗi khi xử lý dữ liệu QR')
  }
  finally {
    setTimeout(() => { isUpdating.value = false }, 500)
  }
}

async function startScanner() {
  showScanner.value = true
  isScanning.value = true
  await nextTick()
  html5QrCode.value = new Html5Qrcode('reader')
  html5QrCode.value.start(
    { facingMode: 'environment' },
    { fps: 10, qrbox: { width: 250, height: 250 } },
    onScanSuccess,
    () => {},
  ).catch((err) => {
    console.error(err)
    message.error('Không thể truy cập Camera. Vui lòng cấp quyền!')
    showScanner.value = false
  })
}

function stopScanner() {
  if (html5QrCode.value?.isScanning) {
    html5QrCode.value.stop().then(() => {
      html5QrCode.value?.clear()
      isScanning.value = false
    })
  }
}

onBeforeUnmount(() => stopScanner())

// --- UPLOAD LOGIC ---
function triggerSelectImage() { fileInputRef.value?.click() }

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0)
    return

  const file = input.files[0]
  if (!file.type.startsWith('image/')) {
    message.error('Chỉ chấp nhận file ảnh!')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('Ảnh tối đa 5MB')
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
    message.error('Lỗi tải ảnh')
  }
  finally {
    isAvatarUploading.value = false
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
    isUpdating.value = true // Chặn watcher

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
      districtCode: null, // Reset district vì không dùng
      communeCode: data.communeCode ? Number(data.communeCode) : null,
    })

    // Load lại xã nếu có tỉnh
    if (formData.provinceCode) {
      await handleProvinceChange(formData.provinceCode)
      // Set lại communeCode vì handleProvinceChange sẽ reset nó
      if (data.communeCode) {
        formData.communeCode = Number(data.communeCode)
      }
    }
  }
  catch {
    router.push('/users/staff')
  }
  finally {
    setTimeout(() => isUpdating.value = false, 300)
  }
}

async function handleValidateAndSubmit() {
  if (!formRef.value)
    return

  // 1. Validate Form
  await formRef.value.validate((errors) => {
    if (!errors) {
      dialog.warning({
        title: 'Xác nhận',
        content: 'Bạn có chắc chắn muốn lưu thông tin này?',
        positiveText: 'Lưu',
        negativeText: 'Hủy',
        positiveButtonProps: { color: '#16a34a', textColor: '#ffffff', style: { fontWeight: 'bold' } },
        onPositiveClick: processSubmit,
      })
    }
    else {
      message.error('Vui lòng kiểm tra lại các trường báo đỏ')
    }
  })
}

async function processSubmit() {
  submitting.value = true
  try {
    // 2. Chuẩn hóa dữ liệu trước khi gửi
    const payload = {
      ...formData,
      fullName: formData.fullName.trim(),
      // Email: Chuyển về chữ thường
      email: formData.email.trim().toLowerCase(),
      // District: Gửi null vì API v2 không dùng
      districtCode: null,
    }

    if (isEditMode.value) {
      await updateStaff(staffId.value!, payload)
      message.success('Cập nhật thành công!')
    }
    else {
      await createStaff(payload)
      message.success('Thêm mới thành công!')
    }
    router.push('/users/staff')
  }
  catch (e: any) {
    const res = e.response?.data
    const code = res?.code
    const msg = (res?.message || '').toLowerCase()

    if (code === 'PHONE_EXISTS' || msg.includes('phone'))
      message.error('Số điện thoại đã tồn tại!')
    else if (code === 'EMAIL_EXISTS' || msg.includes('email'))
      message.error('Email đã tồn tại!')
    else if (code === 'CCCD_EXISTS' || msg.includes('cccd'))
      message.error('Số CCCD đã tồn tại!')
    else message.error(res?.message || 'Có lỗi xảy ra')
  }
  finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadProvinces()
  if (isEditMode.value)
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
      <NForm
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="top"
        size="medium"
        require-mark-placement="right-hanging"
      >
        <NGrid :x-gap="32" :y-gap="16" cols="1 768:12">
          <NGridItem span="1 768:3">
            <div class="w-full flex flex-col items-center">
              <input ref="fileInputRef" type="file" accept="image/*" class="hidden" style="display: none" @change="onFileSelected">

              <div class="cursor-pointer transition-opacity hover:opacity-90 group relative" @click="triggerSelectImage">
                <NSpin :show="isAvatarUploading">
                  <NAvatar
                    :size="160"
                    round
                    class="bg-white border-4 border-gray-100 shadow-sm flex items-center justify-center"
                    :src="formData.avatarUrl || undefined"
                    object-fit="cover"
                  >
                    <template #default>
                      <NIcon v-if="!formData.avatarUrl" size="80" class="text-gray-300">
                        <Icon icon="carbon:user-avatar-filled-alt" />
                      </NIcon>
                    </template>
                  </NAvatar>
                  <div class="absolute inset-0 bg-black/30 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all">
                    <Icon icon="mdi:camera" class="text-white text-3xl" />
                  </div>
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
            <div class="mb-2">
              <NFormItem label="Số CCCD/CMND" path="citizenIdentifyCard">
                <NInput v-model:value="formData.citizenIdentifyCard" placeholder="Nhập số CCCD (9-12 số)" size="large" clearable>
                  <template #suffix>
                    <NTooltip trigger="hover">
                      <template #trigger>
                        <NButton text type="primary" class="mr-1" @click="startScanner">
                          <Icon icon="mdi:qrcode-scan" width="24" class="text-green-600" />
                        </NButton>
                      </template>
                      Quét mã QR trên CCCD
                    </NTooltip>
                  </template>
                </NInput>
              </NFormItem>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
              <div class="md:col-span-5">
                <NFormItem label="Họ và tên" path="fullName">
                  <NInput v-model:value="formData.fullName" placeholder="Nhập họ tên đầy đủ" size="large" />
                </NFormItem>
              </div>
              <div class="md:col-span-4">
                <NFormItem label="Ngày sinh" path="birthday">
                  <NDatePicker v-model:value="formData.birthday" type="date" class="w-full" size="large" format="dd/MM/yyyy" placeholder="Chọn ngày sinh" />
                </NFormItem>
              </div>
              <div class="md:col-span-3">
                <NFormItem label="Giới tính" path="gender">
                  <NRadioGroup v-model:value="formData.gender">
                    <NSpace>
                      <NRadio :value="true">
                        Nam
                      </NRadio>
                      <NRadio :value="false">
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
                  <NInput v-model:value="formData.phone" placeholder="VD: 0987..." size="large" />
                </NFormItem>
              </div>
              <div class="md:col-span-4">
                <NFormItem label="Email" path="email">
                  <NInput v-model:value="formData.email" placeholder="example@email.com" size="large" style="text-transform: lowercase;" />
                </NFormItem>
              </div>
              <div class="md:col-span-4">
                <NFormItem label="Chức vụ" path="role">
                  <NSelect v-model:value="formData.role" :options="[{ label: 'Quản lý', value: 'QUAN_LY' }, { label: 'Nhân viên', value: 'NHAN_VIEN' }]" placeholder="Chọn chức vụ" size="large" />
                </NFormItem>
              </div>
            </div>

            <div class="mt-6 border border-gray-200 bg-gray-50/30 p-5 rounded-lg">
              <h3 class="font-bold text-gray-700 mb-4 flex items-center gap-2 text-sm uppercase tracking-wide">
                <Icon icon="carbon:location" class="text-green-600" /> Địa chỉ liên hệ
              </h3>
              <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
                <div class="md:col-span-6">
                  <NFormItem label="Tỉnh/Thành phố" path="provinceCode">
                    <NSelect
                      v-model:value="formData.provinceCode"
                      :options="provinces"
                      filterable
                      clearable
                      placeholder="Chọn Tỉnh/TP"
                      @update:value="handleProvinceChange"
                    />
                  </NFormItem>
                </div>
                <div class="md:col-span-6">
                  <NFormItem label="Phường/Xã/Thị trấn" path="communeCode">
                    <NSelect
                      v-model:value="formData.communeCode"
                      :options="communes"
                      filterable
                      clearable
                      :loading="addressLoading"
                      placeholder="Chọn Phường/Xã"
                      :disabled="!formData.provinceCode"
                    />
                  </NFormItem>
                </div>
                <div class="md:col-span-12">
                  <NFormItem label="Địa chỉ chi tiết (Số nhà, đường...)" path="hometown">
                    <NInput v-model:value="formData.hometown" placeholder="VD: Số 12, Ngõ 5..." type="textarea" :autosize="{ minRows: 2 }" />
                  </NFormItem>
                </div>
              </div>
            </div>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <div class="sticky bottom-0 bg-white/90 backdrop-blur border-t p-4 mt-8 flex justify-end gap-3 z-20 shadow-lg">
      <NButton size="large" @click="router.push('/users/staff')">
        Hủy bỏ
      </NButton>
      <NButton type="primary" :loading="submitting" size="large" class="px-8 font-bold shadow-green-500/20 shadow-lg" @click="handleValidateAndSubmit">
        {{ submitButtonText }}
      </NButton>
    </div>

    <NModal v-model:show="showScanner" :mask-closable="false" preset="card" title="Quét mã QR CCCD" style="width: 500px; max-width: 95%;">
      <div class="flex flex-col items-center">
        <div id="reader" class="w-full rounded-lg overflow-hidden bg-black" style="min-height: 300px;" />
        <p class="mt-4 text-gray-600 text-center text-sm">
          Di chuyển camera đến mã QR trên góc thẻ CCCD.<br>Giữ cố định để lấy nét.
        </p>
        <NButton class="mt-4" secondary type="error" @click="stopScanner; showScanner = false">
          Đóng Camera
        </NButton>
      </div>
    </NModal>
  </div>
</template>
