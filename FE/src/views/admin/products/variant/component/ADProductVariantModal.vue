<script setup lang="ts">
import type { ADPDImeiResponse, ADProductDetailDetailResponse, ADPRPropertiesComboboxResponse } from '@/service/api/admin/product/productDetail.api'
import { changeStatusImei, getImeiProductDetail, getProductDetailById, updateProductDetail } from '@/service/api/admin/product/productDetail.api'
import { Icon } from '@iconify/vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NSpace, NSwitch } from 'naive-ui'
import type { Ref } from 'vue'
import { ref, watch } from 'vue'

const props = defineProps<{
  isOpen: boolean
  id: string | undefined
  cpus: ADPRPropertiesComboboxResponse[]
  gpus: ADPRPropertiesComboboxResponse[]
  materials: ADPRPropertiesComboboxResponse[]
  hardDrives: ADPRPropertiesComboboxResponse[]
  rams: ADPRPropertiesComboboxResponse[]
  colors: ADPRPropertiesComboboxResponse[]
}>()

const emit = defineEmits(['success', 'close'])

const detailProduct: Ref<Partial<ADProductDetailDetailResponse>> = ref({
  id: undefined,
  code: undefined,
  name: undefined,
  idCPU: undefined,
  idGPU: undefined,
  idColor: undefined,
  idRAM: undefined,
  idHardDrive: undefined,
  idMaterial: undefined,
  price: 0,
})

async function fetchDetailProduct() {
  const res = await getProductDetailById(props.id as string)

  detailProduct.value = res.data
}

const imeisProductDetail: Ref<ADPDImeiResponse[]> = ref([])

async function getImeisProductDetail() {
  const res = await getImeiProductDetail(props.id as string)

  imeisProductDetail.value = res.data as ADPDImeiResponse[]
}

function resetField() {
  detailProduct.value = {
    id: undefined,
    code: undefined,
    name: undefined,
    idCPU: undefined,
    idGPU: undefined,
    idColor: undefined,
    idRAM: undefined,
    idHardDrive: undefined,
    idMaterial: undefined,
    price: 0,
  }
}

watch(
  () => props.id,
  (newId) => {
    if (newId) {
      fetchDetailProduct()
      getImeisProductDetail()
    }
    else {
      resetField()
    }
  },
)

function handleClickCancel() {
  emit('close')
}

const notification = useNotification()

async function handleClickOK() {
  const res = await updateProductDetail({
    ...detailProduct.value as ADProductDetailDetailResponse,
    imei: [],
  })

  if (res.success)
    notification.success({ content: props.id ? 'Cập nhật sản phẩm thành công' : 'Thêm sản phẩm thành công', duration: 3000 })
  else notification.error({ content: props.id ? 'Cập nhật sản phẩm thất bại' : 'Thêm sản phẩm thất bại', duration: 3000 })
  if (!props.id)
    resetField()
  emit('success')
}

const columnsImei: DataTableColumns<ADPDImeiResponse> = [
  { type: 'selection', fixed: 'left' },
  {
    title: '#',
    key: 'orderNumber',
    width: 50,
    fixed: 'left',
    render: (_, index: number) => {
      return h('span', { innerText: index + 1 })
    },
  },
  { title: 'Mã', key: 'code', width: 100, fixed: 'left' },
  { title: 'Tên', key: 'name', width: 100 },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 70,
    align: 'center',
    render: (data: ADPDImeiResponse) => h(NSwitch, { value: data.status == 'ACTIVE', onUpdateValue: (_) => { handleChangeStatusImei(data.id as string) } }),
  },
]

async function handleChangeStatusImei(idImei: string) {
  const res = await changeStatusImei(idImei)
  if (res.success)
    notification.success({ content: 'Thay đổi trạng thái IMEI thành công', duration: 3000 })
  else notification.error({ content: 'Thay đổi trạng thái IMEI thất bại', duration: 3000 })

  getImeisProductDetail()
}
</script>

<template>
  <n-modal :show="isOpen">
    <n-card
      style="width: 50%" title="Cập nhật sản phẩm chi tiết" :bordered="false" size="huge" role="dialog"
      aria-modal="true"
    >
    <template #header-extra>
        <NButton @click="handleClickCancel">
          <Icon icon="ic:outline-close" />
        </NButton>
      </template>

      <!-- content -->
      <div :style="{ overflowY: 'auto', maxHeight: '500px' }">
        <n-form>
          <n-grid :span="24" :x-gap="24">
            <n-form-item-gi :span="12" label="Mã">
              <n-input v-model:value="detailProduct.code" disabled placeholder="Nhập mã" />
            </n-form-item-gi>
            <n-form-item-gi :span="12" label="Tên">
              <n-input v-model:value="detailProduct.name" disabled placeholder="Nhập tên" />
            </n-form-item-gi>
            <n-form-item-gi :span="8" label="CPU">
              <n-select
                v-model:value="detailProduct.idCPU" :options="cpus"
                placeholder="Chọn tấm nền"
              />
            </n-form-item-gi>
            <n-form-item-gi :span="8" label="GPU">
              <n-select
                v-model:value="detailProduct.idGPU" :options="gpus"
                placeholder="Chọn kích thước màn hình"
              />
            </n-form-item-gi>
            <n-form-item-gi :span="8" label="RAM">
              <n-select
                v-model:value="detailProduct.idRAM" :options="rams"
                placeholder="Chọn kích thước màn hình"
              />
            </n-form-item-gi>
            <n-form-item-gi :span="8" label="Chất liệu">
              <n-select
                v-model:value="detailProduct.idMaterial" :options="materials"
                placeholder="Chọn kích thước màn hình"
              />
            </n-form-item-gi>
            <n-form-item-gi :span="8" label="Màu sắc">
              <n-select
                v-model:value="detailProduct.idColor" :options="colors"
                placeholder="Chọn kích thước màn hình"
              />
            </n-form-item-gi>
            <n-form-item-gi :span="8" label="Ổ cứng">
              <n-select
                v-model:value="detailProduct.idHardDrive" :options="hardDrives"
                placeholder="Chọn kích thước màn hình"
              />
            </n-form-item-gi>
            <n-form-item-gi :span="24" label="Giá sản phẩm">
              <n-input-number v-model:value="detailProduct.price" style="width: 100%;" placeholder="Nhập giá" />
            </n-form-item-gi>
          </n-grid>
        </n-form>

        <span>Danh sách IMEI</span>
        <n-data-table v-show="imeisProductDetail && imeisProductDetail.length > 0" :columns="columnsImei" :data="imeisProductDetail" />
      </div>

      <!-- footer -->
      <template #footer>
        <NSpace justify="end">
          <NButton @click="handleClickCancel">
            Hủy
          </NButton>
          <n-popconfirm
            :positive-button-props="{ type: 'info' }" @positive-click="handleClickOK"
            @negative-click="handleClickCancel"
            :positive-text="'Xác nhận'" :negative-text="'Hủy'"
          >
            <template #trigger>
              <NButton type="success">
                Xác nhận
              </NButton>
            </template>
            Bạn chắc chắn muốn thao tác
          </n-popconfirm>
        </NSpace>
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
