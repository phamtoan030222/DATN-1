<template>
  <n-card class="mb-4">
    <NSpace vertical :size="8">
      <NSpace :align="'center'">
        <NButton @click="$router.back()" quaternary circle>
          <template #icon>
            <NIcon size="20"><Icon icon="carbon:arrow-left" /></NIcon>
          </template>
        </NButton>
        <NIcon size="24"><Icon icon="carbon:add" /></NIcon>
        <span class="header-title">Thêm đợt giảm giá</span>
      </NSpace>
      <span class="sub-title">Tạo mới và chọn sản phẩm (Hệ thống tự động ẩn các sản phẩm đang bận ở đợt khác)</span>
    </NSpace>
  </n-card>

  <div class="main-layout">
    <div class="form-section">
      <NCard title="Thông tin đợt giảm giá">
        <NForm ref="formRef" :model="formData" :rules="formRules">
          <NSpace vertical :size="16">
            <div class="form-row">
              <NFormItem label="Tên đợt giảm giá" path="discountName" required>
                <NInput v-model:value="formData.discountName" placeholder="Ví dụ: Sale tết 2025" maxlength="100" show-count />
              </NFormItem>
              <NFormItem label="Mã giảm giá" path="discountCode" required>
                <NInput v-model:value="formData.discountCode" placeholder="Mã giảm giá" maxlength="50">
                  <template #suffix>
                    <NButton text @click="generateCode" title="Tạo mã tự động"><NIcon size="16"><Icon icon="carbon:rotate" /></NIcon></NButton>
                  </template>
                </NInput>
              </NFormItem>
            </div>

            <div class="form-row">
              <NFormItem label="Ngày bắt đầu" path="startDate" required>
                <NDatePicker v-model:value="formData.startDate" type="datetime" placeholder="Bắt đầu" style="width: 100%" :is-date-disabled="(ts) => ts < Date.now() - 86400000" />
              </NFormItem>
              <NFormItem label="Ngày kết thúc" path="endDate" required>
                <NDatePicker v-model:value="formData.endDate" type="datetime" placeholder="Kết thúc" style="width: 100%" :is-date-disabled="(ts) => ts <= formData.startDate" />
              </NFormItem> 
            </div>

            <NFormItem label="Phần trăm giảm giá (%)" path="percentage" required>
              <NInputNumber v-model:value="formData.percentage" :min="1" :max="100" placeholder="Nhập 1-100" style="width: 100%" @keydown="preventNonNumericInput" />
            </NFormItem>

            <NFormItem label="Mô tả" path="description">
              <NInput v-model:value="formData.description" type="textarea" placeholder="Mô tả..." maxlength="500" show-count :rows="3" />
            </NFormItem>

            <NSpace justify="space-between" style="margin-top: 24px;">
              <NButton @click="$router.back()">Hủy</NButton>
              <NPopconfirm @positive-click="handleSubmit" positive-text="Đồng ý" negative-text="Hủy">
                <template #trigger>
                  <NButton type="primary" :loading="submitting">
                    <template #icon><NIcon><Icon icon="carbon:save" /></NIcon></template> Tạo và Áp dụng
                  </NButton>
                </template>
                Bạn có chắc chắn muốn tạo đợt giảm giá này không?
              </NPopconfirm>
            </NSpace>
          </NSpace>
        </NForm>
      </NCard>
    </div>

    <div class="products-section">
      <NCard title="Danh sách sản phẩm gốc">
        <template #header-extra>
          <div style="width: 250px">
            <NInput v-model:value="productFilterState.keyword" placeholder="Tìm tên/mã..." clearable size="small">
              <template #prefix><NIcon><Icon icon="carbon:search" /></NIcon></template>
            </NInput>
          </div>
        </template>
        <div class="flex justify-between items-center mb-2" v-if="checkedProductKeys.length > 0">
          <NTag type="info" size="small">Đã chọn: {{ checkedProductKeys.length }} sản phẩm cha</NTag>
        </div>
        <NDataTable :columns="productColumns" :data="paginatedProducts" :loading="loadingProducts" :row-key="(row) => row.id" v-model:checked-row-keys="checkedProductKeys" :pagination="false" striped max-height="400px" />
        <div class="flex justify-end mt-4" v-if="filteredProducts.length > 0">
          <NPagination v-model:page="productCurrentPage" :page-size="productPageSize" :page-count="Math.ceil(filteredProducts.length / productPageSize)" />
        </div>
      </NCard>
    </div>
  </div>

  <NCard title="Danh sách Sản Phẩm Chi Tiết sẽ áp dụng" style="margin-top: 16px;" v-if="selectedProductDetails.length > 0">
    <div class="filter-container mb-4">
      <NGrid :x-gap="12" :y-gap="8" :cols="24">
        <NGridItem :span="5"><div class="filter-label">Tên SPCT</div><NInput v-model:value="filterState.name" placeholder="Tìm tên..." clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">Màu sắc</div><NSelect v-model:value="filterState.color" :options="uniqueColors" placeholder="Màu" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">RAM</div><NSelect v-model:value="filterState.ram" :options="uniqueRams" placeholder="RAM" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">Ổ cứng</div><NSelect v-model:value="filterState.hardDrive" :options="uniqueHardDrives" placeholder="Ổ cứng" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">GPU</div><NSelect v-model:value="filterState.gpu" :options="uniqueGpus" placeholder="GPU" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">CPU</div><NSelect v-model:value="filterState.cpu" :options="uniqueCpus" placeholder="CPU" clearable size="small" /></NGridItem>
        <NGridItem :span="4" class="flex items-end" style="padding-bottom: 2px;">
          <NButton circle type="default" secondary @click="resetFilters" title="Làm mới bộ lọc"><template #icon><NIcon size="18"><Icon icon="carbon:rotate-360" /></NIcon></template></NButton>
        </NGridItem>
      </NGrid>
    </div>

    <div class="flex justify-between items-center mb-2">
      <NTag type="success" size="small">Hiển thị: {{ filteredDetails.length }} | Đã chọn: {{ selectedDetailKeys.length }}</NTag>
    </div>

    <NDataTable :columns="productDetailColumns" :data="paginatedDetails" :row-key="(row) => row.id" v-model:checked-row-keys="selectedDetailKeys" :pagination="false" striped size="small" max-height="400" />
    <div class="flex justify-end mt-4"><NPagination v-model:page="detailCurrentPage" :page-size="detailPageSize" :page-count="Math.ceil(filteredDetails.length / detailPageSize)" /></div>
  </NCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h } from "vue";
import { useRouter } from "vue-router";
import {
  NButton, NSpace, NCard, NForm, NFormItem, NInput, NInputNumber,
  NIcon, NDatePicker, NDataTable, NTag, NPagination, NGrid, NGridItem, 
  NSelect, useMessage, NPopconfirm,
  type FormInst, type FormRules, type DataTableColumns
} from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  createDiscount, getAllProducts, getProductDetailsByProductId,
  applyMultipleProducts, getAllDiscounts, getAppliedProducts,
  type CreateDiscountRequest, type ProductResponse, type ProductDetailResponse, type DiscountResponse
} from '@/service/api/admin/discount/discountApi';

interface ExtendedProductDetail extends ProductDetailResponse { _parentId?: string | number; }

const router = useRouter();
const message = useMessage();
const formRef = ref<FormInst>();
const submitting = ref(false);

const formData = reactive<CreateDiscountRequest>({
  discountName: "", discountCode: "", percentage: 10,
  startDate: Date.now(), endDate: Date.now() + 7 * 24 * 60 * 60 * 1000, description: ""
});

const formRules: FormRules = {
  discountName: [{ required: true, message: 'Vui lòng nhập tên', trigger: 'blur' }],
  discountCode: [{ required: true, message: 'Vui lòng nhập mã', trigger: 'blur' }],
  percentage: [{ required: true, type: 'number', message: 'Nhập % giảm', trigger: 'blur' }],
  startDate: [{ required: true, type: 'number', message: 'Chọn ngày bắt đầu', trigger: 'blur' }],
  endDate: [
    { required: true, type: 'number', message: 'Chọn ngày kết thúc', trigger: 'blur' },
    { validator: (_rule, value) => value > formData.startDate, message: 'Ngày kết thúc phải sau ngày bắt đầu', trigger: 'blur' }
  ]
};

const productPageSize = ref(10);
const detailPageSize = ref(10);
const allProducts = ref<ProductResponse[]>([]); 
const checkedProductKeys = ref<(string | number)[]>([]); 
const loadingProducts = ref(false);
const productCurrentPage = ref(1);
const productFilterState = reactive({ keyword: '' });
const selectedProductDetails = ref<ExtendedProductDetail[]>([]);
const selectedDetailKeys = ref<(string | number)[]>([]);
const loadingProductDetails = ref(false);
const detailCurrentPage = ref(1);
const filterState = reactive({ name: '', color: null, ram: null, hardDrive: null, gpu: null, cpu: null });

// === LOGIC TẠO CHỮ KÝ (SIGNATURE) ĐỂ SO SÁNH CHÍNH XÁC ===
const getProductSignature = (item: any) => {
    // Tạo 1 chuỗi unique dựa trên các thuộc tính
    const code = (item.productCode || '').trim();
    const color = (item.colorName || item.color || '').trim();
    const ram = (item.ramName || item.ram || '').trim();
    const hdd = (item.hardDriveName || item.hardDrive || '').trim();
    const gpu = (item.gpuName || item.gpu || '').trim();
    const cpu = (item.cpuName || item.cpu || '').trim();
    
    // Ví dụ: "LENOVO_XAM_8GB_256GB_RTX3050_I5"
    return `${code}_${color}_${ram}_${hdd}_${gpu}_${cpu}`.toUpperCase();
};

const busySignatures = ref<Set<string>>(new Set()); 

const fetchBusyProducts = async () => {
  if (!formData.startDate || !formData.endDate) return;

  busySignatures.value.clear();

  try {
    const res = await getAllDiscounts({ page: 1, size: 2000 });
    const allDiscounts = res.items || [];
    
    const currentStart = Number(formData.startDate);
    const currentEnd = Number(formData.endDate);

    const conflictingDiscounts = allDiscounts.filter((d: any) => {
        const dStart = new Date(d.startTime).getTime();
        const dEnd = new Date(d.endTime).getTime();
        return (currentStart <= dEnd) && (currentEnd >= dStart);
    });

    if (conflictingDiscounts.length > 0) {
        const promises = conflictingDiscounts.map((d: any) => 
            getAppliedProducts(d.id, { page: 1, size: 5000 })
        );
        const results = await Promise.all(promises);
        
        results.forEach(res => {
            const items = res.items || [];
            items.forEach((item: any) => {
                // Tạo chữ ký từ sản phẩm đã áp dụng
                const sig = getProductSignature(item);
                busySignatures.value.add(sig);
            });
        });
        console.log(`Bận ${busySignatures.value.size} biến thể (theo thuộc tính).`);
    }

    // Tự động xóa khỏi danh sách đang chọn nếu bị trùng
    const conflictInSelected = selectedProductDetails.value.filter(d => busySignatures.value.has(getProductSignature(d)));
    if (conflictInSelected.length > 0) {
        selectedProductDetails.value = selectedProductDetails.value.filter(d => !busySignatures.value.has(getProductSignature(d)));
        // Cập nhật lại key đã chọn
        const remainingIds = selectedProductDetails.value.map(d => d.id);
        selectedDetailKeys.value = selectedDetailKeys.value.filter(k => remainingIds.includes(String(k)));
        message.warning(`Đã gỡ ${conflictInSelected.length} biến thể do trùng lịch.`);
    }

  } catch (e) { console.error("Lỗi check trùng:", e); }
};

watch([() => formData.startDate, () => formData.endDate], () => {
    if (formData.startDate < formData.endDate) fetchBusyProducts();
});

// ... (Các filter computed giữ nguyên) ...
const filteredProducts = computed(() => {
  const keyword = productFilterState.keyword.toLowerCase().trim();
  if (!keyword) return allProducts.value;
  return allProducts.value.filter(item => item.productName.toLowerCase().includes(keyword) || item.productCode.toLowerCase().includes(keyword));
});
watch(() => productFilterState.keyword, () => { productCurrentPage.value = 1; });
const paginatedProducts = computed(() => {
  const start = (productCurrentPage.value - 1) * productPageSize.value;
  return filteredProducts.value.slice(start, start + productPageSize.value);
});

const uniqueColors = computed(() => [...new Set(selectedProductDetails.value.map(d => d.colorName).filter(Boolean))].map(c => ({ label: c, value: c })));
const uniqueRams = computed(() => [...new Set(selectedProductDetails.value.map(d => d.ramName).filter(Boolean))].map(r => ({ label: r, value: r })));
const uniqueHardDrives = computed(() => [...new Set(selectedProductDetails.value.map(d => d.hardDriveName).filter(Boolean))].map(h => ({ label: h, value: h })));
const uniqueGpus = computed(() => [...new Set(selectedProductDetails.value.map(d => d.gpuName).filter(Boolean))].map(g => ({ label: g, value: g })));
const uniqueCpus = computed(() => [...new Set(selectedProductDetails.value.map(d => d.cpuName).filter(Boolean))].map(c => ({ label: c, value: c })));
const resetFilters = () => { filterState.name = ''; filterState.color = null; filterState.ram = null; filterState.hardDrive = null; filterState.gpu = null; filterState.cpu = null; };

const filteredDetails = computed(() => {
  return selectedProductDetails.value.filter(item => {
    const matchName = !filterState.name || item.productName.toLowerCase().includes(filterState.name.toLowerCase());
    const matchColor = !filterState.color || item.colorName === filterState.color;
    const matchRam = !filterState.ram || item.ramName === filterState.ram;
    const matchHDD = !filterState.hardDrive || item.hardDriveName === filterState.hardDrive;
    const matchGPU = !filterState.gpu || item.gpuName === filterState.gpu;
    const matchCPU = !filterState.cpu || item.cpuName === filterState.cpu;
    return matchName && matchColor && matchRam && matchHDD && matchGPU && matchCPU;
  });
});
watch(filteredDetails, () => { detailCurrentPage.value = 1; });
const paginatedDetails = computed(() => {
  const start = (detailCurrentPage.value - 1) * detailPageSize.value;
  return filteredDetails.value.slice(start, start + detailPageSize.value);
});

const formatPrice = (price: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
const preventNonNumericInput = (event: KeyboardEvent) => {
  if (['Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 'ArrowLeft', 'ArrowRight'].includes(event.key)) return;
  if (!/[0-9]/.test(event.key)) event.preventDefault();
};
const generateCode = () => {
  const timestamp = Date.now().toString().slice(-6);
  const random = Math.random().toString(36).substring(2, 6).toUpperCase();
  formData.discountCode = `PROMO${timestamp}${random}`;
};

const fetchProducts = async () => {
  loadingProducts.value = true;
  try {
    const res = await getAllProducts({ page: 1, size: 1000, q: '' });
    allProducts.value = res.items; 
  } catch (error) { message.error('Lỗi tải danh sách sản phẩm'); } finally { loadingProducts.value = false; }
};

watch(checkedProductKeys, async (newKeys, oldKeys) => {
  const addedIds = newKeys.filter(id => !oldKeys.includes(id));
  const removedIds = oldKeys.filter(id => !newKeys.includes(id));
  for (const id of addedIds) await fetchAndAddDetails(id.toString());
  if (removedIds.length > 0) {
    selectedProductDetails.value = selectedProductDetails.value.filter(d => !d._parentId || !removedIds.includes(d._parentId));
    selectedDetailKeys.value = selectedDetailKeys.value.filter(k => selectedProductDetails.value.some(d => d.id === k));
  }
});

const fetchAndAddDetails = async (productId: string) => {
  loadingProductDetails.value = true;
  try {
    const res = await getProductDetailsByProductId(productId);
    const details = res?.data || [];
    if (details.length > 0) {
      // LOGIC LỌC BẰNG CHỮ KÝ (SIGNATURE)
      const availableDetails = details.filter((d: any) => !busySignatures.value.has(getProductSignature(d)));

      // Nếu số lượng khả dụng ít hơn tổng số (nghĩa là có cái bị bận)
      if (availableDetails.length < details.length && availableDetails.length > 0) {
          // Vẫn hiện những cái chưa bận
      } else if (availableDetails.length === 0) {
          message.warning(`Tất cả biến thể của sản phẩm này đã bận trong khung giờ này.`);
      }

      const detailsWithParent: ExtendedProductDetail[] = availableDetails.map((d: any) => ({...d, _parentId: productId}));
      const newDetails = detailsWithParent.filter(d => !selectedProductDetails.value.some(ex => ex.id === d.id));
      selectedProductDetails.value = [...selectedProductDetails.value, ...newDetails];
      selectedDetailKeys.value = [...selectedDetailKeys.value, ...newDetails.map(d => d.id)];
    }
  } catch (e) { message.error('Lỗi tải biến thể sản phẩm'); } finally { loadingProductDetails.value = false; }
}

const handleSubmit = async () => {
  try { await formRef.value?.validate(); } catch (e) { return; }
  
  const itemsToApply = selectedProductDetails.value.filter(d => selectedDetailKeys.value.includes(d.id));
  if (itemsToApply.length === 0) { message.warning("Vui lòng chọn ít nhất một biến thể!"); return; }

  submitting.value = true;
  try {
    const now = Date.now();
    if (formData.startDate <= now) {
        formData.startDate = now + 60000; 
        
        if (formData.startDate >= formData.endDate) {
             formData.endDate = formData.startDate + (24 * 60 * 60 * 1000); 
        }
    }
    // ======================================

    const createRes = await createDiscount(formData);
    if (!createRes?.data?.id) throw new Error("Lỗi hệ thống: Không có ID");
    
    const productsPayload = itemsToApply.map(detail => ({
      productDetailId: detail.id.toString(),
      originalPrice: detail.price,
      salePrice: Math.round(detail.price * (100 - formData.percentage) / 100),
      description: formData.description || 'Áp dụng mới'
    }));

    await applyMultipleProducts(createRes.data.id, productsPayload);
    message.success(`Đã tạo đợt giảm giá thành công!`);
    router.back();
  } catch (error: any) {
    if(error.response && error.response.status === 400) {
        message.error("Lỗi 400: Dữ liệu không hợp lệ (Có thể do trùng mã hoặc thời gian).");
    } else {
        message.error(error.message || "Có lỗi xảy ra");
    }
  } finally { 
    submitting.value = false; 
  }
};

const productColumns: DataTableColumns<ProductResponse> = [
  { type: 'selection' },
  { title: "STT", key: "stt", width: 50, align: "center", render: (_, i) => (productCurrentPage.value - 1) * productPageSize.value + i + 1 },
  { title: 'Mã', key: 'productCode', width: 100, render: r => h('strong', r.productCode) },
  { title: 'Tên SP', key: 'productName', ellipsis: { tooltip: true } },
  { title: 'Thương hiệu', key: 'productBrand', width: 120 },
];

const productDetailColumns: DataTableColumns<ExtendedProductDetail> = [
  { type: 'selection' },
  { title: "STT", key: "stt", width: 50, align: "center", render: (_, i) => (detailCurrentPage.value - 1) * detailPageSize.value + i + 1 },
  { title: 'Tên SPCT', key: 'productName', ellipsis: { tooltip: true } },
  { title: 'Màu', key: 'colorName', align: 'center', render: (r:any) => r.colorName || r.color || '-' },
  { title: 'RAM', key: 'ramName', align: 'center', render: (r:any) => r.ramName || r.ram || '-' },
  { title: 'HDD', key: 'hardDriveName', align: 'center', render: (r:any) => r.hardDriveName || r.hardDrive || '-' },
  { title: 'GPU', key: 'gpuName', align: 'center', render: (r:any) => r.gpuName || r.gpu || '-' },
  { title: 'CPU', key: 'cpuName', align: 'center', render: (r:any) => r.cpuName || r.cpu || '-' },
  { title: 'Giá áp dụng', key: 'price', width: 140, align: 'right', render: (r) => {
      const sale = Math.round(r.price * (100 - formData.percentage) / 100);
      return h('div', { class: 'price-cell' }, [
        h('div', { style: 'text-decoration: line-through; color: #999; font-size: 11px;' }, formatPrice(r.price)),
        h('div', { style: 'color: #d03050; font-weight: bold;' }, formatPrice(sale))
      ]);
    }
  },
];

onMounted(async () => {
  generateCode();
  fetchProducts();
  await fetchBusyProducts();
});
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; } .mb-2 { margin-bottom: 8px; }
.header-title { font-weight: 600; font-size: 20px; margin-left: 8px; }
.sub-title { color: #666; font-size: 13px; }
.main-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.flex { display: flex; } .items-end { align-items: flex-end; } .justify-between { justify-content: space-between; }
.filter-label { font-size: 11px; color: #666; margin-bottom: 2px; font-weight: 500; }
:deep(.n-card .n-card__content) { padding: 20px; } :deep(.n-form-item-label) { font-weight: 500; }
@media (max-width: 1024px) { .main-layout { grid-template-columns: 1fr; } }
</style>