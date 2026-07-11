<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  connected: boolean
  busy?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  connect: []
  login: [payload: { userId: string; password: string }]
}>()

const userId = ref('')
const password = ref('')

function handleSubmit() {
  const trimmedUserId = userId.value.trim()
  if (!trimmedUserId || !password.value) {
    return
  }
  emit('login', { userId: trimmedUserId, password: password.value })
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <h2>登录</h2>

      <div v-if="!connected" class="connect-banner">
        <p>尚未连接服务器</p>
        <button type="button" class="btn btn-primary" @click="emit('connect')">
          连接服务器
        </button>
      </div>

      <form v-else class="login-form" @submit.prevent="handleSubmit">
        <label>
          <span>账号 ID</span>
          <input
            v-model="userId"
            type="text"
            autocomplete="username"
            maxlength="64"
            required
          />
        </label>
        <label>
          <span>密码</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            maxlength="64"
            required
          />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <button
          type="submit"
          class="btn btn-primary btn-block"
          :disabled="busy || !userId.trim() || password.length < 4"
        >
          {{ busy ? '登录中…' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  max-width: 420px;
  margin: 0 auto;
  padding: 48px 24px;
}

.login-card {
  padding: 28px 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.login-card h2 {
  margin: 0 0 24px;
  font-size: 24px;
  font-weight: 600;
}

.connect-banner {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.connect-banner p {
  margin: 0;
  color: #d1d5db;
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #9ca3af;
}

input {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.35);
  color: #f3f4f6;
  font-size: 14px;
  font-family: inherit;
}

input:focus {
  outline: none;
  border-color: rgba(34, 197, 94, 0.55);
}

.error {
  margin: 0;
  font-size: 13px;
  color: #f87171;
}

.btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #166534, #22c55e);
  color: #fff;
}

.btn-block {
  width: 100%;
}
</style>
