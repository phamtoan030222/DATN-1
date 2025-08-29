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
  getAllOperatingSystems,
  createOperatingSystem,
  updateOperatingSystem,
  updateOperatingSystemStatus,
  type OperatingSystemResponse,
  type CreateOperatingSystemRequest,
} from "@/service/api/admin/product/operating.api";

// ================= STATE =================
const message = useMessage();
const tableData = ref<OperatingSystemResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);
const searchKeyword = ref("");

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= MODAL =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<OperatingSystemResponse | null>(null);

const formData = reactive<CreateOperatingSystemRequest>({
  code: "",
  name: "",
  version: "",
  description: "",
});

// ================= API CALL =================
async function fetchOperatingSystems() {
  loading.value = true;
  try {
    const res = await getAllOperatingSystems({
      page: currentPage.value,
      size: pageSize.value,
      name: searchKeyword.value || undefined,
    });
    tableData.value = res.items;
    total.value = res.totalItems;
  } catch (e) {
    message.error("Không thể tải dữ liệu Hệ điều hành");
  } finally {
    loading.value = false;
  }
}

onMounted(fetchOperatingSystems);

// ================= CRUD =================
function openModal(mode: "add" | "edit", row?: OperatingSystemResponse) {
  modalMode.value = mode;
  if (mode === "edit" && row) {
    modalRow.value = row;
    formData.code = row.code;
    formData.name = row.name;
    formData.version = row.version;
    formData.description = row.description;
  } else {
    modalRow.value = null;
    formData.code = "";
    formData.name = "";
    formData.version = "";
    formData.description = "";
  }
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveOperatingSystem() {
  if (!formData.code || !formData.name || !formData.version) {
    message.warning("Vui lòng nhập đầy đủ Mã, Tên, Version");
    return;
  }

  try {
    if (modalMode.value === "add") {
      await createOperatingSystem(formData);
      message.success("Thêm Hệ điều hành thành công");
    } else if (modalMode.value === "edit" && modalRow.value) {
      await updateOperatingSystem(modalRow.value.id, formData);
      message.success("Cập nhật Hệ điều hành thành công");
    }
    closeModal();
    fetchOperatingSystems();
  } catch (e) {
    message.error("Có lỗi xảy ra khi lưu Hệ điều hành");
  }
}

async function handleDelete(id: string) {
  try {
    // API bạn chưa có deleteOperatingSystem, bạn có thể bổ sung
    message.info("TODO: Thêm API xóa hệ điều hành");
  } catch {
    message.error("Xóa thất bại");
  }
}

async function handleStatusChange(
  row: OperatingSystemResponse,
  value: boolean
) {
  try {
    await updateOperatingSystemStatus(row.id, value ? "ACTIVE" : "INACTIVE");
    message.success(`Cập nhật trạng thái ${row.name} thành công`);
    fetchOperatingSystems();
  } catch {
    message.error("Cập nhật trạng thái thất bại");
  }
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: "selection" as const },
  { title: "Mã", key: "code" },
  { title: "Tên", key: "name" },
  { title: "Phiên bản", key: "version" },
  { title: "Mô tả", key: "description" },
  {
    title: "Trạng thái",
    key: "status",
    render(row: OperatingSystemResponse) {
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
    render(row: OperatingSystemResponse) {
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
  fetchOperatingSystems();
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchOperatingSystems();
}
</script>

<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon :icon="'carbon:carbon-for-ibm-dotcom'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Quản lý Hệ điều hành
        </span>
      </NSpace>
      <span>Quản lý danh sách hệ điều hành sản phẩm</span>
    </NSpace>
  </n-card>

  <!-- Table -->
  <NCard title="Danh sách Hệ điều hành" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NInput
          v-model:value="searchKeyword"
          placeholder="Tìm kiếm hệ điều hành..."
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
          Xác nhận xóa tất cả thương hiệu đã chọn?
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
    title="Hệ điều hành"
  >
    <NForm>
      <NGrid cols="1" x-gap="12">
        <NGridItem>
          <NFormItem label="Mã" required>
            <NInput v-model:value="formData.code" placeholder="Nhập mã" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Tên" required>
            <NInput
              v-model:value="formData.name"
              placeholder="Nhập tên hệ điều hành"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Phiên bản" required>
            <NInput
              v-model:value="formData.version"
              placeholder="Nhập phiên bản"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Mô tả">
            <NInput
              type="textarea"
              v-model:value="formData.description"
              placeholder="Nhập mô tả"
            />
          </NFormItem>
        </NGridItem>
      </NGrid>
    </NForm>
    <template #footer>
      <NSpace justify="end">
        <NButton @click="closeModal">Hủy</NButton>
        <NButton type="primary" @click="saveOperatingSystem">Lưu</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
