<template>
    <div>
        <n-card title="Thông tin cơ bản">
            <n-space justify="center">
                <n-form>
                    
                </n-form>
            </n-space>
        </n-card>
    </div>
</template>

<script lang="ts" setup>
import {
    ADProductCreateUpdateRequest,
    ADPRPropertiesComboboxResponse,
    getBatteries,
    getBrands,
    getOperatingSystems,
    getScreens,
} from '@/service/api/admin/product/product.api'
import { getColors, getCPUs, getGPUs, getHardDrives, getMaterials, getRAMs } from '@/service/api/admin/product/productDetail.api'
import { onMounted, Reactive, reactive, ref, Ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const idProduct: Ref<string> = ref(route.params.id as string)

const dataProperties = reactive({
    basicInformation: {
        screens: [] as ADPRPropertiesComboboxResponse[],
        batteries: [] as ADPRPropertiesComboboxResponse[],
        operatingSystems: [] as ADPRPropertiesComboboxResponse[],
        brands: [] as ADPRPropertiesComboboxResponse[],
    },
    variantInformation: {
        materials: [] as ADPRPropertiesComboboxResponse[],
        colors: [] as ADPRPropertiesComboboxResponse[],
        rams: [] as ADPRPropertiesComboboxResponse[],
        hardDrives: [] as ADPRPropertiesComboboxResponse[],
        cpus: [] as ADPRPropertiesComboboxResponse[],
        gpus: [] as ADPRPropertiesComboboxResponse[],
    },
})

const fetchDataProperties = async () => {
    try {
        const [
            screenProperties,
            batteryProperties,
            operatingSystemProperty,
            brandProperties,
            colorProperties,
            cpuProperties,
            gpuProperties,
            ramProperties,
            hardDriveProperties,
            materialProperties,
        ] = await Promise.all([
            getScreens(),
            getBatteries(),
            getOperatingSystems(),
            getBrands(),
            getColors(),
            getCPUs(),
            getGPUs(),
            getRAMs(),
            getHardDrives(),
            getMaterials(),
        ])

        dataProperties.basicInformation.screens = screenProperties.data
        dataProperties.basicInformation.batteries = batteryProperties.data
        dataProperties.basicInformation.operatingSystems = operatingSystemProperty.data
        dataProperties.basicInformation.brands = brandProperties.data

        dataProperties.variantInformation.colors = colorProperties.data
        dataProperties.variantInformation.materials = materialProperties.data
        dataProperties.variantInformation.cpus = cpuProperties.data
        dataProperties.variantInformation.gpus = gpuProperties.data
        dataProperties.variantInformation.hardDrives = hardDriveProperties.data
        dataProperties.variantInformation.rams = ramProperties.data
    } catch (error) {
        console.log(error)
    }
}

const formDataBasic: Reactive<ADProductCreateUpdateRequest> = reactive({
    code: '',
    name: '',
    idBrand: '',
    idBattery: '',
    idScreen: '',
    idOperatingSystem: '',
})

const formDataVariant = reactive({
    idColor: undefined as undefined | [],
    idMaterial: '',
    idCpu: '',
    idGpu: '',
    idRam: '',
    idHardDrive: '',
})

onMounted(() => {
    fetchDataProperties()
})

const file = ref()

const onChange = (_: any, currentFile: any) => {
    file.value = {
        ...currentFile,
        // url: URL.createObjectURL(currentFile.file),
    }
}
const onProgress = (currentFile: any) => {
    file.value = currentFile
}

type ADPRTableProductDetail = {
    idColor: string
    idMaterial: string
    idCPU: string
    idGPU: string
    idRAM: string
    idHardDrive: string
    price?: number
    imei?: number
}

const productDetails: Reactive<ADPRTableProductDetail[]> = reactive([])

const columns = [
        { type: 'selection', fixed: 'left' },
    { title: '#', name: 'orderNumber', dataIndex: 'orderNumber', width: 50, align: 'center', slotName: 'orderNumber' },
    { title: 'Cấu hình', name: 'configuration', dataIndex: 'configuration', width: 120, align: 'center', slotName: 'configuration' },
    {
        title: 'Ảnh biến thể',
        name: 'imageProductDetail',
        dataIndex: 'imageProductDetail',
        width: 50,
        align: 'center',
        slotName: 'imageProductDetail',
    },
    { title: 'Giá bán', name: 'price', dataIndex: 'price', width: 50, align: 'center', slotName: 'price' },
    { title: 'Tồn kho', name: 'quantity', dataIndex: 'quantity', width: 50, align: 'center', slotName: 'quantity' },
    { title: '', name: 'action', dataIndex: 'action', width: 50, align: 'center', slotName: 'action' },
]

const createVariant = () => {
    if (formDataVariant.idColor) {
        formDataVariant.idColor.forEach((element) => {
            productDetails.push({
                idColor: element,
                idMaterial: formDataVariant.idMaterial,
                idCPU: formDataVariant.idCpu,
                idGPU: formDataVariant.idGpu,
                idRAM: formDataVariant.idRam,
                idHardDrive: formDataVariant.idHardDrive,
            })
        })
    }
}

const isEditPriceInputTable: Ref<number> = ref(-1)

const handleClickPriceTable = (index: number) => {
    isEditPriceInputTable.value = index
}

const priceTableValue: Ref<number> = ref(0)

const handleEnterPrice = (index: number) => {
    productDetails[index].price = priceTableValue.value
    isEditPriceInputTable.value = -1
    priceTableValue.value = 0
}

const isOpenModalIMEIProduct = ref<boolean>(false)

const openModalIMEIProduct = (index: number) => {
    isOpenModalIMEIProduct.value = true
}

const imeiInput = ref<string>()
</script>

<style scoped>
.container {
    padding: 0 20px 20px;
}

.size-upload-image {
    width: 100% !important;
    height: 100% !important;
}

.ml-32px {
    margin-left: 32px;
}

.mb-64px {
    margin-bottom: 64px;
}

.mb-32px {
    margin-bottom: 32px;
}

.ml-8px {
    margin-left: 8px;
}

.mt-20px {
    margin-top: 20px;
}

.size-icon {
    width: 16px;
    height: 16px;
}

.price-hover:hover {
    cursor: pointer;
}
</style>