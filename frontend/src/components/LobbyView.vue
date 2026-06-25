<script setup lang="ts">
import { ref } from 'vue'
import type { RoomInfo } from '../composables/useGameSocket'

const props = defineProps<{
  connected: boolean
  rooms: RoomInfo[]
  currentRoomId: string
}>()

const emit = defineEmits<{
  connect: []
  refreshRoomList: []
  createRoom: [options: { maxSeats: number; smallBlind: number; bigBlind: number }]
  joinRoom: [roomId: string, seatIndex: number]
  'update:currentRoomId': [value: string]
}>()

const wsHost = location.host
const maxSeats = ref(6)
const smallBlind = ref(10)
const bigBlind = ref(20)
const joinSeatIndex = ref(0)

function formatGameType(gameType: number): string {
  if (gameType === 1) return '德州扑克'
  return '未知'
}

function handleCreateRoom() {
  emit('createRoom', {
    maxSeats: maxSeats.value,
    smallBlind: smallBlind.value,
    bigBlind: bigBlind.value,
  })
}

function handleJoinCurrentRoom() {
  if (!props.currentRoomId.trim()) {
    return
  }
  emit('joinRoom', props.currentRoomId.trim(), joinSeatIndex.value)
}

function handleJoinRoom(roomId: string) {
  emit('joinRoom', roomId, joinSeatIndex.value)
}

function updateRoomId(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:currentRoomId', target.value)
}
</script>

<template>
  <main class="lobby">
    <section class="hero">
      <h2>游戏大厅</h2>
      <p class="hero-desc">
        创建或加入房间，至少两名玩家坐下后自动开局。
        <span class="ws-hint">网关 <code>ws://{{ wsHost }}/ws</code></span>
      </p>
      <div v-if="!connected" class="connect-banner">
        <p>请先点击右上角「连接服务器」，或</p>
        <button type="button" class="btn btn-primary" @click="emit('connect')">
          立即连接
        </button>
      </div>
    </section>

    <div class="lobby-grid">
      <section class="panel create-panel">
        <h3>创建房间</h3>
        <div class="form-grid">
          <label>
            <span>座位数</span>
            <input v-model.number="maxSeats" type="number" min="2" max="9" />
          </label>
          <label>
            <span>小盲</span>
            <input v-model.number="smallBlind" type="number" min="1" />
          </label>
          <label>
            <span>大盲</span>
            <input v-model.number="bigBlind" type="number" min="1" />
          </label>
        </div>
        <button
          type="button"
          class="btn btn-primary btn-block"
          :disabled="!connected"
          @click="handleCreateRoom"
        >
          创建德州房间
        </button>
      </section>

      <section class="panel join-panel">
        <h3>加入房间</h3>
        <div class="form-grid">
          <label class="wide">
            <span>房间号</span>
            <input
              :value="currentRoomId"
              type="text"
              placeholder="创建后自动填入"
              @input="updateRoomId"
            />
          </label>
          <label>
            <span>预选座位</span>
            <input v-model.number="joinSeatIndex" type="number" min="0" max="8" />
          </label>
        </div>
        <button
          type="button"
          class="btn btn-secondary btn-block"
          :disabled="!connected || !currentRoomId.trim()"
          @click="handleJoinCurrentRoom"
        >
          进入房间
        </button>
      </section>
    </div>

    <section class="panel room-list-panel">
      <div class="panel-head">
        <h3>房间列表</h3>
        <button
          type="button"
          class="btn btn-ghost btn-sm"
          :disabled="!connected"
          @click="emit('refreshRoomList')"
        >
          刷新
        </button>
      </div>

      <p v-if="rooms.length === 0" class="empty">暂无房间，创建一个新房间或点击刷新</p>

      <div v-else class="room-cards">
        <article v-for="room in rooms" :key="room.roomId" class="room-card">
          <div class="room-card-head">
            <span class="room-id">#{{ room.roomId }}</span>
            <span class="game-badge">{{ formatGameType(room.gameType) }}</span>
          </div>
          <div class="room-stats">
            <div class="stat">
              <span class="label">玩家</span>
              <span class="value">{{ room.seatedCount }} / {{ room.maxSeats }}</span>
            </div>
            <div class="stat">
              <span class="label">盲注</span>
              <span class="value">{{ room.smallBlind }} / {{ room.bigBlind }}</span>
            </div>
          </div>
          <button
            type="button"
            class="btn btn-primary btn-block"
            :disabled="!connected"
            @click="handleJoinRoom(room.roomId)"
          >
            进入
          </button>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.lobby {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.hero {
  margin-bottom: 28px;
}

.hero h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #f3f4f6;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
  line-height: 1.6;
}

.ws-hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
}

.ws-hint code {
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 11px;
}

.connect-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(234, 179, 8, 0.08);
  border: 1px solid rgba(234, 179, 8, 0.2);
  color: #fbbf24;
  font-size: 14px;
}

.connect-banner p {
  margin: 0;
}

.lobby-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 640px) {
  .lobby-grid {
    grid-template-columns: 1fr;
  }
}

.panel {
  padding: 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.panel h3 {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #e5e7eb;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.form-grid .wide {
  grid-column: span 2;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

label span {
  font-size: 12px;
  color: #6b7280;
}

label input {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.25);
  color: #f3f4f6;
  font-size: 14px;
  font-family: inherit;
}

label input:focus {
  outline: none;
  border-color: #22c55e;
}

.btn {
  padding: 10px 18px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, opacity 0.15s;
}

.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn-block {
  width: 100%;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, #166534, #22c55e);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #15803d, #16a34a);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.08);
  color: #e5e7eb;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.12);
}

.btn-ghost {
  background: transparent;
  color: #9ca3af;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-ghost:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.05);
  color: #d1d5db;
}

.empty {
  margin: 0;
  padding: 32px;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}

.room-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.room-card {
  padding: 16px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.room-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.room-id {
  font-size: 16px;
  font-weight: 600;
  color: #f3f4f6;
}

.game-badge {
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  font-size: 11px;
}

.room-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat .label {
  font-size: 11px;
  color: #6b7280;
}

.stat .value {
  font-size: 14px;
  color: #d1d5db;
  font-weight: 500;
}
</style>
