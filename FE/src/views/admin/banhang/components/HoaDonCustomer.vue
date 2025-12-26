<template>
  <n-card title="Khách hàng">
    <!-- Header extra: nút chọn + thêm khách hàng -->
    <template #header-extra>
      <n-space>
        <n-button type="info" size="small" @click="chonKhachHang">
          Chọn KH
        </n-button>
        <n-button type="success" size="small" @click="themKhachHang">
          Thêm KH
        </n-button>
      </n-space>
    </template>

    <!-- Nội dung: form khách hàng -->
    <n-input placeholder="Tên KH" />
    <n-input placeholder="SĐT" class="mt-2" />
  </n-card>
</template>

<script setup lang="ts">
function chonKhachHang() {
  // logic chọn khách hàng
  console.log('Chọn khách hàng')
}

function themKhachHang() {
  // logic thêm khách hàng mới
  console.log('Thêm khách hàng')
}
</script>
