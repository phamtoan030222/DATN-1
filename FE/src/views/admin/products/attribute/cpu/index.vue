<template>
    <div>
        <n-card title="CPU">
            <n-grid x-gap="12" :cols="11">
                <n-grid-item span="2">
                    <span>Tìm kiếm</span>
                    <n-input v-model:value="state.search.q" placeholder="Tìm kiếm" />
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Hãng</span>
                    <n-select v-model:value="state.search.brand" :options="brandOptionsSelect"></n-select>
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Thế hệ</span>
                    <n-input v-model:value="state.search.generation" placeholder="Nhập thế hệ" />
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Năm phát hành</span>
                    <n-date-picker v-model:value="state.search.releaseYear" type="year" clearable />
                </n-grid-item>
                <n-grid-item span="2">
                    <span>Dòng sản phẩm</span>
                    <n-input v-model:value="state.search.series" placeholder="Nhập thế hệ" />
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
            <div class="line"></div>
            <n-data-table :columns="columns" :data="state.data.cpus" :pagination="paginationReactive" :bordered="false">F
                <template #action="{row}">
                </template>
            </n-data-table>
        </n-card>

    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, Ref, ref, watch } from 'vue'
import { debounce } from 'lodash'
import { ADProductCPUResponse, getCPUs } from '@/service/api/admin/product/cpu.api'
import { DataTableColumns } from 'naive-ui'

const state = reactive({
    search: {
        q: '',
        brand: '',
        generation: '',
        series: '',
        releaseYear: null as number | null,
    },
    data: {
        cpus: [] as ADProductCPUResponse[],
    },
    pagination: {
        page: 1,
        size: 10,
        totalPages: undefined as number | undefined,
    },
})

const fetchCPUs = async () => {
    const res = await getCPUs({
        page: state.pagination.page,
        size: state.pagination.size,
        q: state.search.q,
        brand: state.search.brand,
        releaseYear: state.search.releaseYear,
        series: state.search.series,
        generation: state.search.generation,
    })

    console.table(res.data.data)

    state.data.cpus = res.data.data
    state.pagination.totalPages = res.data.totalPages
}

const refreshFilter = () => {
    state.search.q = ''
    state.search.brand = ''
    state.search.generation = ''
    state.search.series = ''
    state.search.releaseYear = null
}

const columns: DataTableColumns<ADProductCPUResponse> = [
    { type: 'selection', fixed: 'left' },
    { title: '#', key: 'orderNumber', width: 50, fixed: 'left', },
    { title: 'Mã', key: 'code', width: 100, fixed: 'left', },
    { title: 'Tên CPU', key: 'name', width: 150, fixed: 'left', },
    { title: 'Thế hệ', key: 'generation', width: 150, align: 'center', },
    { title: 'Hãng', key: 'brand', width: 150, align: 'center', },
    { title: 'Năm phát hàng', key: 'releaseYear', width: 150, align: 'center', },
    { title: 'Dòng CPU', key: 'series', width: 150, align: 'center', },
    { title: '', key: 'action', width: 100, fixed: 'right', },
]

onMounted(() => {
    fetchCPUs()
})

const isOpenModal = ref<boolean>(false)

const isDetailModal: Ref<boolean> = ref(true)

const cpuIDSelected = ref<string>()

const clickOpenModal = (id?: string, isDetail?: boolean) => {
    console.log(id)
    cpuIDSelected.value = id
    isOpenModal.value = true
    isDetailModal.value = isDetail ?? false
}

const closeModal = () => {
    isOpenModal.value = false
}

const handleSuccessModifyModal = () => {
    fetchCPUs()
    closeModal()
}

const debounceFetchCPUs = debounce(fetchCPUs, 300)

watch(
    () => [state.search.q, state.search.brand, state.search.releaseYear, state.search.generation, state.search.releaseYear],
    () => {
        debounceFetchCPUs()
    }
)
// configuration select
const brandOptionsSelect = [
    { label: 'Intel', value: 'Intel' },
    { label: 'AMD', value: 'AMD' },
    { label: 'Apple', value: 'Apple' },
]

const paginationReactive = reactive({
    page: 2,
    pageSize: 5,
    showSizePicker: true,
    pageSizes: [3, 5, 7],
    onChange: (page: number) => {
        paginationReactive.page = page
    },
    onUpdatePageSize: (pageSize: number) => {
        paginationReactive.pageSize = pageSize
        paginationReactive.page = 1
    }
})
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