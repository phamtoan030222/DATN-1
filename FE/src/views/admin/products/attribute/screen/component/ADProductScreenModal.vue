<script setup lang="ts">
import TypeScreenResolution from '@/constants/TypeScreenResolution'
import type { ADProductScreenDetailResponse } from '@/service/api/admin/product/screen.api'
import { getScreenById, modifyScreen } from '@/service/api/admin/product/screen.api'
import type { Ref } from 'vue'
import { ref, watch } from 'vue'
import { Icon } from '@iconify/vue'

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  isDetail: boolean
}>()

const emit = defineEmits(['success', 'close'])

const detailScreen: Ref<ADProductScreenDetailResponse> = ref({
  id: '',
  code: '',
  name: '',
  physicalSize: 0,
  resolution: '',
  panelType: '',
  technology: '',
})

async function fetchDetailScreen() {
  const res = await getScreenById(props.id as string)

  detailScreen.value = res.data
  detailScreen.value.resolution = TypeScreenResolution[res.data.resolution as keyof typeof TypeScreenResolution]
}

function resetField() {
  detailScreen.value = {
    id: '',
    code: '',
    name: '',
    physicalSize: 0,
    resolution: '',
    panelType: '',
    technology: '',
  }
}

watch(
  () => props.id,
  (newId) => {
    if (newId)
      fetchDetailScreen()
    else resetField()
  },
)

function handleClickCancel() {
  emit('close')
}

const notification = useNotification()

async function handleClickOK() {
  try {
    const res = await modifyScreen(detailScreen.value)

    if (res.success)
      notification.success({ content: props.id ? 'Cập nhật màn hình thành công' : 'Thêm màn hình thành công', duration: 3000 })
    else notification.error({ content: props.id ? 'Cập nhật màn hình thất bại' : 'Thêm màn hình thất bại', duration: 3000 })
    emit('success')
  }
  catch (error) {
    console.log(error)
  }
}

const screenResolutionOptionsSelect = [
  { label: '1920 x 1080 pixels', value: '1920x1080' },
  { label: '1366 x 768 pixels', value: '1366x768' },
  { label: '2880 x 1800 pixels', value: '2880x1800' },
  { label: '3840 x 2160 pixels', value: '3840x2160' },
  { label: '2560 x 1440 pixels', value: '2560x1440' },
]

const panelTypeOptionsSelect = [
  { label: 'LCD', value: 'LCD' },
  { label: 'OLED', value: 'OLED' },
  { label: 'LED_BACKLIT_LCD', value: 'LED_BACKLIT_LCD' },
  { label: 'AMOLED', value: 'AMOLED' },
  { label: 'MINI_LED', value: 'MINI_LED' },
  { label: 'MICRO_LED', value: 'MICRO_LED' },
]

const screenSizeOptionsSelect = [
  { label: '11.6 inch', value: '11.6' },
  { label: '13.3 inch', value: '13.3' },
  { label: '14 inch', value: '14' },
  { label: '15.6 inch', value: '15.6' },
  { label: '16 inch', value: '16' },
  { label: '17.3 inch', value: '17.3' },
]
</script>

<template>
  <n-modal :show="isOpen">
    <n-card
      style="width: 600px" :title="id ? (isDetail ? 'Chi tiết GPU' : 'Cập nhật GPU') : 'Thêm GPU'"
      :bordered="false" size="huge" role="dialog" aria-modal="true"
    >
      <template #header-extra>
        <n-button @click="handleClickCancel">
          <Icon icon="ic:outline-close" />
        </n-button>
      </template>

      <!-- content -->
      <div :style="{ maxHeight: '400px', overflowY: 'auto' }">
        <n-form>
          <n-form-item label="Mã">
            <n-input v-model:value="detailScreen.code" placeholder="Nhập mã" />
          </n-form-item>
          <n-form-item label="Tên">
            <n-input v-model:value="detailScreen.name" placeholder="Nhập tên" />
          </n-form-item>
          <n-form-item label="Tấm nền">
            <n-select
              v-model:value="detailScreen.panelType" :options="panelTypeOptionsSelect"
              placeholder="Chọn tấm nền"
            />
          </n-form-item>
          <n-form-item label="Độ phân giải">
            <n-select
              v-model:value="detailScreen.resolution" :options="screenResolutionOptionsSelect"
              placeholder="Chọn kích thước màn hình"
            />
          </n-form-item>
          <n-form-item label="Kích thước màn hình">
            <n-select
              v-model:value="detailScreen.physicalSize" :options="screenSizeOptionsSelect"
              placeholder="Chọn kích thước màn hình"
            />
          </n-form-item>
          <n-form-item label="Công nghệ">
            <n-input v-model:value="detailScreen.technology" placeholder="Nhập công nghệ" />
          </n-form-item>
        </n-form>
      </div>

      <!-- footer -->
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleClickCancel">
            Hủy
          </n-button>
          <n-popconfirm
            :positive-button-props="{ type: 'info' }" @positive-click="handleClickOK"
            @negative-click="handleClickCancel"
          >
            <template #trigger>
              <n-button>Xác nhận</n-button>
            </template>
            Bạn chắc chắn muốn thao tác
          </n-popconfirm>
        </n-space>
      </template>
    </n-card>
  </n-modal>
</template>

<style scoped>
.container {
    max-height: 400px;
    overflow-y: auto;
}
</style>
