<template>
    <div>
        <n-card title="Thông tin cơ bản">
            <n-space justify="center">
                <n-form ref="formRef">
                    <n-grid :span="24" :x-gap="36">
                        <n-form-item-gi :span="12" label="Mã sản phẩm">
                            <n-input placeholder="Nhập mã sản phẩm" v-model:value="formDataBasic.code"></n-input>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Tên sản phẩm">
                            <n-input placeholder="Nhập tên sản phẩm" v-model:value="formDataBasic.name"></n-input>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Hãng">
                            <n-select v-model:value="formDataBasic.idBrand"
                                :options="dataProperties.basicInformation.brands"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Pin">
                            <n-select v-model:value="formDataBasic.idBattery"
                                :options="dataProperties.basicInformation.batteries"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Màn hình">
                            <n-select v-model:value="formDataBasic.idScreen"
                                :options="dataProperties.basicInformation.screens"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" label="Hệ điều hành">
                            <n-select v-model:value="formDataBasic.idOperatingSystem"
                                :options="dataProperties.basicInformation.operatingSystems"></n-select>
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
            </n-space>
            <div>
                <span>Ảnh chính</span>
                <n-upload :default-upload="false" list-type="image-card" @change="handleChange">
                    Click to Upload
                </n-upload>
            </div>
        </n-card>
        <n-card title="Thông tin biến thể" class="mt-20px">
            <n-space justify="center">
                <n-form ref="formRef">
                    <n-grid :span="24" :x-gap="36">
                        <n-form-item-gi :span="8" label="Màu sắc">
                            <n-select v-model:value="formDataVariant.idColor"
                                :options="dataProperties.variantInformation.colors" multiple
                                placeholder="Chọn màu sắc"></n-select> </n-form-item-gi>
                        <n-form-item-gi :span="8" label="CPU">
                            <n-select v-model:value="formDataVariant.idCpu"
                                :options="dataProperties.variantInformation.cpus"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="GPU">
                            <n-select v-model:value="formDataVariant.idGpu"
                                :options="dataProperties.variantInformation.gpus"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="Chất liệu">
                            <n-select v-model:value="formDataVariant.idMaterial"
                                :options="dataProperties.variantInformation.materials"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="RAM">
                            <n-select v-model:value="formDataVariant.idRam"
                                :options="dataProperties.variantInformation.rams"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="8" label="Ổ cứng">
                            <n-select v-model:value="formDataVariant.idHardDrive"
                                :options="dataProperties.variantInformation.hardDrives"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="24" justify="end">
                            <n-space justify="end" :style="{ width: '100%' }">
                                <n-button @click="createVariant" type="primary">Tạo biến thể</n-button>
                            </n-space>
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
            </n-space>
        </n-card>
        <n-card title="Danh sách biến thể" class="mt-20px">
            <n-data-table :data="productDetails" :columns="columns" :bordered="false"></n-data-table>

            <template #footer>
                <n-space justify="end">
                    <n-button>Hủy</n-button>
                    <n-button @click="submitVariantHandler">Xác nhận</n-button>
                </n-space>
            </template>
        </n-card>

        <ADImeiProductDetail :is-open="isOpenModalIMEIProduct" :index="indexRowDataImei" @close="handleEmitClose"
            @update:imei="handleEmitUpdateImei" />

    </div>
</template>

<script lang="ts" setup>
import { ADProductCreateUpdateRequest, ADPRPropertiesComboboxResponse, getBatteries, getBrands, getOperatingSystems, getProductById, getScreens, uploadImages } from '@/service/api/admin/product/product.api'
import { ADProductDetailCreateUpdateRequest, createProductVariant, getColors, getCPUs, getGPUs, getHardDrives, getMaterials, getRAMs } from '@/service/api/admin/product/productDetail.api'
import { Icon } from '@iconify/vue'
import { DataTableColumns, NButton, NImage, NInput, NInputNumber, NSpace, UploadFileInfo } from 'naive-ui'
import { Reactive } from 'vue'
import ADImeiProductDetail from './ADImeiProductDetail.vue'
import axios from 'axios'

const route = useRoute()

const router = useRouter()

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


const fetchProductById = async () => {
    const res = await getProductById(idProduct.value)

    formDataBasic.id = res.data.id
    formDataBasic.code = res.data.code
    formDataBasic.name = res.data.name
    formDataBasic.idBattery = res.data.idBattery
    formDataBasic.idBrand = res.data.idBrand
    formDataBasic.idScreen = res.data.idScreen
    formDataBasic.idOperatingSystem = res.data.idOperatingSystem
}

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

const imageProduct: Reactive<UploadFileInfo[]> = reactive([])

const formDataBasic: Reactive<ADProductCreateUpdateRequest> = reactive({
    code: '',
    name: '',
    idBrand: '',
    idBattery: '',
    idScreen: '',
    idOperatingSystem: '',

})

const formDataVariant = reactive({
    idColor: undefined as undefined | string[],
    idMaterial: '',
    idCpu: '',
    idGpu: '',
    idRam: '',
    idHardDrive: '',
})

onMounted(() => {
    fetchProductById()
    fetchDataProperties()
})

type ADPRTableProductDetail = {
    idColor: string
    idMaterial: string
    idCPU: string
    idGPU: string
    idRAM: string
    idHardDrive: string
    price?: number
    imei?: string[]
}

const productDetails: Reactive<ADPRTableProductDetail[]> = reactive([])

const columns: DataTableColumns<ADPRTableProductDetail> = [
    { type: 'selection', width: 50, fixed: 'left' },
    {
        title: '#', key: 'orderNumber', width: 50, align: 'center', fixed: 'left',
        render: (data, index) => h('span', { innerText: index + 1 })
    },
    {
        title: 'Cấu hình', key: 'configuration', width: 200, align: 'left',
        render: (row: ADPRTableProductDetail) => h('div',
            [
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'iconoir:fill-color' }), h('span', { style: { marginLeft: '8px' }, innerText: dataProperties.variantInformation.colors.filter(data => data.value == row.idColor).map(data => data.label) })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'solar:cpu-bold' }), h('span', { style: { marginLeft: '8px' }, innerText: dataProperties.variantInformation.cpus.filter(data => data.value == row.idCPU).map(data => data.label) })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'gravity-ui:gpu' }), h('span', { style: { marginLeft: '8px' }, innerText: dataProperties.variantInformation.gpus.filter(data => data.value == row.idGPU).map(data => data.label) })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'material-symbols:hard-drive-outline-sharp' }), h('span', { style: { marginLeft: '8px' }, innerText: dataProperties.variantInformation.hardDrives.filter(data => data.value == row.idHardDrive).map(data => data.label) })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'lets-icons:materials' }), h('span', { style: { marginLeft: '8px' }, innerText: dataProperties.variantInformation.materials.filter(data => data.value == row.idMaterial).map(data => data.label) })]),
                h('div', { style: { display: 'flex', alignItems: 'center' } }, [h(Icon, { icon: 'lets-icons:materials' }), h('span', { style: { marginLeft: '8px' }, innerText: dataProperties.variantInformation.rams.filter(data => data.value == row.idRAM).map(data => data.label) })]),
            ]
        )
    },
    {
        title: 'Ảnh biến thể',
        key: 'imageProductDetail',
        width: 100,
        align: 'center',
        render: (data: ADPRTableProductDetail) => h(NImage, { lazy: true, alt: 'Ảnh sản phẩm' })
    },
    {
        title: 'Giá bán', key: 'price', width: 200, align: 'center',
        render: (data: ADPRTableProductDetail, index: number) => {
            return !(isEditPriceInputTable.value == index) ?
                h('span', {
                    innerText: data.price ? (data.price + '').split('').reduce((prev, curr, index, arr) => {
                        if ((arr.length - index) % 3 == 0) return prev + ' ' + curr
                        return prev + curr
                    }, '') + ' vnđ' : 'Chưa có giá cho biến thể này', onClick: () => { handleClickPriceTable(index) }
                }) :
                h(NInputNumber, {
                    style: { width: '100%' },
                    placeholder: 'Nhập giá',
                    value: priceTableValue.value,
                    onUpdateValue: (val) => {
                        priceTableValue.value = val as number
                    },
                    onBlur: () => {
                        handleEnterPrice(index)
                    },
                },
                )
        }
    },
    {
        title: 'Tồn kho', key: 'quantity', width: 150, align: 'center',
        render: (data: ADPRTableProductDetail) => h('span', data.imei ? data.imei.length + ' sản phẩm' : 'Không có sản phẩm')
    },
    {
        title: 'Thao tác', key: 'action', width: 100, align: 'center',
        render: (data, index) => h(NSpace, { justify: 'center' },
            [
                h(NButton, { quaternary: true, circle: true, onClick: () => { openModalIMEIProduct(index) } }, h(Icon, { icon: 'mdi:barcode-scan' })),
            ]
        )
    },
]

const createVariant = () => {
    // formDataVariant.idColor = [...dataProperties.variantInformation.colors.map(data => data.value)]
    // formDataVariant.idCpu = dataProperties.variantInformation.cpus[0].value
    // formDataVariant.idGpu = dataProperties.variantInformation.gpus[0].value
    // formDataVariant.idHardDrive = dataProperties.variantInformation.hardDrives[0].value
    // formDataVariant.idMaterial = dataProperties.variantInformation.materials[0].value
    // formDataVariant.idRam = dataProperties.variantInformation.rams[0].value

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

// imei
const isOpenModalIMEIProduct = ref<boolean>(false)

const openModalIMEIProduct = (index: number) => {
    isOpenModalIMEIProduct.value = true
    indexRowDataImei.value = index
}

const imeiInput = ref<string[]>()

const indexRowDataImei = ref<number>()

const handleEmitClose = () => {
    isOpenModalIMEIProduct.value = false
}

const handleEmitUpdateImei = (imeis: string[], index: number) => {
    imeiInput.value = imeis

    productDetails[index].imei = imeis

    handleEmitClose()
}

const notification = useNotification();

const submitVariantHandler = async () => {

    // const dataVariant = productDetails.filter(data => {
    //     let isValid = true
    //     Object.keys(data).forEach(key => {
    //         const value = data[key as keyof ADPRTableProductDetail]
    //         if (!value) isValid = false
    //     })

    //     if (!(data.imei && Array.isArray(data.imei) && data.imei.length != 0)) return false

    //     if (!data.price) return false

    //     return isValid
    // })

    // const res = await createProductVariant(formDataBasic, dataVariant.map(data => ({
    //     idColor: data.idColor,
    //     idCPU: data.idCPU,
    //     idGPU: data.idGPU,
    //     idRAM: data.idRAM,
    //     idHardDrive: data.idHardDrive,
    //     idMaterial: data.idMaterial,
    //     imei: data.imei,
    //     price: data.price
    // })) as ADProductDetailCreateUpdateRequest[])


    // if(!idProduct.value) idProduct.value = res.data
    console.log(idProduct.value)
    if (formDataBasic.imageProduct && idProduct.value) {
        const formData = new FormData()

        formData.append("imageProduct", formDataBasic.imageProduct)

        // const resUpload = uploadImages(
        //     formData
        //     ,idProduct.value)

        await axios.post(`http://localhost:2345/api/v1/admin/products/upload-images/${idProduct.value}`, formData, {
            headers: {
                "Content-Type": "multipart/form-data"
            }
        })
    }

    // if (res.success) notification.success({ content: 'Thêm sản phẩm thành công', duration: 3000 })
    // else notification.success({ content: 'Thêm sản phẩm thất bại', duration: 3000 })

    // router.push({ name: 'products_list' })
}

const handleChange = (data: { fileList: UploadFileInfo[] }) => {
    imageProduct.push(data.fileList[0])
    formDataBasic.imageProduct = data.fileList[0].file
}

// watch(idProduct,
//     (newValue) => {
//         fetchProductById()
//     }
// )
</script>

<style scoped>
.line-configuration {
    display: flex;
    align-items: center;
}
</style>