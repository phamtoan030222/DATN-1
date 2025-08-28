<!-- <script setup lang="ts">
import { ref, watch } from "vue";
import {
  NModal,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NSpace,
  NSwitch,
} from "naive-ui";
import type { Ref } from "vue";
import {
  createBrand,
  updateBrand,
} from "@/service/api/admin/products/attribute/brand";

interface ModalState {
  visible: boolean;
  mode: "add" | "edit";
  rowData: AppBrand.RowBrand | null;
}

// --- state ---
const modalState = ref<ModalState>({
  visible: false,
  mode: "add",
  rowData: null,
});

const formRef = ref<InstanceType<typeof NForm> | null>(null);
const formData = ref({
  code: "",
  name: "",
  status: true,
});

// --- methods ---
function openModal(mode: "add" | "edit", row?: AppBrand.RowBrand) {
  modalState.value.mode = mode;
  modalState.value.visible = true;
  if (mode === "edit" && row) {
    modalState.value.rowData = row;
    formData.value.code = row.code;
    formData.value.name = row.name;
    formData.value.status = row.status === "active";
  } else {
    modalState.value.rowData = null;
    formData.value.code = "";
    formData.value.name = "";
    formData.value.status = true;
  }
}

function closeModal() {
  modalState.value.visible = false;
}

async function handleSubmit() {
  const dataToSend: Partial<AppBrand.RowBrand> = {
    code: formData.value.code,
    name: formData.value.name,
    status: formData.value.status ? "active" : "inactive",
  };

  if (modalState.value.mode === "add") {
    await createBrand(dataToSend);
    NMessage.success("Thêm thương hiệu thành công");
  } else if (modalState.value.mode === "edit" && modalState.value.rowData) {
    await updateBrand(modalState.value.rowData.id, dataToSend);
    NMessage.success("Cập nhật thương hiệu thành công");
  }

  closeModal();
  // gọi lại component cha để refresh data nếu cần
  emitRefresh();
}

// emit event lên component cha
const emitRefresh = () => {
  // @ts-ignore
  emit("refresh");
};
</script>

<template>
  <NModal
    v-model:show="modalState.visible"
    :title="
      modalState.mode === 'add'
        ? 'Thêm mới thương hiệu'
        : 'Chỉnh sửa thương hiệu'
    "
    mask-closable
  >
    <NForm ref="formRef" labelPlacement="left" labelWidth="120px">
      <NFormItem label="Mã">
        <NInput
          v-model:value="formData.code"
          placeholder="Nhập mã thương hiệu"
        />
      </NFormItem>
      <NFormItem label="Tên thương hiệu">
        <NInput
          v-model:value="formData.name"
          placeholder="Nhập tên thương hiệu"
        />
      </NFormItem>
      <NFormItem label="Trạng thái">
        <NSwitch
          v-model:value="formData.status"
          checked-value="true"
          unchecked-value="false"
        />
      </NFormItem>
    </NForm>

    <NSpace justify="end" style="margin-top: 16px">
      <NButton @click="closeModal">Hủy</NButton>
      <NButton type="primary" @click="handleSubmit">
        {{ modalState.mode === "add" ? "Thêm mới" : "Cập nhật" }}
      </NButton>
    </NSpace>
  </NModal>
</template> -->
