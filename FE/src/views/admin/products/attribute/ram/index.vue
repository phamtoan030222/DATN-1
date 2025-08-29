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
  getAllRams,
  createRam,
  updateRam,
  updateRamStatus,
  type RamResponse,
  type CreateRamRequest,
} from "@/service/api/admin/product/ram.api";

// ================= STATE =================
const message = useMessage();
const tableData = ref<RamResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);
const searchKeyword = ref("");

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= MODAL =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<RamResponse | null>(null);

const formData = reactive<CreateRamRequest>({
  name: "",
  brand: "",
  type: "",
  capacity: 0,
  busSpeed: 0,
  slotConFig: 0,
  maxSupported: 0,
  description: "",
});

// ================= API CALL =================
async function fetchRams() {
  loading.value = true;
  try {
    const res = await getAllRams({
      page: currentPage.value,
      size: pageSize.value,
      name: searchKeyword.value || undefined,
    });
    tableData.value = res.items;
    total.value = res.totalItems;
  } catch (e) {
    message.error("Không thể tải dữ liệu RAM");
  } finally {
    loading.value = false;
  }
}

onMounted(fetchRams);

// ================= CRUD =================
function openModal(mode: "add" | "edit", row?: RamResponse) {
  modalMode.value = mode;
  if (mode === "edit" && row) {
    modalRow.value = row;
    Object.assign(formData, {
      name: row.name,
      brand: row.brand,
      type: row.type,
      capacity: row.capacity,
      busSpeed: row.busSpeed,
      slotConFig: row.slotConFig,
      maxSupported: row.maxSupported,
      description: row.description,
    });
  } else {
    modalRow.value = null;
    Object.assign(formData, {
      name: "",
      brand: "",
      type: "",
      capacity: 0,
      busSpeed: 0,
      slotConFig: 0,
      maxSupported: 0,
      description: "",
    });
  }
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveRam() {
  if (!formData.name || !formData.brand || !formData.type) {
    message.warning("Vui lòng nhập đầy đủ Tên, Thương hiệu, Loại");
    return;
  }

  try {
    if (modalMode.value === "add") {
      await createRam(formData);
      message.success("Thêm RAM thành công");
    } else if (modalMode.value === "edit" && modalRow.value) {
      await updateRam(modalRow.value.id, formData);
      message.success("Cập nhật RAM thành công");
    }
    closeModal();
    fetchRams();
  } catch (e) {
    message.error("Có lỗi xảy ra khi lưu RAM");
  }
}

async function handleDelete(id: string) {
  message.info("TODO: Thêm API xóa RAM");
}

async function handleStatusChange(row: RamResponse, value: boolean) {
  try {
    await updateRamStatus(row.id, value ? "ACTIVE" : "INACTIVE");
    message.success(`Cập nhật trạng thái ${row.name} thành công`);
    fetchRams();
  } catch {
    message.error("Cập nhật trạng thái thất bại");
  }
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: "selection" as const },
  { title: "Tên", key: "name" },
  { title: "Thương hiệu", key: "brand" },
  { title: "Loại", key: "type" },
  { title: "Dung lượng (GB)", key: "capacity" },
  {
    title: "Trạng thái",
    key: "status",
    render(row: RamResponse) {
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
    render(row: RamResponse) {
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
  fetchRams();
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchRams();
}

function refreshTable() {
  searchKeyword.value = "";
  currentPage.value = 1;
  fetchRams();
}

function handleDeleteSelected() {
  message.info("TODO: Xóa hàng loạt RAM");
}
</script>

<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon :icon="'carbon:chip'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px"> Quản lý RAM </span>
      </NSpace>
      <span>Quản lý danh sách RAM sản phẩm</span>
    </NSpace>
  </n-card>

  <!-- Table -->
  <NCard title="Danh sách RAM" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NInput
          v-model:value="searchKeyword"
          placeholder="Tìm kiếm RAM..."
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
          Xác nhận xóa tất cả RAM đã chọn?
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
    style="width: 600px"
    title="RAM"
  >
    <NForm>
      <NGrid cols="2" x-gap="12">
        <NGridItem>
          <NFormItem label="Tên" required>
            <NInput v-model:value="formData.name" placeholder="Nhập tên RAM" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Thương hiệu" required>
            <NInput
              v-model:value="formData.brand"
              placeholder="Nhập thương hiệu"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Loại" required>
            <NInput v-model:value="formData.type" placeholder="VD: DDR4" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Dung lượng (GB)" required>
            <NInput v-model:value="formData.capacity" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Bus Speed (MHz)" required>
            <NInput v-model:value="formData.busSpeed" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Slot Config" required>
            <NInput v-model:value="formData.slotConFig" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Hỗ trợ tối đa (GB)" required>
            <NInput v-model:value="formData.maxSupported" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem span="2">
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
        <NButton type="primary" @click="saveRam">Lưu</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
