<script setup lang="tsx">
import { onMounted, ref, reactive } from "vue";
import {
  NButton,
  NSpace,
  NCard,
  NDataTable,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NGrid,
  NGridItem,
  NPopconfirm,
  NSwitch,
  useMessage,
  NPagination,
  NIcon,
} from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  getAllMaterials,
  createMaterial,
  updateMaterial,
  updateMaterialStatus,
  type MaterialResponse,
  type CreateMaterialRequest,
} from "@/service/api/admin/product/material.api";

// ================= STATE =================
const message = useMessage();
const tableData = ref<MaterialResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);
const searchKeyword = ref("");

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= MODAL =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<MaterialResponse | null>(null);

const formData = reactive<CreateMaterialRequest>({
  code: "",
  topCaseMaterial: "",
  bottomCaseMaterial: "",
  keyboardMaterial: "",
});

// ================= API CALL =================
async function fetchMaterials() {
  loading.value = true;
  try {
    const res = await getAllMaterials({
      page: currentPage.value,
      size: pageSize.value,
      name: searchKeyword.value || undefined,
    });
    tableData.value = res.items;
    total.value = res.totalItems;
  } catch {
    message.error("Không thể tải dữ liệu Chất liệu");
  } finally {
    loading.value = false;
  }
}

onMounted(fetchMaterials);

// ================= CRUD =================
function openModal(mode: "add" | "edit", row?: MaterialResponse) {
  modalMode.value = mode;
  if (mode === "edit" && row) {
    modalRow.value = row;
    formData.code = row.code;
    formData.topCaseMaterial = row.topCaseMaterial || "";
    formData.bottomCaseMaterial = row.bottomCaseMaterial || "";
    formData.keyboardMaterial = row.keyboardMaterial || "";
  } else {
    modalRow.value = null;
    formData.code = "";
    formData.topCaseMaterial = "";
    formData.bottomCaseMaterial = "";
    formData.keyboardMaterial = "";
  }
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveMaterial() {
  if (!formData.code) {
    message.warning("Vui lòng nhập Mã chất liệu");
    return;
  }

  try {
    if (modalMode.value === "add") {
      await createMaterial(formData);
      message.success("Thêm Chất liệu thành công");
    } else if (modalMode.value === "edit" && modalRow.value) {
      await updateMaterial(modalRow.value.id, formData);
      message.success("Cập nhật Chất liệu thành công");
    }
    closeModal();
    fetchMaterials();
  } catch {
    message.error("Có lỗi xảy ra khi lưu Chất liệu");
  }
}

async function handleDelete(id: string) {
  try {
    // TODO: Thêm API xóa Material
    message.info("TODO: Thêm API xóa chất liệu");
  } catch {
    message.error("Xóa thất bại");
  }
}

async function handleDeleteSelected() {
  if (!checkedRowKeys.value.length) {
    message.warning("Chưa chọn chất liệu nào để xóa");
    return;
  }
  // TODO: gọi API xóa hàng loạt
  message.info("TODO: Thêm API xóa nhiều chất liệu");
}

async function handleStatusChange(row: MaterialResponse, value: boolean) {
  try {
    await updateMaterialStatus(row.id, value ? "ACTIVE" : "INACTIVE");
    message.success(`Cập nhật trạng thái ${row.code} thành công`);
    fetchMaterials();
  } catch {
    message.error("Cập nhật trạng thái thất bại");
  }
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: "selection" as const },
  { title: "Mã", key: "code" },
  { title: "Top Case", key: "topCaseMaterial" },
  { title: "Bottom Case", key: "bottomCaseMaterial" },
  { title: "Keyboard", key: "keyboardMaterial" },
  {
    title: "Trạng thái",
    key: "status",
    render(row: MaterialResponse) {
      return (
        <NSwitch
          value={row.status === "ACTIVE"}
          onUpdateValue={(val: boolean) => handleStatusChange(row, val)}
        />
      );
    },
  },
  {
    title: "Thao tác",
    key: "actions",
    render(row: MaterialResponse) {
      return (
        <NSpace>
          <NButton
            size="small"
            quaternary
            circle
            onClick={() => openModal("edit", row)}
          >
            <Icon icon="carbon:edit" width="18" />
          </NButton>
          <NPopconfirm onPositiveClick={() => handleDelete(row.id)}>
            {{
              trigger: () => (
                <NButton size="small" quaternary circle type="error">
                  <Icon icon="carbon:trash-can" width="18" />
                </NButton>
              ),
              default: () => "Bạn có chắc muốn xóa?",
            }}
          </NPopconfirm>
        </NSpace>
      );
    },
  },
];

// ================= SEARCH & PAGINATION =================
function handleSearch() {
  currentPage.value = 1;
  fetchMaterials();
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchMaterials();
}

function refreshTable() {
  fetchMaterials();
}
</script>

<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon :icon="'mdi:material-design'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Quản lý Chất liệu
        </span>
      </NSpace>
      <span>Quản lý danh sách chất liệu sản phẩm</span>
    </NSpace>
  </n-card>

  <!-- Table -->
  <NCard title="Danh sách Chất liệu" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NInput
          v-model:value="searchKeyword"
          placeholder="Tìm kiếm chất liệu..."
          clearable
          style="width: 220px"
          @input="handleSearch"
        >
          <template #prefix>
            <NIcon size="18">
              <Icon :icon="'carbon:search'" />
            </NIcon>
          </template>
        </NInput>
        <NButton
          type="primary"
          circle
          title="Thêm mới"
          @click="openModal('add')"
        >
          <NIcon size="24">
            <Icon :icon="'carbon:add'" />
          </NIcon>
        </NButton>
        <NButton
          type="primary"
          secondary
          circle
          title="Làm mới"
          @click="refreshTable"
        >
          <NIcon size="24">
            <Icon :icon="'carbon:rotate'" />
          </NIcon>
        </NButton>
        <NPopconfirm @positive-click="handleDeleteSelected">
          <template #trigger>
            <NButton type="error" secondary circle title="Xóa hàng loạt">
              <NIcon size="24">
                <Icon :icon="'icon-park-outline:delete'" />
              </NIcon>
            </NButton>
          </template>
          Xác nhận xóa tất cả chất liệu đã chọn?
        </NPopconfirm>
      </NSpace>
    </template>

    <NDataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="checkedRowKeys"
      :pagination="false"
      bordered
    />

    <div class="flex justify-center mt-4">
      <NPagination
        :page="currentPage"
        :page-size="pageSize"
        :page-count="Math.ceil(total / pageSize)"
        @update:page="handlePageChange"
      />
    </div>
  </NCard>

  <!-- Modal thêm/sửa -->
  <NModal
    v-model:show="showModal"
    preset="card"
    style="width: 500px"
    title="Chất liệu"
  >
    <NForm>
      <NGrid cols="1" x-gap="12">
        <NGridItem>
          <NFormItem label="Mã chất liệu" required>
            <NInput
              v-model:value="formData.code"
              placeholder="Nhập mã chất liệu"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Top Case">
            <NInput
              v-model:value="formData.topCaseMaterial"
              placeholder="Nhập chất liệu top case"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Bottom Case">
            <NInput
              v-model:value="formData.bottomCaseMaterial"
              placeholder="Nhập chất liệu bottom case"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Keyboard">
            <NInput
              v-model:value="formData.keyboardMaterial"
              placeholder="Nhập chất liệu keyboard"
            />
          </NFormItem>
        </NGridItem>
      </NGrid>
    </NForm>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="closeModal">Hủy</NButton>
        <NButton type="primary" @click="saveMaterial">Lưu</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
