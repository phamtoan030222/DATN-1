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
} from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  getAllBrands,
  createBrand,
  updateBrand,
  updateBrandStatus,
  deleteBrand,
  type BrandResponse,
  type CreateBrandRequest,
} from "@/service/api/admin/product/brand.api";

// ================= STATE =================
const message = useMessage();
const tableData = ref<BrandResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);
const searchKeyword = ref("");

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= MODAL =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<BrandResponse | null>(null);

const formData = reactive<CreateBrandRequest>({
  code: "",
  name: "",
});

// ================= API CALL =================
async function fetchBrands() {
  loading.value = true;
  try {
    const res = await getAllBrands({
      page: currentPage.value,
      size: pageSize.value,
      name: searchKeyword.value || undefined,
    });
    tableData.value = res.items;
    total.value = res.totalItems;
  } catch (e) {
    message.error("Không thể tải dữ liệu thương hiệu");
  } finally {
    loading.value = false;
  }
}

onMounted(fetchBrands);

// ================= CRUD =================
function openModal(mode: "add" | "edit", row?: BrandResponse) {
  modalMode.value = mode;
  if (mode === "edit" && row) {
    modalRow.value = row;
    formData.code = row.code;
    formData.name = row.name;
  } else {
    modalRow.value = null;
    formData.code = "";
    formData.name = "";
  }
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveBrand() {
  if (!formData.code || !formData.name) {
    message.warning("Vui lòng nhập đầy đủ Mã và Tên thương hiệu");
    return;
  }

  try {
    if (modalMode.value === "add") {
      await createBrand({
        code: formData.code,
        name: formData.name,
      });
      message.success("Thêm thương hiệu thành công");
    } else if (modalMode.value === "edit" && modalRow.value) {
      const req: CreateBrandRequest = {
        code: formData.code,
        name: formData.name,
      };
      await updateBrand(modalRow.value.id, req);
      message.success("Cập nhật thương hiệu thành công");
    }
    closeModal();
    fetchBrands();
  } catch (e) {
    message.error("Có lỗi xảy ra khi lưu thương hiệu");
  }
}

async function handleDelete(id: string) {
  try {
    await deleteBrand(id);
    message.success("Xóa thương hiệu thành công");
    fetchBrands();
  } catch {
    message.error("Xóa thất bại");
  }
}

async function handleDeleteSelected() {
  if (checkedRowKeys.value.length === 0) {
    message.warning("Chưa chọn thương hiệu nào");
    return;
  }
  try {
    await Promise.all(
      checkedRowKeys.value.map((id) => deleteBrand(id.toString()))
    );
    message.success("Đã xóa các thương hiệu đã chọn");
    checkedRowKeys.value = [];
    fetchBrands();
  } catch {
    message.error("Xóa hàng loạt thất bại");
  }
}

async function handleStatusChange(row: BrandResponse, value: boolean) {
  try {
    await updateBrandStatus(row.id, value ? "ACTIVE" : "INACTIVE");
    message.success(`Cập nhật trạng thái ${row.name} thành công`);
    fetchBrands();
  } catch {
    message.error("Cập nhật trạng thái thất bại");
  }
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: "selection" as const },
  { title: "Mã", key: "code" },
  { title: "Tên thương hiệu", key: "name" },
  {
    title: "Trạng thái",
    key: "status",
    render(row: BrandResponse) {
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
    render(row: BrandResponse) {
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
  fetchBrands();
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchBrands();
}
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
  <NCard title="Danh sách thương hiệu" style="margin-top: 16px">
    <template #header-extra>
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
    title="Thương hiệu"
  >
    <NForm>
      <NGrid cols="1" x-gap="12">
        <NGridItem>
          <NFormItem label="Mã" required>
            <NInput v-model:value="formData.code" placeholder="Nhập mã" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Tên thương hiệu" required>
            <NInput
              v-model:value="formData.name"
              placeholder="Nhập tên thương hiệu"
            />
          </NFormItem>
        </NGridItem>
      </NGrid>
    </NForm>
    <template #footer>
      <NSpace justify="end">
        <NButton onClick="closeModal">Hủy</NButton>
        <NButton type="primary" @click="saveBrand">Lưu</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
