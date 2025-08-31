<script setup lang="tsx">
import { onMounted, ref, reactive, watch, h } from "vue";
import { NSwitch, UploadFileInfo, useMessage } from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  getAllStaff,
  createStaff,
  updateStaffStatus,
  deleteStaff,
  deleteManyStaff,
  type StaffResponse,
  type CreateStaffRequest,
  type ParamGetStaff,
} from "@/service/api/admin/users/staff.api";
import {
  getProvinces,
  getDistricts,
  getCommunes,
} from "@/service/api/admin/geo.api";
import {
  NCard,
  NSpace,
  NButton,
  NInput,
  NDataTable,
  NPagination,
  NModal,
  NForm,
  NFormItem,
  NSelect,
  NDatePicker,
  NUpload,
  NAvatar,
  NRadioGroup,
  NRadio,
  NPopconfirm,
  NIcon,
} from "naive-ui";

// ================= STATE =================
const message = useMessage();
const tableData = ref<StaffResponse[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(5);
const loading = ref(false);

const filter = reactive<ParamGetStaff>({
  fullName: "",
  role: "",
  email: "",
  phone: "",
  status: null,
});

const checkedRowKeys = ref<(string | number)[]>([]);

// ================= MODAL =================
const showModal = ref(false);
const modalMode = ref<"add" | "edit">("add");
const modalRow = ref<StaffResponse | null>(null);

const formSearch = reactive<ParamGetStaff>({
  code: "",
  fullName: "",
  email: "",
  phone: "",
  status: null,
});

const formData = reactive<CreateStaffRequest>({
  fullName: "",
  role: "",
  birthday: 0,
  citizenIdentifyCard: "",
  hometown: "",
  avatarUrl: "",
  phone: "",
  email: "",
  gender: true,
  provinceCode: "",
  districtCode: "",
  communeCode: "",
});

// ================= GEO =================
const provinces = ref<{ label: string; value: string }[]>([]);
const districts = ref<{ label: string; value: string }[]>([]);
const communes = ref<{ label: string; value: string }[]>([]);

async function loadProvinces() {
  const data = await getProvinces();
  provinces.value = data.map((p: any) => ({
    label: p.name,
    value: p.code,
  }));
}

watch(
  () => formData.provinceCode,
  async (newCode) => {
    formData.districtCode = "";
    formData.communeCode = "";
    if (newCode) {
      const data = await getDistricts(newCode);
      districts.value = data.map((d: any) => ({
        label: d.name,
        value: d.code,
      }));
    } else {
      districts.value = [];
    }
    communes.value = [];
  }
);

watch(
  () => formData.districtCode,
  async (newCode) => {
    formData.communeCode = "";
    if (newCode) {
      const data = await getCommunes(newCode);
      communes.value = data.map((c: any) => ({
        label: c.name,
        value: c.code,
      }));
    } else {
      communes.value = [];
    }
  }
);

// ================= API =================
async function fetchStaff() {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      ...filter,
      status:
        filter.status === null || filter.status === ""
          ? undefined
          : filter.status,
    };
    const res = await getAllStaff(params);
    tableData.value = res.items;
    total.value = res.totalItems;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadProvinces();
  fetchStaff();
});

// ================= CRUD =================
function openModal(mode: "add" | "edit", row?: StaffResponse) {
  modalMode.value = mode;
  if (mode === "edit" && row) {
    modalRow.value = row;
    Object.assign(formData, {
      ...row,
      birthday: row.birthday ? new Date(row.birthday).getTime() : 0,
    });
  } else {
    modalRow.value = null;
    Object.assign(formData, {
      fullName: "",
      role: "",
      birthday: 0,
      citizenIdentifyCard: "",
      hometown: "",
      avatarUrl: "",
      phone: "",
      email: "",
      gender: true,
      provinceCode: "",
      districtCode: "",
      communeCode: "",
    });
  }
  showModal.value = true;
  loadProvinces();
}

function closeModal() {
  showModal.value = false;
}

async function saveStaff() {
  try {
    await createStaff(formData);
    message.success("Lưu nhân viên thành công");
    closeModal();
    fetchStaff();
  } catch {
    message.error("Lưu thất bại");
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  fetchStaff();
}

// ================= UPLOAD =================
function handleUploadChange({
  file,
}: {
  file: UploadFileInfo;
  fileList: UploadFileInfo[];
}) {
  if (file.file) {
    const rawFile = file.file as File;
    const previewUrl = URL.createObjectURL(rawFile);
    formData.avatarUrl = previewUrl;
    // TODO: upload rawFile lên server và gán lại avatarUrl thực tế
  }
}

// ================= ACTIONS =================
async function handleStatusChange(row: StaffResponse, val: boolean) {
  try {
    await updateStaffStatus(row.id, val ? "ACTIVE" : "INACTIVE");
    message.success("Cập nhật trạng thái thành công");
    fetchStaff();
  } catch {
    message.error("Cập nhật trạng thái thất bại");
  }
}

async function handleDelete(id: number) {
  try {
    await deleteStaff(id);
    message.success("Xóa thành công");
    fetchStaff();
  } catch {
    message.error("Xóa thất bại");
  }
}

async function handleDeleteSelected() {
  if (!checkedRowKeys.value.length) {
    message.warning("Chưa chọn nhân viên nào");
    return;
  }
  try {
    await deleteManyStaff(checkedRowKeys.value as number[]);
    message.success("Xóa hàng loạt thành công");
    checkedRowKeys.value = [];
    fetchStaff();
  } catch {
    message.error("Xóa hàng loạt thất bại");
  }
}

function refreshTable() {
  Object.assign(filter, {
    fullName: "",
    role: "",
    email: "",
    phone: "",
    status: null,
  });
  currentPage.value = 1;
  fetchStaff();
}

function openCamera() {
  message.info("Tính năng quét CCCD chưa được triển khai");
}

// ================= TABLE COLUMNS =================
const columns = [
  { type: "selection" },
  {
    title: "Họ và tên",
    key: "fullName",
    render: (row: StaffResponse) =>
      h(
        NSpace,
        { align: "center" },
        {
          default: () => [
            h(NAvatar, { size: "small", src: row.avatarUrl }),
            h("span", row.fullName),
          ],
        }
      ),
  },
  { title: "Chức vụ", key: "role" },
  {
    title: "Giới tính",
    key: "gender",
    render: (row: StaffResponse) => (row.gender ? "Nam" : "Nữ"),
  },
  { title: "Email", key: "email" },
  { title: "Số điện thoại", key: "phone" },
  {
    title: "Địa chỉ",
    key: "address",
    render(row: StaffResponse) {
      const parts = [
        row.hometown,
        row.communeName,
        row.districtName,
        row.provinceName,
      ].filter(Boolean);
      return parts.join(", ");
    },
  },
  {
    title: "Trạng thái",
    key: "status",
    render(row: StaffResponse) {
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
    render(row: StaffResponse) {
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
</script>

<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace align="center">
        <NIcon size="24">
          <Icon :icon="'icon-park-outline:address-book'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Quản lý Nhân viên
        </span>
      </NSpace>
      <span>Quản lý nhân viên làm việc tại cửa hàng</span>
    </NSpace>
  </n-card>

  <n-card title="Bộ lọc" style="margin-top: 16px">
    <n-form
      label-placement="top"
      :model="formSearch"
      :label-style="{ fontWeight: 'bold' }"
    >
      <div class="space-y-3">
        <div class="flex gap-4 items-end">
          <!-- Mã nhân viên -->
          <NFormItem label="Mã nhân viên:" class="flex-1">
            <NInput
              v-model:value="formSearch.code"
              placeholder="Nhập mã nhân viên..."
            />
          </NFormItem>

          <!-- Tên nhân viên -->
          <NFormItem label="Tên nhân viên:" class="flex-1">
            <NInput
              v-model:value="formSearch.fullName"
              placeholder="Nhập tên nhân viên..."
            />
          </NFormItem>

          <!-- Trạng thái -->
          <NFormItem label="Trạng thái:" class="flex-1">
            <NRadioGroup v-model:value="formSearch.status">
              <NRadio :value="null">Tất cả</NRadio>
              <NRadio :value="true">Đang làm</NRadio>
              <NRadio :value="false">Đã nghỉ</NRadio>
            </NRadioGroup>
          </NFormItem>

          <!-- Nút reset -->
          <div class="pb-6">
            <NButton
              type="primary"
              secondary
              circle
              title="Làm mới"
              @click="refreshTable"
            >
              <NIcon size="20">
                <Icon icon="carbon:filter-reset" />
              </NIcon>
            </NButton>
          </div>
        </div>
      </div>
    </n-form>
  </n-card>

  <n-card title="Danh sách nhân viên" style="margin-top: 16px">
    <template #header-extra>
      <NSpace>
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
          title="Xuất Excel"
          @click="refreshTable"
        >
          <NIcon size="24">
            <Icon :icon="'icon-park-outline:download'" />
          </NIcon>
        </NButton>
        <NButton
          type="primary"
          secondary
          circle
          title="Nhập từ Excel"
          @click="refreshTable"
        >
          <NIcon size="24">
            <Icon :icon="'carbon:layers-external'" />
          </NIcon>
        </NButton>
        <NButton
          type="primary"
          secondary
          circle
          title="Tải mẫu templete"
          @click="refreshTable"
        >
          <NIcon size="24">
            <Icon :icon="'icon-park-outline:align-text-bottom'" />
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
          Xác nhận xóa tất cả nhân viên đã chọn?
        </NPopconfirm>
      </NSpace>
    </template>

    <NDataTable
      :columns="columns"
      :data="tableData"
      :row-key="(row) => row.id"
      :loading="loading"
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
  </n-card>

  <!-- Modal -->
  <n-modal
    v-model:show="showModal"
    :title="modalMode === 'add' ? 'Thêm nhân viên' : 'Sửa nhân viên'"
    preset="card"
    style="width: 800px"
  >
    <n-form
      label-placement="left"
      :model="formData"
      label-width="130"
      class="p-4 space-y-4"
    >
      <!-- Avatar ở giữa -->
      <div class="flex justify-center">
        <div class="flex flex-col items-center">
          <NUpload
            :max="1"
            accept="image/*"
            :on-change="handleUploadChange"
            list-type="image-card"
            class="mb-2"
          >
            <img
              :src="
                formData.avatarUrl ||
                'https://via.placeholder.com/120x120.png?text=Avatar'
              "
              class="w-36 h-36 rounded-full object-cover"
            />
          </NUpload>
          <span class="text-xs text-gray-500">Chọn ảnh đại diện</span>
        </div>
      </div>

      <!-- Thông tin -->
      <div class="space-y-3">
        <!-- Họ tên + Giới tính -->
        <div class="flex gap-4">
          <NFormItem label="Họ và tên" required class="flex-1">
            <NInput
              v-model:value="formData.fullName"
              placeholder="Nhập họ và tên"
            />
          </NFormItem>
          <NFormItem label="Giới tính" class="flex w-1/3">
            <NRadioGroup v-model:value="formData.gender">
              <NRadio :value="true">Nam</NRadio>
              <NRadio :value="false">Nữ</NRadio>
            </NRadioGroup>
          </NFormItem>
        </div>

        <!-- Chức vụ + Ngày sinh -->
        <div class="flex gap-4">
          <NFormItem label="Chức vụ" required class="flex-1">
            <NSelect
              v-model:value="formData.role"
              :options="[
                { label: 'Quản lý', value: 'QUAN_LY' },
                { label: 'Nhân viên', value: 'NHAN_VIEN' },
              ]"
              placeholder="Chọn chức vụ"
              filterable
              clearable
            />
          </NFormItem>
          <NFormItem label="Ngày sinh" class="flex-1">
            <NDatePicker
              v-model:value="formData.birthday"
              type="date"
              placeholder="Chọn ngày sinh"
              clearable
            />
          </NFormItem>
        </div>

        <!-- Điện thoại + Email -->
        <div class="flex gap-4">
          <NFormItem label="Điện thoại" required class="flex-1">
            <NInput
              v-model:value="formData.phone"
              placeholder="Nhập số điện thoại"
            />
          </NFormItem>
          <NFormItem label="Email" required class="flex-1">
            <NInput v-model:value="formData.email" placeholder="Nhập email" />
          </NFormItem>
        </div>

        <!-- CCCD -->
        <NFormItem label="CMND/CCCD" required>
          <div class="flex gap-2 w-full">
            <NInput
              class="flex-1"
              v-model:value="formData.citizenIdentifyCard"
              placeholder="Nhập số CMND/CCCD"
            />
            <NButton type="info" @click="openCamera">Quét</NButton>
          </div>
        </NFormItem>

        <!-- Địa chỉ cụ thể -->
        <NFormItem label="Địa chỉ cụ thể">
          <NInput
            v-model:value="formData.hometown"
            placeholder="Số nhà, đường..."
          />
        </NFormItem>

        <!-- Địa chỉ (tỉnh, huyện, xã) -->
        <NFormItem label="Địa chỉ">
          <div class="flex gap-2 w-full">
            <NSelect
              class="flex-1"
              v-model:value="formData.provinceCode"
              :options="provinces"
              placeholder="Tỉnh/Thành phố"
              filterable
              clearable
            />
            <NSelect
              class="flex-1"
              v-model:value="formData.districtCode"
              :options="districts"
              placeholder="Quận/Huyện"
              filterable
              clearable
            />
            <NSelect
              class="flex-1"
              v-model:value="formData.communeCode"
              :options="communes"
              placeholder="Xã/Phường"
              filterable
              clearable
            />
          </div>
        </NFormItem>
      </div>
    </n-form>

    <template #footer>
      <NSpace justify="end">
        <NButton @click="closeModal">Hủy</NButton>
        <NButton type="primary" @click="saveStaff">
          {{ modalMode === "add" ? "Thêm mới" : "Lưu thay đổi" }}
        </NButton>
      </NSpace>
    </template>
  </n-modal>
</template>
