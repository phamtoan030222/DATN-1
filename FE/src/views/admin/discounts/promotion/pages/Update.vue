<template>
  <n-card class="mb-4">
    <NSpace vertical :size="8">
      <NSpace :align="'center'">
        <NButton @click="$router.back()" quaternary circle>
          <template #icon><NIcon size="20"><Icon icon="carbon:arrow-left" /></NIcon></template>
        </NButton>
        <NIcon size="24"><Icon icon="carbon:edit" /></NIcon>
        <span class="header-title">Sửa đợt giảm giá</span>
      </NSpace>
      <span class="sub-title">Chỉnh sửa thông tin và quản lý sản phẩm áp dụng</span>
    </NSpace>
  </n-card>

  <div class="main-layout">
    <div class="form-section">
      <NCard title="Thông tin đợt giảm giá">
        <NSkeleton v-if="loadingDiscount" text :repeat="6" />
        <NForm v-else ref="formRef" :model="formData" :rules="formRules">
          <NSpace vertical :size="16">
            
            <NFormItem label="Trạng thái hiện tại">
              <NTag :type="getStatusType()" :bordered="false" style="font-weight: 600;">
                {{ getStatusText() }}
              </NTag>
              <span v-if="!canUpdateInfo && getDiscountStatus() === 'active'" style="margin-left: 10px; color: #d03050; font-size: 12px;">
                * Đang diễn ra: Chỉ được sửa danh sách sản phẩm.
              </span>
            </NFormItem>

            <div class="form-row">
              <NFormItem label="Tên đợt giảm giá" path="discountName" required>
                <NInput 
                  v-model:value="formData.discountName" 
                  placeholder="Nhập tên" 
                  maxlength="100" 
                  show-count 
                  :disabled="!canUpdateInfo" 
                />
              </NFormItem>
              <NFormItem label="Mã giảm giá" required>
                <NInput v-model:value="formData.discountCode" placeholder="Mã giảm giá" disabled />
              </NFormItem>
            </div>

            <div class="form-row">
              <NFormItem label="Ngày bắt đầu" path="startDate" required>
                <NDatePicker 
                  v-model:value="formData.startDate" 
                  type="datetime" 
                  style="width: 100%" 
                  :disabled="!canUpdateInfo" 
                />
              </NFormItem>
              <NFormItem label="Ngày kết thúc" path="endDate" required>
                <NDatePicker 
                  v-model:value="formData.endDate" 
                  type="datetime" 
                  style="width: 100%" 
                  :is-date-disabled="(ts) => ts <= formData.startDate" 
                  :disabled="!canUpdateInfo" 
                />
              </NFormItem>
            </div>

            <NFormItem label="Phần trăm giảm giá (%)" path="percentage" required>
              <NInputNumber 
                v-model:value="formData.percentage" 
                :min="1" :max="100" 
                style="width: 100%" 
                :disabled="!canUpdateInfo" 
              />
            </NFormItem>

            <NFormItem label="Mô tả">
              <NInput 
                v-model:value="formData.description" 
                type="textarea" 
                placeholder="Mô tả..." 
                :rows="3" 
                :disabled="!canUpdateInfo" 
              />
            </NFormItem>

            <NSpace justify="space-between" style="margin-top: 12px;">
              <NButton @click="$router.back()">Thoát</NButton>
              
              <NPopconfirm 
                v-if="canUpdateInfo"
                @positive-click="handleSubmit" 
                positive-text="Đồng ý" 
                negative-text="Hủy"
              >
                <template #trigger>
                  <NButton type="primary" :loading="submitting">
                    <template #icon><NIcon><Icon icon="carbon:save" /></NIcon></template> Cập nhật thông tin
                  </NButton>
                </template>
                Bạn có chắc chắn muốn cập nhật thông tin này?
              </NPopconfirm>
              
              <NButton v-else disabled secondary type="warning">
                <template #icon><NIcon><Icon icon="carbon:locked" /></NIcon></template>
                {{ getDiscountStatus() === 'active' ? 'Đã khóa thông tin' : 'Đã kết thúc' }}
              </NButton>
            </NSpace>
          </NSpace>
        </NForm>
      </NCard>
    </div>

    <div class="products-section">
      <NCard title="Danh sách Sản Phẩm">
        <template #header-extra>
          <div style="width: 200px">
            <NInput 
              v-model:value="productSearchKeyword" 
              placeholder="Tìm tên/mã..." 
              clearable 
              size="small"
            >
              <template #prefix><NIcon><Icon icon="carbon:search" /></NIcon></template>
            </NInput>
          </div>
        </template>

        <div :style="!canUpdateProducts ? 'opacity: 0.6; pointer-events: none;' : ''">
          <NDataTable
            :columns="productColumns"
            :data="paginatedRawProducts" 
            :loading="loadingProducts"
            :row-key="(row) => row.id"
            v-model:checked-row-keys="checkedProductKeys" 
            :pagination="false"
            size="small"
            max-height="450px"
          />
        </div>

        <div class="flex justify-center mt-4" v-if="filteredRawProducts.length > productPageSize">
          <NPagination
            :page="productCurrentPage"
            :page-size="productPageSize"
            :page-count="Math.ceil(filteredRawProducts.length / productPageSize)"
            @update:page="(p) => productCurrentPage = p"
            size="small"
          />
        </div>
      </NCard>
    </div>
  </div>

  <NCard 
    v-if="unappliedProducts.length > 0" 
    title="Danh sách Sản Phẩm Chi Tiết chưa áp dụng" 
    style="margin-top: 16px;"
  >
    <div class="filter-container mb-4">
      <NGrid :x-gap="12" :y-gap="8" :cols="24">
        <NGridItem :span="5"><div class="filter-label">Tên SPCT</div><NInput v-model:value="filterUnapplied.name" placeholder="Tìm tên..." size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">Màu</div><NSelect v-model:value="filterUnapplied.color" :options="uniqueColorsUnapplied" placeholder="Màu" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">RAM</div><NSelect v-model:value="filterUnapplied.ram" :options="uniqueRamsUnapplied" placeholder="RAM" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">Ổ cứng</div><NSelect v-model:value="filterUnapplied.hardDrive" :options="uniqueHardDrivesUnapplied" placeholder="HDD" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">GPU</div><NSelect v-model:value="filterUnapplied.gpu" :options="uniqueGpusUnapplied" placeholder="GPU" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">CPU</div><NSelect v-model:value="filterUnapplied.cpu" :options="uniqueCpusUnapplied" placeholder="CPU" clearable size="small" /></NGridItem>
        <NGridItem :span="4" class="flex items-end" style="padding-bottom: 2px;">
          <NButton circle type="error" secondary @click="resetUnappliedFilters" size="small"><template #icon><NIcon size="18"><Icon icon="carbon:rotate-360" /></NIcon></template></NButton>
        </NGridItem>
      </NGrid>
    </div>

    <div class="flex justify-between items-center mb-2">
      <NSpace>
        <NTag type="warning" size="small">Hiển thị: {{ filteredUnapplied.length }} | Đang chọn: {{ selectedUnappliedKeys.length }}</NTag>
        
        <NPopconfirm @positive-click="handleBulkApply" positive-text="Đồng ý" negative-text="Hủy">
          <template #trigger>
            <NButton 
              type="primary" size="small" 
              :disabled="selectedUnappliedKeys.length === 0 || !canUpdateProducts" 
              :loading="bulkApplying"
            >
              <template #icon><NIcon><Icon icon="carbon:checkmark-outline" /></NIcon></template> Áp dụng ngay ({{ selectedUnappliedKeys.length }})
            </NButton>
          </template>
          Áp dụng giảm giá cho các sản phẩm đã chọn?
        </NPopconfirm>
      </NSpace>


<NPopconfirm 
  @positive-click="clearUnapplied" 
  positive-text="Xóa" 
  negative-text="Hủy"
>
  <template #trigger>
    <NButton quaternary size="small">
      <template #icon><NIcon><Icon icon="carbon:close" /></NIcon></template>
      Xóa hết danh sách này
    </NButton>
  </template>
  Bạn có chắc chắn muốn xóa toàn bộ danh sách chờ này không?
</NPopconfirm>
    </div>

    <NDataTable
      :columns="unappliedProductColumns"
      :data="paginatedUnapplied"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="selectedUnappliedKeys"
      :pagination="false"
      size="small"
    />

    <div class="flex justify-center mt-4" v-if="filteredUnapplied.length > unappliedPageSize">
      <NPagination
        :page="unappliedCurrentPage"
        :page-size="unappliedPageSize"
        :page-count="Math.ceil(filteredUnapplied.length / unappliedPageSize)"
        @update:page="(p) => unappliedCurrentPage = p"
        size="small"
      />
    </div>
  </NCard>

  <NCard title="Danh sách Sản Phẩm Chi Tiết đã áp dụng" style="margin-top: 16px;">
    <div class="filter-container mb-4">
      <NGrid :x-gap="12" :y-gap="8" :cols="24">
        <NGridItem :span="5"><div class="filter-label">Tên sản phẩm</div><NInput v-model:value="filterApplied.name" placeholder="Tìm tên ...." size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">Màu</div><NSelect v-model:value="filterApplied.color" :options="uniqueColorsApplied" placeholder="Màu" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">RAM</div><NSelect v-model:value="filterApplied.ram" :options="uniqueRamsApplied" placeholder="RAM" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">Ổ cứng</div><NSelect v-model:value="filterApplied.hardDrive" :options="uniqueHardDrivesApplied" placeholder="HDD" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">GPU</div><NSelect v-model:value="filterApplied.gpu" :options="uniqueGpusApplied" placeholder="GPU" clearable size="small" /></NGridItem>
        <NGridItem :span="3"><div class="filter-label">CPU</div><NSelect v-model:value="filterApplied.cpu" :options="uniqueCpusApplied" placeholder="CPU" clearable size="small" /></NGridItem>
        <NGridItem :span="4" class="flex items-end" style="padding-bottom: 2px;">
          <NButton circle type="error" secondary @click="resetAppliedFilters" size="small"><template #icon><NIcon size="18"><Icon icon="carbon:rotate-360" /></NIcon></template></NButton>
        </NGridItem>
      </NGrid>
    </div>

    <template #header-extra>
      <NSpace>
        <NPopconfirm @positive-click="handleBulkRemove" positive-text="Xóa" negative-text="Hủy">
          <template #trigger>
            <NButton 
              type="error" secondary size="small" 
              :disabled="selectedAppliedKeys.length === 0 || !canUpdateProducts" 
              :loading="bulkRemoving"
            >
              <template #icon><NIcon><Icon icon="carbon:trash-can" /></NIcon></template> Gỡ bỏ ({{ selectedAppliedKeys.length }})
            </NButton>
          </template>
          Bạn có chắc chắn muốn gỡ bỏ giảm giá cho các sản phẩm đã chọn?
        </NPopconfirm>
      </NSpace>
    </template>

    <NDataTable
      :columns="appliedProductColumns"
      :data="paginatedApplied"
      :loading="loadingApplied"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="selectedAppliedKeys"
      :pagination="false"
      size="small"
      max-height="400px"
    />

    <div class="flex justify-center mt-4" v-if="filteredAppliedProducts.length > appliedPageSize">
      <NPagination
        :page="appliedCurrentPage"
        :page-size="appliedPageSize"
        :page-count="Math.ceil(filteredAppliedProducts.length / appliedPageSize)"
        @update:page="(p) => appliedCurrentPage = p"
        size="small"
      />
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  NButton, NSpace, NCard, NForm, NFormItem, NInput, NInputNumber,
  NIcon, NDatePicker, NDataTable, NTag, NPagination, NSkeleton,
  NPopconfirm, NGrid, NGridItem, NSelect, useMessage,
  type FormInst, type FormRules, type DataTableColumns
} from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  updateDiscount, 
  getAppliedProducts, 
  getUnappliedProducts,
  getAllDiscounts,
  getAllProducts, 
  applyMultipleProducts, 
  removeProductsFromDiscount,
  type UpdateDiscountRequest, type DiscountResponse,
  type ProductDetailResponse, type AppliedProductResponse, type ProductResponse,
} from '@/service/api/admin/discount/discountApi';

interface ExtendedProductDetail extends ProductDetailResponse { _parentId?: string | number; }

const router = useRouter();
const route = useRoute();
const message = useMessage();
const formRef = ref<FormInst>();

const discountId = route.params.id as string;
const submitting = ref(false);
const loadingDiscount = ref(false);
const bulkRemoving = ref(false);
const bulkApplying = ref(false);


const originalDiscount = ref<DiscountResponse | null>(null);
const appliedProducts = ref<AppliedProductResponse[]>([]);
const allRawProducts = ref<ProductResponse[]>([]); // Chứa toàn bộ SP cha
const unappliedProducts = ref<ExtendedProductDetail[]>([]);


const checkedProductKeys = ref<(string | number)[]>([]); 
const selectedAppliedKeys = ref<(string | number)[]>([]);
const selectedUnappliedKeys = ref<(string | number)[]>([]);


const unappliedCurrentPage = ref(1);
const unappliedPageSize = ref(10);

const appliedCurrentPage = ref(1);
const appliedPageSize = ref(10);
const loadingApplied = ref(false);

const productCurrentPage = ref(1);
const productPageSize = ref(10);
const loadingProducts = ref(false);


const getDiscountStatus = () => {
  if (!originalDiscount.value) return 'unknown';
  const now = Date.now();
  const start = Number(originalDiscount.value.startTime);
  const end = Number(originalDiscount.value.endTime);
  
  if (now < start) return 'upcoming';
  if (now >= start && now <= end) return 'active';
  return 'expired';
};

const getStatusType = () => { const s = getDiscountStatus(); return s === 'active' ? 'success' : s === 'upcoming' ? 'info' : 'default'; };
const getStatusText = () => { const s = getDiscountStatus(); return s === 'active' ? 'Đang diễn ra' : s === 'upcoming' ? 'Sắp diễn ra' : 'Đã kết thúc'; };

const canUpdateInfo = computed(() => getDiscountStatus() === 'upcoming');
const canUpdateProducts = computed(() => getDiscountStatus() === 'upcoming' || getDiscountStatus() === 'active');


const productSearchKeyword = ref('');

const filteredRawProducts = computed(() => {
  if (!productSearchKeyword.value) return allRawProducts.value;
  const kw = productSearchKeyword.value.toLowerCase();

  return allRawProducts.value.filter(p => 
    p.productName.toLowerCase().includes(kw) || 
    p.productCode.toLowerCase().includes(kw)
  );
});

watch(filteredRawProducts, () => productCurrentPage.value = 1);

const paginatedRawProducts = computed(() => {
  const start = (productCurrentPage.value - 1) * productPageSize.value;
  return filteredRawProducts.value.slice(start, start + productPageSize.value);
});

const fetchProducts = async () => {
  loadingProducts.value = true;
  try {
    const res = await getAllProducts({ page: 1, size: 2000, q: '' });
    allRawProducts.value = res.items; 
  } catch (e) { message.error('Lỗi tải danh sách SP'); } finally { loadingProducts.value = false; }
};


const filterUnapplied = reactive({ name: '', color: null, ram: null, hardDrive: null, gpu: null, cpu: null });
const uniqueColorsUnapplied = computed(() => [...new Set(unappliedProducts.value.map(d => d.colorName).filter(Boolean))].map(c => ({ label: c, value: c })));
const uniqueRamsUnapplied = computed(() => [...new Set(unappliedProducts.value.map(d => d.ramName).filter(Boolean))].map(r => ({ label: r, value: r })));
const uniqueHardDrivesUnapplied = computed(() => [...new Set(unappliedProducts.value.map(d => d.hardDriveName).filter(Boolean))].map(h => ({ label: h, value: h })));
const uniqueGpusUnapplied = computed(() => [...new Set(unappliedProducts.value.map(d => d.gpuName).filter(Boolean))].map(g => ({ label: g, value: g })));
const uniqueCpusUnapplied = computed(() => [...new Set(unappliedProducts.value.map(d => d.cpuName).filter(Boolean))].map(c => ({ label: c, value: c })));

const resetUnappliedFilters = () => {
  filterUnapplied.name = ''; filterUnapplied.color = null; filterUnapplied.ram = null;
  filterUnapplied.hardDrive = null; filterUnapplied.gpu = null; filterUnapplied.cpu = null;
};

const filteredUnapplied = computed(() => {
  return unappliedProducts.value.filter(item => {
    const matchName = !filterUnapplied.name || item.productName.toLowerCase().includes(filterUnapplied.name.toLowerCase());
    const matchColor = !filterUnapplied.color || item.colorName === filterUnapplied.color;
    const matchRam = !filterUnapplied.ram || item.ramName === filterUnapplied.ram;
    const matchHDD = !filterUnapplied.hardDrive || item.hardDriveName === filterUnapplied.hardDrive;
    const matchGPU = !filterUnapplied.gpu || item.gpuName === filterUnapplied.gpu;
    const matchCPU = !filterUnapplied.cpu || item.cpuName === filterUnapplied.cpu;
    return matchName && matchColor && matchRam && matchHDD && matchGPU && matchCPU;
  });
});
watch(filteredUnapplied, () => unappliedCurrentPage.value = 1);
const paginatedUnapplied = computed(() => {
  const start = (unappliedCurrentPage.value - 1) * unappliedPageSize.value;
  return filteredUnapplied.value.slice(start, start + unappliedPageSize.value);
});

const filterApplied = reactive({ name: '', color: null, ram: null, hardDrive: null, gpu: null, cpu: null });
const uniqueColorsApplied = computed(() => [...new Set(appliedProducts.value.map(d => d.colorName).filter(Boolean))].map(c => ({ label: c, value: c })));
const uniqueRamsApplied = computed(() => [...new Set(appliedProducts.value.map(d => d.ramName).filter(Boolean))].map(r => ({ label: r, value: r })));
const uniqueHardDrivesApplied = computed(() => [...new Set(appliedProducts.value.map(d => d.hardDriveName).filter(Boolean))].map(h => ({ label: h, value: h })));
const uniqueGpusApplied = computed(() => [...new Set(appliedProducts.value.map(d => d.gpuName).filter(Boolean))].map(g => ({ label: g, value: g })));
const uniqueCpusApplied = computed(() => [...new Set(appliedProducts.value.map(d => d.cpuName).filter(Boolean))].map(c => ({ label: c, value: c })));

const resetAppliedFilters = () => {
  filterApplied.name = ''; filterApplied.color = null; filterApplied.ram = null;
  filterApplied.hardDrive = null; filterApplied.gpu = null; filterApplied.cpu = null;
};

const filteredAppliedProducts = computed(() => {
  return appliedProducts.value.filter(item => {
    const matchName = !filterApplied.name || 
                      item.productName.toLowerCase().includes(filterApplied.name.toLowerCase()) ||
                      item.productCode.toLowerCase().includes(filterApplied.name.toLowerCase());
    const matchColor = !filterApplied.color || item.colorName === filterApplied.color;
    const matchRam = !filterApplied.ram || item.ramName === filterApplied.ram;
    const matchHDD = !filterApplied.hardDrive || item.hardDriveName === filterApplied.hardDrive;
    const matchGPU = !filterApplied.gpu || item.gpuName === filterApplied.gpu;
    const matchCPU = !filterApplied.cpu || item.cpuName === filterApplied.cpu;
    return matchName && matchColor && matchRam && matchHDD && matchGPU && matchCPU;
  });
});
watch(filteredAppliedProducts, () => appliedCurrentPage.value = 1);
const paginatedApplied = computed(() => {
  const start = (appliedCurrentPage.value - 1) * appliedPageSize.value;
  return filteredAppliedProducts.value.slice(start, start + appliedPageSize.value);
});


watch(checkedProductKeys, async (newKeys, oldKeys) => {
  const addedIds = newKeys.filter(id => !oldKeys.includes(id));
  const removedIds = oldKeys.filter(id => !newKeys.includes(id));

  for (const id of addedIds) {
    await fetchAndAddUnappliedDetails(id.toString());
  }

  if (removedIds.length > 0) {
    unappliedProducts.value = unappliedProducts.value.filter(d => 
      !d._parentId || !removedIds.includes(d._parentId)
    );
    selectedUnappliedKeys.value = selectedUnappliedKeys.value.filter(k => 
      unappliedProducts.value.some(d => d.id === k)
    );
  }
});

const fetchAndAddUnappliedDetails = async (productId: string) => {
  const parentProduct = allRawProducts.value.find(p => p.id == productId);
  if (!parentProduct) return;

  try {
    const res = await getUnappliedProducts(discountId, { page: 1, size: 500, q: parentProduct.productCode });
    const details = res.items || [];
    if (details.length > 0) {
      const correctDetails = details.filter((d: any) => d.productCode === parentProduct.productCode);
      const detailsWithParent: ExtendedProductDetail[] = correctDetails.map((d: any) => ({...d, _parentId: productId}));
      const newDetails = detailsWithParent.filter(d => !unappliedProducts.value.some(ex => ex.id === d.id));
      unappliedProducts.value = [...unappliedProducts.value, ...newDetails];
      selectedUnappliedKeys.value = [...selectedUnappliedKeys.value, ...newDetails.map(d => d.id)];
    }
  } catch (e) { message.error('Lỗi tải biến thể'); }
};

const clearUnapplied = () => {
  unappliedProducts.value = [];
  selectedUnappliedKeys.value = [];
  checkedProductKeys.value = [];
};


const formData = reactive<UpdateDiscountRequest>({
  discountName: "", discountCode: "", percentage: 0,
  startDate: Date.now(), endDate: Date.now() + 86400000, description: ""
});

const formRules: FormRules = {
  discountName: [{ required: true, message: "Nhập tên đợt", trigger: "blur" }],
  startDate: [{ required: true, type: 'number', message: "Chọn ngày bắt đầu", trigger: "blur" }],
  endDate: [{ required: true, type: 'number', message: "Chọn ngày kết thúc", trigger: "blur" }, { validator: (_r, v) => v > formData.startDate || new Error("Phải sau ngày bắt đầu"), trigger: "blur" }],
  percentage: [{ required: true, type: 'number', message: "Nhập % giảm", trigger: "blur" }]
};


const formatPrice = (p: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);


const fetchDiscount = async () => {
  if (!discountId) return router.back();
  loadingDiscount.value = true;
  try {
    const res = await getAllDiscounts({ page: 1, size: 1000 });
    const discount = res.items.find((item: any) => item.id === discountId);
    if (!discount) throw new Error("Not found");
    originalDiscount.value = discount;
    Object.assign(formData, {
      discountName: discount.discountName, discountCode: discount.discountCode,
      percentage: discount.percentage, startDate: discount.startTime,
      endDate: discount.endTime, description: discount.description || ''
    });
  } catch (e) { message.error('Không tìm thấy mã'); router.back(); }
  finally { loadingDiscount.value = false; }
};

const fetchAppliedProducts = async () => {
  loadingApplied.value = true;
  try {
    const res = await getAppliedProducts(discountId, { page: 1, size: 2000, q: '' });
    appliedProducts.value = res.items; 
  } catch (e) { message.error('Lỗi tải SP đã áp dụng'); } finally { loadingApplied.value = false; }
};


const handleBulkApply = async () => {
  if (!canUpdateProducts.value) return; 
  bulkApplying.value = true;
  try {
    const items = selectedUnappliedKeys.value.map(id => {
      const p = unappliedProducts.value.find(x => x.id.toString() === id.toString());
      return p ? {
        productDetailId: p.id.toString(), originalPrice: p.price,
        salePrice: Math.round(p.price * (100 - formData.percentage) / 100),
        description: 'Update apply'
      } : null;
    }).filter(Boolean);

    await applyMultipleProducts(discountId, items as any);
    message.success(`Đã thêm ${items.length} sản phẩm`);
    clearUnapplied(); 
    fetchAppliedProducts(); 
  } catch (e) { message.error('Lỗi khi áp dụng'); }
  finally { bulkApplying.value = false; }
};

const handleBulkRemove = async () => {
  if (!canUpdateProducts.value) return;
  bulkRemoving.value = true;
  try {
    await Promise.all(selectedAppliedKeys.value.map(id => removeProductsFromDiscount(id.toString())));
    message.success(`Đã gỡ ${selectedAppliedKeys.value.length} sản phẩm`);
    selectedAppliedKeys.value = [];
    fetchAppliedProducts();
  } catch (e) { message.error('Lỗi khi gỡ nhiều'); } finally { bulkRemoving.value = false; }
};

const handleRemoveProduct = async (id: string) => {
  if (!canUpdateProducts.value) return;
  try { await removeProductsFromDiscount(id); message.success('Đã gỡ sản phẩm'); fetchAppliedProducts(); } catch (e) { message.error('Lỗi khi gỡ'); }
};

const handleSubmit = async () => {
  if (!canUpdateInfo.value) return;
  try { await formRef.value?.validate(); } catch(e) { return; }
  submitting.value = true;
  try {
    await updateDiscount(discountId, formData);
    message.success('Cập nhật thành công!');
    router.back();
  } catch (e: any) { if(!e.errorFields) message.error(e.message || "Lỗi cập nhật"); }
  finally { submitting.value = false; }
};

const handleAppliedPageChange = (p: number) => { appliedCurrentPage.value = p; };

const productColumns: DataTableColumns<ProductResponse> = [
  { type: 'selection', disabled: () => !canUpdateProducts.value }, 
   { title: "STT", key: "stt", width: 50, align: "center", render: (_, i) => (productCurrentPage.value - 1) * productPageSize.value + i + 1 },
  { title: 'Mã', key: 'productCode', width: 100, render: r => h('strong', r.productCode) },
  { title: 'Tên', key: 'productName', ellipsis: { tooltip: true } },
  { title: 'Thương hiệu', key: 'productBrand', width: 100 }
];

const unappliedProductColumns: DataTableColumns<ExtendedProductDetail> = [
  { type: 'selection' },
  { title: "STT", width: 50, align: "center", render: (_, i) => (unappliedCurrentPage.value - 1) * unappliedPageSize.value + i + 1 },
   { title: 'Tên SPCT', key: 'productName', ellipsis: { tooltip: true } },
  
  { title: 'Màu', key: 'colorName', align: 'center', render: r => r.colorName || '-' },
  { title: 'RAM', key: 'ramName', align: 'center', render: r => r.ramName || '-' },
  { title: 'Ổ cứng', key: 'hardDriveName', align: 'center', render: r => r.hardDriveName || '-' },
  { title: 'GPU', key: 'gpuName', align: 'center', render: r => r.gpuName || '-' },
  { title: 'CPU', key: 'cpuName', align: 'center', render: r => r.cpuName || '-' },
  { title: 'Giá giảm', key: 'salePrice', width: 120, align: 'right', render: r => 
    h('div', { class: 'price-cell' }, [
      h('div', { style: 'text-decoration:line-through;color:#999;font-size:11px' }, formatPrice(r.price)),
      h('div', { style: 'color:#d03050;font-weight:bold' }, formatPrice(Math.round(r.price * (100 - formData.percentage) / 100)))
    ]) 
  }
];

const appliedProductColumns: DataTableColumns<AppliedProductResponse> = [
  { type: 'selection', disabled: () => !canUpdateProducts.value },
  { title: "STT", key: "stt", width: 50, align: "center", render: (_, i) => (appliedCurrentPage.value - 1) * appliedPageSize.value + i + 1 },
  { title: 'Tên SPCT', key: 'productName', ellipsis: { tooltip: true } },
  

  { title: 'Màu', key: 'colorName', align: 'center', render: r => r.colorName || '-' },
  { title: 'RAM', key: 'ramName', align: 'center', render: r => r.ramName || '-' },
  { title: 'Ổ cứng', key: 'hardDriveName', align: 'center', render: r => r.hardDriveName || '-' },
  { title: 'GPU', key: 'gpuName', align: 'center', render: r => r.gpuName || '-' },
  { title: 'CPU', key: 'cpuName', align: 'center', render: r => r.cpuName || '-' },
  { title: 'Giá giảm', key: 'salePrice', width: 120, align: 'right', render: r => 
    h('div', [
      h('div', { style: 'color:#d03050;font-weight:bold' }, formatPrice(Math.round(r.price * (100 - r.percentageDiscount) / 100))),
      h('div', { style: 'font-size:10px;color:#666' }, `-${r.percentageDiscount}%`)
    ]) 
  },
  { 
    title: '', 
    key: 'actions', 
    width: 60, 
    render: r => {

      if (!canUpdateProducts.value) return null;
      return h(NPopconfirm, { onPositiveClick: () => handleRemoveProduct(r.id), positiveText: 'Xóa', negativeText: 'Hủy' }, { trigger: () => h(NButton, { size: 'small', type: 'error', quaternary: true, circle: true }, { icon: () => h(Icon, { icon: 'carbon:trash-can' }) }), default: () => 'Gỡ sản phẩm này?' });
    }
  }
];

watch(() => formData.percentage, () => {
  if (unappliedProducts.value.length > 0) unappliedProducts.value = [...unappliedProducts.value];
});

onMounted(() => { fetchDiscount(); fetchAppliedProducts(); fetchProducts(); });
</script>

<style scoped>
.mb-4 { margin-bottom: 16px; } .mb-2 { margin-bottom: 8px; }
.header-title { font-weight: 600; font-size: 20px; margin-left: 8px; }
.sub-title { color: #666; font-size: 13px; }
.main-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.flex { display: flex; } .items-center { align-items: center; } .justify-between { justify-content: space-between; }
.filter-label { font-size: 11px; color: #666; margin-bottom: 2px; font-weight: 500; }
:deep(.n-card .n-card__content) { padding: 20px; } :deep(.n-form-item-label) { font-weight: 500; }
@media (max-width: 1024px) { .main-layout { grid-template-columns: 1fr; } }
</style>