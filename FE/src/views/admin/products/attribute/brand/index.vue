<script setup lang="ts">
import { ref, h, reactive, computed } from "vue";
import {
  NCard,
  NSpace,
  NIcon,
  NButton,
  NPopconfirm,
  NDataTable,
  NSwitch,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NPagination,
  useMessage,
} from "naive-ui";
import { Icon } from "@iconify/vue";
import { createIcon } from "@/utils";

const message = useMessage();

interface Brand {
  id: number;
  code: string;
  name: string;
  status: "active" | "inactive";
}

const tableData = ref<Brand[]>([
  { id: 1, code: "BR001", name: "Apple", status: "active" },
  { id: 2, code: "BR002", name: "Samsung", status: "inactive" },
  { id: 3, code: "BR003", name: "Xiaomi", status: "active" },
]);

const checkedRowKeys = ref<number[]>([]);

const modalVisible = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<Brand | null>(null);
const formData = reactive({
  code: "",
  name: "",
  status: true,
});

const currentPage = ref(1);
const pageSize = ref(5);
const total = ref(tableData.value.length);

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return tableData.value.slice(start, start + pageSize.value);
});

function openModal(type: "add" | "edit", row?: Brand) {
  modalMode.value = type;
  if (type === "edit" && row) {
    modalRow.value = row;
    formData.code = row.code;
    formData.name = row.name;
    formData.status = row.status === "active";
  } else {
    modalRow.value = null;
    formData.code = "";
    formData.name = "";
    formData.status = true;
  }
  modalVisible.value = true;
}

function closeModal() {
  modalVisible.value = false;
}

function saveBrand() {
  if (!formData.code || !formData.name) {
    message.warning("Vui lòng nhập đầy đủ Mã và Tên thương hiệu");
    return;
  }

  if (modalMode.value === "add") {
    const newBrand: Brand = {
      id: tableData.value.length
        ? Math.max(...tableData.value.map((b) => b.id)) + 1
        : 1,
      code: formData.code,
      name: formData.name,
      status: formData.status ? "active" : "inactive",
    };
    tableData.value.push(newBrand);
    total.value = tableData.value.length;
    message.success(`Thêm thương hiệu ${newBrand.name} thành công`);
  } else if (modalMode.value === "edit" && modalRow.value) {
    modalRow.value.code = formData.code;
    modalRow.value.name = formData.name;
    modalRow.value.status = formData.status ? "active" : "inactive";
    message.success(`Cập nhật thương hiệu ${modalRow.value.name} thành công`);
  }

  closeModal();
}

function deleteBrand(id: number) {
  tableData.value = tableData.value.filter((b) => b.id !== id);
  total.value = tableData.value.length;
  message.success(`Xóa thương hiệu id: ${id} thành công`);
}

function handleDeleteSelected() {
  tableData.value = tableData.value.filter(
    (b) => !checkedRowKeys.value.includes(b.id)
  );
  message.success(
    `Xóa hàng loạt thương hiệu id: ${checkedRowKeys.value.join(", ")}`
  );
  checkedRowKeys.value = [];
  total.value = tableData.value.length;
}

function handleStatusChange(row: Brand, value: boolean) {
  row.status = value ? "active" : "inactive";
  message.success(`Cập nhật trạng thái thành công cho ${row.name}`);
}

function handlePageChange(page: number) {
  currentPage.value = page;
}

function refreshTable() {
  message.info("Đã làm mới bảng dữ liệu");
}

// --- columns có thêm selection ---
const columns = [
  { type: "selection", width: 50, align: "center" },
  { title: "Mã", key: "code", width: 120 },
  { title: "Tên thương hiệu", key: "name", ellipsis: { tooltip: true } },
  {
    title: "Trạng thái",
    key: "status",
    align: "center",
    width: 100,
    render: (row: Brand) =>
      h(NSwitch, {
        value: row.status === "active",
        onUpdateValue: (val: boolean) => handleStatusChange(row, val),
        size: "small",
      }),
  },
  {
    title: "Hành động",
    key: "actions",
    align: "center",
    width: 150,
    render: (row: Brand) =>
      h(NSpace, { justify: "center" }, () => [
        h(
          NButton,
          {
            circle: true,
            size: "small",
            type: "primary",
            title: "Chỉnh sửa",
            onClick: () => openModal("edit", row),
          },
          () => createIcon("icon-park-outline:write", { size: 18 })
        ),
        h(
          NPopconfirm,
          { onPositiveClick: () => deleteBrand(row.id) },
          {
            trigger: () =>
              h(
                NButton,
                {
                  circle: true,
                  size: "small",
                  type: "error",
                  title: "Xóa",
                },
                () => createIcon("icon-park-outline:delete-five", { size: 18 })
              ),
            default: () => "Xác nhận xóa thương hiệu này?",
          }
        ),
      ]),
  },
];
</script>

<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon :icon="'carbon:carbon-ui-builder'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Quản lý Thương hiệu
        </span>
      </NSpace>
      <span>Quản lý danh sách thương hiệu có mặt tại cửa hàng</span>
    </NSpace>
  </n-card>

  <!-- Bảng + Nút -->
  <n-card style="margin-top: 16px">
    <!-- Header card -->
    <template #header>
      <div
        style="
          display: flex;
          justify-content: space-between;
          align-items: center;
        "
      >
        <span style="font-weight: 600; font-size: 24px">
          Danh sách thương hiệu
        </span>
        <NSpace>
          <NInput
            v-model:value="searchKeyword"
            placeholder="Tìm kiếm thương hiệu..."
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
      </div>
    </template>

    <!-- Bảng dữ liệu -->
    <n-data-table
      v-model:checked-row-keys="checkedRowKeys"
      :row-key="(row) => row.id"
      :columns="columns"
      :data="paginatedData"
      size="small"
    />

    <!-- Phân trang -->
    <n-pagination
      v-model:page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:page="handlePageChange"
      style="margin-top: 16px"
      :show-size-picker="false"
    />
  </n-card>
  <!-- Modal -->
  <NModal v-model:show="modalVisible" mask-closable>
    <NCard
      :title="
        modalMode === 'add' ? 'Thêm mới thương hiệu' : 'Chỉnh sửa thương hiệu'
      "
      style="max-width: 600px; background: #f9fafb; border-radius: 12px"
    >
      <NForm labelPlacement="left" labelWidth="120px">
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
          <NSwitch v-model:value="formData.status" />
        </NFormItem>
      </NForm>

      <NSpace justify="end" style="margin-top: 16px">
        <NButton @click="closeModal">Hủy</NButton>
        <NButton type="primary" @click="saveBrand">
          {{ modalMode === "add" ? "Thêm mới" : "Cập nhật" }}
        </NButton>
      </NSpace>
    </NCard>
  </NModal>
</template>
