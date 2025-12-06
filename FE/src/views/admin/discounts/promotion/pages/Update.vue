<template>
  <!-- Header -->
  <n-card>
    <NSpace vertical :size="8">
      <NSpace :align="'center'">
        <NButton 
          @click="$router.back()" 
          quaternary 
          circle
        >
          <NIcon size="20">
            <Icon :icon="'carbon:arrow-left'" />
          </NIcon>
        </NButton>
        <NIcon size="24">
          <Icon :icon="'carbon:edit'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Sửa đợt giảm giá
        </span>
      </NSpace>
      <span>Chỉnh sửa thông tin đợt giảm giá</span>
    </NSpace>
  </n-card>

  <!-- Main Content Layout -->
  <div style="display: grid; grid-template-columns: 630px 610px; gap: 16px; margin-top: 16px;">
    <!-- Left Side - Form -->
    <div>
      <NCard title="Thông tin đợt giảm giá">
        <NSkeleton v-if="loadingDiscount" text :repeat="6" />
        <NForm v-else ref="formRef" :model="formData">
          <NSpace vertical :size="16">
            <div class="form-row">
              <NFormItem label="Tên đợt giảm giá" required>
              <NInput 
                v-model:value="formData.discountName" 
                placeholder="Nhập tên đợt giảm giá"
                maxlength="100"
                show-count
              />
            </NFormItem>
            
            <NFormItem label="Mã giảm giá" required>
              <NInput 
                v-model:value="formData.discountCode" 
                placeholder="Nhập mã giảm giá"
                maxlength="50"
                :disabled="true"
              />
            </NFormItem>
            </div>

            <div class="form-row">
                 <NFormItem label="Ngày bắt đầu" required>
              <NDatePicker
                v-model:value="formData.startDate"
                type="datetime"
                placeholder="Chọn ngày bắt đầu"
                style="width: 100%"
                :disabled="getDiscountStatus() === 'active'"
              />
            </NFormItem>

            <NFormItem label="Ngày kết thúc" required>
              <NDatePicker
                v-model:value="formData.endDate"
                type="datetime"
                placeholder="Chọn ngày kết thúc"
                style="width: 100%"
                :is-date-disabled="(ts: number) => ts <= formData.startDate"
              />
            </NFormItem>
            </div>
         

            <NFormItem label="Phần trăm giảm giá (%)" required>
              <NInputNumber
                v-model:value="formData.percentage"
                :min="1"
                :max="100"
                :precision="0"
                placeholder="Nhập % giảm giá"
                style="width: 100%"
              />
            </NFormItem>

            <NFormItem label="Mô tả">
              <NInput
                v-model:value="formData.description"
                type="textarea"
                placeholder="Nhập mô tả cho đợt giảm giá"
                maxlength="500"
                show-count
                :rows="3"
              />
            </NFormItem>

            <!-- Status Info -->
            <NFormItem label="Trạng thái">
              <NTag :type="getStatusType()">
                {{ getStatusText() }}
              </NTag>
            </NFormItem>

            <NSpace justify="space-between" style="margin-top: 24px;">
              <NButton @click="$router.back()">
                Hủy
              </NButton>
              <NButton 
                type="primary" 
                @click="handleSubmit"
                :loading="submitting"
                :disabled="getDiscountStatus() === 'expired'"
              >
                Cập nhật
              </NButton>
            </NSpace>
          </NSpace>
        </NForm>
      </NCard>
    </div>

    <!-- Right Side - Products Table -->
    <div>
      <NCard title="Danh sách sản phẩm">
        <template #header-extra>
          <NSpace>
            <NInput
              v-model:value="productSearchKeyword"
              placeholder="Tìm sản phẩm..."
              clearable
              style="width: 250px"
              @input="debouncedProductSearch"
            >
              <template #prefix>
                <NIcon size="18">
                  <Icon :icon="'carbon:search'" />
                </NIcon>
              </template>
            </NInput>
            <NTag type="info" size="small">
              {{ productTotal }} sản phẩm
            </NTag>
          </NSpace>
        </template>

        <NDataTable
          :columns="productColumns"
          :data="products"
          :loading="loadingProducts"
          :row-key="(row) => row.id"
          :pagination="false"
          size="small"
          max-height="500px"
        />

        <div class="flex justify-center mt-4" v-if="productTotal > productPageSize">
          <NPagination
            :page="productCurrentPage"
            :page-size="productPageSize"
            :page-count="Math.ceil(productTotal / productPageSize)"
            @update:page="handleProductPageChange"
            size="small"
          />
        </div>
      </NCard>
    </div>
  </div>

  <!-- Product Details Not Applied Table - Show when product is selected -->
  <NCard 
    v-if="selectedProduct" 
    :title="`Sản phẩm chi tiết chưa áp dụng - ${selectedProduct.productName}`" 
    style="margin-top: 16px;"
  >
    <template #header-extra>
      <NSpace>
        <NTag type="warning" size="small">
          {{ unappliedTotal }} sản phẩm chi tiết
        </NTag>
        <NButton
          type="primary"
          size="small"
          @click="handleBulkApply"
          :disabled="selectedUnappliedKeys.length === 0"
          :loading="bulkApplying"
        >
          <template #icon>
            <NIcon><Icon icon="carbon:checkmark" /></NIcon>
          </template>
          Áp dụng đã chọn ({{ selectedUnappliedKeys.length }})
        </NButton>
        <NButton
          quaternary
          size="small"
          @click="selectedProduct = null"
        >
          <template #icon>
            <NIcon><Icon icon="carbon:close" /></NIcon>
          </template>
          Đóng
        </NButton>
      </NSpace>
    </template>

    <NDataTable
      :columns="unappliedProductColumns"
      :data="unappliedProducts"
      :loading="loadingUnapplied"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="selectedUnappliedKeys"
      :pagination="false"
      size="small"
      max-height="400px"
      :scroll-x="1200"
    />

    <div class="flex justify-center mt-4" v-if="unappliedTotal > unappliedPageSize">
      <NPagination
        :page="unappliedCurrentPage"
        :page-size="unappliedPageSize"
        :page-count="Math.ceil(unappliedTotal / unappliedPageSize)"
        @update:page="handleUnappliedPageChange"
        size="small"
      />
    </div>
  </NCard>

  <!-- Bottom - Applied Products Table -->
  <NCard title="Sản phẩm chi tiết đã áp dụng giảm giá" style="margin-top: 16px;">
    <template #header-extra>
      <NSpace>
        <NTag type="success" size="small">
          <template #icon>
            <NIcon>
              <Icon icon="carbon:checkmark-filled" />
            </NIcon>
          </template>
          {{ appliedTotal }} sản phẩm đã áp dụng
        </NTag>
        <NInput
          v-model:value="appliedSearchKeyword"
          placeholder="Tìm sản phẩm đã áp dụng..."
          clearable
          style="width: 250px"
          @input="debouncedAppliedSearch"
        >
          <template #prefix>
            <NIcon size="18">
              <Icon :icon="'carbon:search'" />
            </NIcon>
          </template>
        </NInput>
        <NButton
          type="error"
          secondary
          size="small"
          @click="handleBulkRemove"
          :disabled="selectedAppliedKeys.length === 0"
          :loading="bulkRemoving"
        >
          <template #icon>
            <NIcon><Icon icon="carbon:trash-can" /></NIcon>
          </template>
          Xóa đã chọn ({{ selectedAppliedKeys.length }})
        </NButton>
      </NSpace>
    </template>

    <NDataTable
      :columns="appliedProductColumns"
      :data="appliedProducts"
      :loading="loadingApplied"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="selectedAppliedKeys"
      :pagination="false"
      size="small"
      max-height="400px"
      :scroll-x="1200"
    />

    <div class="flex justify-center mt-4" v-if="appliedTotal > appliedPageSize">
      <NPagination
        :page="appliedCurrentPage"
        :page-size="appliedPageSize"
        :page-count="Math.ceil(appliedTotal / appliedPageSize)"
        @update:page="handleAppliedPageChange"
        size="small"
      />
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  NButton,
  NSpace,
  NCard,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NIcon,
  NDatePicker,
  NDataTable,
  NTag,
  NPagination,
  NSkeleton,
  NPopconfirm,
  useMessage,
  FormInst,
  DataTableColumns
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
  type UpdateDiscountRequest,
  type DiscountResponse,
  type ProductDetailResponse,
  type AppliedProductResponse,
  type ProductResponse,
} from '@/service/api/admin/discount/discountApi';

const router = useRouter();
const route = useRoute();
const message = useMessage();
const formRef = ref<FormInst>();

const discountId = route.params.id as string;
const submitting = ref(false);
const loadingDiscount = ref(false);
const bulkRemoving = ref(false);

// Original discount data
const originalDiscount = ref<DiscountResponse | null>(null);

// Applied products states
const appliedProducts = ref<AppliedProductResponse[]>([]);
const selectedAppliedKeys = ref<(string | number)[]>([]);
const loadingApplied = ref(false);
const appliedSearchKeyword = ref('');
const appliedCurrentPage = ref(1);
const appliedPageSize = ref(10);
const appliedTotal = ref(0);

// Products states (main products)
const products = ref<ProductResponse[]>([]);
const loadingProducts = ref(false);
const productSearchKeyword = ref('');
const productCurrentPage = ref(1);
const productPageSize = ref(10);
const productTotal = ref(0);
const selectedProduct = ref<ProductResponse | null>(null);

// Unapplied products states (product details)
const unappliedProducts = ref<ProductDetailResponse[]>([]);
const selectedUnappliedKeys = ref<(string | number)[]>([]);
const loadingUnapplied = ref(false);
const unappliedCurrentPage = ref(1);
const unappliedPageSize = ref(10);
const unappliedTotal = ref(0);
const bulkApplying = ref(false);

let appliedSearchTimeout: ReturnType<typeof setTimeout>;
let productSearchTimeout: ReturnType<typeof setTimeout>;

const formData = reactive<UpdateDiscountRequest>({
  discountName: "",
  discountCode: "",
  percentage: 1,
  startDate: Date.now(),
  endDate: Date.now() + 24 * 60 * 60 * 1000,
  description: ""
});

// Format price
const formatPrice = (price: number) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(price);
};

// Format date time
const formatDateTime = (timestamp: number) => {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
};

// Status helpers
const getDiscountStatus = () => {
  if (!originalDiscount.value) return 'unknown';
  const now = Date.now();
  const start = originalDiscount.value.startTime;
  const end = originalDiscount.value.endTime;
  
  if (now < start) return 'upcoming';
  if (now >= start && now <= end) return 'active';
  return 'expired';
};

const getStatusType = () => {
  const status = getDiscountStatus();
  switch (status) {
    case 'upcoming': return 'info';
    case 'active': return 'success';
    case 'expired': return 'default';
    default: return 'default';
  }
};

const getStatusText = () => {
  const status = getDiscountStatus();
  switch (status) {
    case 'upcoming': return 'Sắp diễn ra';
    case 'active': return 'Đang diễn ra';
    case 'expired': return 'Đã hết hạn';
    default: return 'Không xác định';
  }
};

// Fetch discount data
const fetchDiscount = async () => {
  if (!discountId) {
    message.error('ID đợt giảm giá không hợp lệ');
    router.back();
    return;
  }

  loadingDiscount.value = true;
  try {
    const res = await getAllDiscounts({ page: 1, size: 1000 });
    const discount = res.items.find((item: any) => item.id === discountId);
    
    if (!discount) {
      message.error('Không tìm thấy đợt giảm giá');
      router.back();
      return;
    }

    originalDiscount.value = discount;
    
    // Fill form data
    formData.discountName = discount.discountName;
    formData.discountCode = discount.discountCode;
    formData.percentage = discount.percentage;
    formData.startDate = discount.startTime;
    formData.endDate = discount.endTime;
    formData.description = discount.description || '';
    
  } catch (error) {
    console.error('Error fetching discount:', error);
    message.error('Không thể tải thông tin đợt giảm giá');
    router.back();
  } finally {
    loadingDiscount.value = false;
  }
};

// Fetch applied products
const fetchAppliedProducts = async () => {
  loadingApplied.value = true;
  try {
    const params = {
      page: appliedCurrentPage.value,
      size: appliedPageSize.value,
      q: appliedSearchKeyword.value.trim()
    };
    
    const res = await getAppliedProducts(discountId, params);
    appliedProducts.value = res.items;
    appliedTotal.value = res.totalItems;
  } catch (error) {
    console.error('Error fetching applied products:', error);
    message.error('Không thể tải danh sách sản phẩm đã áp dụng');
    appliedProducts.value = [];
    appliedTotal.value = 0;
  } finally {
    loadingApplied.value = false;
  }
};

// Fetch main products
const fetchProducts = async () => {
  loadingProducts.value = true;
  try {
    const params = {
      page: productCurrentPage.value,
      size: productPageSize.value,
      q: productSearchKeyword.value.trim()
    };
    
    const res = await getAllProducts(params);
    products.value = res.items;
    productTotal.value = res.totalItems;
  } catch (error) {
    console.error('Error fetching products:', error);
    message.error('Không thể tải danh sách sản phẩm');
    products.value = [];
    productTotal.value = 0;
  } finally {
    loadingProducts.value = false;
  }
};

// Fetch product details for selected product
const fetchUnappliedProducts = async () => {
  if (!selectedProduct.value) return;
  
  loadingUnapplied.value = true;
  try {
    const params = {
      page: unappliedCurrentPage.value,
      size: unappliedPageSize.value,
      q: ''
    };
    
    const res = await getUnappliedProducts(discountId, params);
    // Filter by selected product
    const filteredProducts = res.items.filter((item:any) => 
      item.productCode === selectedProduct.value?.productCode
    );
    
    unappliedProducts.value = filteredProducts;
    unappliedTotal.value = filteredProducts.length;
  } catch (error) {
    console.error('Error fetching unapplied products:', error);
    message.error('Không thể tải danh sách sản phẩm chi tiết');
    unappliedProducts.value = [];
    unappliedTotal.value = 0;
  } finally {
    loadingUnapplied.value = false;
  }
};

// Search functions
const debouncedAppliedSearch = () => {
  clearTimeout(appliedSearchTimeout);
  appliedSearchTimeout = setTimeout(() => {
    appliedCurrentPage.value = 1;
    fetchAppliedProducts();
  }, 500);
};

const debouncedProductSearch = () => {
  clearTimeout(productSearchTimeout);
  productSearchTimeout = setTimeout(() => {
    productCurrentPage.value = 1;
    fetchProducts();
  }, 500);
};

// Page change handlers
const handleAppliedPageChange = (page: number) => {
  appliedCurrentPage.value = page;
  fetchAppliedProducts();
};

const handleProductPageChange = (page: number) => {
  productCurrentPage.value = page;
  fetchProducts();
};

const handleUnappliedPageChange = (page: number) => {
  unappliedCurrentPage.value = page;
  fetchUnappliedProducts();
};

// Handle product selection
const handleProductSelect = async (product: ProductResponse) => {
  selectedProduct.value = product;
  selectedUnappliedKeys.value = [];
  unappliedCurrentPage.value = 1;
  await fetchUnappliedProducts();
};

// Bulk apply selected products
const handleBulkApply = async () => {
  if (selectedUnappliedKeys.value.length === 0) {
    message.warning('Chưa chọn sản phẩm nào để áp dụng');
    return;
  }

  bulkApplying.value = true;
  try {
    const productsToApply: Array<{
      productDetailId: string;
      originalPrice: number;
      salePrice: number;
      description: string;
    }> = [];

    selectedUnappliedKeys.value.forEach(id => {
      const product = unappliedProducts.value.find(p => p.id.toString() === id.toString());
      if (product) {
        const salePrice = Math.round(product.price * (100 - formData.percentage) / 100);
        productsToApply.push({
          productDetailId: product.id.toString(),
          originalPrice: product.price,
          salePrice: salePrice,
          description: 'Áp dụng từ trang cập nhật'
        });
      }
    });

    if (productsToApply.length > 0) {
      await applyMultipleProducts(discountId, productsToApply);
      message.success(`Đã áp dụng giảm giá cho ${productsToApply.length} sản phẩm`);
      
      selectedUnappliedKeys.value = [];
      fetchAppliedProducts();
      fetchUnappliedProducts();
    }
  } catch (error) {
    console.error('Error bulk applying products:', error);
    message.error('Có lỗi xảy ra khi áp dụng giảm giá');
  } finally {
    bulkApplying.value = false;
  }
};

// Remove products from discount
const handleRemoveProduct = async (productId: string) => {
  try {
    await removeProductsFromDiscount(productId);
    message.success('Đã xóa sản phẩm khỏi đợt giảm giá');
    
    // Refresh both tables
    fetchAppliedProducts();
    if (selectedProduct.value) {
      fetchUnappliedProducts();
    }
  } catch (error) {
    console.error('Error removing product:', error);
    message.error('Không thể xóa sản phẩm khỏi đợt giảm giá');
  }
};

// Bulk remove applied products
const handleBulkRemove = async () => {
  if (selectedAppliedKeys.value.length === 0) {
    message.warning('Chưa chọn sản phẩm nào để xóa');
    return;
  }

  bulkRemoving.value = true;
  try {
    const promises = selectedAppliedKeys.value.map(id => 
      removeProductsFromDiscount(id.toString())
    );
    
    await Promise.all(promises);
    message.success(`Đã xóa ${selectedAppliedKeys.value.length} sản phẩm khỏi đợt giảm giá`);
    
    selectedAppliedKeys.value = [];
    fetchAppliedProducts();
    if (selectedProduct.value) {
      fetchUnappliedProducts();
    }
  } catch (error) {
    console.error('Error bulk removing products:', error);
    message.error('Có lỗi xảy ra khi xóa sản phẩm');
  } finally {
    bulkRemoving.value = false;
  }
};

// Table columns for main products
const productColumns: DataTableColumns<ProductResponse> = [
  {
    title: "STT",
    key: "stt",
    width: 40,
    align: "center",
    render(_: ProductResponse, index: number) {
      return (productCurrentPage.value - 1) * productPageSize.value + index + 1;
    }
  },
  {
    title: 'Mã sản phẩm',
    key: 'productCode',
    width: 120,
    render(row) {
      return h('strong', row.productCode);
    }
  },
  {
    title: 'Tên sản phẩm',
    key: 'productName',
    width: 130,
    ellipsis: { tooltip: true }
  },
  {
    title: 'Thương hiệu',
    key: 'productBrand',
    width: 120
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 60,
    render(row) {
      return h(NButton, {
        size: 'small',
        type: 'info',
        secondary: true,
        onClick: () => handleProductSelect(row)
      }, {
        default: () => 'Chọn'
      });
    }
  }
];

// Table columns for unapplied product details
const unappliedProductColumns: DataTableColumns<ProductDetailResponse> = [
  { type: 'selection' },
  {
    title: "STT",
    key: "stt",
    width: 60,
    align: "center",
    render(_: ProductDetailResponse, index: number) {
      return (unappliedCurrentPage.value - 1) * unappliedPageSize.value + index + 1;
    }
  },
  {
    title: 'Mã sản phẩm',
    key: 'productCode',
    width: 120,
    render(row) {
      return h('strong', row.productCode);
    }
  },
  {
    title: 'Tên sản phẩm',
    key: 'productName',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: 'Giá hiện tại',
    key: 'price',
    width: 120,
    render(row) {
      return formatPrice(row.price);
    }
  },
  {
    title: 'Giá sau giảm',
    key: 'salePrice',
    width: 120,
    render(row) {
      const salePrice = Math.round(row.price * (100 - formData.percentage) / 100);
      return h('div', { style: 'color: #f56565; font-weight: 600;' }, formatPrice(salePrice));
    }
  },
  {
    title: 'Cấu hình',
    key: 'specs',
    width: 200,
    render(row) {
      const specs = [];
      if (row.colorName) specs.push(`Màu: ${row.colorName}`);
      if (row.ramName) specs.push(`RAM: ${row.ramName}`);
      if (row.hardDriveName) specs.push(`Ổ cứng: ${row.hardDriveName}`);
      
      return h('div', { style: 'font-size: 12px; line-height: 1.4' },
        specs.length > 0 
          ? specs.slice(0, 2).map(spec => h('div', spec))
          : '-'
      );
    }
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 100,
    render(row) {
      return h(NButton, {
        size: 'small',
        type: 'primary',
        secondary: true,
        onClick: async () => {
          try {
            const salePrice = Math.round(row.price * (100 - formData.percentage) / 100);
            const productsToApply = [{
              productDetailId: row.id.toString(),
              originalPrice: row.price,
              salePrice: salePrice,
              description: 'Áp dụng từ trang cập nhật'
            }];

            await applyMultipleProducts(discountId, productsToApply);
            message.success('Đã áp dụng giảm giá cho sản phẩm');
            
            fetchAppliedProducts();
            fetchUnappliedProducts();
          } catch (error) {
            console.error('Error applying product:', error);
            message.error('Không thể áp dụng giảm giá cho sản phẩm');
          }
        }
      }, {
        default: () => 'Áp dụng'
      });
    }
  }
];

const appliedProductColumns: DataTableColumns<AppliedProductResponse> = [
  { type: 'selection' },
  {
    title: "STT",
    key: "stt",
    width: 60,
    align: "center",
    render(_: AppliedProductResponse, index: number) {
      return (appliedCurrentPage.value - 1) * appliedPageSize.value + index + 1;
    }
  },
  {
    title: 'Mã sản phẩm',
    key: 'productCode',
    width: 120,
    render(row) {
      return h('strong', row.productCode);
    }
  },
  {
    title: 'Tên sản phẩm',
    key: 'productName',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: 'Giá gốc',
    key: 'price',
    width: 120,
    render(row) {
      return h('div', { style: 'text-decoration: line-through; color: #999;' }, formatPrice(row.price));
    }
  },
  {
    title: 'Giảm giá',
    key: 'discount',
    width: 100,
    render(row) {
      return `${row.percentageDiscount}%`;
    }
  },
  {
    title: 'Giá sau giảm',
    key: 'salePrice',
    width: 120,
    render(row) {
      const salePrice = Math.round(row.price * (100 - row.percentageDiscount) / 100);
      return h('div', { style: 'color: #f56565; font-weight: 600;' }, formatPrice(salePrice));
    }
  },
  {
    title: 'Thời gian áp dụng',
    key: 'timeRange',
    width: 200,
    render(row) {
      return h('div', { style: 'font-size: 12px; line-height: 1.4;' }, [
        h('div', `${formatDateTime(row.startTime)}`),
        h('div', `đến ${formatDateTime(row.endTime)}`)
      ]);
    }
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 100,
    render(row) {
      return h(NPopconfirm, {
        onPositiveClick: () => handleRemoveProduct(row.id),
        positiveText: 'Xác nhận',
        negativeText: 'Hủy'
      }, {
        trigger: () => h(NButton, {
          size: 'small',
          type: 'error',
          quaternary: true
        }, {
          default: () => h(Icon, { icon: 'carbon:trash-can', width: '16' })
        }),
        default: () => 'Xác nhận xóa sản phẩm khỏi đợt giảm giá?'
      });
    }
  }
];

// Submit form
const handleSubmit = async () => {
  submitting.value = true;
  
  try {
    await updateDiscount(discountId, formData);
    message.success('Đã cập nhật đợt giảm giá thành công');
    router.back();
    
  } catch (error: any) {
    console.error('Update error:', error);
    message.error(error?.message || "Có lỗi xảy ra khi cập nhật đợt giảm giá");
  } finally {
    submitting.value = false;
  }
};

// Watch percentage changes to update sale prices
watch(() => formData.percentage, () => {
  // Force re-render of unapplied products table to show updated sale prices
  if (unappliedProducts.value.length > 0) {
    unappliedProducts.value = [...unappliedProducts.value];
  }
});

onMounted(() => {
  fetchDiscount();
  fetchAppliedProducts();
  fetchProducts();
});
</script>

<style scoped>
/* Form row layout */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

:deep(.n-card .n-card__content) {
  padding: 20px;
}

:deep(.n-form-item-label) {
  font-weight: 500;
}

:deep(.n-input-number) {
  width: 100%;
}

:deep(.n-date-picker) {
  width: 100%;
}

.flex {
  display: flex;
}

.justify-center {
  justify-content: center;
}

.mt-4 {
  margin-top: 16px;
}

@media (max-width: 1200px) {
  div[style*="grid-template-columns: 400px 1fr"] {
    grid-template-columns: 1fr !important;
  }
}
</style>