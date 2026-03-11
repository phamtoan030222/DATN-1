<script setup lang="ts">
import { useRouter } from 'vue-router'
import { NButton, NResult } from 'naive-ui'

const router = useRouter()

const route = useRoute()

const maHoaDon = computed(() => route.query['ma-hoa-don'] || '')

function goHome() {
  router.push('/')
}

function continueShopping() {
  router.push('/san-pham')
}
</script>

<template>
  <div class="result-container">
    <div class="content-box">
      <NResult
        status="success"
        title="Đặt Hàng Thành Công!"
        description="Cảm ơn bạn đã tin tưởng và mua sắm.
        "
        size="huge"
      >
        <template #default>
          <div class="text-left">
            <p>Mã hóa đơn của bạn: <strong>{{ maHoaDon }}</strong></p>
            <p>Thông tin đơn hàng đã được gửi đến email của bạn.</p>
            <p>Vui lòng giữ mã hóa đơn để theo dõi trạng thái đơn hàng.</p>
          </div>
        </template>
        <template #footer>
          <div class="btn-group">
            <NButton size="large" @click="goHome">
              Về Trang Chủ
            </NButton>
            <NButton type="primary" color="#00D752" size="large" @click="continueShopping">
              Tiếp Tục Mua Sắm
            </NButton>
          </div>
        </template>
      </NResult>
    </div>
  </div>
</template>

<style scoped>
.result-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  /* background-color: #f9fafb; */
  padding: 20px;
}

.content-box {
  background: white;
  padding: 50px;
  border-radius: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  text-align: center;
  max-width: 600px;
  width: 100%;
}

.btn-group {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}
</style>
