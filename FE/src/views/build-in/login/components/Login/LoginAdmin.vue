<script setup lang="ts">
import { URL_OAUTH2_GOOGLE_ADMIN, VITE_BASE_URL_CLIENT } from "@/constants/url";
import { postLogin } from "@/service/api/auth/auth.api";
import { local } from "@/utils";
import type { FormInst } from "naive-ui";

const emit = defineEmits(["update:modelValue"]);

function toOtherForm(type: any) {
    emit("update:modelValue", type);
}

const { t } = useI18n();
const rules = computed(() => {
    return {
        account: {
            required: true,
            trigger: "blur",
            message: t("login.accountRuleTip"),
        },
        pwd: {
            required: true,
            trigger: "blur",
            message: t("login.passwordRuleTip"),
        },
    };
});
const formValue = ref({
    account: "",
    pwd: "",
});
const isRemember = ref(false);
const isLoading = ref(false);

const notication = useNotification();

const formRef = ref<FormInst | null>(null);
function handleLogin() {
    formRef.value?.validate(async (errors) => {
        if (errors) return;

        isLoading.value = true;
        const { account, pwd } = formValue.value;

        if (isRemember.value) local.set("loginAccount", { account, pwd });
        else local.remove("loginAccount");
        try {
            const token = await postLogin(account, pwd, "ADMIN");
            if (token) {
                window.location.href = VITE_BASE_URL_CLIENT + "/redirect?state=" + token.state;
            }
        } catch (error) {
            notication.error({ content: "Đăng nhập thất bại. Vui lòng kiểm tra lại tài khoản hoặc mật khẩu." });
        }
        isLoading.value = false;
    });
}
onMounted(() => {
    checkUserAccount();
});
function checkUserAccount() {
    const loginAccount = local.get("loginAccount");
    if (!loginAccount) return;

    formValue.value = loginAccount;
    isRemember.value = true;
}


const appName = import.meta.env.VITE_APP_NAME

const handleRedirectLoginADMIN = () => {
    window.location.href = URL_OAUTH2_GOOGLE_ADMIN;
};

</script>

<template>
    <n-el class="wh-full flex-center" style="background-color: var(--body-color);">
        <div class="fixed top-40px right-40px text-lg">
            <DarkModeSwitch />
            <LangsSwitch />
        </div>
        <div class="p-4xl h-full w-full sm:w-450px sm:h-unset"
            style="background: var(--card-color);box-shadow: var(--box-shadow-1);">
            <div class="w-full flex flex-col items-center">
                <SvgIconsLogo class="text-6em" />
                <n-h3>{{ appName }} </n-h3>
                <transition name="fade-slide" mode="out-in">
                    <div>
                        <n-h2 depth="3" class="text-center">
                            {{ $t("login.signInTitle") }}
                        </n-h2>
                        <n-form ref="formRef" :rules="rules" :model="formValue" :show-label="false" size="large">
                            <n-form-item path="account">
                                <n-input v-model:value="formValue.account" clearable
                                    :placeholder="$t('login.accountPlaceholder')" />
                            </n-form-item>
                            <n-form-item path="pwd">
                                <n-input v-model:value="formValue.pwd" type="password"
                                    :placeholder="$t('login.passwordPlaceholder')" clearable show-password-on="click">
                                    <template #password-invisible-icon>
                                        <icon-park-outline-preview-close-one />
                                    </template>
                                    <template #password-visible-icon>
                                        <icon-park-outline-preview-open />
                                    </template>
                                </n-input>
                            </n-form-item>
                            <n-space vertical :size="20">
                                <div class="flex-y-center justify-between">
                                    <n-checkbox v-model:checked="isRemember">
                                        {{ $t("login.rememberMe") }}
                                    </n-checkbox>
                                    <n-button type="primary" text @click="toOtherForm('resetPwd')">
                                        {{ $t("login.forgotPassword") }}
                                    </n-button>
                                </div>
                                <n-button block type="primary" size="large" :loading="isLoading" :disabled="isLoading"
                                    @click="handleLogin">
                                    {{ $t("login.signIn") }}
                                </n-button>
                                <n-flex>
                                    <n-text>{{ $t("login.noAccountText") }}</n-text>
                                    <n-button type="primary" text @click="toOtherForm('register')">
                                        {{ $t("login.signUp") }}
                                    </n-button>
                                </n-flex>
                            </n-space>
                        </n-form>
                        <n-divider>
                            <span op-80>{{ $t("login.or") }}</span>
                        </n-divider>
                        <n-space justify="center">
                            <n-button @click="handleRedirectLoginADMIN" circle>
                                <template #icon>
                                    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                                        viewBox="0 0 1024 1024">
                                        <path
                                            d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448s448-200.6 448-448S759.4 64 512 64zm167 633.6C638.4 735 583 757 516.9 757c-95.7 0-178.5-54.9-218.8-134.9C281.5 589 272 551.6 272 512s9.5-77 26.1-110.1c40.3-80.1 123.1-135 218.8-135c66 0 121.4 24.3 163.9 63.8L610.6 401c-25.4-24.3-57.7-36.6-93.6-36.6c-63.8 0-117.8 43.1-137.1 101c-4.9 14.7-7.7 30.4-7.7 46.6s2.8 31.9 7.7 46.6c19.3 57.9 73.3 101 137 101c33 0 61-8.7 82.9-23.4c26-17.4 43.2-43.3 48.9-74H516.9v-94.8h230.7c2.9 16.1 4.4 32.8 4.4 50.1c0 74.7-26.7 137.4-73 180.1z"
                                            fill="currentColor"></path>
                                    </svg>
                                </template>
                            </n-button>
                            <!-- <n-button circle>
        <template #icon>
          <n-icon><icon-park-outline-tencent-qq /></n-icon>
        </template>
      </n-button>
      <n-button circle>
        <template #icon>
          <n-icon><icon-park-outline-github-one /></n-icon>
        </template>
      </n-button> -->
                        </n-space>
                    </div>
                </transition>
            </div>
        </div>

        <div />
    </n-el>
</template>

<style scoped></style>
