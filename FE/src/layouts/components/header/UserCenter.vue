<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { FormInst, FormRules, UploadFileInfo } from 'naive-ui'
import { useMessage } from 'naive-ui'
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { useAuthStore } from '@/store'
import { localStorageAction } from '@/utils/storage.helper'
import heic2any from 'heic2any' // Import thư viện xử lý ảnh iPhone

import IconLogout from '~icons/icon-park-outline/logout'
import IconUser from '~icons/icon-park-outline/user'
import IconCamera from '~icons/icon-park-outline/camera'

// IMPORT API TỪ FILE SERVICE
import { changePassword, getStaffById, updateStaff } from '@/service/api/admin/users/staff.api'
import { uploadAvatar } from '@/service/api/admin/users/customer/customer'

const { t } = useI18n()
const { logout } = useAuthStore()
const router = useRouter()
const message = useMessage()

// Lấy thông tin cơ bản từ LocalStorage (để lấy ID người dùng)
const userInfo = ref(localStorageAction.get(USER_INFO_STORAGE_KEY) || {})

// --- STATE CHO MODAL & THÔNG TIN ---
const showProfileModal = ref(false)
const staffProfile = ref<any>({})
const isFetchingProfile = ref(false)
const isUploadingAvatar = ref(false)
const addressText = ref('Đang tải địa chỉ...')

// --- STATE ĐỔI MẬT KHẨU ---
const formRef = ref<FormInst | null>(null)
const isSubmittingPwd = ref(false)
const pwdFormValue = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại', trigger: ['blur', 'input'] }],
  newPassword: [
    { required: true, message: 'Vui lòng nhập mật khẩu mới', trigger: ['blur', 'input'] },
    { min: 6, message: 'Mật khẩu phải có ít nhất 6 ký tự', trigger: ['blur', 'input'] },
  ],
  confirmPassword: [
    { required: true, message: 'Vui lòng xác nhận mật khẩu mới', trigger: ['blur', 'input'] },
    {
      validator: (rule, value) => value === pwdFormValue.value.newPassword,
      message: 'Mật khẩu xác nhận không khớp!',
      trigger: ['blur', 'input'],
    },
  ],
}

// -------------------------------------------------------------
// TỰ ĐỘNG LẤY CHỨC VỤ ĐỂ HIỂN THỊ
// -------------------------------------------------------------
const displayRole = computed(() => {
  const roleCode = staffProfile.value?.role || staffProfile.value?.roleConstant || staffProfile.value?.account?.roleConstant
  if (roleCode === 'QUAN_LY')
    return { text: 'Quản lý', type: 'error' }
  if (roleCode === 'NHAN_VIEN')
    return { text: 'Nhân viên', type: 'success' }
  return { text: 'Chưa phân quyền', type: 'default' }
})

const options = computed(() => [
  { label: 'Thông tin cá nhân', key: 'userCenter', icon: () => h(IconUser) },
  { type: 'divider', key: 'd1' },
  { label: t('app.loginOut') || 'Đăng xuất', key: 'loginOut', icon: () => h(IconLogout) },
])

// -------------------------------------------------------------
// TẢI THÔNG TIN VÀ DỊCH ĐỊA CHỈ
// -------------------------------------------------------------
async function loadStaffProfile() {
  const currentUserId = userInfo.value?.id || userInfo.value?.userId || userInfo.value?.staffId
  if (!currentUserId)
    return message.error('Không tìm thấy ID người dùng!')

  try {
    isFetchingProfile.value = true
    const res = await getStaffById(currentUserId)
    // Hỗ trợ cả trường hợp API trả về thẳng object hoặc bọc trong data
    const data = res?.data || res
    staffProfile.value = data
    await translateAddress(data)
  }
  catch (error) {
    message.error('Không thể tải thông tin nhân viên')
    addressText.value = 'N/A'
  }
  finally {
    isFetchingProfile.value = false
  }
}

async function translateAddress(data: any) {
  if (!data.provinceCode || !data.communeCode) {
    addressText.value = data.hometown || 'N/A'
    return
  }
  try {
    const [provRes, wardRes] = await Promise.all([
      fetch(`https://provinces.open-api.vn/api/p/${data.provinceCode}`),
      fetch(`https://provinces.open-api.vn/api/w/${data.communeCode}`),
    ])
    const provData = await provRes.json()
    const wardData = await wardRes.json()
    addressText.value = [data.hometown, wardData.name, provData.name].filter(Boolean).join(', ')
  }
  catch (error) {
    addressText.value = [data.hometown, data.communeCode, data.provinceCode].filter(Boolean).join(', ')
  }
}

function handleSelect(key: string | number) {
  if (key === 'loginOut') {
    window.$dialog?.success({
      title: 'Xác nhận đăng xuất',
      content: 'Bạn có chắc chắn muốn đăng xuất không?',
      positiveText: 'Đăng xuất',
      negativeText: 'Hủy bỏ',
      positiveButtonProps: { type: 'success' },
      onPositiveClick: async () => {
        await logout()
        router.push('/login-admin')
      },
    })
  }
  if (key === 'userCenter') {
    showProfileModal.value = true
    loadStaffProfile()
  }
}

// -------------------------------------------------------------
// ĐỔI MẬT KHẨU
// -------------------------------------------------------------
async function handleChangePassword() {
  try {
    await formRef.value?.validate()
    isSubmittingPwd.value = true

    await changePassword({
      matKhauCu: pwdFormValue.value.oldPassword,
      matKhauMoi: pwdFormValue.value.newPassword,
    })

    message.success('Đổi mật khẩu thành công!')
    showProfileModal.value = false
    pwdFormValue.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  }
  catch (error: any) {
    let errorMsg = 'Đổi mật khẩu thất bại!'
    const responseData = error?.response?.data
    if (typeof responseData === 'string')
      errorMsg = responseData
    else if (responseData && typeof responseData === 'object')
      errorMsg = responseData.message || responseData.error || errorMsg
    else if (error?.message)
      errorMsg = error.message
    message.error(errorMsg)
  }
  finally {
    isSubmittingPwd.value = false
  }
}

// -------------------------------------------------------------
// CÔNG CỤ XỬ LÝ ẢNH (DỊCH HEIC & NÉN CANVAS)
// -------------------------------------------------------------
async function processAndCompressImage(file: File, maxSizeMB: number, maxWidth = 800): Promise<File> {
  let fileToProcess = file

  // 1. DỊCH ẢNH HEIC SANG JPEG (Dành cho iPhone)
  if (file.type === 'image/heic' || file.name.toLowerCase().endsWith('.heic')) {
    try {
      const convertedBlob = await heic2any({
        blob: file,
        toType: 'image/jpeg',
        quality: 0.8,
      })
      const blob = Array.isArray(convertedBlob) ? convertedBlob[0] : convertedBlob
      fileToProcess = new File([blob], file.name.replace(/\.[^/.]+$/, '.jpg'), { type: 'image/jpeg' })
    }
    catch (error) {
      throw new Error('Không thể đọc định dạng ảnh HEIC của iPhone.')
    }
  }

  // 2. NÉN ẢNH CANVAS
  return new Promise((resolve, reject) => {
    if (fileToProcess.size / 1024 / 1024 <= maxSizeMB) {
      resolve(fileToProcess)
      return
    }

    const reader = new FileReader()
    reader.readAsDataURL(fileToProcess)
    reader.onload = (event) => {
      const img = new Image()
      img.src = event.target?.result as string

      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        if (width > maxWidth) {
          height = Math.round((height * maxWidth) / width)
          width = maxWidth
        }

        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (blob) {
              const compressedFile = new File([blob], fileToProcess.name, {
                type: 'image/jpeg',
                lastModified: Date.now(),
              })
              resolve(compressedFile)
            }
            else {
              reject(new Error('Lỗi khi nén ảnh!'))
            }
          },
          'image/jpeg',
          0.7, // Chất lượng ảnh sau nén (70%)
        )
      }
    }
    reader.onerror = error => reject(error)
  })
}

// -------------------------------------------------------------
// UPLOAD AVATAR LÊN SERVER VÀ ĐỒNG BỘ STORE
// -------------------------------------------------------------
async function handleAvatarChange(options: { fileList: UploadFileInfo[] }) {
  const fileInfo = options.fileList[0]
  if (!fileInfo || !fileInfo.file)
    return

  const actualFile = fileInfo.file as File
  const type = actualFile.type || ''
  const name = actualFile.name || ''

  // Bộ lọc cho phép file ảnh và .heic
  const isImage = type.includes('image/') || /\.(jpg|jpeg|png|webp|gif|heic)$/i.test(name)
  if (!isImage) {
    message.error('Chỉ hỗ trợ tải lên file ảnh (JPG, PNG, HEIC, WEBP)!')
    options.fileList.pop()
    return
  }

  const currentUserId = userInfo.value?.id || userInfo.value?.userId || userInfo.value?.staffId

  try {
    isUploadingAvatar.value = true

    // 1. Xử lý ảnh (Dịch HEIC, Nén dung lượng)
    const finalFile = await processAndCompressImage(actualFile, 2, 800)

    // 2. Gọi API Upload ảnh thật lên server để lấy URL
    const uploadRes = await uploadAvatar(finalFile)
    const newImageUrl = uploadRes?.url || uploadRes?.data?.url || uploadRes

    if (!newImageUrl || typeof newImageUrl !== 'string') {
      throw new Error('Không lấy được đường dẫn ảnh từ server!')
    }

    // 3. Chuẩn bị Payload cập nhật thông tin nhân viên (Gắn lại Role để tránh lỗi 400)
    const updatePayload = {
      fullName: staffProfile.value.fullName || staffProfile.value.name,
      role: staffProfile.value.account?.roleConstant || staffProfile.value.roleConstant || staffProfile.value.role || 'NHAN_VIEN',
      birthday: staffProfile.value.birthday,
      citizenIdentifyCard: staffProfile.value.citizenIdentifyCard,
      hometown: staffProfile.value.hometown,
      phone: staffProfile.value.phone,
      email: staffProfile.value.email,
      gender: staffProfile.value.gender,
      provinceCode: staffProfile.value.provinceCode,
      districtCode: staffProfile.value.districtCode || null,
      communeCode: staffProfile.value.communeCode,
      avatarUrl: newImageUrl,
    }

    // 4. Gọi API cập nhật thông tin xuống DB
    await updateStaff(currentUserId, updatePayload)

    // 5. Cập nhật hiển thị trong Modal
    staffProfile.value.avatarUrl = newImageUrl

    // 6. ĐỒNG BỘ ẢNH LÊN HEADER (Bằng Store và LocalStorage)
    const authStore = useAuthStore()

    // Nếu bạn đã viết action updateUserProfile trong store thì gọi hàm này
    if (typeof authStore.updateUserProfile === 'function') {
      authStore.updateUserProfile({
        avatar: newImageUrl,
        pictureUrl: newImageUrl,
      })
    }
    else {
      // Fallback: Tự động ghi đè thẳng vào store và local nếu chưa có action
      if (authStore.userInfo)
        authStore.userInfo.pictureUrl = newImageUrl
      if (authStore.userInfoDatn)
        authStore.userInfoDatn.pictureUrl = newImageUrl

      const currentUserLocal = localStorageAction.get(USER_INFO_STORAGE_KEY)
      if (currentUserLocal) {
        currentUserLocal.avatar = newImageUrl
        currentUserLocal.pictureUrl = newImageUrl
        localStorageAction.set(USER_INFO_STORAGE_KEY, currentUserLocal)
      }
    }

    // Cập nhật biến local để góc trên cùng nhảy ảnh ngay lập tức
    userInfo.value.pictureUrl = newImageUrl
    userInfo.value.avatar = newImageUrl

    message.success('Cập nhật ảnh đại diện thành công!')
  }
  catch (error: any) {
    console.error('Lỗi upload avatar:', error)
    message.error(error.message || 'Lỗi khi lưu ảnh lên hệ thống!')
  }
  finally {
    isUploadingAvatar.value = false
  }
}
</script>

<template>
  <n-dropdown trigger="click" :options="options" @select="handleSelect">
    <n-avatar round class="cursor-pointer" size="small" :src="userInfo?.pictureUrl || userInfo?.avatar">
      <template #fallback>
        <div class="wh-full flex-center">
          <icon-park-outline-user />
        </div>
      </template>
    </n-avatar>
  </n-dropdown>

  <n-modal v-model:show="showProfileModal">
    <n-card
      style="width: 500px; border-radius: 12px;" title="Quản lý tài khoản" :bordered="false" size="huge"
      role="dialog" aria-modal="true"
    >
      <n-tabs type="line" animated justify-content="space-evenly">
        <n-tab-pane name="info" tab="Thông tin chung">
          <n-spin :show="isFetchingProfile">
            <div class="flex flex-col items-center gap-3 py-4">
              <div class="relative group cursor-pointer">
                <n-spin :show="isUploadingAvatar">
                  <n-upload
                    action=""
                    :show-file-list="false"
                    accept="image/png, image/jpeg, image/webp, .heic"
                    :custom-request="() => {}"
                    @change="handleAvatarChange"
                  >
                    <n-avatar
                      round
                      :size="100"
                      :src="staffProfile?.avatarUrl"
                      fallback-src="https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg"
                      class="transition-opacity group-hover:opacity-80 object-cover"
                    />
                    <div class="absolute inset-0 flex items-center justify-center rounded-full bg-black bg-opacity-40 opacity-0 group-hover:opacity-100 transition-opacity">
                      <n-icon size="30" color="white">
                        <IconCamera />
                      </n-icon>
                    </div>
                  </n-upload>
                </n-spin>
              </div>

              <div class="text-center mt-2">
                <h2 class="text-xl font-bold m-0">
                  {{ staffProfile?.name || staffProfile?.fullName || 'Đang tải...' }}
                </h2>
                <n-tag
                  :type="displayRole.type"
                  size="small"
                  class="mt-1 font-semibold"
                >
                  {{ displayRole.text }}
                </n-tag>
              </div>

              <div class="w-full mt-4">
                <n-form label-placement="left" label-width="120">
                  <n-form-item label="Mã nhân viên">
                    <n-input :value="staffProfile?.code || 'N/A'" readonly />
                  </n-form-item>
                  <n-form-item label="Email">
                    <n-input :value="staffProfile?.email || 'N/A'" readonly />
                  </n-form-item>
                  <n-form-item label="Số điện thoại">
                    <n-input :value="staffProfile?.phone || 'N/A'" readonly />
                  </n-form-item>
                  <n-form-item label="Địa chỉ">
                    <n-input :value="addressText" readonly type="textarea" :autosize="{ minRows: 1, maxRows: 3 }" />
                  </n-form-item>
                </n-form>
              </div>
            </div>
          </n-spin>
        </n-tab-pane>

        <n-tab-pane name="password" tab="Đổi mật khẩu">
          <div class="pt-4">
            <n-form ref="formRef" :model="pwdFormValue" :rules="rules" label-placement="top">
              <n-form-item label="Mật khẩu hiện tại" path="oldPassword">
                <n-input
                  v-model:value="pwdFormValue.oldPassword" type="password" show-password-on="click"
                  placeholder="Nhập mật khẩu hiện tại" @keyup.enter="handleChangePassword"
                />
              </n-form-item>

              <n-form-item label="Mật khẩu mới" path="newPassword">
                <n-input
                  v-model:value="pwdFormValue.newPassword" type="password" show-password-on="click"
                  placeholder="Nhập mật khẩu mới" @keyup.enter="handleChangePassword"
                />
              </n-form-item>

              <n-form-item label="Xác nhận mật khẩu mới" path="confirmPassword">
                <n-input
                  v-model:value="pwdFormValue.confirmPassword" type="password" show-password-on="click"
                  placeholder="Nhập lại mật khẩu mới" @keyup.enter="handleChangePassword"
                />
              </n-form-item>
            </n-form>

            <div class="flex justify-end gap-3 mt-4">
              <n-button @click="showProfileModal = false">
                Đóng
              </n-button>
              <n-button type="primary" :loading="isSubmittingPwd" @click="handleChangePassword">
                Cập nhật
              </n-button>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </n-modal>
</template>

<style scoped></style>
