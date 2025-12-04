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
                <n-flex>
                    <n-input style="flex: 1;" placeholder="Nhập giá trị imei" v-model:value="imei">
                    </n-input>
                    <n-button @click="clickAddIMEIHandler" type="primary">
                        Thêm
                    </n-button>

                </n-flex>
                <n-space class="mt-20px">
                    <n-button @click="downloadIMEITemplate">Tải template IMEI</n-button>
                    <n-upload :custom-request="handleUploadImportExcel" accept=".xls,.xlsx" :show-file-list="false">
                        <n-button> Upload File </n-button>
                    </n-upload>
                </n-space>
                <n-data-table class="mt-20px" :columns="columns" :data="data">

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
import { checkIMEIExist, downloadTemplateImei, IMEIExcelResponse, importIMEIExcel } from '@/service/api/admin/product/productDetail.api';
import { Icon } from '@iconify/vue';
import { DataTableColumns, NButton, NTag, UploadCustomRequestOptions } from 'naive-ui';
import { Reactive, Ref } from 'vue';

const props = defineProps<{
    isOpen: boolean,
    index: number | undefined
}>()

const emit = defineEmits(['success', 'close', 'update:imei'])

const notification = useNotification()

const imei: Ref<string | undefined> = ref()

const handleClickCancel = () => {
    emit('close')
}

const resetField = () => {
    imei.value = undefined
}

const data: Reactive<IMEIExcelResponse[]> = reactive([])

const clickAddIMEIHandler = async () => {
    const imeis = imei.value?.split(/[,;|]/);
    const imeiExists = (await checkIMEIExist(imeis as string[])).data;

    imeis?.forEach(ele => data.push({imei: ele, isExist: imeiExists.some(imei => imei == ele)}))
}

const handleClickOK = () => {
    if(data.some(ele => ele.isExist)) {
        notification.error({content: 'Không thể thêm IMEI đã tồn tại', duration: 3000})
        return false;
    }

    const imeis = data.map(ele => ele.imei)
    resetField()
    data.splice(0, data.length)
    emit('update:imei', imeis, props.index)
}

const columns: DataTableColumns<IMEIExcelResponse> = [
    {
        title: '#', key: 'orderNumber', width: 50, align: 'center',
        render: (data, index) => h('span', { innerText: index + 1 })
    },
    {
        title: 'Giá trị', key: 'imei', width: 50, align: 'center',
    },
    {
        title: 'Trạng thái', key: 'status', width: 50, align: 'center',
        render: (data: IMEIExcelResponse) => h(NTag, { type: !data.isExist ? 'success' : 'error', innerText: !data.isExist ? 'Hợp lệ' : 'Đã tồn tại' })
    },
    {
        title: 'Thao tác', key: 'status', width: 50, align: 'center',
        render: (rowData, index) => h(NButton, { quaternary: true, onClick: () => { data.splice(index, 1); imei.value = data.map(element => element.imei).join(',') } },
            {
                default:() => h(Icon, { icon: 'tabler:trash' })
            }
        )
    },
]

const downloadIMEITemplate = async () => {
    try {
        const response = await downloadTemplateImei()
        const blob = new Blob([response.data], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        })
        const contentDisposition = response.headers['content-disposition']
        let filename = 'template-download'
        if (contentDisposition) {
            const match = contentDisposition.match(/filename="(.+)"/)
            if (match) {
                filename = match[1]
            }
        }
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
    } catch (error) {
        notification.error({ content: 'Lỗi khi tải file xuống', duration: 3000 })
    }
}

const handleUploadImportExcel = async ({ file }: UploadCustomRequestOptions) => {
    try {
        const response = await importIMEIExcel(file);

        data.splice(0, data.length, ...response.data);
    } catch (error) {
        notification.error({ content: 'Xảy ra lỗi khi import excel. Vui lòng thử lại !!!', duration: 3000})
    }
}
</script>

<style scoped>
.mt-20px {
    margin-top: 20px;
}
</style>