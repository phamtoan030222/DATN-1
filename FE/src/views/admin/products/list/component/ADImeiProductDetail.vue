<template>
    <n-modal :show="isOpen">
        <n-card style="width: 600px" title="Quản lý IMEI biến thể sản phẩm" :bordered="false" size="huge" role="dialog"
            aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>

            <!-- content -->
            <div :style="{ maxHeight: '400px', overflowY: 'auto' }">
                <div>
                    <span>IMEI</span>
                    <n-input placeholder="Nhập giá trị imei" v-model:value="imei">
                    </n-input>
                    <n-button class="mt-20px" @click="clickAddIMEIHandler" type="primary">
                        Thêm
                    </n-button>

                </div>
                <n-data-table class="mt-20px" :columns="columns" :data="data" >

                </n-data-table>
            </div>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                        :positive-button-props="{ type: 'info' }">
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

<script setup lang="ts">
import { isIMEIExists } from '@/service/api/admin/product/productDetail.api';
import { Icon } from '@iconify/vue';
import { DataTableColumns, NButton } from 'naive-ui';
import { Reactive, Ref } from 'vue';

const props = defineProps<{
    isOpen: boolean,
    index: number | undefined
}>()

const emit = defineEmits(['success', 'close', 'update:imei'])

const imei: Ref<string | undefined> = ref()

const handleClickCancel = () => {
    emit('close')
}

const resetField = () => {
    imei.value = undefined
}

type DataIMEI = {imei: string, status?: boolean}

const data: Reactive<DataIMEI[]> = reactive([])

const clickAddIMEIHandler = () => {
    imei.value?.split(/[,;|]/).forEach(element => data.push({imei: element}))

    fetchIMEIExist()
}

const handleClickOK = () => {
    const imeis = data.map(ele => ele.imei)
    resetField()
    data.splice(0, data.length)
    emit('update:imei', imeis, props.index)
}

const columns: DataTableColumns<DataIMEI> = [
    {
        title: '#', key: 'orderNumber', width: 50, align: 'center',
        render: (data, index) => h('span', { innerText: index + 1 })
    },
    {
        title: 'Giá trị', key: 'imei', width: 50, align: 'center',
    },
    // {
    //     title: 'Trạng thái', key: 'status', width: 50, align: 'center',
    //     render: (data: DataIMEI) => h(NTag, {type: data.status ? 'success' : 'error', innerText: data.status ? 'Hợp lệ' : 'Đã tồn tại'})
    // },
    {
        title: 'Thao tác', key: 'status', width: 50, align: 'center',
        render: (rowData, index) => h(NButton, {quaternary: true, onClick: () => {data.splice(index, 1); imei.value = data.map(element => element.imei).join(',')}}, h(Icon, {icon: 'tabler:trash'}))
    },
]

const fetchIMEIExist = async () => {
    const res = await isIMEIExists(data.map(element => element.imei))
    console.log(res.data)
    const set = new Set(res.data)
    console.log(set)

    data.map(element => ({imei: element, status: !(set.has(element.imei))}))
}
</script>

<style scoped>
.mt-20px {
    margin-top: 20px;
}
</style>