<script setup lang="ts">
import { NGrid, NGridItem } from 'naive-ui'
</script>

<template>
  <footer class="main-footer">
    <div class="container">
      <NGrid x-gap="24" y-gap="24" cols="1 s:2 m:4" responsive="screen">
        <NGridItem>
          <h4 class="footer-heading">
            VỀ CHÚNG TÔI
          </h4>
          <p class="footer-text">
            My Laptop cung cấp các giải pháp công nghệ hiện đại, mang lại giá trị bền vững cho khách hàng sinh viên.
          </p>
          <img src="/favicon.svg" style="height: 40px; margin-top: 10px;">
        </NGridItem>

        <NGridItem>
          <h4 class="footer-heading">
            HỖ TRỢ KHÁCH HÀNG
          </h4>
          <ul class="footer-links">
            <li><a href="#">Hướng dẫn mua hàng</a></li>
            <li><a href="#">Chính sách bảo hành</a></li>
            <li><a href="#">Vận chuyển & Giao nhận</a></li>
            <li><a href="#">Hình thức thanh toán</a></li>
          </ul>
        </NGridItem>

        <NGridItem>
          <h4 class="footer-heading">
            LIÊN HỆ
          </h4>
          <ul class="footer-contact">
            <li>📍 Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</li>
            <li>📞 0965.237.19</li>
            <li>✉️ mylaptop4t@gmail.com</li>
          </ul>
        </NGridItem>

        <NGridItem>
          <h4 class="footer-heading">
            BẢN ĐỒ
          </h4>
          <div class="map-box">
            <div class="map-placeholder">
              <iframe
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3723.863980698379!2d105.74459841473156!3d21.03812779283556!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x313454b991d80fd5%3A0x53cefc99d6b0bf86!2zVHLGsOG7nW5nIENhbyDEkSZuZyBGUFQgUG9seXRlY2huaWM!5e0!3m2!1svi!2s!4v1647416738596!5m2!1svi!2s"
                width="100%" height="150" style="border:0;" allowfullscreen="" loading="lazy"
              />
            </div>
          </div>
        </NGridItem>
      </NGrid>

      <div class="copyright">
        © 2026 My Laptop - Redamancy Project. All rights reserved.
      </div>
    </div>
  </footer>
</template>

<style scoped>
.container { max-width: 1200px; margin: 0 auto; padding: 0 15px; width: 100%; }

.main-footer { background: #30822a; color: #ffffff; padding: 40px 0 20px; margin-top: auto; }

.footer-heading {
  color: #ffffff; font-weight: bold; border-bottom: 2px solid #049d14;
  display: inline-block; padding-bottom: 5px; margin-bottom: 15px;
}
.footer-text { font-size: 0.9rem; line-height: 1.6; }
.footer-links, .footer-contact { list-style: none; padding: 0; }
.footer-links li a { color: #ffffff; text-decoration: none; line-height: 2; transition: 0.2s; }
.footer-links li a:hover { color: #ffffff; font-size: 1rem; padding-left: 5px; }
.footer-contact li { margin-bottom: 10px; }

.map-placeholder {
  background: #eee; height: 150px; display: flex; align-items: center;
  justify-content: center; border-radius: 8px; overflow: hidden;
}

.copyright {
  text-align: center; border-top: 1px solid rgba(255,255,255,0.2);
  margin-top: 30px; padding-top: 15px; font-size: 0.85rem;
}
</style>
