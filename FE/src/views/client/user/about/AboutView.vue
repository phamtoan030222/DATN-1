<script setup>
import {
  NButton,
  NCard,
  NDivider,
  NGi,
  NGrid,
  NIcon,
  NImage,
  NLayoutContent,
  NSpace,
  NStatistic,
} from 'naive-ui'
import {
  ShieldCheckmarkOutline as QualityIcon,
  PeopleOutline as ServiceIcon,
  StorefrontOutline as StoreIcon,
  BuildOutline as WarrantyIcon,
} from '@vicons/ionicons5' // Đảm bảo bạn đã cài @vicons/ionicons5

// Logic xử lý nếu cần (ví dụ: fetch dữ liệu dynamic)
</script>

<template>
  <div class="about-container">
    <div class="hero-section">
      <NGrid x-gap="12" :cols="1" class="hero-content">
        <NGi>
          <h1 class="hero-title">
            MY LAPTOP
          </h1>
          <p class="hero-subtitle">
            Nâng tầm trải nghiệm công nghệ - Đồng hành cùng đam mê của bạn.
          </p>
          <NButton type="primary" size="large" round class="cta-button">
            Khám Phá Sản Phẩm Ngay
          </NButton>
        </NGi>
      </NGrid>
    </div>

    <NLayoutContent content-style="padding: 40px 10%; background-color: #f7f9fc;">
      <NGrid x-gap="24" y-gap="24" cols="1 s:1 m:2" responsive="screen" item-responsive>
        <NGi>
          <NCard :bordered="false" class="story-card">
            <h2 class="section-title">
              <NIcon size="30" color="#2080f0" :component="StoreIcon" style="vertical-align: middle; margin-right: 10px;" />
              Về Chúng Tôi
            </h2>
            <p class="text-content">
              Được thành lập với niềm đam mê công nghệ cháy bỏng, <strong>MY LAPTOP</strong> không chỉ là nơi bán máy tính, mà là nơi cung cấp các giải pháp tối ưu cho công việc và học tập.
            </p>
            <p class="text-content">
              Chúng tôi hiểu rằng mỗi chiếc Laptop là một người bạn đồng hành quan trọng. Dù bạn là sinh viên, dân văn phòng hay lập trình viên chuyên nghiệp, chúng tôi luôn có những lựa chọn phù hợp nhất với cấu hình mạnh mẽ và giá thành hợp lý.
            </p>
          </NCard>
        </NGi>
        <NGi>
          <NImage
            width="100%"
            src="https://images.unsplash.com/photo-1531297425971-ec1533791a7e?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80"
            preview-disabled
            object-fit="cover"
            style="border-radius: 12px; height: 100%; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"
          />
        </NGi>
      </NGrid>

      <NDivider />

      <div class="values-section">
        <h2 class="section-title center-text">
          Tại Sao Chọn MY LAPTOP?
        </h2>
        <NGrid x-gap="24" y-gap="24" cols="1 s:2 m:3" responsive="screen">
          <NGi>
            <NCard hoverable class="value-card">
              <template #header>
                <NSpace justify="center">
                  <NIcon size="48" color="#18a058" :component="QualityIcon" />
                </NSpace>
              </template>
              <h3 class="card-heading">
                Chất Lượng Đỉnh Cao
              </h3>
              <p>Cam kết 100% sản phẩm chính hãng, nguyên seal, kiểm tra nghiêm ngặt trước khi đến tay khách hàng.</p>
            </NCard>
          </NGi>
          <NGi>
            <NCard hoverable class="value-card">
              <template #header>
                <NSpace justify="center">
                  <NIcon size="48" color="#f0a020" :component="ServiceIcon" />
                </NSpace>
              </template>
              <h3 class="card-heading">
                Dịch Vụ Tận Tâm
              </h3>
              <p>Đội ngũ tư vấn am hiểu kỹ thuật, hỗ trợ cài đặt phần mềm và vệ sinh máy trọn đời miễn phí.</p>
            </NCard>
          </NGi>
          <NGi>
            <NCard hoverable class="value-card">
              <template #header>
                <NSpace justify="center">
                  <NIcon size="48" color="#d03050" :component="WarrantyIcon" />
                </NSpace>
              </template>
              <h3 class="card-heading">
                Bảo Hành Uy Tín
              </h3>
              <p>Chính sách đổi trả trong 30 ngày, bảo hành siêu tốc, hỗ trợ mượn máy trong thời gian sửa chữa.</p>
            </NCard>
          </NGi>
        </NGrid>
      </div>

      <NDivider />

      <div class="stats-section">
        <NGrid x-gap="24" cols="2 s:4" responsive="screen">
          <NGi>
            <NStatistic label="Năm Kinh Nghiệm" :value="5">
              <template #suffix>
                +
              </template>
            </NStatistic>
          </NGi>
          <NGi>
            <NStatistic label="Sản Phẩm Đã Bán" :value="12000">
              <template #suffix>
                +
              </template>
            </NStatistic>
          </NGi>
          <NGi>
            <NStatistic label="Khách Hàng Hài Lòng" :value="98">
              <template #suffix>
                %
              </template>
            </NStatistic>
          </NGi>
          <NGi>
            <NStatistic label="Chi Nhánh" :value="3" />
          </NGi>
        </NGrid>
      </div>
    </NLayoutContent>
  </div>
</template>

<style scoped>
.about-container {
  font-family: 'Inter', sans-serif;
}

/* Hero Section Styling */
.hero-section {
  background: linear-gradient(135deg, #549de6 0%, #74a6d4 100%); /* Màu xanh chuyên nghiệp */
  color: white;
  padding: 80px 20px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero-title {
color: green;
  font-size: 3.5rem;
  font-weight: 800;
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
  margin-bottom: 30px;
}

.cta-button {
  background-color: #fff;
  color: #0055a5;
  font-weight: bold;
}

/* Content Styling */
.section-title {
  font-size: 1.8rem;
  color: #333;
  margin-bottom: 20px;
  font-weight: 700;
}

.center-text {
  text-align: center;
  margin-bottom: 40px;
}

.text-content {
  font-size: 1.1rem;
  line-height: 1.6;
  color: #555;
  margin-bottom: 15px;
  text-align: justify;
}

.story-card {
  background: transparent;
}

.values-section {
  margin: 40px 0;
}

.value-card {
  text-align: center;
  height: 100%;
  transition: transform 0.3s ease;
}

.value-card:hover {
  transform: translateY(-5px);
  border-color: #2080f0;
}

.card-heading {
  margin-top: 10px;
  font-size: 1.2rem;
  color: #2c3e50;
}

.stats-section {
  background: #fff;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  text-align: center;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2.5rem;
  }
}
</style>
