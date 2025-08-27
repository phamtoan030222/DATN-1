<script setup lang="ts">
import type { ToolbarNames } from "md-editor-v3";

import { useAppStore } from "@/store";

import { MdEditor } from "md-editor-v3";
import "md-editor-v3/lib/style.css";

const model = defineModel<string>();

const appStore = useAppStore();

const toolbarsExclude: ToolbarNames[] = [
  "mermaid",
  "katex",
  "github",
  "htmlPreview",
  "catalog",
];
</script>

<template>
  <MdEditor
    v-model="model"
    :theme="appStore.colorMode"
    :toolbars-exclude="toolbarsExclude"
  />
</template>

<style scoped></style>
