<template>
    <n-card title="Giỏ hàng">
  <!-- Header extra: nút thêm sản phẩm + nút quét QR -->
  <template #header-extra>
    <n-space>
      <n-button type="info" size="small" @click="themSanPham">
        Thêm sản phẩm
      </n-button>
      <n-button type="success" size="small" @click="quetQR">
        Quét QR
      </n-button>
    </n-space>
  </template>

  <!-- Nội dung: bảng giỏ hàng -->
  <n-data-table
    :columns="columns"
    :data="cart"
    size="small"
  />
</n-card>

</template>