<script setup lang="ts">
import { useAuthStore } from "@/store";

const authStore = useAuthStore();
const { userInfo } = authStore;

const formRef = ref();
const formValue = ref({
  user: {
    name: "",
    age: "",
  },
  phone: "",
});
const rules = {
  user: {
    name: {
      required: true,
      message: "Vui lòng nhập tên",
      trigger: "blur",
    },
    age: {
      required: true,
      message: "Vui lòng nhập tuổi",
      trigger: ["input", "blur"],
    },
  },
  phone: {
    required: true,
    message: "Vui lòng nhập số điện thoại",
    trigger: ["input"],
  },
};

function handleValidateClick() {
  formRef.value?.validate((errors: any) => {
    if (!errors) window.$message.success("Xác thực thành công");
    else window.$message.error("Xác thực không thành công");
  });
}
</script>

<template>
  <n-space vertical>
    <n-card title="Thông tin cá nhân">
      <n-space size="large">
        <n-avatar round :size="128" :src="userInfo?.avatar" />

        <n-descriptions
          label-placement="left"
          :column="2"
          :title="`Xin chào buổi tối, ${userInfo?.nickname}, đây là mẫu trung tâm cá nhân đơn giản`"
        >
          <n-descriptions-item label="ID">
            {{ userInfo?.id }}
          </n-descriptions-item>
          <n-descriptions-item label="Tên đăng nhập">
            {{ userInfo?.userName }}
          </n-descriptions-item>
          <n-descriptions-item label="Tên thật">
            {{ userInfo?.nickname }}
          </n-descriptions-item>
          <n-descriptions-item label="Vai trò">
            {{ userInfo?.role }}
          </n-descriptions-item>
        </n-descriptions>
      </n-space>
    </n-card>

    <n-card title="Chỉnh sửa thông tin">
      <n-space justify="center">
        <n-form
          ref="formRef"
          class="w-500px"
          :label-width="80"
          :model="formValue"
          :rules="rules"
        >
          <n-form-item label="Tên" path="user.name">
            <n-input
              v-model:value="formValue.user.name"
              placeholder="Nhập tên"
            />
          </n-form-item>

          <n-form-item label="Tuổi" path="user.age">
            <n-input
              v-model:value="formValue.user.age"
              placeholder="Nhập tuổi"
            />
          </n-form-item>

          <n-form-item label="Số điện thoại" path="phone">
            <n-input
              v-model:value="formValue.phone"
              placeholder="Nhập số điện thoại"
            />
          </n-form-item>

          <n-form-item>
            <n-button
              type="primary"
              attr-type="button"
              block
              @click="handleValidateClick"
            >
              Xác thực
            </n-button>
          </n-form-item>
        </n-form>
      </n-space>
    </n-card>
  </n-space>
</template>

<style scoped></style>
