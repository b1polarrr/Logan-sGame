<script setup lang="ts">
defineProps<{
  connected: boolean
  sessionToken?: string
  showBack?: boolean
}>()

defineEmits<{
  connect: []
  disconnect: []
  backToLobby: []
}>()
</script>

<template>
  <header class="app-header">
    <div class="brand">
      <span class="logo">♠</span>
      <div>
        <h1>Mercury Poker</h1>
        <p class="subtitle">德州扑克</p>
      </div>
    </div>

    <div class="header-actions">
      <div class="connection">
        <span class="dot" :class="connected ? 'online' : 'offline'" />
        <span>{{ connected ? '已连接' : '未连接' }}</span>
        <span v-if="sessionToken" class="token" :title="sessionToken">
          Session {{ sessionToken.slice(0, 8) }}…
        </span>
      </div>

      <template v-if="showBack">
        <button type="button" class="btn btn-ghost" @click="$emit('backToLobby')">
          返回大厅
        </button>
      </template>
      <template v-else>
        <button
          v-if="!connected"
          type="button"
          class="btn btn-primary"
          @click="$emit('connect')"
        >
          连接服务器
        </button>
        <button v-else type="button" class="btn btn-ghost" @click="$emit('disconnect')">
          断开
        </button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(0, 0, 0, 0.2);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #166534, #22c55e);
  font-size: 22px;
  color: #fff;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.25);
}

.brand h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #f3f4f6;
  letter-spacing: 0.02em;
}

.subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.connection {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #9ca3af;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.online {
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.6);
}

.dot.offline {
  background: #ef4444;
}

.token {
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 11px;
  font-family: Consolas, monospace;
}

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, transform 0.1s;
}

.btn:active {
  transform: scale(0.98);
}

.btn-primary {
  background: linear-gradient(135deg, #166534, #22c55e);
  color: #fff;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #15803d, #16a34a);
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.06);
  color: #d1d5db;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.1);
}
</style>
