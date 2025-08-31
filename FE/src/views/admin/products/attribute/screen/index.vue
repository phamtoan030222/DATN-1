<template>
    <div>
        <n-card>
            <NSpace vertical :size="8">
                <NSpace align="center">
                    <NIcon size="24">
                        <Icon icon="icon-park-outline:monitor" />
                    </NIcon>
                    <span style="font-weight: 600; font-size: 24px">
                        Quản lý màn hình
                    </span>
                </NSpace>
                <span>Quản lý thuộc tính màn hình của sản phẩm</span>
            </NSpace>
        </n-card>
        <n-card title="Bộ lọc" class="mt-20px">
            <n-grid x-gap="12" :cols="11">
                <n-grid-item span="2">
                    <span>Tìm kiếm</span>
                    <n-input v-model:value="state.search.q" placeholder="Tìm kiếm" />
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Độ phân giải</span>
                    <n-select v-model:value="state.search.resolution"
                        :options="screenResolutionOptionsSelect"></n-select>
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Dạng tấm nền</span>
                    <n-select v-model:value="state.search.panelType" :options="panelTypeOptionsSelect"></n-select>
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Kích thước màn hình</span>
                    <n-select v-model:value="state.search.physicalSize" :options="screenSizeOptionsSelect"></n-select>
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Công nghệ</span>
                    <n-input v-model:value="state.search.technology" placeholder="Nhập công nghệ" />
                </n-grid-item>
                <n-grid-item span="1">
                    <n-flex vertical justify="end" style="height: 100%;">
                        <n-button @click="refreshFilter" strong secondary circle type="success">
                            <template #icon>
                                <n-icon>
                                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg"
                                        xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 512 512"
                                        enable-background="new 0 0 512 512" xml:space="preserve">
                                        <path d="M433,288.8c-7.7,0-14.3,5.9-14.9,13.6c-6.9,83.1-76.8,147.9-161.8,147.9c-89.5,0-162.4-72.4-162.4-161.4
                                    c0-87.6,70.6-159.2,158.2-161.4c2.3-0.1,4.1,1.7,4.1,4v50.3c0,12.6,13.9,20.2,24.6,13.5L377,128c10-6.3,10-20.8,0-27.1l-96.1-66.4
                                    c-10.7-6.7-24.6,0.9-24.6,13.5v45.7c0,2.2-1.7,4-3.9,4C148,99.8,64,184.6,64,288.9C64,394.5,150.1,480,256.3,480
                                    c100.8,0,183.4-76.7,191.6-175.1C448.7,296.2,441.7,288.8,433,288.8L433,288.8z">
                                        </path>
                                    </svg>
                                </n-icon>
                            </template>
                        </n-button>
                    </n-flex>
                </n-grid-item>
            </n-grid>
        </n-card>
        <n-card title="Danh sách màn hình" class="mt-20px">
            <template #header-extra>
                <n-space justify="end">
                    <n-button circle type="primary" @click="clickOpenModal()">
                        <Icon icon="material-symbols:add" />
                    </n-button>
                </n-space>
            </template>
            <n-data-table :columns="columns" :data="state.data.screens" :bordered="false" />
            <n-space justify="center" class="mt-20px">
                <NPagination :page="state.pagination.page" :page-size="state.pagination.size"
                    :page-count="state.pagination.totalPages" @update:page="handlePageChange" />
            </n-space>
        </n-card>

        <ADProductScreenModal @success="handleSuccessModifyModal" :isDetail="isDetailModal" :isOpen="isOpenModal"
            :id="ScreenIDSelected" @close="closeModal" />
    </div>
</template>

<script lang="ts" setup>
import { debounce } from 'lodash'
import { onMounted, reactive, Ref, ref, watch } from 'vue'
import ADProductScreenModal from './component/ADProductScreenModal.vue'
import { ADProductScreenResponse, changeScreenStatus, getScreens } from '@/service/api/admin/product/screen.api'
import { DataTableColumns, NButton, NSpace, NSwitch } from 'naive-ui'
import { Icon } from '@iconify/vue'


const state = reactive({
    search: {
        q: '',
        resolution: '' as string | undefined,
        technology: '',
        panelType: '',
        physicalSize: undefined as number | undefined,
    },
    data: {
        screens: [] as ADProductScreenResponse[],
    },
    pagination: {
        page: 1,
        size: 10,
        totalPages: undefined as number | undefined,
    },
})

const fetchScreens = async () => {
    const res = await getScreens({
        page: state.pagination.page,
        size: state.pagination.size,
        q: state.search.q,
        resolution: state.search.resolution as string,
        physicalSize: state.search.physicalSize as number,
        panelType: state.search.panelType,
        technology: state.search.technology,
    })

    state.data.screens = res.data.data
    state.pagination.totalPages = res.data.totalPages
}

const refreshFilter = () => {
    state.search.q = ''
    state.search.resolution = undefined
    state.search.technology = ''
    state.search.panelType = ''
    state.search.physicalSize = undefined
}

const columns: DataTableColumns<ADProductScreenResponse> = [
    { type: 'selection', fixed: 'left' },
    {
        title: '#', key: 'orderNumber', width: 50, fixed: 'left', render: (data, index) => {
            return h('span', { innerText: index + 1 })
        }
    },
    { title: 'Mã', key: 'code', width: 100, fixed: 'left', },
    { title: 'Tên', key: 'name', width: 150, fixed: 'left', },
    {
        title: 'Trạng thái', key: 'status', width: 70, align: 'center',
        render: (data: ADProductScreenResponse) => h(NSwitch, {value: data.status == 'ACTIVE', onUpdateValue: (value: boolean) => {handleChangeStatus(data.id as string)}})
    },
    { title: 'Đô phân giải', key: 'resolution', width: 150, align: 'center', },
    { title: 'Kích thước vật lý', key: 'physicalSize', width: 150, align: 'center', },
    { title: 'Tấm nền', key: 'panelType', width: 150, align: 'center', },
    { title: 'Công nghệ', key: 'technology', width: 150, align: 'center', },
    {
        title: 'Thao tác', key: 'action', width: 100, fixed: 'right',
        render: (data: ADProductScreenResponse) => {
            return h(NSpace,
                [
                    h(NButton, {
                        quaternary: true, size: 'small', circle: true,
                        onClick: () => clickOpenModal(data.id, true)
                    },
                        h(Icon, { icon: 'carbon:edit' })
                    ),
                    // h(NButton, {
                    //     strong: true, circle: true, type: 'warning',
                    //     onClick: () => clickOpenModal(data.id)
                    // },
                    //     h(Icon, { icon: 'carbon:edit' })
                    // )
                ]
            )
        }
    },
]

onMounted(() => {
    fetchScreens()
})

const isOpenModal = ref<boolean>(false)

const isDetailModal: Ref<boolean> = ref(true)

const ScreenIDSelected = ref<string>()

const clickOpenModal = (id?: string, isDetail?: boolean) => {
    ScreenIDSelected.value = id
    isOpenModal.value = true
    isDetailModal.value = isDetail ?? false
}

const closeModal = () => {
    isOpenModal.value = false
}

const handleSuccessModifyModal = () => {
    fetchScreens()
    closeModal()
}

const debounceFetchScreens = debounce(fetchScreens, 300)

watch(
    () => [state.search.q, state.search.resolution, state.search.physicalSize, state.search.technology, state.search.panelType],
    () => {
        debounceFetchScreens()
    }
)

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

const handlePageChange = (page: number) => {
    state.pagination.page = page
}

const notification = useNotification()

const handleChangeStatus = async (id: string) => {
    const res = await changeScreenStatus(id)

    if(res.success) notification.success({content: 'Thay đổi trạng thái thành công', duration: 3000})
    else notification.error({content: 'Thay đổi trạng thái thất bại', duration: 3000})

    fetchScreens();
}
</script>

<style scoped>
.container {
    padding: 0 20px 20px 20px;
}

.mt-20px {
    margin-top: 20px;
}

.mb-20px {
    margin-bottom: 20px;
}

.pt-20px {
    padding-top: 20px;
}

.p-20px {
    padding: 20px;
}

.line {
    margin: 32px 0;
    border-top: 1px solid #a4a5a8;
}

.d-inline {
    display: inline;
}
</style>