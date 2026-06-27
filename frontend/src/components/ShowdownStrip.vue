<script setup lang="ts">
import { computed } from 'vue'
import { formatChips } from '../utils/chips'
import type { ShowdownResult } from '../types/table'

const props = defineProps<{
  result: ShowdownResult
  bigBlind: number
}>()

const title = computed(() =>
  props.result.reason === 'fold' ? '对手弃牌' : '摊牌比大小',
)

const potLabel = computed(() => formatChips(props.result.potTotal, props.bigBlind))
</script>

<template>
  <div class="showdown-strip">
    <div class="strip-head">
      <span class="strip-title">{{ title }}</span>
      <span class="strip-pot">底池 {{ potLabel }}</span>
    </div>
    <ul class="strip-players">
      <li
        v-for="player in result.players"
        :key="player.seatIndex"
        class="strip-player"
        :class="{ winner: player.isWinner }"
      >
        <span class="name">{{ player.username }}</span>
        <span v-if="player.handTypeName" class="hand-type">{{ player.handTypeName }}</span>
        <span v-if="player.isWinner" class="win-amount">
          +{{ formatChips(player.chipsWon, bigBlind) }}
        </span>
      </li>
    </ul>
    <p class="strip-hint">10 秒后下一局</p>
  </div>
</template>

<style scoped>
.showdown-strip {
  margin-top: 4px;
  padding: 8px 14px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid rgba(241, 196, 15, 0.35);
  max-width: 100%;
}

.strip-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 6px;
}

.strip-title {
  font-size: 13px;
  font-weight: 700;
  color: #f1c40f;
}

.strip-pot {
  font-size: 13px;
  color: #ecf0f1;
}

.strip-players {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 16px;
}

.strip-player {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #d1d5db;
}

.strip-player.winner {
  color: #2ecc71;
  font-weight: 600;
}

.hand-type {
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(52, 152, 219, 0.2);
  color: #5dade2;
  font-size: 11px;
}

.win-amount {
  color: #2ecc71;
  font-weight: 700;
}

.strip-hint {
  margin: 6px 0 0;
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
}
</style>
