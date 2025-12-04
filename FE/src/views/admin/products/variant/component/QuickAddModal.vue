<template>
    <n-modal :show="isOpen">
        <n-card style="width: 50%" :title="getTextByProductPropertiesType" :bordered="false" size="huge" role="dialog"
            aria-modal="true">
            <template #header-extra>
                <n-button @click="handleClickCancel">
                    <Icon icon="ic:outline-close" />
                </n-button>
            </template>

            <!-- content -->
            <div :style="{  }">
                
            </div>

            <!-- footer -->
            <template #footer>
                <n-space justify="end">
                    <n-button @click="handleClickCancel">Hủy</n-button>
                    <n-popconfirm @positive-click="handleClickOK" @negative-click="handleClickCancel"
                        :positive-button-props="{ type: 'info' }">
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

const props = defineProps<{
    isOpen: boolean,
    type: ProductPropertiesType
}>()

const emit = defineEmits(['close', 'success'])

const handleClickCancel = () => emit('close')

const handleClickOK = async () => {

}

const getTextByProductPropertiesType = computed(() => {
    return translateProperty(props.type)
})
</script>