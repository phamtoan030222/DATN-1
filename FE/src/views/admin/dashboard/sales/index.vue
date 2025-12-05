<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { NCard, NButton, NInput, NInputNumber, NSelect, NTag, useMessage, NSpin } from 'naive-ui';
// import { OrderApi } from '@/service/api/admin/order.api';
import { useAuthStore } from '@/store/auth'; // Lấy thông tin nhân viên đăng nhập

const message = useMessage();
const authStore = useAuthStore();

// --- STATE ---
const loading = ref(false);
const processing = ref(false);
const searchQuery = ref('');
const products = ref<any[]>([]);
const cart = ref<any[]>([]);
const customerKeyword = ref('');
const customers = ref<any[]>([]); // Danh sách khách hàng tìm được
const selectedCustomer = ref<string | null>(null);

// Form thanh toán
const payment = reactive({
  method: 0, // 0: Tiền mặt, 1: Chuyển khoản, 2: Kết hợp
  cash: 0,
  transfer: 0,
  note: ''
});

// --- LOGIC SẢN PHẨM ---
// Hàm lấy danh sách sản phẩm (Demo: gọi API thật của bạn)
async function fetchProducts() {
  loading.value = true;
  try {
    // Thay bằng API search product thực tế của dự án
    // const res = await OrderApi.searchProducts({ keyword: searchQuery.value });
    // products.value = res.data;

    // MOCK DATA để test giao diện
    products.value = [
      { id: 'sp1', name: 'iPhone 15 Pro Max 256GB', price: 29000000, quantity: 5, code: 'IP15PM' },
      { id: 'sp2', name: 'Samsung S24 Ultra', price: 26000000, quantity: 3, code: 'S24U' },
      { id: 'sp3', name: 'Ốp lưng MagSafe', price: 500000, quantity: 50, code: 'OPIP' },
    ];
  } catch (e) {
    message.error('Lỗi tải sản phẩm');
  } finally {
    loading.value = false;
  }
}

// Thêm vào giỏ
function addToCart(product: any) {
  const index = cart.value.findIndex(item => item.productDetailId === product.id);

  if (index !== -1) {
    // Đã có trong giỏ -> Tăng số lượng
    if (cart.value[index].quantity >= product.quantity) {
      message.warning('Đã hết hàng trong kho!');
      return;
    }
    cart.value[index].quantity++;
  } else {
    // Chưa có -> Thêm mới
    cart.value.push({
      productDetailId: product.id,
      name: product.name,
      price: product.price,
      quantity: 1,
      maxQuantity: product.quantity
    });
  }
}

// Xóa khỏi giỏ
function removeFromCart(index: number) {
  cart.value.splice(index, 1);
}

// --- LOGIC THANH TOÁN ---
const totalAmount = computed(() => {
  return cart.value.reduce((sum, item) => sum + (item.price * item.quantity), 0);
});

// Tự động điền tiền mặt/chuyển khoản khi chọn phương thức
function onPaymentMethodChange(val: number) {
  payment.method = val;
  if (val === 0) { // Tiền mặt
    payment.cash = totalAmount.value;
    payment.transfer = 0;
  } else if (val === 1) { // Chuyển khoản
    payment.cash = 0;
    payment.transfer = totalAmount.value;
  }
}

async function handleCheckout() {
  if (cart.value.length === 0) {
    message.warning('Giỏ hàng đang trống');
    return;
  }

  // Validate tiền thanh toán
  const totalPaid = (payment.cash || 0) + (payment.transfer || 0);
  if (totalPaid < totalAmount.value) {
    message.error(`Khách còn thiếu ${(totalAmount.value - totalPaid).toLocaleString()} đ`);
    return;
  }

  const payload = {
    staffId: authStore.userInfo?.id || 'unknown', // Cần ID nhân viên
    customerId: selectedCustomer.value,
    totalAmount: totalAmount.value,
    totalAmountAfterDecrease: totalAmount.value, // Chưa tính voucher
    paymentMethod: payment.method,
    cashAmount: payment.cash,
    transferAmount: payment.transfer,
    description: payment.note,
    typeInvoice: 1, // Tại quầy
    products: cart.value.map(item => ({
      productDetailId: item.productDetailId,
      quantity: item.quantity,
      price: item.price,
      listImeiIds: [] // TODO: Bổ sung chọn IMEI nếu cần
    }))
  };

  try {
    processing.value = true;
    const res = await OrderApi.createOrderAtCounter(payload);
    if (res.data) {
      message.success('Tạo hóa đơn thành công!');
      // Reset
      cart.value = [];
      payment.cash = 0;
      payment.transfer = 0;
    }
  } catch (error) {
    message.error('Thanh toán thất bại');
    console.error(error);
  } finally {
    processing.value = false;
  }
}

onMounted(() => {
  fetchProducts();
});
</script>

<template>
  <div class="flex h-[calc(100vh-100px)] gap-4 p-2 bg-[#f0f2f5]">
    <div class="w-[65%] flex flex-col gap-3">
      <NCard size="small" class="flex-1 flex flex-col">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-bold text-lg">🛍️ Danh sách sản phẩm</span>
            <div class="flex gap-2">
              <NInput v-model:value="searchQuery" placeholder="Tìm tên, mã SP..." @keyup.enter="fetchProducts" />
              <NButton type="primary" @click="fetchProducts">Tìm</NButton>
            </div>
          </div>
        </template>

        <div class="h-[70vh] overflow-y-auto pr-2">
          <NSpin :show="loading">
            <div class="grid grid-cols-3 gap-3">
              <div v-for="prod in products" :key="prod.id"
                class="bg-white border rounded-lg p-3 hover:shadow-lg cursor-pointer transition-all flex flex-col justify-between h-[120px]"
                @click="addToCart(prod)">
                <div>
                  <div class="font-bold text-gray-800 line-clamp-2">{{ prod.name }}</div>
                  <div class="text-xs text-gray-500">Mã: {{ prod.code }}</div>
                </div>
                <div class="flex justify-between items-end mt-2">
                  <div class="text-red-600 font-bold">{{ prod.price.toLocaleString() }} đ</div>
                  <NTag type="info" size="small">Kho: {{ prod.quantity }}</NTag>
                </div>
              </div>
            </div>
          </NSpin>
        </div>
      </NCard>
    </div>

    <div class="w-[35%] flex flex-col gap-3">
      <NCard size="small" class="h-full flex flex-col" title="🧾 Đơn hàng">
        <div class="mb-3">
          <NSelect v-model:value="selectedCustomer" filterable placeholder="Khách lẻ (hoặc tìm khách hàng...)"
            :options="customers" clearable />
        </div>

        <div class="flex-1 bg-gray-50 rounded border p-2 overflow-y-auto mb-3 max-h-[40vh]">
          <div v-if="cart.length === 0" class="text-center text-gray-400 mt-10">
            Chưa có sản phẩm nào
          </div>
          <div v-else class="flex flex-col gap-2">
            <div v-for="(item, idx) in cart" :key="idx"
              class="bg-white p-2 rounded shadow-sm flex justify-between items-center">
              <div class="flex-1">
                <div class="font-medium text-sm">{{ item.name }}</div>
                <div class="text-xs text-gray-500">
                  {{ item.price.toLocaleString() }} x
                  <span class="font-bold text-black">{{ item.quantity }}</span>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <div class="font-bold text-red-600 text-sm">
                  {{ (item.price * item.quantity).toLocaleString() }}
                </div>
                <div class="text-gray-400 hover:text-red-500 cursor-pointer p-1" @click="removeFromCart(idx)">
                  ✖
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="border-t pt-3 space-y-3">
          <div class="flex justify-between items-center text-lg">
            <span class="font-bold">Tổng tiền:</span>
            <span class="font-bold text-red-600 text-xl">{{ totalAmount.toLocaleString() }} đ</span>
          </div>

          <div class="grid grid-cols-3 gap-2">
            <NButton size="small" :type="payment.method === 0 ? 'primary' : 'default'"
              @click="onPaymentMethodChange(0)">
              Tiền mặt
            </NButton>
            <NButton size="small" :type="payment.method === 1 ? 'primary' : 'default'"
              @click="onPaymentMethodChange(1)">
              Chuyển khoản
            </NButton>
            <NButton size="small" :type="payment.method === 2 ? 'primary' : 'default'"
              @click="onPaymentMethodChange(2)">
              Kết hợp
            </NButton>
          </div>

          <div class="space-y-2">
            <div v-if="payment.method !== 1" class="flex items-center gap-2">
              <span class="w-20 text-xs">Tiền mặt:</span>
              <NInputNumber v-model:value="payment.cash" class="flex-1" :show-button="false"
                :parse="(x) => x.replace(/,/g, '')" :format="(x) => x.toLocaleString()" />
            </div>
            <div v-if="payment.method !== 0" class="flex items-center gap-2">
              <span class="w-20 text-xs">CK:</span>
              <NInputNumber v-model:value="payment.transfer" class="flex-1" :show-button="false"
                :parse="(x) => x.replace(/,/g, '')" :format="(x) => x.toLocaleString()" />
            </div>
          </div>

          <NInput v-model:value="payment.note" type="textarea" placeholder="Ghi chú đơn hàng..." :rows="2"
            size="small" />

          <NButton type="primary" block class="h-12 text-lg font-bold mt-2" :loading="processing"
            @click="handleCheckout">
            THANH TOÁN (F9)
          </NButton>
        </div>
      </NCard>
    </div>
  </div>
</template>
