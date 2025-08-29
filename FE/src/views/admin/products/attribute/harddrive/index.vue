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
  getAllHardDrives,
  createHardDrive,
  updateHardDrive,
  updateHardDriveStatus,
  //   deleteHardDrive,
  type HardDriveResponse,
  type CreateHardDriveRequest,
} from "@/service/api/admin/product/harddrive.api";

// ================= STATE =================
const message = useMessage();
const tableData = ref<HardDriveResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);
const searchKeyword = ref("");

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= MODAL =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<HardDriveResponse | null>(null);

const formData = reactive<CreateHardDriveRequest>({
  name: "",
  brand: "",
  type: "",
  typeConnect: "",
  capacity: 0,
  readSpeed: 0,
  writeSpeed: 0,
  cacheMemory: 0,
  physicalSize: 0,
  description: "",
});

// ================= API CALL =================
async function fetchHardDrives() {
  loading.value = true;
  try {
    const res = await getAllHardDrives({
      page: currentPage.value,
      size: pageSize.value,
      name: searchKeyword.value || undefined,
    });
    tableData.value = res.items;
    total.value = res.totalItems;
  } catch (e) {
    message.error("Không thể tải dữ liệu Ổ cứng");
  } finally {
    loading.value = false;
  }
}

onMounted(fetchHardDrives);

// ================= CRUD =================
function openModal(mode: "add" | "edit", row?: HardDriveResponse) {
  modalMode.value = mode;
  if (mode === "edit" && row) {
    modalRow.value = row;
    Object.assign(formData, row);
  } else {
    modalRow.value = null;
    Object.assign(formData, {
      name: "",
      brand: "",
      type: "",
      typeConnect: "",
      capacity: 0,
      readSpeed: 0,
      writeSpeed: 0,
      cacheMemory: 0,
      physicalSize: 0,
      description: "",
    });
  }
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

async function saveHardDrive() {
  if (!formData.name || !formData.brand || !formData.type) {
    message.warning("Vui lòng nhập đầy đủ Tên, Thương hiệu, Loại");
    return;
  }

  try {
    if (modalMode.value === "add") {
      await createHardDrive(formData);
      message.success("Thêm Ổ cứng thành công");
    } else if (modalMode.value === "edit" && modalRow.value) {
      await updateHardDrive(modalRow.value.id, formData);
      message.success("Cập nhật Ổ cứng thành công");
    }
    closeModal();
    fetchHardDrives();
  } catch (e) {
    message.error("Có lỗi xảy ra khi lưu Ổ cứng");
  }
}

async function handleDelete(id: string) {
  try {
    await deleteHardDrive(id);
    message.success("Xóa Ổ cứng thành công");
    fetchHardDrives();
  } catch {
    message.error("Xóa thất bại");
  }
}

async function handleStatusChange(row: HardDriveResponse, value: boolean) {
  try {
    await updateHardDriveStatus(row.id, value ? "ACTIVE" : "INACTIVE");
    message.success(`Cập nhật trạng thái ${row.name} thành công`);
    fetchHardDrives();
  } catch {
    message.error("Cập nhật trạng thái thất bại");
  }
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: "selection" as const },
  { title: "Tên", key: "name" },
  { title: "Hãng", key: "brand" },
  { title: "Loại", key: "type" },
  { title: "Kết nối", key: "typeConnect" },
  { title: "Dung lượng (GB)", key: "capacity" },
  { title: "Kích thước (inch)", key: "physicalSize" },
  {
    title: "Trạng thái",
    key: "status",
    render(row: HardDriveResponse) {
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
    render(row: HardDriveResponse) {
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
  fetchHardDrives();
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchHardDrives();
}
</script>

<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon :icon="'carbon:storage'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px"> Quản lý Ổ cứng </span>
      </NSpace>
      <span>Quản lý danh sách ổ cứng sản phẩm</span>
    </NSpace>
  </n-card>

  <!-- Table -->
  <NCard title="Danh sách Ổ cứng" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
        <NInput
          v-model:value="searchKeyword"
          placeholder="Tìm kiếm ổ cứng..."
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
    title="Ổ cứng"
  >
    <NForm>
      <NGrid cols="2" x-gap="12">
        <NGridItem>
          <NFormItem label="Tên" required>
            <NInput
              v-model:value="formData.name"
              placeholder="Nhập tên ổ cứng"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Hãng" required>
            <NInput v-model:value="formData.brand" placeholder="Nhập hãng" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Loại" required>
            <NInput v-model:value="formData.type" placeholder="Nhập loại" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Kết nối" required>
            <NInput
              v-model:value="formData.typeConnect"
              placeholder="Nhập kết nối"
            />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Dung lượng (GB)" required>
            <NInput v-model:value="formData.capacity" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Tốc độ đọc (MB/s)" required>
            <NInput v-model:value="formData.readSpeed" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Tốc độ ghi (MB/s)" required>
            <NInput v-model:value="formData.writeSpeed" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Cache (MB)" required>
            <NInput v-model:value="formData.cacheMemory" type="number" />
          </NFormItem>
        </NGridItem>
        <NGridItem>
          <NFormItem label="Kích thước (inch)" required>
            <NInput v-model:value="formData.physicalSize" type="number" />
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
        <NButton type="primary" @click="saveHardDrive">Lưu</NButton>
      </NSpace>
    </template>
  </NModal>
</template>
