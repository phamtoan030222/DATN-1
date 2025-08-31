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
                <span>IMEI</span>
                <n-input placeholder="Nhập giá trị imei" v-model:value="imei">
                </n-input>
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
import { Icon } from '@iconify/vue';
import { ref, Ref } from 'vue';

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

const handleClickOK = () => {
    const data = imei.value?.split(/[,;|]/)
    resetField()
    emit('update:imei', data, props.index)
}
</script>