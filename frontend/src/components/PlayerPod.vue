<script setup lang="ts">
import { computed } from 'vue'
import PlayingCard from './PlayingCard.vue'
import { formatChips } from '../utils/chips'
import { isHiddenCard } from '../utils/cards'
import type { PlayerState } from '../types/table'
import { isAllIn, isFolded, isStoodUp } from '../types/table'

const props = defineProps<{
  player: PlayerState | null
  seatIndex: number
  bigBlind: number
  isMe: boolean
  isActive: boolean
  isDealer: boolean
  isSmallBlind: boolean
  isBigBlind: boolean
  holeCards: string[]
  showCards: boolean
  handTypeLabel?: string
  showReadyStatus?: boolean
  isStillInHand?: boolean
  isWinner?: boolean
}>()

const emit = defineEmits<{
  sitDown: []
}>()

const avatarColor = computed(() => {
  if (!props.player) return '#4b5563'
  const hash = props.player.username
    .split('')
    .reduce((sum, char) => sum + char.charCodeAt(0), 0)
  const hues = [210, 280, 160, 30, 340, 190]
  return `hsl(${hues[hash % hues.length]}, 55%, 45%)`
})

const avatarInitial = computed(() => {
  if (!props.player) return '?'
  return props.player.username.replace(/^玩家_/, '').charAt(0).toUpperCase() || 'P'
})

const chipLabel = computed(() => {
  if (!props.player) return ''
  return formatChips(props.player.chips, props.bigBlind)
})
</script>

<template>
  <div
    class="player-pod"
    :class="{
      empty: !player,
      mine: isMe,
      active: isActive,
      folded: player != null && isFolded(player),
      stood: player != null && isStoodUp(player),
      offline: player && !player.isOnline,
    }"
  >
    <div
      v-if="showCards && player && (!isFolded(player) || isMe)"
      class="hole-cards"
    >
      <PlayingCard
        v-for="(card, index) in holeCards"
        :key="index"
        :card="card"
        :hidden="isHiddenCard(card)"
        :size="isMe ? 'hero' : 'sm'"
        :class="{ tilted: isMe }"
      />
    </div>

    <template v-if="player">
      <div
        class="avatar-wrap"
        :class="{
          glowing: isActive,
          folded: isFolded(player),
          winner: isWinner,
        }"
      >
        <div v-if="isDealer" class="dealer-button">D</div>
        <div v-if="isSmallBlind" class="blind-button sb-button">SB</div>
        <div v-if="isBigBlind" class="blind-button bb-button">BB</div>
        <div v-if="isWinner" class="fireworks" aria-hidden="true">
          <span v-for="particle in 12" :key="particle" class="firework-particle" />
        </div>
        <div class="avatar" :style="{ background: avatarColor }">
          {{ avatarInitial }}
        </div>
        <div v-if="isActive" class="timer-ring" />
      </div>

      <div class="info-plate">
        <div class="name-row">
          <span class="name">{{ player.username }}</span>
          <span v-if="showReadyStatus && player.isReady" class="badge ready">已准备</span>
          <span v-else-if="showReadyStatus && player.chips > 0 && !isStoodUp(player)" class="badge not-ready">未准备</span>
          <span v-else-if="isStoodUp(player)" class="badge stood">起身</span>
          <span v-else-if="isFolded(player)" class="badge folded">弃牌</span>
          <span v-else-if="isAllIn(player) && isStillInHand" class="badge all-in">全下</span>
          <span v-else-if="player.chips === 0 && player.willRebuy" class="badge rebuying">补码中</span>
          <span v-else-if="player.chips === 0 && !player.willRebuy" class="badge rebuy-next">下把玩</span>
          <span v-else-if="!player.isOnline" class="badge offline">离线</span>
        </div>
        <div class="chips-plate">{{ chipLabel }}</div>
        <div v-if="handTypeLabel" class="hand-type-plate">{{ handTypeLabel }}</div>
      </div>
    </template>

    <template v-else>
      <button type="button" class="sit-btn" @click="emit('sitDown')">坐下</button>
    </template>
  </div>
</template>

<style scoped>
.player-pod {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 100px;
}

.player-pod.empty {
  opacity: 0.85;
}

.player-pod.folded .info-plate {
  opacity: 0.75;
}

.player-pod.stood .info-plate {
  opacity: 0.8;
}

.dealer-button,
.blind-button {
  position: absolute;
  z-index: 4;
  height: 22px;
  min-width: 22px;
  padding: 0 5px;
  border-radius: 11px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: -0.2px;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
  pointer-events: none;
}

.dealer-button {
  top: -8px;
  right: -10px;
  width: 22px;
  padding: 0;
  border-radius: 50%;
  background: #f1c40f;
  color: #1a1a1a;
  font-size: 11px;
}

.sb-button {
  top: -8px;
  left: -10px;
  background: #3498db;
  color: #fff;
}

.bb-button {
  bottom: -8px;
  left: -10px;
  background: #e67e22;
  color: #fff;
}

.player-pod.mine .hole-cards {
  margin-bottom: -14px;
  transform: translateY(-10px);
}

.hole-cards {
  display: flex;
  justify-content: center;
  margin-bottom: -8px;
  z-index: 2;
}

.hole-cards :deep(.playing-card.sm) {
  margin-left: -6px;
}

.hole-cards :deep(.playing-card.sm:first-child) {
  margin-left: 0;
  transform: rotate(-6deg);
}

.hole-cards :deep(.playing-card.sm:last-child) {
  transform: rotate(6deg);
}

.hole-cards :deep(.playing-card.hero) {
  margin-left: -10px;
}

.hole-cards :deep(.playing-card.hero:first-child) {
  margin-left: 0;
  transform: rotate(-8deg);
}

.hole-cards :deep(.playing-card.hero:last-child) {
  transform: rotate(8deg);
}

.avatar-wrap {
  position: relative;
  overflow: visible;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.35);
}

.player-pod.mine .avatar {
  width: 56px;
  height: 56px;
  font-size: 20px;
  border-color: rgba(52, 152, 219, 0.5);
}

.avatar-wrap.folded .avatar {
  filter: grayscale(1);
  opacity: 0.55;
  border-color: rgba(255, 255, 255, 0.08);
}

.avatar-wrap.winner .avatar {
  border-color: #f1c40f;
  box-shadow: 0 0 20px rgba(241, 196, 15, 0.65);
}

.avatar-wrap.glowing .avatar {
  border-color: #2ecc71;
  box-shadow: 0 0 16px rgba(46, 204, 113, 0.55);
}

.fireworks {
  position: absolute;
  inset: -18px;
  pointer-events: none;
}

.firework-particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 6px;
  height: 6px;
  margin: -3px;
  border-radius: 50%;
  background: #f1c40f;
  opacity: 0;
  animation: firework-burst 1.4s ease-out infinite;
}

.firework-particle:nth-child(3n) {
  background: #e74c3c;
}

.firework-particle:nth-child(3n + 1) {
  background: #3498db;
}

.firework-particle:nth-child(3n + 2) {
  background: #2ecc71;
}

.firework-particle:nth-child(1) {
  animation-delay: 0s;
  --angle: 0deg;
}

.firework-particle:nth-child(2) {
  animation-delay: 0.15s;
  --angle: 30deg;
}

.firework-particle:nth-child(3) {
  animation-delay: 0.3s;
  --angle: 60deg;
}

.firework-particle:nth-child(4) {
  animation-delay: 0.45s;
  --angle: 90deg;
}

.firework-particle:nth-child(5) {
  animation-delay: 0.6s;
  --angle: 120deg;
}

.firework-particle:nth-child(6) {
  animation-delay: 0.75s;
  --angle: 150deg;
}

.firework-particle:nth-child(7) {
  animation-delay: 0.2s;
  --angle: 180deg;
}

.firework-particle:nth-child(8) {
  animation-delay: 0.35s;
  --angle: 210deg;
}

.firework-particle:nth-child(9) {
  animation-delay: 0.5s;
  --angle: 240deg;
}

.firework-particle:nth-child(10) {
  animation-delay: 0.65s;
  --angle: 270deg;
}

.firework-particle:nth-child(11) {
  animation-delay: 0.8s;
  --angle: 300deg;
}

.firework-particle:nth-child(12) {
  animation-delay: 0.95s;
  --angle: 330deg;
}

@keyframes firework-burst {
  0% {
    opacity: 1;
    transform: rotate(var(--angle)) translateY(0) scale(1);
  }
  70% {
    opacity: 0.85;
    transform: rotate(var(--angle)) translateY(-28px) scale(0.6);
  }
  100% {
    opacity: 0;
    transform: rotate(var(--angle)) translateY(-36px) scale(0.2);
  }
}

.timer-ring {
  position: absolute;
  inset: -4px;
  border-radius: 14px;
  border: 2px solid transparent;
  border-top-color: #2ecc71;
  border-right-color: #2ecc71;
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.info-plate {
  min-width: 110px;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 5px 10px;
  font-size: 12px;
  color: #e5e7eb;
}

.name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
}

.badge.rebuy-next {
  background: rgba(52, 152, 219, 0.2);
  color: #3498db;
}

.badge.rebuying {
  background: rgba(241, 196, 15, 0.22);
  color: #f1c40f;
}

.badge.ready {
  background: rgba(46, 204, 113, 0.25);
  color: #2ecc71;
}

.badge.not-ready {
  background: rgba(149, 165, 166, 0.15);
  color: #7f8c8d;
}

.badge.folded {
  background: rgba(231, 76, 60, 0.25);
  color: #e74c3c;
}

.badge.stood {
  background: rgba(155, 89, 182, 0.22);
  color: #bb8fce;
}

.badge.all-in {
  background: rgba(241, 196, 15, 0.2);
  color: #f1c40f;
}

.badge.offline {
  background: rgba(149, 165, 166, 0.2);
  color: #95a5a6;
}

.chips-plate {
  padding: 4px 10px 6px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #3498db;
  background: rgba(0, 0, 0, 0.35);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.hand-type-plate {
  padding: 3px 10px 5px;
  text-align: center;
  font-size: 11px;
  font-weight: 600;
  color: #5dade2;
  background: rgba(52, 152, 219, 0.12);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.sit-btn {
  padding: 8px 20px;
  border: 1px dashed rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: #95a5a6;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}

.sit-btn:hover {
  border-color: #2ecc71;
  color: #2ecc71;
  background: rgba(46, 204, 113, 0.1);
}
</style>
