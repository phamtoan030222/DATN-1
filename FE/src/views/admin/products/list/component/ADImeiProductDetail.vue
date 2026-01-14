<template>
    <n-modal :show="isOpen">
        <n-card style="width: 800px; height: 600px" title="Quản lý IMEI biến thể sản phẩm" :bordered="false" size="huge" role="dialog"
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
                </n-flex>
                <n-space class="mt-20px">
                    <n-button @click="downloadIMEITemplate">Tải template IMEI</n-button>
                    <n-upload :custom-request="handleUploadImportExcel" accept=".xls,.xlsx" :show-file-list="false">
                        <n-button> Upload File </n-button>
                    </n-upload>
                    <n-button @click="() => isScanQrCode = !isScanQrCode">Quét QR Code</n-button>
                </n-space>
                <div v-if="isScanQrCode" class="qrcode-container qr-card">
                    <h2 class="qr-title">Scan QR Code</h2>
                    <div class="qr-box">
                        <QrcodeStream :constraints="{ facingMode: 'environment' }" @detect="onDetect" />
                    </div>
                </div>

                <n-data-table :loading="isLoadingTable" class="mt-20px" :columns="columns" max-height="220px" :data="data">

                </n-data-table>
            </div>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                        :positive-text="'Xác nhận'" :negative-text="'Hủy'"
                        
                        >
                        <template #trigger>
                            <n-button type="success">Xác nhận</n-button>
                        </template>
                        Bạn chắc chắn muốn thao tác
                    </n-popconfirm>
                </n-space>
            </template>
        </n-card>
    </n-modal>
</template>

<script setup lang="ts">
import { Regex } from '@/constants';
import { checkIMEIExist, downloadTemplateImei, importIMEIExcel } from '@/service/api/admin/product/productDetail.api';
import { Icon } from '@iconify/vue';
import _ from 'lodash';
import { DataTableColumns, NButton, NTag, UploadCustomRequestOptions } from 'naive-ui';
import { Reactive, Ref } from 'vue';
import { QrcodeStream } from 'vue-qrcode-reader';

const props = defineProps<{
    isOpen: boolean,
    index: number | undefined,
    idColorImei: string | undefined,
}>()

const emit = defineEmits(['success', 'close', 'update:imei'])

const notification = useNotification()

const imei: Ref<string | undefined> = ref()
const isLoadingTable: Ref<boolean> = ref(false)

const handleClickCancel = () => {
    emit('close')
    data.splice(0, data.length)
}

const resetField = () => {
    imei.value = undefined
}

type IMEITableType = {
    imei: string,
    isValid: boolean,
    // note: string | undefined,
}

const data: Reactive<IMEITableType[]> = reactive([])

const clickAddIMEIHandler = async () => {
    if (!imei.value) {
        return
    }

    const imeis = imei.value?.split(/[,;|]/)
                    .map(imei => imei.trim())
                    .filter(imei => imei);

    const imeiExists = (await checkIMEIExist(imeis as string[])).data;

    const serialNumberDuplicate = new Map<string, number>();

    imeis.forEach(ele => {
        if (serialNumberDuplicate.has(ele)) {
            serialNumberDuplicate.set(ele, (serialNumberDuplicate.get(ele) as number) + 1);
        } else {
            serialNumberDuplicate.set(ele, 1);
        }
    });

    imeis?.forEach(ele => {
        let isValid = true;
        let note = undefined;

        if (!validateSerialNumber(ele)) {
            isValid = false;
            // note = 'Số serial không hợp lệ! Số serial chỉ bao gồm chữ hoa, số và dấu gạch ngang, độ dài từ 5-30 ký tự';
        }

        if (imeiExists.includes(ele)) {
            isValid = false;
            // note = 'Số serial đã tồn tại';
        }

        if (serialNumberDuplicate.get(ele) && serialNumberDuplicate.get(ele)! > 1) {
            isValid = false;
            // note = 'Số serial bị trùng lặp trong danh sách nhập';
        }

        if (isValid) data.push({
            imei: ele,
            isValid: isValid,
            // note: note,
        })
    })
    resetField()
}

const debouncedClickAddIMEIHandler = _.debounce(clickAddIMEIHandler, 300);

const handleClickOK = () => {
    if (data.some(ele => !ele.isValid)) {
        notification.error({ content: 'Không thể thêm IMEI không hợp lệ', duration: 3000 })
        return false;
    }

    const imeis = data.map(ele => ele.imei)
    resetField()
    data.splice(0, data.length)
    emit('update:imei', imeis, props.idColorImei, props.index)
}

const columns: DataTableColumns<IMEITableType> = [
    {
        title: '#', key: 'orderNumber', width: 30, align: 'left',
        render: (_, index) => h('span', { innerText: index + 1 })
    },
    {
        title: 'Giá trị', key: 'imei', width: 100, align: 'center',
    },
    {
        title: 'Trạng thái', key: 'isValid', width: 50, align: 'center',
        render: (data: IMEITableType) => h(NTag, { type: data.isValid ? 'success' : 'error', innerText: data.isValid ? 'Hợp lệ' : 'Không hợp lệ' })
    },
    // {
    //     title: 'Ghi chú', key: 'note', width: 150, align: 'center',
    //     render: (data: IMEITableType) => h('span', { innerText: data.note })
    // },
    {
        title: 'Thao tác', key: 'status', width: 50, align: 'center',
        render: (_, index) => h(NButton, { quaternary: true, onClick: () => { 
            data.splice(index, 1)
            imei.value = data.map(ele => ele.imei).join(', ')
         } },
            {
                default: () => h(Icon, { icon: 'tabler:trash' })
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

        data.splice(0, data.length, ...response.data.map(ele => ({
            imei: ele.imei,
            isValid: !ele.isExist,
            note: ele.isExist ? 'Đã tồn tại' : undefined,
        })));
    } catch (error) {
        notification.error({ content: 'Xảy ra lỗi khi import excel. Vui lòng thử lại !!!', duration: 3000 })
    }
}

const validateSerialNumber = (serialNumber: string): boolean => {
    const regex = new RegExp(Regex.SerialNumber);
    return regex.test(serialNumber);
}

const isScanQrCode = ref<boolean>(false);

function onDetect(detectedCodes: any) {
    console.log(detectedCodes)
    imei.value = detectedCodes.map((code: any) => code.rawValue)[0];
    clickAddIMEIHandler();
}

watch(imei, async () => {
    if (imei.value) {
        data.splice(0, data.length);
        isLoadingTable.value = true;
        await debouncedClickAddIMEIHandler();
        isLoadingTable.value = false;
    }
});
</script>

<style scoped>
.mt-20px {
    margin-top: 20px;
}

.qr-card {
    background: #ffffff;
    border-radius: 20px;
    padding: 28px 32px;
    width: 300px;
    height: 300px;
    text-align: center;
    box-shadow: 0 20px 40px rgba(0, 128, 0, 0.15);
    border: 2px solid #a5d6a7;
    margin: 16px auto;
}

/* Title */
.qr-title {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: #2e7d32;
}

.qr-box {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    border-radius: 16px;
    background: linear-gradient(145deg, #e8f5e9, #c8e6c9);
    border: 2px dashed #66bb6a;
    box-shadow: inset 0 0 12px rgba(76, 175, 80, 0.3);
    margin: 12px 0;
}
</style>