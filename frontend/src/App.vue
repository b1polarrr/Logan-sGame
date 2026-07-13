<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppHeader from './components/AppHeader.vue'
import LoginView from './components/LoginView.vue'
import LobbyView from './components/LobbyView.vue'
import TableView from './components/TableView.vue'
import LogPanel from './components/LogPanel.vue'
import { useGameSocket } from './composables/useGameSocket'
import { isStoodUp } from './types/table'

const gameSocket = useGameSocket()

const {
  connected,
  authenticated,
  username,
  logs,
  showdownResult,
  rooms,
  currentRoomId,
  tableSnapshot,
  inTable,
  mySeatIndex,
  sessionToken,
  loginBusy,
  loginError,
  connect,
  disconnect,
  login,
  refreshRoomList,
  createRoom,
  joinRoom,
  sitDown,
  standUp,
  fold,
  check,
  call,
  raiseBet,
  rebuy,
  declineRebuy,
  ready,
  backToLobby,
} = gameSocket

const canLeaveToLobby = computed(() => {
  if (!tableSnapshot.value || mySeatIndex.value < 0) {
    return false
  }
  const player = tableSnapshot.value.players.find(
    (entry) => entry.seatIndex === mySeatIndex.value,
  )
  return player != null && isStoodUp(player)
})

const tableMaxSeats = computed(() => {
  const room = rooms.value.find((item) => item.roomId === currentRoomId.value)
  if (room) {
    return room.maxSeats
  }
  if (tableSnapshot.value) {
    const maxSeat = tableSnapshot.value.players.reduce(
      (max, player) => Math.max(max, player.seatIndex),
      0,
    )
    return maxSeat + 1
  }
  return 6
})

const tableBigBlind = computed(() => {
  const room = rooms.value.find((item) => item.roomId === currentRoomId.value)
  return room?.bigBlind ?? 20
})

async function handleCreateRoom(options: {
  maxSeats: number
  smallBlind: number
  bigBlind: number
}) {
  await createRoom(options)
  if (currentRoomId.value) {
    await joinRoom(currentRoomId.value)
  }
}

function handleLogin(payload: { userId: string; password: string }) {
  login(payload.userId, payload.password)
}

onMounted(() => {
  connect()
})
</script>

<template>
  <div class="app-shell">
    <AppHeader
      :connected="connected"
      :authenticated="authenticated"
      :username="username"
      :session-token="sessionToken"
      :show-back="authenticated && inTable && !!tableSnapshot"
      :back-enabled="canLeaveToLobby"
      @connect="connect"
      @disconnect="disconnect"
      @back-to-lobby="backToLobby"
    />

    <LoginView
      v-if="!authenticated"
      :connected="connected"
      :busy="loginBusy"
      :error-message="loginError"
      @connect="connect"
      @login="handleLogin"
    />

    <TableView
      v-else-if="inTable && tableSnapshot"
      :connected="connected"
      :snapshot="tableSnapshot"
      :my-seat-index="mySeatIndex"
      :max-seats="tableMaxSeats"
      :big-blind="tableBigBlind"
      :default-buy-in="1000"
      :showdown-result="showdownResult"
      @sit-down="sitDown"
      @stand-up="standUp"
      @fold="fold"
      @check="check"
      @call="call"
      @raise="raiseBet"
      @rebuy="rebuy"
      @decline-rebuy="declineRebuy"
      @ready="ready"
    />

    <LobbyView
      v-else
      :connected="connected"
      :rooms="rooms"
      @connect="connect"
      @refresh-room-list="refreshRoomList"
      @create-room="handleCreateRoom"
      @join-room="joinRoom"
    />

    <LogPanel :logs="logs" />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #121212;
  color: #eee;
}

.app-shell:has(.table-view) {
  background: #121212;
}
</style>
