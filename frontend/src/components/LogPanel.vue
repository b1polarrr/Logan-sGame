<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  logs: string[]
}>()

const expanded = ref(false)
</script>

<template>
  <section class="log-panel">
    <button type="button" class="log-toggle" @click="expanded = !expanded">
      <span>调试日志</span>
      <span class="count">{{ logs.length }}</span>
      <span class="chevron" :class="{ open: expanded }">▼</span>
    </button>
    <pre v-show="expanded" class="log-output">{{ logs.join('\n') || '暂无日志' }}</pre>
  </section>
</template>

<style scoped>
.log-panel {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 24px;
}

.log-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.25);
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}

.log-toggle:hover {
  background: rgba(0, 0, 0, 0.35);
  color: #d1d5db;
}

.count {
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
}

.chevron {
  margin-left: auto;
  font-size: 10px;
  transition: transform 0.2s;
}

.chevron.open {
  transform: rotate(180deg);
}

.log-output {
  margin: 8px 0 0;
  padding: 12px;
  min-height: 80px;
  max-height: 180px;
  overflow-y: auto;
  background: #0a0a12;
  color: #9ca3af;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 11px;
  white-space: pre-wrap;
}
</style>
