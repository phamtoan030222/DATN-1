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
          <Icon :icon="'carbon:add'" />
        </NIcon>
        <span style="font-weight: 600; font-size: 24px">
          Thêm đợt giảm giá
        </span>
      </NSpace>
      <span>Tạo mới đợt giảm giá cho hệ thống</span>
    </NSpace>
  </n-card>

  <!-- Main Content Layout - 50:50 -->
  <div class="main-layout">
    <!-- Left Side - Form -->
    <div class="form-section">
      <NCard title="Thông tin đợt giảm giá">
        <NForm ref="formRef" :model="formData" :rules="formRules">
          <NSpace vertical :size="16">
            <!-- Row 1: Tên và Mã giảm giá -->
            <div class="form-row">
              <NFormItem label="Tên đợt giảm giá" path="discountName" required class="form-item-half">
                <NInput 
                  v-model:value="formData.discountName" 
                  placeholder="Nhập tên đợt giảm giá"
                  maxlength="100"
                  show-count
                />
              </NFormItem>
              
              <NFormItem label="Mã giảm giá" path="discountCode" required class="form-item-half">
                <NInput 
                  v-model:value="formData.discountCode" 
                  placeholder="Nhập mã giảm giá"
                  maxlength="50"
                >
                  <template #suffix>
                    <NButton text @click="generateCode" title="Tạo mã tự động">
                      <NIcon size="16">
                        <Icon :icon="'carbon:rotate'" />
                      </NIcon>
                    </NButton>
                  </template>
                </NInput>
              </NFormItem>
            </div>

            <!-- Row 2: Ngày bắt đầu và Ngày kết thúc -->
            <div class="form-row">
              <NFormItem label="Ngày bắt đầu" path="startDate" required class="form-item-half">
                <NDatePicker
                  v-model:value="formData.startDate"
                  type="datetime"
                  placeholder="Chọn ngày bắt đầu"
                  style="width: 100%"
                  :is-date-disabled="(ts: number) => ts < Date.now() - 24 * 60 * 60 * 1000"
                />
              </NFormItem>

              <NFormItem label="Ngày kết thúc" path="endDate" required class="form-item-half">
                <NDatePicker
                  v-model:value="formData.endDate"
                  type="datetime"
                  placeholder="Chọn ngày kết thúc"
                  style="width: 100%"
                  :is-date-disabled="(ts: number) => ts <= formData.startDate"
                />
              </NFormItem>
            </div>

            <!-- Row 3: Phần trăm giảm giá (full width) -->
            <NFormItem 
              label="Phần trăm giảm giá (%)" 
              path="percentage"
              required
              :validation-status="formErrors.percentage ? 'error' : undefined"
              :feedback="formErrors.percentage"
            >
              <NInputNumber
                v-model:value="formData.percentage"
                :min="1"
                :max="100"
                :precision="0"
                placeholder="Nhập % giảm giá"
                style="width: 100%"
                @keydown="preventNonNumericInput"
                @update:value="validatePercentageRealtime"
                @blur="validatePercentageOnBlur"
              />
            </NFormItem>

            <!-- Row 4: Mô tả (full width) -->
            <NFormItem label="Mô tả" path="description">
              <NInput
                v-model:value="formData.description"
                type="textarea"
                placeholder="Nhập mô tả cho đợt giảm giá"
                maxlength="500"
                show-count
                :rows="3"
              />
            </NFormItem>

            <NSpace justify="space-between" style="margin-top: 24px;">
              <NButton @click="$router.back()">
                Hủy
              </NButton>
              <NButton 
                type="primary" 
                @click="handleSubmit"
                :loading="submitting"
              >
                Tạo và Áp dụng
              </NButton>
            </NSpace>
          </NSpace>
        </NForm>
      </NCard>
    </div>

    <!-- Right Side - Products Table -->
    <div class="products-section">
    
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
              {{ totalProducts }} sản phẩm
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

        <div class="flex justify-center mt-4" v-if="totalProducts > productPageSize">
          <NPagination
            :page="productCurrentPage"
            :page-size="productPageSize"
            :page-count="Math.ceil(totalProducts / productPageSize)"
            @update:page="handleProductPageChange"
            size="small"
          />
        </div>
      </NCard>
    </div>
  </div>

  <!-- Bottom - Product Details Table -->
  <NCard 
    title="Chi tiết sản phẩm sẽ áp dụng giảm giá" 
    style="margin-top: 16px;" 
    v-if="selectedProductDetails.length > 0"
  >
    <template #header-extra>
      <NSpace>
        <NTag type="success" size="small">
          <template #icon>
            <NIcon>
              <Icon icon="carbon:checkmark-filled" />
            </NIcon>
          </template>
          {{ selectedProductDetails.length }} sản phẩm chi tiết
        </NTag>
        <NButton
          type="error"
          secondary
          size="small"
          @click="clearSelectedProductDetails"
        >
          <template #icon>
            <NIcon><Icon icon="carbon:trash-can" /></NIcon>
          </template>
          Xóa tất cả
        </NButton>
      </NSpace>
    </template>

    <NDataTable
      :columns="productDetailColumns"
      :data="selectedProductDetails"
      :row-key="(row) => row.id"
      v-model:checked-row-keys="selectedDetailKeys"
      :pagination="false"
      size="small"
      max-height="400px"
      :scroll-x="1000"
    />
  </NCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h } from "vue";
import { useRouter } from "vue-router";
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
  useMessage,
  FormInst,
  FormRules,
  DataTableColumns
} from "naive-ui";
import { Icon } from "@iconify/vue";
import {
  createDiscount,
  getAllProducts,
  getProductDetailsByProductId,
  type CreateDiscountRequest,
  type ProductResponse,
  type ProductDetailResponse,
  applyMultipleProducts
} from '@/service/api/admin/discount/discountApi';

const router = useRouter();
const message = useMessage();
const formRef = ref<FormInst>();
const submitting = ref(false);

// Product related states
const products = ref<ProductResponse[]>([]);
const selectedProductDetails = ref<ProductDetailResponse[]>([]);
const selectedDetailKeys = ref<(string | number)[]>([]);
const loadingProducts = ref(false);
const loadingProductDetails = ref(false);
const productSearchKeyword = ref('');
const productCurrentPage = ref(1);
const productPageSize = ref(10);
const totalProducts = ref(0);

let productSearchTimeout: ReturnType<typeof setTimeout>;

const formData = reactive<CreateDiscountRequest>({
  discountName: "",
  discountCode: "",
  percentage: 1,
  startDate: Date.now(), // Thời gian hiện tại
  endDate: Date.now() + 7 * 24 * 60 * 60 * 1000, // 7 ngày sau
  description: ""
});

const formErrors = reactive({
  percentage: ''
});

const formRules: FormRules = {
  // discountName: [
  //   { required: true, message: 'Vui lòng nhập tên đợt giảm giá', trigger: 'blur' }
  // ],
  // discountCode: [
  //   { required: true, message: 'Vui lòng nhập mã giảm giá', trigger: 'blur' }
  // ],
  // percentage: [
  //   { required: true, message: 'Vui lòng nhập phần trăm giảm giá', trigger: 'blur' }
  // ],
  // startDate: [
  //   { required: true, message: 'Vui lòng chọn ngày bắt đầu', trigger: 'blur' }
  // ],
  // endDate: [
  //   { required: true, message: 'Vui lòng chọn ngày kết thúc', trigger: 'blur' }
  // ]
};


const formatPrice = (price: number) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(price);
};


const validatePercentageRealtime = (value: number | null) => {
  if (formErrors.percentage) {
    formErrors.percentage = '';
  }
  
  if (value === null || value === undefined) {
    return;
  }
  
  setTimeout(() => {
    validatePercentageValue(value);
  }, 100);
};

const validatePercentageOnBlur = () => {
  validatePercentageValue(formData.percentage);
};

const validatePercentageValue = (value: number | null) => {
  formErrors.percentage = '';
  
  if (value === null || value === undefined) {
    formErrors.percentage = 'Vui lòng nhập phần trăm giảm giá';
    return false;
  }
  
  if (isNaN(Number(value))) {
    formErrors.percentage = 'Phần trăm giảm giá phải là số hợp lệ';
    return false;
  }
  
  if (value <= 0) {
    formErrors.percentage = 'Phần trăm giảm giá phải lớn hơn 0';
    return false;
  }
  
  if (value > 100) {
    formErrors.percentage = 'Phần trăm giảm giá không được vượt quá 100%';
    return false;
  }
  
  const decimalPlaces = (value.toString().split('.')[1] || '').length;
  if (decimalPlaces > 2) {
    formErrors.percentage = 'Phần trăm giảm giá chỉ được có tối đa 2 chữ số thập phân';
    return false;
  }
  
  return true;
};

const preventNonNumericInput = (event: KeyboardEvent) => {
  const allowedKeys = [
    'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
    'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
    'Home', 'End'
  ];
  
  if (allowedKeys.includes(event.key)) {
    return;
  }
  
  if (event.ctrlKey && ['a', 'c', 'v', 'x'].includes(event.key.toLowerCase())) {
    return;
  }
  
  if (!/[0-9.]/.test(event.key)) {
    event.preventDefault();
    formErrors.percentage = 'Chỉ được nhập số và dấu thập phân';
    return;
  }
  
  const currentValue = (event.target as HTMLInputElement).value;
  if (event.key === '.' && currentValue.includes('.')) {
    event.preventDefault();
    formErrors.percentage = 'Chỉ được nhập một dấu thập phân';
    return;
  }
};

const generateCode = () => {
  const timestamp = Date.now().toString().slice(-6);
  const random = Math.random().toString(36).substring(2, 6).toUpperCase();
  formData.discountCode = `PROMO${timestamp}${random}`;
};

// Product functions
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
    totalProducts.value = res.totalItems;
  } catch (error) {
    console.error('Không thể tải danh sách sản phẩm:', error);
    message.error('Không thể tải danh sách sản phẩm');
    products.value = [];
    totalProducts.value = 0;
  } finally {
    loadingProducts.value = false;
  }
};

const debouncedProductSearch = () => {
  clearTimeout(productSearchTimeout);
  productSearchTimeout = setTimeout(() => {
    productCurrentPage.value = 1;
    fetchProducts();
  }, 500);
};

const handleProductPageChange = (page: number) => {
  productCurrentPage.value = page;
  fetchProducts();
};

// Handle product click to get product details
const handleProductClick = async (productId: string) => {
  try {
    loadingProductDetails.value = true;
    const res = await getProductDetailsByProductId(productId);
    
    // API trả về response với structure: { data: ProductDetailResponse[] }
    const productDetails = res?.data;
    
    if (productDetails && Array.isArray(productDetails)) {
      // Add new product details, avoiding duplicates
      const newDetails = productDetails.filter((newDetail: ProductDetailResponse) => 
        !selectedProductDetails.value.some(existing => existing.id === newDetail.id)
      );
      
      if (newDetails.length > 0) {
        selectedProductDetails.value = [...selectedProductDetails.value, ...newDetails];
        
        // Auto-select all new details
        const newDetailIds = newDetails.map(detail => detail.id);
        selectedDetailKeys.value = [...selectedDetailKeys.value, ...newDetailIds];
        
        message.success(`Đã thêm ${newDetails.length} sản phẩm chi tiết`);
      } else {
        message.info('Tất cả sản phẩm chi tiết đã được thêm trước đó');
      }
    } else {
      message.warning('Sản phẩm này không có sản phẩm chi tiết');
    }
  } catch (error: any) {
    console.error('Lỗi khi tải chi tiết sản phẩm:', error);
    
    // Xử lý các loại lỗi cụ thể
    if (error?.response?.status === 404) {
      message.error('Không tìm thấy sản phẩm chi tiết');
    } else if (error?.response?.status === 500) {
      message.error('Lỗi server khi tải sản phẩm chi tiết');
    } else {
      message.error('Không thể tải chi tiết sản phẩm');
    }
  } finally {
    loadingProductDetails.value = false;
  }
};

const clearSelectedProductDetails = () => {
  selectedProductDetails.value = [];
  selectedDetailKeys.value = [];
};

// Product table columns
const productColumns: DataTableColumns<ProductResponse> = [
  {
    title: "STT",
    key: "stt",
    width: 30,
    align: "center",
    render(_: ProductResponse, index: number) {
      return (productCurrentPage.value - 1) * productPageSize.value + index + 1;
    }
  },
  {
    title: 'Mã sản phẩm',
    key: 'productCode',
    width: 70,
    render(row) {
      return h('strong', row.productCode);
    }
  },
  {
    title: 'Tên sản phẩm',
    key: 'productName',
    width: 60,
    ellipsis: { tooltip: true }
  },
  {
    title: 'Thương hiệu',
    key: 'productBrand',
    width: 50,
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 30,
    render(row) {
      return h(NButton, {
        size: 'small',
        type: 'primary',
        secondary: true,
        loading: loadingProductDetails.value,
        onClick: () => handleProductClick(row.id)
      }, {
        default: () => 'Thêm'
      });
    }
  }
];

// Product details table columns
const productDetailColumns: DataTableColumns<ProductDetailResponse> = [
  { type: 'selection' },
  {
    title: "STT",
    key: "stt",
    width: 60,
    align: "center",
    render(_: ProductDetailResponse, index: number) {
      return index + 1;
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
    title: 'Giá sau giảm',
    key: 'salePrice',
    width: 120,
    render(row) {
      const salePrice = Math.round(row.price * (100 - formData.percentage) / 100);
      return h('div', { style: 'color: #f56565; font-weight: 600; font-size: 16px' }, formatPrice(salePrice));
    }
  },
  {
    title: 'Tiết kiệm',
    key: 'savings',
    width: 120,
    render(row) {
      const savings = row.price - Math.round(row.price * (100 - formData.percentage) / 100);
      return h('div', { style: 'color: #52c41a; font-weight: 600' }, formatPrice(savings));
    }
  },
  {
    title: 'Cấu hình',
    key: 'specs',
    width: 250,
    render(row) {
      const specs = [];
      if (row.colorName) specs.push(`Màu: ${row.colorName}`);
      if (row.ramName) specs.push(`RAM: ${row.ramName}`);
      if (row.hardDriveName) specs.push(`Ổ cứng: ${row.hardDriveName}`);
      if (row.gpuName) specs.push(`GPU: ${row.gpuName}`);
      if (row.cpuName) specs.push(`CPU: ${row.cpuName}`);
      
      return h('div', { style: 'font-size: 12px; line-height: 1.4' },
        specs.length > 0 
          ? specs.map(spec => h('div', spec))
          : '-'
      );
    }
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    render(row) {
      return h(NButton, {
        size: 'small',
        type: 'error',
        quaternary: true,
        onClick: () => {
          selectedProductDetails.value = selectedProductDetails.value.filter(detail => detail.id !== row.id);
          selectedDetailKeys.value = selectedDetailKeys.value.filter(id => id !== row.id);
        }
      }, {
        default: () => h(Icon, { icon: 'carbon:trash-can', width: '16' })
      });
    }
  }
];

const handleSubmit = async () => {
  console.log('🚀 Starting submit process...');
  
  try {

    // await formRef.value?.validate();
    
    // // Custom validations
    // if (!formData.discountName || !formData.discountCode || !formData.percentage) {
    //   message.warning("Vui lòng nhập đầy đủ thông tin bắt buộc");
    //   return;
    // }

    // const now = Date.now();
    // if (formData.startDate <= now) {
    //   message.warning("Thời gian bắt đầu phải lớn hơn thời gian hiện tại");
    //   return;
    // }

    // if (formData.startDate >= formData.endDate) {
    //   message.warning("Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc");
    //   return;
    // }

    // const isPercentageValid = validatePercentageValue(formData.percentage);
    // if (!isPercentageValid) {
    //   message.warning("Vui lòng kiểm tra lại phần trăm giảm giá");
    //   return;
    // }

    // Get selected product details
    const selectedDetails = selectedProductDetails.value.filter(detail => 
      selectedDetailKeys.value.includes(detail.id)
    );

    console.log('📦 Selected details:', selectedDetails);
    console.log('🎯 Selected detail keys:', selectedDetailKeys.value);

    if (selectedDetails.length === 0) {
      message.warning("Vui lòng chọn ít nhất một sản phẩm chi tiết để áp dụng giảm giá");
      return;
    }

    submitting.value = true;
    
    console.log('📝 Creating discount with data:', formData);
    
    // Tạo discount trước
    const createdDiscount = await createDiscount(formData);
    
    console.log('✅ Created discount:', createdDiscount);
    
    if (!createdDiscount?.data?.id) {
      throw new Error("Không thể tạo đợt giảm giá - không nhận được ID");
    }

    // Prepare products data for applying discount
    const productsToApply = selectedDetails.map(detail => ({
      productDetailId: detail.id.toString(),
      originalPrice: detail.price,
      salePrice: Math.round(detail.price * (100 - formData.percentage) / 100),
      description: formData.description || 'Áp dụng sản phẩm khi tạo discount'
    }));

    console.log('🎯 Products to apply:', productsToApply);

    // Áp dụng discount cho các sản phẩm chi tiết đã chọn
    const discountId = createdDiscount.data.id;
    
    try {
      await applyMultipleProducts(discountId, productsToApply);
      message.success(`Đã tạo đợt giảm giá và áp dụng thành công cho ${selectedDetails.length} sản phẩm chi tiết`);
    } catch (applyError: any) {
      console.error('❌ Error applying products:', applyError);
      message.warning(`Đã tạo đợt giảm giá nhưng có lỗi khi áp dụng sản phẩm: ${applyError?.message || 'Unknown error'}`);
    }
    
    router.back();
    
  } catch (error: any) {
    console.error('❌ Submit error:', error);
    message.error(error?.message || "Có lỗi xảy ra khi tạo đợt giảm giá");
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  generateCode();
  fetchProducts();
});
</script>

<style scoped>
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

/* Main layout - 50:50 split */
.main-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.form-section,
.products-section {
  min-width: 0; /* Prevents overflow in grid */
}

/* Form row layout */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-item-half {
  margin-bottom: 0;
}

/* Responsive design */
@media (max-width: 1400px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .main-layout {
    grid-template-columns: 1fr;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>