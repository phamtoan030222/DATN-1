<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useDialog, useNotification } from 'naive-ui'
import type { FormInst, FormRules, UploadCustomRequestOptions } from 'naive-ui'
import heic2any from 'heic2any'

import { changeCustomerPassword, postInformation } from '@/service/api/client/customer/customer.api'
import { uploadAvatar } from '@/service/api/admin/users/customer/customer'
import { useAuthStore } from '@/store'

const authStore = useAuthStore()
const { userInfoDatn } = storeToRefs(authStore)
const notification = useNotification()
const dialog = useDialog()

// ==========================================
// STATE: THÔNG TIN HỒ SƠ
// ==========================================
const isSubmittingProfile = ref(false)
const isUploadingAvatar = ref(false)

interface ProfileState {
  username: string
  name: string
  email: string
  phone: string
  avatarUrl: string
}

const profile = ref<ProfileState>({
  username: userInfoDatn.value?.username ?? '',
  name: userInfoDatn.value?.fullName ?? '',
  email: userInfoDatn.value?.email as string ?? '',
  phone: userInfoDatn.value?.phone as string ?? '',
  avatarUrl: userInfoDatn.value?.pictureUrl || userInfoDatn.value?.avatarUrl || '',
})

// ==========================================
// STATE: ĐỔI MẬT KHẨU
// ==========================================
const pwdFormRef = ref<FormInst | null>(null)
const isSubmittingPwd = ref(false)
const pwdFormValue = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules: FormRules = {
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

// ==========================================
// LOGIC: ĐỔI MẬT KHẨU
// ==========================================
async function handleUpdatePassword() {
  try {
    await pwdFormRef.value?.validate()
    isSubmittingPwd.value = true

    await changeCustomerPassword({
      matKhauCu: pwdFormValue.value.oldPassword,
      matKhauMoi: pwdFormValue.value.newPassword,
    })

    notification.success({ content: 'Đổi mật khẩu thành công!', duration: 3000 })
    pwdFormValue.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  }
  catch (error: any) {
    let errorMsg = 'Đổi mật khẩu thất bại!'
    const responseData = error?.response?.data
    if (typeof responseData === 'string')
      errorMsg = responseData
    else if (responseData && typeof responseData === 'object')
      errorMsg = responseData.message || responseData.error || errorMsg
    else if (error?.message && !error.message.includes('Validate'))
      errorMsg = error.message

    if (!errorMsg.includes('Validate')) {
      notification.error({ content: errorMsg, duration: 3000 })
    }
  }
  finally {
    isSubmittingPwd.value = false
  }
}

// ==========================================
// LOGIC: XỬ LÝ ẢNH (HEIC -> JPEG -> CANVAS)
// ==========================================
async function processAndCompressImage(file: File, maxSizeMB: number, maxWidth = 800): Promise<File> {
  let fileToProcess = file

  if (file.type === 'image/heic' || file.name.toLowerCase().endsWith('.heic')) {
    try {
      const convertedBlob = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.8 })
      const blob = Array.isArray(convertedBlob) ? convertedBlob[0] : convertedBlob
      fileToProcess = new File([blob], file.name.replace(/\.[^/.]+$/, '.jpg'), { type: 'image/jpeg' })
    }
    catch (error) {
      throw new Error('Không thể đọc định dạng ảnh HEIC của iPhone.')
    }
  }

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
            if (blob)
              resolve(new File([blob], fileToProcess.name, { type: 'image/jpeg', lastModified: Date.now() }))
            else reject(new Error('Lỗi khi nén ảnh!'))
          },
          'image/jpeg',
          0.7,
        )
      }
    }
    reader.onerror = error => reject(error)
  })
}

// ==========================================
// LOGIC: ĐỔI ẢNH ĐẠI DIỆN (ĐỘC LẬP & TỰ ĐỘNG LƯU)
// ==========================================
async function handleCustomUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const actualFile = file.file as File | null | undefined
  if (!actualFile) {
    onError()
    return
  }

  const type = actualFile.type || ''
  const name = actualFile.name || ''
  const isImage = type.includes('image/') || /\.(jpg|jpeg|png|webp|gif|heic)$/i.test(name)

  if (!isImage) {
    notification.error({ content: 'Chỉ hỗ trợ tải lên file ảnh!', duration: 3000 })
    onError()
    return
  }

  try {
    isUploadingAvatar.value = true

    const finalFile = await processAndCompressImage(actualFile, 2, 800)
    const uploadRes = await uploadAvatar(finalFile)

    let newImageUrl = uploadRes?.url || uploadRes?.data?.url || uploadRes
    if (!newImageUrl || typeof newImageUrl !== 'string') {
      throw new Error('Không lấy được đường dẫn ảnh từ server!')
    }

    if (!newImageUrl.startsWith('http')) {
      newImageUrl = `http://localhost:2345${newImageUrl.startsWith('/') ? '' : '/'}${newImageUrl}`
    }

    profile.value.avatarUrl = newImageUrl

    // Tự động gọi API lưu thông tin
    await postInformation({
      fullName: profile.value.name,
      email: profile.value.email,
      phoneNumber: profile.value.phone,
      avatarUrl: profile.value.avatarUrl,
    })

    // ÉP CẬP NHẬT GIAO DIỆN (Chữa cháy lỗi Reactivity của Pinia)
    if (userInfoDatn.value) {
      userInfoDatn.value.pictureUrl = newImageUrl
      userInfoDatn.value.avatarUrl = newImageUrl
    }

    // Gọi store
    if (typeof authStore.updateUserProfile === 'function') {
      authStore.updateUserProfile({
        avatar: newImageUrl,
        pictureUrl: newImageUrl,
      })
    }

    // XỬ LÝ FIX LỖI TẠM THỜI CHO VIỆC F5 (NẾU BẠN CHƯA CẬP NHẬT TRONG STORE)
    const storageKey = 'user-info' // LƯU Ý: Đổi tên key này thành tên key bạn đang dùng trong DevTools (ví dụ: 'user', 'userInfo', 'auth')
    const localData = localStorage.getItem(storageKey)
    if (localData) {
      try {
        const parsed = JSON.parse(localData)
        parsed.pictureUrl = newImageUrl
        parsed.avatarUrl = newImageUrl
        localStorage.setItem(storageKey, JSON.stringify(parsed))
      }
      catch (e) {
        // Bỏ qua lỗi parse
      }
    }

    notification.success({ content: 'Cập nhật ảnh đại diện thành công!', duration: 3000 })
    onFinish()
  }
  catch (error: any) {
    notification.error({ content: error.message || 'Lỗi khi lưu ảnh!', duration: 3000 })
    onError()
  }
  finally {
    isUploadingAvatar.value = false
  }
}

// ==========================================
// LOGIC: LƯU THÔNG TIN HỒ SƠ CÓ XÁC NHẬN
// ==========================================
function confirmSaveProfile() {
  dialog.warning({
    title: 'Xác nhận lưu thay đổi',
    content: 'Bạn có chắc chắn muốn cập nhật thông tin hồ sơ này?',
    positiveText: 'Lưu thay đổi',
    negativeText: 'Hủy bỏ',
    // Cấu hình đổi màu nút xác nhận thành xanh lá
    positiveButtonProps: { color: '#16a34a', textColor: '#fff' },
    onPositiveClick: () => {
      handleClickSave()
    },
  })
}

async function handleClickSave() {
  try {
    isSubmittingProfile.value = true

    await postInformation({
      fullName: profile.value.name,
      email: profile.value.email,
      phoneNumber: profile.value.phone,
      avatarUrl: profile.value.avatarUrl,
    })

    // ÉP CẬP NHẬT GIAO DIỆN NẾU THAY ĐỔI TÊN
    if (userInfoDatn.value) {
      userInfoDatn.value.fullName = profile.value.name
    }

    if (typeof authStore.updateUserProfile === 'function') {
      authStore.updateUserProfile({
        fullName: profile.value.name,
        avatar: profile.value.avatarUrl,
        pictureUrl: profile.value.avatarUrl,
      })
    }

    notification.success({ content: 'Cập nhật thông tin thành công', duration: 3000 })
  }
  catch (e) {
    notification.error({ content: 'Lỗi khi thay đổi. Vui lòng kiểm tra lại', duration: 3000 })
  }
  finally {
    isSubmittingProfile.value = false
  }
}
</script>

<template>
  <div class="bg-white min-h-screen py-10">
    <div class="bg-white p-6 md:p-10 rounded-lg shadow-md border border-gray-100 w-full max-w-[1100px] mx-auto">
      <n-tabs type="line" animated size="large" class="custom-tabs">
        <n-tab-pane name="profile" tab="Hồ sơ của tôi">
          <div class="mt-4">
            <div class="text-gray-500 mb-6">
              Quản lý thông tin hồ sơ để bảo mật tài khoản
            </div>

            <div class="flex flex-col-reverse md:flex-row">
              <div class="w-full md:w-2/3 md:pr-14 md:border-r border-gray-100">
                <n-form label-placement="left" label-width="140" require-mark-placement="right-hanging">
                  <n-form-item label="Tên đăng nhập">
                    <div class="text-gray-700 font-medium">
                      {{ profile.username }}
                    </div>
                  </n-form-item>

                  <n-form-item label="Tên">
                    <n-input v-model:value="profile.name" placeholder="Nhập tên của bạn" size="large" />
                  </n-form-item>

                  <n-form-item label="Email">
                    <n-input v-model:value="profile.email" placeholder="Nhập email" size="large" />
                  </n-form-item>

                  <n-form-item label="Số điện thoại">
                    <n-input v-model:value="profile.phone" placeholder="Nhập số điện thoại" size="large" />
                  </n-form-item>

                  <div class="ml-[140px] mt-6">
                    <n-button
                      type="primary" size="large"
                      class="w-[120px] bg-green-600 hover:bg-green-700 font-semibold border-none"
                      :loading="isSubmittingProfile"
                      @click="confirmSaveProfile"
                    >
                      Lưu
                    </n-button>
                  </div>
                </n-form>
              </div>

              <div class="w-full md:w-1/3 flex flex-col items-center pt-4 md:pl-10 mb-8 md:mb-0">
                <div class="relative group cursor-pointer">
                  <n-spin :show="isUploadingAvatar">
                    <n-upload
                      action=""
                      :show-file-list="false"
                      accept="image/png, image/jpeg, image/webp, .heic"
                      :custom-request="handleCustomUpload"
                    >
                      <n-avatar
                        round :size="120" :src="profile.avatarUrl"
                        fallback-src="https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg"
                        class="border border-gray-200 transition-opacity group-hover:opacity-80 object-cover"
                      />
                      <div
                        class="absolute inset-0 flex items-center justify-center rounded-full bg-black bg-opacity-40 opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <span class="text-white text-sm font-medium">Sửa ảnh</span>
                      </div>
                    </n-upload>
                  </n-spin>
                </div>
                <div class="mt-4 text-gray-400 text-sm text-center">
                  <p>Dụng lượng file tối đa 2 MB</p>
                  <p>Định dạng: JPEG, PNG, WEBP, HEIC</p>
                </div>
              </div>
            </div>
          </div>
        </n-tab-pane>

        <n-tab-pane name="password" tab="Đổi mật khẩu">
          <div class="mt-4">
            <div class="text-gray-500 mb-8">
              Để bảo mật tài khoản, vui lòng không chia sẻ mật khẩu cho người khác
            </div>

            <div class="w-full md:w-1/2">
              <n-form
                ref="pwdFormRef" :model="pwdFormValue" :rules="pwdRules" label-placement="left" label-width="180"
                require-mark-placement="right-hanging"
              >
                <n-form-item label="Mật khẩu hiện tại" path="oldPassword">
                  <n-input
                    v-model:value="pwdFormValue.oldPassword" type="password" show-password-on="click"
                    placeholder="Nhập mật khẩu hiện tại" size="large" @keyup.enter="handleUpdatePassword"
                  />
                </n-form-item>

                <n-form-item label="Mật khẩu mới" path="newPassword">
                  <n-input
                    v-model:value="pwdFormValue.newPassword" type="password" show-password-on="click"
                    placeholder="Nhập mật khẩu mới" size="large" @keyup.enter="handleUpdatePassword"
                  />
                </n-form-item>

                <n-form-item label="Xác nhận mật khẩu mới" path="confirmPassword">
                  <n-input
                    v-model:value="pwdFormValue.confirmPassword" type="password" show-password-on="click"
                    placeholder="Nhập lại mật khẩu mới" size="large" @keyup.enter="handleUpdatePassword"
                  />
                </n-form-item>

                <div class="ml-[180px] mt-6">
                  <n-button
                    type="primary" size="large" class="w-[120px] bg-green-600 hover:bg-green-700 font-semibold border-none"
                    :loading="isSubmittingPwd" @click="handleUpdatePassword"
                  >
                    Cập nhật
                  </n-button>
                </div>
              </n-form>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>
  </div>
</template>

<style scoped>
:deep(.custom-tabs .n-tabs-tab) {
  font-size: 16px;
  font-weight: 500;
  padding: 12px 20px;
}
</style>
