<template>
    <div class="unauthorized">
        <svg class="icon" viewBox="0 0 64 64" aria-hidden="true" focusable="false">
            <rect x="12" y="28" width="40" height="24" rx="3" fill="none" stroke="currentColor" stroke-width="3" />
            <path d="M20 28v-6a12 12 0 1 1 24 0v6" fill="none" stroke="currentColor" stroke-width="3" />
        </svg>

        <div class="code">401</div>
        <p class="desc">Bạn chưa được cấp quyền truy cập trang này.</p>

        <div class="actions">
            <button class="btn btn-primary" @click="goBack">Quay lại</button>
            <router-link to="/" class="btn btn-primary">Về trang chủ</router-link>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router';
const router = useRouter();
function goBack() {
    router.back();
}
</script>

<style scoped>
:root {
    --primary: #36ad6a;
}

.unauthorized {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, rgba(54, 173, 106, 0.06), #ffffff);
}

/* .card {

  background: #fff;
  border-radius: 12px;
  padding: 3rem;
  box-shadow: 0 8px 30px rgba(54,173,106,0.08);
  text-align: center;
} */

.icon {
    width: 64px;
    height: 64px;
    margin: 0 auto 0.5rem;
    color: var(--primary);
    opacity: 0.95;
}

.code {
    font-size: clamp(48px, 10vw, 96px);
    font-weight: 800;
    color: var(--primary);
    line-height: 1;
}

.title {
    margin-top: 0.25rem;
    font-size: 1.5rem;
    color: #222;
    font-weight: 600;
}

.desc {
    margin-top: 0.5rem;
    color: #555;
}

.actions {
    margin-top: 1.5rem;
    display: flex;
    gap: 0.75rem;
    justify-content: center;
    align-items: center;
    flex-wrap: wrap;
}

.btn {
    padding: 0.6rem 1rem;
    border-radius: 8px;
    border: none;
    cursor: pointer;
    font-weight: 600;
}

.btn-primary {
    background: var(--primary);
    box-shadow: 0 10px 28px rgba(54, 173, 106, 0.16);
    transition: transform .12s ease, box-shadow .12s ease;
}

.btn-primary:hover {
    transform: translateY(-3px);
    box-shadow: 0 14px 36px rgba(54, 173, 106, 0.22);
}

.link {
    color: var(--primary);
    text-decoration: none;
    font-weight: 600;
    padding: 0.5rem 0.75rem;
    border-radius: 8px;
}

@media (max-width: 480px) {
    .card {
        padding: 2rem;
    }

    .code {
        font-size: 64px;
    }
}
</style>