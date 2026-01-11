<template>
    <div>
        <n-card title="Thông tin cơ bản">
            <n-space>
                <n-form ref="formDataBasicRef" :rules="rulesDataBasic" :model="formDataBasic">
                    <n-grid :span="24" :x-gap="36" :y-gap="8">
                        <n-form-item-gi :span="24" label="Tên sản phẩm" path="name">
                            <n-input placeholder="Nhập tên sản phẩm" v-model:value="formDataBasic.name"></n-input>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Hãng</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.BRAND)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataBasic.idBrand" filterable placeholder="Chọn hãng"
                                :options="dataProperties.basicInformation.brands"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Pin</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.BATTERY)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataBasic.idBattery" filterable placeholder="Chọn pin"
                                :options="dataProperties.basicInformation.batteries"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Màn hình</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.SCREEN)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataBasic.idScreen" filterable placeholder="Chọn màn hình"
                                :options="dataProperties.basicInformation.screens"></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Hệ điều hành</span>
                                    <n-button
                                        @click="clickAddQuickPropertiesHandler(ProductPropertiesType.OPERATING_SYSTEM)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataBasic.idOperatingSystem" filterable
                                placeholder="Chọn hệ điều hành"
                                :options="dataProperties.basicInformation.operatingSystems"></n-select>
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
            </n-space>
            <div v-if="!idProduct" class="mt-40px">
                <n-h4>Ảnh sản phẩm</n-h4>
                <n-upload :default-upload="false" list-type="image-card" multiple style="height: 100px;"
                    @change="handleChangeImageProduct">
                    Chọn ảnh
                </n-upload>
            </div>
        </n-card>
        <n-card title="Thông tin biến thể" class="mt-20px">
            <n-space>
                <n-form ref="formDataVariantRef" :model="formDataVariant" :rules="rulesDataVariant" style="width: 50%;">
                    <n-grid :span="24" :x-gap="36" :y-gap="8">
                        <n-form-item-gi :span="12" path="idColor">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Màu sắc</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.COLOR)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataVariant.idColor"
                                :options="dataProperties.variantInformation.colors" multiple clearable filterable
                                placeholder="Chọn màu sắc"></n-select> </n-form-item-gi>
                        <n-form-item-gi :span="12" path="idCpu">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>CPU</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.CPU)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataVariant.idCpu" filterable placeholder="Chọn CPU"
                                :options="dataProperties.variantInformation.cpus" multiple clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" path="idGpu">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>GPU</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.GPU)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataVariant.idGpu" filterable placeholder="Chọn GPU"
                                :options="dataProperties.variantInformation.gpus" multiple clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" path="idMaterial">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Chất liệu</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.MATERIAL)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataVariant.idMaterial" filterable placeholder="Chọn chất liệu"
                                :options="dataProperties.variantInformation.materials" multiple clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" path="idRam">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>RAM</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.RAM)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataVariant.idRam" filterable placeholder="Chọn RAM"
                                :options="dataProperties.variantInformation.rams" multiple clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="12" path="idHardDrive">
                            <template #label>
                                <n-space class="w-full" justify="space-between">
                                    <span>Ổ cứng</span>
                                    <n-button @click="clickAddQuickPropertiesHandler(ProductPropertiesType.HARD_DRIVE)"
                                        tertiary size="tiny">
                                        <Icon icon="material-symbols:add" />
                                    </n-button>
                                </n-space>
                            </template>
                            <n-select v-model:value="formDataVariant.idHardDrive" filterable placeholder="Chọn ổ cứng"
                                :options="dataProperties.variantInformation.hardDrives" multiple clearable></n-select>
                        </n-form-item-gi>
                        <n-form-item-gi :span="24">
                            <n-space>
                                <n-button @click="createVariant" type="primary">Tạo biến thể</n-button>
                            </n-space>
                        </n-form-item-gi>
                    </n-grid>
                </n-form>
            </n-space>
        </n-card>
        <!-- title="Danh sách biến thể" -->
        <n-card v-if="productDetails && productDetails.length > 0"
            v-for="productDetailList in partitionProductDetailsByColor" class="mt-20px">
            <template #header>
                <n-space justify="space-between">
                    <n-space align="center">
                        <span>Danh sách biến thể màu {{ getNameColorById(productDetailList[0].idColor) }}</span>
                        <div class="circle" :style="{ backgroundColor: dataProperties.variantInformation.colors.find(color => color.value === productDetailList[0].idColor)?.code }"></div>
                    </n-space>

                    <n-space>
                        <n-input-number
                            :value="statePaginationVariantByColor.find(item => item.idColor === productDetailList[0].idColor)?.priceCommonVariant || 0"
                            placeholder="Nhập giá chung" clearable @update:value="val => {
                                const state = statePaginationVariantByColor.find(item => item.idColor === productDetailList[0].idColor);
                                if (state) {
                                    state.priceCommonVariant = val as number;
                                }
                            }" />
                        <n-button type="success"
                            @click="applyPriceCommonVariantHandler(productDetailList[0].idColor)">Áp dụng</n-button>
                    </n-space>
                </n-space>
            </template>
            <n-data-table :data="productDetailList" :columns="columns" :bordered="false" :max-height="400" :pagination="{
                page: statePaginationVariantByColor.find(item => item.idColor === productDetailList[0].idColor)?.currentPage || 1,
                pageSize: 10,
                itemCount: productDetailList.length,
                onChange: (page: number) => {
                    const state = statePaginationVariantByColor.find(item => item.idColor === productDetailList[0].idColor);
                    if (state) {
                        state.currentPage = page;
                    }
                },
            }"></n-data-table>
        </n-card>

        <n-card class="mt-20px" v-if="productDetails && productDetails.length > 0">
            <n-h4>Thêm ảnh biến thể sản phẩm</n-h4>
            <div class="images-variant">
                <n-card v-for="(productDetails, key) in partitionProductDetailsByColor" :key="key">
                    <template #header>
                        <n-space align="center" justify="space-around" style="font-size: 12px;">
                            <div style="display: flex; align-items: center;">
                                <div class="circle"
                                    :style="{ backgroundColor: dataProperties.variantInformation.colors.find(color => color.value === key)?.code }">
                                </div>
                                <span style="margin: 10px 4px;">
                                    {{dataProperties.variantInformation.colors.find(color => color.value === key)?.label
                                    }}
                                </span>
                            </div>

                            <n-tag round :bordered="false"
                                :color="{ color: '#19bf67', textColor: '#fff', borderColor: '#10b981' }">
                                {{ productDetails.length }} phiên bản
                            </n-tag>
                        </n-space>
                    </template>

                    <template #default>
                        <div>
                            <n-upload style="width: 100%;" :max="1" :default-upload="false" list-type="image-card"
                                :on-change="(data: { fileList: UploadFileInfo[] }) => handleChangeProductDetail(data, key)">
                            </n-upload>
                        </div>
                    </template>
                </n-card>
            </div>

            <template #footer>
                <span> <strong style="color: red;">* Chú ý:</strong> Kích thước tối đa của ảnh: 5MB</span>
            </template>
        </n-card>

        <n-space class="mt-20px" v-if="productDetails && productDetails.length > 0">
            <n-button>Hủy</n-button>
            <n-popconfirm :positive-text="'Xác nhận'" :negative-text="'Hủy'" @positive-click="submitVariantHandler">
                <template #trigger>
                    <n-button :loading="loadingCreateVariant" type="success">Xác nhận</n-button>
                </template>
                Bạn chắc chắn muốn thao tác ?
            </n-popconfirm>
        </n-space>

        <ADImeiProductDetail :is-open="isOpenModalIMEIProduct" :idColorImei="idColorImei" :index="indexRowDataImei"
            @close="handleEmitClose" @update:imei="handleEmitUpdateImei" />

        <QuickAddModal :is-open="isOpenQuickAddModal" :type="dataQuickAdd.type" @close="closeQuickAddModalHandler"
            @success="fetchDataProperties" />
    </div>
</template>

<script lang="ts" setup>
import { ADProductCreateUpdateRequest, ADPRPropertiesComboboxResponse, getBatteries, getBrands, getOperatingSystems, getProductById, getScreens, modifyProduct } from '@/service/api/admin/product/product.api'
import { ADProductDetailCreateUpdateRequest, createProductVariant, getColors, getCPUs, getGPUs, getHardDrives, getMaterials, getRAMs, saveImage } from '@/service/api/admin/product/productDetail.api'
import { Icon } from '@iconify/vue'
import { DataTableColumns, FormInst, FormRules, NButton, NInput, NInputNumber, NSpace, NUpload, UploadFileInfo } from 'naive-ui'
import { Reactive } from 'vue'
import ADImeiProductDetail from './ADImeiProductDetail.vue'
import QuickAddModal from './QuickAddModal.vue'
import { ProductPropertiesType } from '@/constants/ProductPropertiesType'
import _ from 'lodash'
import { useMessage } from 'naive-ui'

const route = useRoute()

const router = useRouter()

const message = useMessage()

const idProduct: Ref<string> = ref(route.params.id as string)
const isOpenQuickAddModal = ref<boolean>(false)
const isEditPriceInputTable: Ref<{ idColor: string | undefined, index: number }> = ref({ idColor: '', index: -1 })
const priceTableValue: Ref<{ idColor: string, value: number }[]> = ref([])
const isOpenModalIMEIProduct = ref<boolean>(false)
const indexRowDataImei = ref<number>()
const idColorImei = ref<string>()
const loadingCreateVariant = ref(false)
const priceCommonVariant: Ref<number> = ref(0)
const statePaginationVariantByColor: Reactive<{ idColor: string, currentPage: number, priceCommonVariant: Ref<number> }[]> = reactive([]);
const formDataBasicRef = ref<FormInst | null>(null)
const formDataVariantRef = ref<FormInst | null>(null)

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

const imageProduct: Reactive<any[]> = reactive([])

const formDataBasic: Reactive<Partial<ADProductCreateUpdateRequest>> = reactive({
    name: undefined as undefined | string,
    idBrand: undefined as undefined | string,
    idBattery: undefined as undefined | string,
    idScreen: undefined as undefined | string,
    idOperatingSystem: undefined as undefined | string,
})

const formDataVariant = reactive({
    idColor: undefined as undefined | string[],
    idMaterial: undefined as undefined | string[],
    idCpu: undefined as undefined | string[],
    idGpu: undefined as undefined | string[],
    idRam: undefined as undefined | string[],
    idHardDrive: undefined as undefined | string[],
})

onMounted(async () => {
    if (idProduct.value) fetchProductById()
    await fetchDataProperties()
    initSelectPropertiesProduct()
})

const initSelectPropertiesProduct = () => {
    formDataBasic.idBrand = dataProperties.basicInformation.brands[0]?.value
    formDataBasic.idBattery = dataProperties.basicInformation.batteries[0]?.value
    formDataBasic.idScreen = dataProperties.basicInformation.screens[0]?.value
    formDataBasic.idOperatingSystem = dataProperties.basicInformation.operatingSystems[0]?.value
}

type ADPRTableProductDetail = {
    idColor: string
    idMaterial: string
    idCPU: string
    idGPU: string
    idRAM: string
    idHardDrive: string
    price?: number
    imei?: string[],
    urlImage?: string,
    publicId?: string,
}

const productDetails: Reactive<ADPRTableProductDetail[]> = reactive([])

const columns: DataTableColumns<ADPRTableProductDetail> = [
    { type: 'selection', width: 50, fixed: 'left' },
    {
        title: '#', key: 'orderNumber', width: 50, align: 'center', fixed: 'left',
        render: (_, index) => h('span', { innerText: index + 1 })
    },
    {
        title: 'Cấu hình', key: 'configuration', width: 200, align: 'left',
        render: (row: ADPRTableProductDetail) => h('div',
            [
                h('div', { style: { display: 'flex', alignItems: 'center', margin: '10px 0' } }, [
                    h(Icon, { icon: 'iconoir:fill-color' }),
                    h('span', { style: { marginLeft: '8px' }, innerText: `Màu: ${dataProperties.variantInformation.colors.filter(data => data.value == row.idColor).map(data => data.label)}` }),
                    // h('div', {
                    //     class: 'circle',
                    //     style: {
                    //         backgroundColor: dataProperties.variantInformation.colors.filter(data => data.value == row.idColor).map(data => data.code)[0],
                    //         marginLeft: '8px',
                    //         width: '12px',
                    //         height: '12px',
                    //         borderRadius: '50%',
                    //         border: '1px solid #00000033'
                    //     }
                    // })
                ]),
                h('div', { style: { display: 'flex', alignItems: 'center', margin: '10px 0' } }, [h(Icon, { icon: 'solar:cpu-bold' }), h('span', { style: { marginLeft: '8px' }, innerText: `CPU: ${dataProperties.variantInformation.cpus.filter(data => data.value == row.idCPU).map(data => data.label)}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center', margin: '10px 0' } }, [h(Icon, { icon: 'gravity-ui:gpu' }), h('span', { style: { marginLeft: '8px' }, innerText: `GPU: ${dataProperties.variantInformation.gpus.filter(data => data.value == row.idGPU).map(data => data.label)}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center', margin: '10px 0' } }, [h(Icon, { icon: 'material-symbols:hard-drive-outline-sharp' }), h('span', { style: { marginLeft: '8px' }, innerText: `Ổ cứng: ${dataProperties.variantInformation.hardDrives.filter(data => data.value == row.idHardDrive).map(data => data.label)}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center', margin: '10px 0' } }, [h(Icon, { icon: 'lets-icons:materials' }), h('span', { style: { marginLeft: '8px' }, innerText: `Chất liệu: ${dataProperties.variantInformation.materials.filter(data => data.value == row.idMaterial).map(data => data.label)}` })]),
                h('div', { style: { display: 'flex', alignItems: 'center', margin: '10px 0' } }, [h(Icon, { icon: 'icon-park-outline:memory' }), h('span', { style: { marginLeft: '8px' }, innerText: `RAM: ${dataProperties.variantInformation.rams.filter(data => data.value == row.idRAM).map(data => data.label)}` })]),
            ]
        )
    },
    {
        title: 'Giá bán', key: 'price', width: 200, align: 'center',
        render: (data: ADPRTableProductDetail, index: number) => {
            return !(isEditPriceInputTable.value.idColor === data.idColor && isEditPriceInputTable.value.index === index) ?
                h('span', {
                    innerText: data.price ? (data.price + '').split('').reduce((prev, curr, index, arr) => {
                        if ((arr.length - index) % 3 == 0) return prev + ' ' + curr
                        return prev + curr
                    }, '') + ' vnđ' : 'Chưa có giá cho biến thể này', onClick: () => { handleClickPriceTable(data.idColor, index) }
                }) :
                h(NInputNumber, {
                    style: { width: '100%' },
                    placeholder: 'Nhập giá',
                    value: priceTableValue.value.find(item => item.idColor === data.idColor)?.value,
                    onUpdateValue: (val) => {
                        const existingItem = priceTableValue.value.find(item => item.idColor === data.idColor)
                        if (existingItem) {
                            priceTableValue.value = priceTableValue.value.map(item => item.idColor === data.idColor ? { ...item, value: val as number } : item)
                            return
                        }

                        priceTableValue.value.push({ idColor: data.idColor, value: val as number })
                    },
                    onBlur: () => {
                        handleEnterPrice(data.idColor, index)
                    },
                },
                )
        }
    },
    {
        title: 'Số lượng', key: 'quantity', width: 150, align: 'center',
        render: (data: ADPRTableProductDetail) => h('span', data.imei ? data.imei.length + ' sản phẩm' : 'Không có sản phẩm')
    },
    {
        title: 'Thao tác', key: 'action', width: 100, align: 'center',
        render: (data, index) => h(NSpace, { justify: 'center' },
            [
                h(NButton, { quaternary: true, circle: true, onClick: () => { openModalIMEIProduct(data.idColor, index) } }, h(Icon, { icon: 'mdi:barcode-scan' })),
                h(NButton, { quaternary: true, circle: true, onClick: () => { deleteProductDetail(data.idColor, index) } }, h(Icon, { icon: 'mdi:delete' })),
            ]
        )
    },
]

const createVariant = async () => {
  try {
    // reset state
    if (productDetails.length > 0) {
      productDetails.splice(0, productDetails.length)
      statePaginationVariantByColor.splice(0, statePaginationVariantByColor.length)
    }

    // validate form
    const isInvalid = !formDataVariantRef.value?.validate(
      error => !!(error && error.length > 0)
    )
    if (isInvalid) return

    // build variants
    if (formDataVariant.idColor) {
      formDataVariant.idColor.forEach((idColor) => {
        statePaginationVariantByColor.push({
          idColor,
          currentPage: 1,
          priceCommonVariant: 0,
        })

        formDataVariant.idCpu?.forEach((cpu) => {
          formDataVariant.idGpu?.forEach((gpu) => {
            formDataVariant.idHardDrive?.forEach((hardDrive) => {
              formDataVariant.idMaterial?.forEach((material) => {
                formDataVariant.idRam?.forEach((ram) => {
                  productDetails.push({
                    idColor,
                    idCPU: cpu,
                    idGPU: gpu,
                    idHardDrive: hardDrive,
                    idMaterial: material,
                    idRAM: ram,
                  })
                })
              })
            })
          })
        })
      })
    }

    // 🔥 GỌI API TẠO VARIANT
    const requests = productDetails.map((detail) =>
      createProductVariant(
        idNewProduct!,
        detail as ADProductDetailCreateUpdateRequest
      )
    )

    await Promise.all(requests)

    message.success('Tạo biến thể thành công')
  } catch (err: any) {
    // 👈 CHỖ NÀY SẼ HIỂN THỊ
    message.error(err.message)
  }
}


const handleClickPriceTable = (idColor: string, index: number) => {
    isEditPriceInputTable.value = { idColor, index }
}


const handleEnterPrice = (idColor: string, idx: number) => {
    const price = priceTableValue.value.find(item => item.idColor === idColor)?.value

    const indexIdColor = productDetails.map((productDetail, index) => ({ idColor: productDetail.idColor, index }))
        .filter(productDetail => productDetail.idColor === idColor).map((indexObj) => indexObj.index)[0]

    productDetails[indexIdColor + idx].price = price

    isEditPriceInputTable.value = { idColor: undefined, index: -1 }
    priceTableValue.value = priceTableValue.value.filter(item => item.idColor !== idColor)
}

// imei

const openModalIMEIProduct = (idColor: string, index: number) => {
    isOpenModalIMEIProduct.value = true
    indexRowDataImei.value = index
    idColorImei.value = idColor
}


const handleEmitClose = () => {
    isOpenModalIMEIProduct.value = false
}

const handleEmitUpdateImei = (imeis: string[], idColorImei: string, index: number) => {
    const indexIdColor = productDetails.map((productDetail, index) => ({ idColor: productDetail.idColor, index }))
        .filter(productDetail => productDetail.idColor === idColorImei).map((indexObj) => indexObj.index)[0]

    productDetails[indexIdColor + index].imei = imeis

    handleEmitClose()
}

const notification = useNotification();

const imageProductDetails = reactive<{ [key: string]: any }>({});

const submitVariantHandler = async () => {

    if (!validateSubmitVariantHandler()) return;

    loadingCreateVariant.value = true;
    try {
        let idNewProduct: string = "";
        if (!idProduct.value) {
            const resCreateProduct = await modifyProduct(formDataBasic as ADProductCreateUpdateRequest, imageProduct);
            idNewProduct = resCreateProduct.data;
        } else {
            idNewProduct = idProduct.value;
        }

        const requestImages = Object.entries(imageProductDetails).map(([idColor, image]) => saveImage(image));
        const imagesResponses = await Promise.all(requestImages);

        imagesResponses.forEach((res, index) => {
            const idColor = Object.keys(imageProductDetails)[index];
            const cloudirayResponse = res.data;
            productDetails.forEach(productDetail => {
                if (productDetail.idColor === idColor) {
                    productDetail.urlImage = cloudirayResponse.url;
                    productDetail.publicId = cloudirayResponse.publicId;
                }
            });
        });

try {
  const requests = productDetails.map((productDetail) =>
    createProductVariant(
      idNewProduct!,
      productDetail as ADProductDetailCreateUpdateRequest
    )
  )

  await Promise.all(requests)

  message.success('Tạo tất cả biến thể thành công')
} catch (err: any) {
  message.error(err.message) // message từ backend
}

        // Gửi đồng thời
        await Promise.all(requests);

        if (idNewProduct) notification.success({ content: 'Thêm sản phẩm thành công', duration: 3000 })
        else notification.success({ content: 'Thêm sản phẩm thất bại', duration: 3000 })

        router.push({ name: 'products_list' })
    } catch (e) {
        notification.error({ content: 'Thêm sản phẩm thất bại', duration: 3000 })
    } finally {
        loadingCreateVariant.value = false;
    }
}

const validateSubmitVariantHandler: () => boolean = () => {
    if (!formDataBasicRef?.value?.validate(error => {
        if (error) notification.error({ content: error[0][0]?.message ?? 'Vui lòng điền đầy đủ thông tin cơ bản', duration: 3000 })
        return !!error
    })) return false;

    if (!formDataVariantRef?.value?.validate(error => {
        if (error) notification.error({ content: error[0][0]?.message ?? 'Vui lòng điền đầy đủ thông tin biến thể', duration: 3000 })
        return !!error
    })) return false;

    if (productDetails.length === 0) {

        notification.error({ content: 'Vui lòng tạo biến thể cho sản phẩm', duration: 3000 })
        return false;
    }

    if (productDetails.some(productDetail => !productDetail.price)) {
        notification.error({ content: 'Vui lòng nhập giá cho tất cả biến thể', duration: 3000 })
        return false;
    }

    if (productDetails.some(productDetail => !productDetail.imei || productDetail.imei.length === 0)) {
        notification.error({ content: 'Vui lòng thêm IMEI cho tất cả biến thể', duration: 3000 })
        return false;
    }

    if (!idProduct.value && imageProduct.length === 0) {
        notification.error({ content: 'Vui lòng thêm ảnh sản phẩm', duration: 3000 })
        return false;
    }

    if (Object.values(imageProductDetails).some(image => !image)) {
        notification.error({ content: 'Vui lòng thêm ảnh cho tất cả biến thể', duration: 3000 })
        return false;
    }

    if (Object.values(imageProductDetails).length != _.uniq(productDetails.map(pd => pd.idColor)).length) {
        notification.error({ content: 'Vui lòng thêm ảnh cho tất cả biến thể', duration: 3000 })
        return false;
    }

    if (new Set(productDetails.map(pd => pd.imei?.join(','))).size !== productDetails.length) {
        notification.error({ content: 'Mỗi biến thể phải có dãy IMEI riêng biệt', duration: 3000 })
        return false;
    }

    if (productDetails.some(pd => pd.imei && pd.imei.length !== new Set(pd.imei).size)) {
        notification.error({ content: 'Dãy IMEI trong cùng một biến thể không được trùng lặp', duration: 3000 })
        return false;
    }

    return true;
}

const handleChangeImageProduct = (data: { fileList: UploadFileInfo[] }) => {
    imageProduct.push(data.fileList[0].file)
}

const handleChangeProductDetail = (data: { fileList: UploadFileInfo[] }, idColor: string) => {
    imageProductDetails[idColor] = (data.fileList[0].file)
}

const dataQuickAdd = reactive<Partial<{
    type: ProductPropertiesType
}>>({
    type: undefined
})

const clickAddQuickPropertiesHandler = (typeData: ProductPropertiesType) => {
    isOpenQuickAddModal.value = true
    dataQuickAdd.type = typeData
}

const closeQuickAddModalHandler = () => {
    isOpenQuickAddModal.value = false
}


const applyPriceCommonVariantHandler = (idColor: string) => {
    productDetails.filter(item => item.idColor === idColor).forEach(item => {
        if (item.price) return
        item.price = statePaginationVariantByColor.find(state => state.idColor === idColor)?.priceCommonVariant
    })
    priceCommonVariant.value = 0
}

const partitionProductDetailsByColor = computed(() => {
    const partitioned: { [key: string]: ADPRTableProductDetail[] } = {}

    productDetails.forEach((item) => {
        if (!partitioned[item.idColor]) {
            partitioned[item.idColor] = []
        }
        partitioned[item.idColor].push(item)
    })

    return partitioned
})

const getNameColorById = (idColor: string) => {
    const color = dataProperties.variantInformation.colors.find(color => color.value === idColor)
    return color ? color.label : 'Không xác định'
}

const rulesDataBasic: FormRules = {
    name: [
        { required: true, message: 'Vui lòng nhập tên sản phẩm', trigger: 'blur' },
        { min: 3, max: 100, message: 'Tên sản phẩm từ 3 đến 100 ký tự', trigger: 'blur' },
    ],
    // idBrand: [
    //     { type: 'array', required: true, message: 'Vui lòng chọn hãng', trigger: 'change' },
    // ],
    // idBattery: [
    //     { type: 'array', required: true, message: 'Vui lòng chọn pin', trigger: 'change' },
    // ],
    // idScreen: [
    //     { type: 'array', required: true, message: 'Vui lòng chọn màn hình', trigger: 'change' },
    // ],
    // idOperatingSystem: [
    //     { type: 'array', required: true, message: 'Vui lòng chọn hệ điều hành', trigger: 'change' },
    // ],
}

const rulesDataVariant: FormRules = {
    idColor: [
        { type: 'array', required: true, message: 'Vui lòng chọn màu sắc', trigger: 'change' },
    ],
    idCpu: [
        { type: 'array', required: true, message: 'Vui lòng chọn CPU', trigger: 'change' },
    ],
    idGpu: [
        { type: 'array', required: true, message: 'Vui lòng chọn GPU', trigger: 'change' },
    ],
    idMaterial: [
        { type: 'array', required: true, message: 'Vui lòng chọn chất liệu', trigger: 'change' },
    ],
    idRam: [
        { type: 'array', required: true, message: 'Vui lòng chọn RAM', trigger: 'change' },
    ],
    idHardDrive: [
        { type: 'array', required: true, message: 'Vui lòng chọn ổ cứng', trigger: 'change' },
    ],
}

const deleteProductDetail = (idColor: string, index: number) => {
    const indexIdColor = productDetails.map((productDetail, index) => ({ idColor: productDetail.idColor, index }))
        .filter(productDetail => productDetail.idColor === idColor).map((indexObj) => indexObj.index)[0]

    productDetails.splice(indexIdColor + index, 1)

    const stateIndex = statePaginationVariantByColor.findIndex(state => state.idColor === idColor)
    if (stateIndex !== -1) {
        statePaginationVariantByColor.splice(stateIndex, 1)
    }
}
</script>

<style scoped>
.mt-40px {
    margin-top: 40px !important;
}

:deep(.rowspan-top) {
    vertical-align: top !important;
}

:deep(.n-form-item-label__text) {
    width: 100% !important;
}

:deep(.n-input-number .n-input-number-control) {
    display: none !important;
}

.images-variant {
    width: 100%;
    gap: 3rem;
    overflow-x: auto;
    display: flex;
}

.images-variant>div {
    flex-shrink: 0;
}

.circle {
    width: 15px;
    height: 15px;
    border-radius: 50%;
    border: 1px solid #00000033;
}

:deep(.images-variant .n-card) {
    width: auto
}

:deep(.images-variant .n-card-header) {
    width: 100%
}

:deep(.images-variant .n-card__content) {
    display: flex;
    justify-content: center;
}

:deep(.images-variant .n-upload) {
    width: 100%;
    height: 100%;
}
</style>