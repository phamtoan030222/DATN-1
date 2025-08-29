<script setup lang="ts">
import { useAuthStore } from "@/store";
import Chart from "./components/chart.vue";

const { userInfo } = useAuthStore();
</script>

<template>
  <n-grid :x-gap="16" :y-gap="16" :cols="3" item-responsive responsive="screen">
    <n-gi span="3 m:2">
      <n-space vertical :size="16">
        <n-card style="--n-padding-left: 0">
          <Chart />
        </n-card>

        <n-grid
          :x-gap="16"
          :y-gap="16"
          :cols="4"
          item-responsive
          responsive="screen"
        >
          <n-gi span="2 l:1">
            <n-card>
              <n-thing>
                <template #avatar>
                  <n-el>
                    <n-icon-wrapper
                      :size="46"
                      color="var(--success-color)"
                      :border-radius="999"
                    >
                      <nova-icon :size="26" icon="icon-park-outline:user" />
                    </n-icon-wrapper>
                  </n-el>
                </template>
                <template #header>
                  <n-statistic label="Kết quả">
                    <n-number-animation show-separator :from="0" :to="12039" />
                  </n-statistic>
                </template>
              </n-thing>
            </n-card>
          </n-gi>
          <n-gi span="2 l:1">
            <n-card>
              <n-thing>
                <template #avatar>
                  <n-el>
                    <n-icon-wrapper
                      :size="46"
                      color="var(--success-color)"
                      :border-radius="999"
                    >
                      <nova-icon
                        :size="26"
                        icon="icon-park-outline:every-user"
                      />
                    </n-icon-wrapper>
                  </n-el>
                </template>
                <template #header>
                  <n-statistic label="Sự thật">
                    <n-number-animation show-separator :from="0" :to="44039" />
                  </n-statistic>
                </template>
              </n-thing>
            </n-card>
          </n-gi>
          <n-gi span="2 l:1">
            <n-card>
              <n-thing>
                <template #avatar>
                  <n-el>
                    <n-icon-wrapper
                      :size="46"
                      color="var(--success-color)"
                      :border-radius="999"
                    >
                      <nova-icon
                        :size="26"
                        icon="icon-park-outline:preview-open"
                      />
                    </n-icon-wrapper>
                  </n-el>
                </template>
                <template #header>
                  <n-statistic label="Giá vàng">
                    <n-number-animation show-separator :from="0" :to="551039" />
                  </n-statistic>
                </template>
              </n-thing>
            </n-card>
          </n-gi>
          <n-gi span="2 l:1">
            <n-card>
              <n-thing>
                <template #avatar>
                  <n-el>
                    <n-icon-wrapper
                      :size="46"
                      color="var(--success-color)"
                      :border-radius="999"
                    >
                      <nova-icon :size="26" icon="icon-park-outline:star" />
                    </n-icon-wrapper>
                  </n-el>
                </template>
                <template #header>
                  <n-statistic label="Tổng cộng">
                    <n-number-animation show-separator :from="0" :to="7739" />
                  </n-statistic>
                </template>
              </n-thing>
            </n-card>
          </n-gi>
        </n-grid>
      </n-space>
    </n-gi>

    <n-gi span="3 m:1"> </n-gi>
  </n-grid>
</template>

<style scoped></style>
