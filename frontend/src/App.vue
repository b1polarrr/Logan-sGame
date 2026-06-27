<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppHeader from './components/AppHeader.vue'
import LobbyView from './components/LobbyView.vue'
import TableView from './components/TableView.vue'
import LogPanel from './components/LogPanel.vue'
import { useGameSocket } from './composables/useGameSocket'

const gameSocket = useGameSocket()

const {
  connected,
  logs,
  showdownResult,
  rooms,
  currentRoomId,
  tableSnapshot,
  inTable,
  mySeatIndex,
  sessionToken,
  connect,
  disconnect,
  refreshRoomList,
  createRoom,
  joinRoom,
  sitDown,
  fold,
  check,
  call,
  raiseBet,
  rebuy,
  declineRebuy,
  ready,
  backToLobby,
} = gameSocket

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
    await joinRoom(currentRoomId.value, 0)
  }
}

function updateCurrentRoomId(value: string) {
  currentRoomId.value = value
}

onMounted(() => {
  connect()
})
</script>

<template>
  <div class="app-shell">
    <AppHeader
      :connected="connected"
      :session-token="sessionToken"
      :show-back="inTable && !!tableSnapshot"
      @connect="connect"
      @disconnect="disconnect"
      @back-to-lobby="backToLobby"
    />

    <TableView
      v-if="inTable && tableSnapshot"
      :connected="connected"
      :snapshot="tableSnapshot"
      :my-seat-index="mySeatIndex"
      :max-seats="tableMaxSeats"
      :big-blind="tableBigBlind"
      :default-buy-in="1000"
      :showdown-result="showdownResult"
      @sit-down="sitDown"
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
      :current-room-id="currentRoomId"
      @connect="connect"
      @refresh-room-list="refreshRoomList"
      @create-room="handleCreateRoom"
      @join-room="joinRoom"
      @update:current-room-id="updateCurrentRoomId"
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
