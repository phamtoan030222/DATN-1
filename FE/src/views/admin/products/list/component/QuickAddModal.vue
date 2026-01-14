<template>
    <n-modal :show="isOpen">
        <n-card style="width: 30%" :title="getTextByProductPropertiesType" :bordered="false" size="huge" role="dialog"
            aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>

            <!-- content -->
            <div>
                <div>
                    <n-input v-model:value="data" placeholder="Nhập giá trị" />
                </div>

                <div v-if="type === ProductPropertiesType.COLOR" class="w-100px mt-5">
                    <span>Chọn mã màu</span>
                    <n-color-picker v-model:value="hexColor" placeholder="Chọn màu sắc" type="color" :modes="['hex']"
                        :show-alpha="false">
                    </n-color-picker>
                </div>
            </div>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                        :positive-text="'Xác nhận'" :negative-text="'Hủy'">
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

<script lang="ts" setup>
import { ProductPropertiesType, translateProperty } from '@/constants/ProductPropertiesType';
import { quickAddProperties } from '@/service/api/admin/product/productDetail.api';
import { Icon } from '@iconify/vue';

const props = defineProps<{
    isOpen: boolean,
    type: ProductPropertiesType | undefined
}>()

const emit = defineEmits(['close', 'success'])

const handleClickCancel = () => {
    clearData();
    emit('close');
}

const hexColor = ref<string | null>('#FFFFFF');

const notification = useNotification();

const handleClickOK = async () => {
    if (!data.value || !props.type) {
        notification.error({ content: `Vui lòng nhập giá trị ${translateProperty(props.type as ProductPropertiesType).toLowerCase()}`, duration: 3000 })
        return;
    }

    try {
        if (props.type === ProductPropertiesType.COLOR) {
            await quickAddProperties(data.value, props.type, hexColor.value || undefined);
        } else {
            await quickAddProperties(data.value, props.type);
        }

        notification.success({ content: `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType).toLowerCase()} thành công`, duration: 3000 })
        emit('success');
        handleClickCancel();
    } catch (error: any) {
        console.log(error);
        if (error.status === 409) {
            notification.error({ content: `Giá trị ${translateProperty(props.type as ProductPropertiesType).toLowerCase()} đã tồn tại`, duration: 3000 })
        } else if (error.status === 404) {
            notification.error({ content: `Thêm nhanh ${translateProperty(props.type as ProductPropertiesType).toLowerCase()} thất bại`, duration: 3000 })
        }
    }
    clearData();
}
const clearData = () => {
    data.value = '';
}

const data = ref<string>('');

const getTextByProductPropertiesType = computed(() => {
    return translateProperty(props.type as ProductPropertiesType);
})
</script>
