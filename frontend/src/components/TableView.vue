<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import PlayingCard from './PlayingCard.vue'
import PlayerPod from './PlayerPod.vue'
import ChipStack from './ChipStack.vue'
import ShowdownStrip from './ShowdownStrip.vue'
import { formatChips } from '../utils/chips'
import { isHiddenCard } from '../utils/cards'
import type { ShowdownResult, TableSnapshot } from '../types/table'

const props = defineProps<{
  connected: boolean
  snapshot: TableSnapshot
  mySeatIndex: number
  maxSeats: number
  bigBlind: number
  defaultBuyIn?: number
  showdownResult?: ShowdownResult | null
}>()

const emit = defineEmits<{
  sitDown: [seatIndex: number]
  fold: []
  check: []
  call: []
  raise: [amount: number]
  rebuy: [amount: number]
  ready: []
}>()

const rebuyDismissed = ref(false)
const handEverStarted = ref(false)

const isSeated = computed(() => props.mySeatIndex >= 0)

const playersWithChips = computed(() =>
  props.snapshot.players.filter((player) => player.chips > 0),
)

const needsReadyBeforeFirstHand = computed(
  () =>
    isSeated.value &&
    !handEverStarted.value &&
    props.snapshot.currentTurnIndex < 0 &&
    !props.showdownResult &&
    playersWithChips.value.length >= 2,
)

const canClickReady = computed(
  () =>
    needsReadyBeforeFirstHand.value &&
    myPlayer.value != null &&
    myPlayer.value.chips > 0 &&
    !myPlayer.value.isReady,
)

const waitingOthersReady = computed(
  () =>
    needsReadyBeforeFirstHand.value &&
    myPlayer.value != null &&
    myPlayer.value.isReady,
)

watch(
  () => props.snapshot.currentTurnIndex,
  (turnIndex) => {
    if (turnIndex >= 0) {
      handEverStarted.value = true
    }
  },
  { immediate: true },
)

watch(
  () => props.snapshot.roomId,
  () => {
    handEverStarted.value = false
    rebuyDismissed.value = false
  },
)

const myPlayer = computed(() =>
  props.snapshot.players.find((player) => player.seatIndex === props.mySeatIndex),
)

const buyInAmount = computed(() => props.defaultBuyIn ?? 1000)

const showRebuyModal = computed(
  () =>
    isSeated.value &&
    myPlayer.value != null &&
    myPlayer.value.chips === 0 &&
    !rebuyDismissed.value &&
    props.snapshot.currentTurnIndex < 0 &&
    !props.showdownResult,
)

const profitRows = computed(() =>
  [...props.snapshot.players].sort((left, right) => left.seatIndex - right.seatIndex),
)

function formatProfit(profit: number): string {
  const label = formatChips(Math.abs(profit), props.bigBlind)
  if (profit > 0) return `+${label}`
  if (profit < 0) return `-${label}`
  return '0'
}

function profitClass(profit: number): string {
  if (profit > 0) return 'profit-positive'
  if (profit < 0) return 'profit-negative'
  return 'profit-even'
}

const isMyTurn = computed(
  () =>
    isSeated.value &&
    props.snapshot.currentTurnIndex >= 0 &&
    props.snapshot.currentTurnIndex === props.mySeatIndex,
)

const callAmount = computed(() => {
  if (!myPlayer.value) return 0
  return Math.max(0, props.snapshot.currentMaxBet - myPlayer.value.currentBet)
})

/** 剩余筹码不够跟注，只能全下 */
const mustAllIn = computed(() => {
  if (!myPlayer.value || callAmount.value <= 0) {
    return false
  }
  return myPlayer.value.chips > 0 && myPlayer.value.chips < callAmount.value
})

const canAllIn = computed(
  () => myPlayer.value != null && myPlayer.value.chips > 0 && isMyTurn.value,
)

const potLabel = computed(() => formatChips(displayPot.value, props.bigBlind))

const tableBetsTotal = computed(() =>
  props.snapshot.players.reduce((sum, player) => sum + player.currentBet, 0),
)

/** 底池 + 当前轮未收拢的前注（与后端 sweep 模型一致） */
const displayPot = computed(() => props.snapshot.pot + tableBetsTotal.value)

/** 合法加注目标总额下限 */
const minRaiseTotal = computed(() => {
  if (!myPlayer.value) {
    return props.bigBlind
  }
  const maxBet = props.snapshot.currentMaxBet
  if (maxBet <= 0) {
    return props.bigBlind
  }
  const raiseIncrement = Math.max(props.bigBlind, maxBet - myPlayer.value.currentBet)
  return maxBet + raiseIncrement
})

const maxRaiseTotal = computed(() => {
  if (!myPlayer.value) {
    return props.bigBlind
  }
  return myPlayer.value.currentBet + myPlayer.value.chips
})

function clampRaiseTotal(targetTotal: number): number {
  return Math.min(Math.max(targetTotal, minRaiseTotal.value), maxRaiseTotal.value)
}

/** 手机端预设加注档位 */
const raisePresets = computed(() => {
  const toCall = callAmount.value
  const pot = displayPot.value
  const myBet = myPlayer.value?.currentBet ?? 0
  const potTotal = clampRaiseTotal(pot + toCall * 2 + myBet)
  const halfPotTotal = clampRaiseTotal(
    myBet + toCall + Math.floor((pot + toCall) / 2),
  )

  const options = [
    { key: 'min', label: '最小', amount: minRaiseTotal.value },
    { key: 'half', label: '½池', amount: halfPotTotal },
    { key: 'pot', label: '满池', amount: potTotal },
    { key: 'allin', label: 'All-in', amount: maxRaiseTotal.value },
  ]

  return options.filter(
    (option, index, list) =>
      list.findIndex((item) => item.amount === option.amount) === index,
  )
})

function canUseRaisePreset(amount: number, presetKey?: string): boolean {
  const myBet = myPlayer.value?.currentBet ?? 0
  const chips = myPlayer.value?.chips ?? 0
  if (chips <= 0 || amount <= myBet) {
    return false
  }
  if (presetKey === 'allin') {
    return true
  }
  if (mustAllIn.value) {
    return false
  }
  return amount >= minRaiseTotal.value && amount <= maxRaiseTotal.value
}

function submitRaise(amount: number) {
  if (!canAllIn.value) {
    return
  }
  const targetTotal = Math.min(amount, maxRaiseTotal.value)
  if (targetTotal <= (myPlayer.value?.currentBet ?? 0)) {
    return
  }
  emit('raise', targetTotal)
}

function submitAllIn() {
  submitRaise(maxRaiseTotal.value)
}

const seatSlots = computed(() => {
  const slots: Array<{ seatIndex: number; player: TableSnapshot['players'][0] | null }> = []
  for (let seatIndex = 0; seatIndex < props.maxSeats; seatIndex++) {
    const player = props.snapshot.players.find((player) => player.seatIndex === seatIndex)
    slots.push({ seatIndex, player: player ?? null })
  }
  return slots
})

function getRelativeSeatIndex(seatIndex: number): number {
  if (props.mySeatIndex < 0) {
    return seatIndex
  }
  return (seatIndex - props.mySeatIndex + props.maxSeats) % props.maxSeats
}

function getSeatPosition(seatIndex: number) {
  const relativeIndex = getRelativeSeatIndex(seatIndex)
  const totalSeats = props.maxSeats
  const angle = (relativeIndex / totalSeats) * 2 * Math.PI + Math.PI / 2
  const isHero = seatIndex === props.mySeatIndex
  const radiusX = isHero ? 48 : 46
  const radiusY = isHero ? 48 : 42
  return {
    left: `${50 + radiusX * Math.cos(angle)}%`,
    top: `${50 + radiusY * Math.sin(angle)}%`,
  }
}

function getBetPosition(seatIndex: number) {
  const relativeIndex = getRelativeSeatIndex(seatIndex)
  const totalSeats = props.maxSeats
  const angle = (relativeIndex / totalSeats) * 2 * Math.PI + Math.PI / 2
  const isHero = seatIndex === props.mySeatIndex
  const radiusX = isHero ? 22 : 30
  const radiusY = isHero ? 18 : 26
  return {
    left: `${50 + radiusX * Math.cos(angle)}%`,
    top: `${50 + radiusY * Math.sin(angle)}%`,
  }
}

function showHoleCards(player: TableSnapshot['players'][0], seatIndex: number): string[] {
  const isMe = seatIndex === props.mySeatIndex
  if (isMe) {
    return player.holeCards.filter((card) => !isHiddenCard(card))
  }
  if (player.holeCards.length > 0 && !player.holeCards.every(isHiddenCard)) {
    return player.holeCards.filter((card) => !isHiddenCard(card))
  }
  return ['', '']
}

function getHandTypeForSeat(seatIndex: number): string {
  if (!props.showdownResult) {
    return ''
  }
  const line = props.showdownResult.players.find((player) => player.seatIndex === seatIndex)
  return line?.handTypeName ?? ''
}

function shouldShowCards(player: TableSnapshot['players'][0] | null, seatIndex: number): boolean {
  if (!player) return false
  const isMe = seatIndex === props.mySeatIndex
  if (isMe) return true
  if (player.isFolded) return false

  if (props.showdownResult) {
    if (props.showdownResult.reason === 'fold') {
      return false
    }
    return props.showdownResult.players.some((line) => line.seatIndex === seatIndex)
  }

  return false
}

watch(
  () => myPlayer.value?.chips,
  (chips) => {
    if (chips != null && chips > 0) {
      rebuyDismissed.value = false
    }
  },
)

</script>

<template>
  <main class="table-view">
    <aside v-if="profitRows.length > 0" class="profit-panel">
      <div class="profit-panel-title">本场盈亏</div>
      <ul class="profit-list">
        <li
          v-for="player in profitRows"
          :key="'profit-' + player.seatIndex"
          class="profit-row"
          :class="{ 'is-me': player.seatIndex === mySeatIndex }"
        >
          <span class="profit-name">{{ player.username }}</span>
          <span class="profit-value" :class="profitClass(player.sessionProfit)">
            {{ formatProfit(player.sessionProfit) }}
          </span>
        </li>
      </ul>
    </aside>

    <div
      v-if="showRebuyModal"
      class="rebuy-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="rebuy-title"
    >
      <div class="rebuy-dialog">
        <h3 id="rebuy-title" class="rebuy-title">筹码已用完</h3>
        <p class="rebuy-desc">
          你的筹码已输光，是否补充
          <strong>{{ formatChips(buyInAmount, bigBlind) }}</strong>
          继续游戏？
        </p>
        <div class="rebuy-actions">
          <button
            type="button"
            class="rebuy-btn secondary"
            @click="rebuyDismissed = true"
          >
            稍后再说
          </button>
          <button
            type="button"
            class="rebuy-btn primary"
            :disabled="!connected"
            @click="emit('rebuy', buyInAmount)"
          >
            补充筹码
          </button>
        </div>
      </div>
    </div>

    <div class="table-stage">
      <div class="table-rail">
        <div class="table-felt">
          <div class="felt-line" />

          <div class="table-center">
            <div class="pot-area">
              <div class="pot-info">底池：{{ potLabel }}</div>
              <ChipStack
                v-if="displayPot > 0"
                :amount="displayPot"
                :big-blind="bigBlind"
                size="sm"
                :show-label="false"
              />
            </div>

            <div class="community-row">
              <PlayingCard
                v-for="(card, index) in snapshot.communityCards"
                :key="'community-' + index"
                :card="card"
                size="lg"
              />
              <div
                v-for="placeholder in Math.max(0, 5 - snapshot.communityCards.length)"
                :key="'ph-' + placeholder"
                class="card-slot"
              />
            </div>

            <ShowdownStrip
              v-if="showdownResult"
              :result="showdownResult"
              :big-blind="bigBlind"
            />

            <div v-if="snapshot.currentMaxBet > 0 && !showdownResult" class="max-bet">
              最高注 {{ formatChips(snapshot.currentMaxBet, bigBlind) }}
            </div>
          </div>

          <div class="room-tag">#{{ snapshot.roomId }}</div>

          <div
            v-for="slot in seatSlots"
            :key="'bet-' + slot.seatIndex"
            class="bet-position"
            :style="getBetPosition(slot.seatIndex)"
          >
            <ChipStack
              v-if="slot.player && slot.player.currentBet > 0"
              :amount="slot.player.currentBet"
              :big-blind="bigBlind"
              size="sm"
            />
          </div>

          <div
            v-for="slot in seatSlots"
            :key="'seat-' + slot.seatIndex"
            class="seat-position"
            :style="getSeatPosition(slot.seatIndex)"
          >
            <PlayerPod
              :player="slot.player"
              :seat-index="slot.seatIndex"
              :big-blind="bigBlind"
              :is-me="slot.seatIndex === mySeatIndex"
              :is-active="slot.seatIndex === snapshot.currentTurnIndex"
              :is-dealer="slot.seatIndex === snapshot.dealerIndex"
              :hole-cards="slot.player ? showHoleCards(slot.player, slot.seatIndex) : []"
              :show-cards="shouldShowCards(slot.player, slot.seatIndex)"
              :hand-type-label="getHandTypeForSeat(slot.seatIndex)"
              @sit-down="emit('sitDown', slot.seatIndex)"
            />
          </div>
        </div>
      </div>
    </div>

    <footer class="action-footer">
      <div v-if="myPlayer" class="my-chips">
        我的筹码 <strong>{{ formatChips(myPlayer.chips, bigBlind) }}</strong>
      </div>

      <div v-if="!isSeated" class="sit-hint">
        点击空位「坐下」加入牌局
      </div>

      <div v-else-if="canClickReady" class="ready-row">
        <button
          type="button"
          class="action-btn primary ready-btn"
          :disabled="!connected"
          @click="emit('ready')"
        >
          准备
        </button>
      </div>

      <div v-else-if="needsReadyBeforeFirstHand" class="ready-waiting">
        <span class="ready-waiting-text">等待全员准备后开局</span>
      </div>

      <div v-else class="action-row">
        <button
          type="button"
          class="action-btn fold"
          :disabled="!connected || !isMyTurn || !!showdownResult"
          @click="emit('fold')"
        >
          <span class="btn-icon">✕</span>
          弃牌
        </button>

        <button
          type="button"
          class="action-btn"
          :disabled="!connected || !isMyTurn || callAmount > 0 || !!showdownResult"
          @click="emit('check')"
        >
          过牌
        </button>

        <button
          v-if="!mustAllIn"
          type="button"
          class="action-btn primary"
          :disabled="!connected || !isMyTurn || callAmount <= 0 || !!showdownResult"
          @click="emit('call')"
        >
          跟注<span v-if="callAmount > 0"> {{ formatChips(callAmount, bigBlind) }}</span>
        </button>

        <button
          v-else
          type="button"
          class="action-btn allin-action"
          :disabled="!connected || !isMyTurn || !canAllIn || !!showdownResult"
          @click="submitAllIn"
        >
          All-in {{ formatChips(maxRaiseTotal, bigBlind) }}
        </button>

        <button
          v-if="!mustAllIn && canAllIn"
          type="button"
          class="action-btn allin-action secondary"
          :disabled="!connected || !isMyTurn || !!showdownResult"
          @click="submitAllIn"
        >
          All-in
        </button>
      </div>

      <div
        v-if="isSeated && isMyTurn && !showdownResult && !mustAllIn"
        class="raise-preset-row"
      >
        <button
          v-for="preset in raisePresets"
          :key="preset.key"
          type="button"
          class="raise-preset-btn"
          :class="{ allin: preset.key === 'allin' }"
          :disabled="!connected || !canUseRaisePreset(preset.amount, preset.key)"
          @click="submitRaise(preset.amount)"
        >
          <span class="preset-label">{{ preset.label }}</span>
          <span class="preset-amount">{{ formatChips(preset.amount, bigBlind) }}</span>
        </button>
      </div>

      <p v-if="isSeated && isMyTurn" class="turn-tip active">轮到你了</p>
      <p v-else-if="isSeated && showdownResult" class="turn-tip">摊牌结算中…</p>
      <p v-else-if="isSeated && canClickReady" class="turn-tip">点击「准备」开始本局</p>
      <p v-else-if="isSeated && waitingOthersReady" class="turn-tip">已准备，等待其他玩家…</p>
      <p v-else-if="isSeated && snapshot.currentTurnIndex < 0 && snapshot.players.some((player) => player.chips === 0)" class="turn-tip">
        等待玩家补充筹码…
      </p>
      <p v-else-if="isSeated && snapshot.currentTurnIndex < 0 && handEverStarted" class="turn-tip">本局结束，即将发牌…</p>
      <p v-else-if="isSeated && snapshot.currentTurnIndex < 0" class="turn-tip">等待开局…</p>
      <p v-else-if="isSeated" class="turn-tip">等待其他玩家…</p>
    </footer>
  </main>
</template>

<style scoped>
.table-view {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 72px);
  background: #121212;
  position: relative;
}

.profit-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  width: 168px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
}

.profit-panel-title {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: rgba(255, 255, 255, 0.45);
  margin-bottom: 8px;
  text-transform: uppercase;
}

.profit-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
}

.profit-row.is-me .profit-name {
  color: #3498db;
}

.profit-name {
  color: rgba(255, 255, 255, 0.75);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 88px;
}

.profit-value {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.profit-positive {
  color: #2ecc71;
}

.profit-negative {
  color: #e74c3c;
}

.profit-even {
  color: rgba(255, 255, 255, 0.4);
}

.rebuy-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.65);
  padding: 16px;
}

.rebuy-dialog {
  width: 100%;
  max-width: 360px;
  padding: 24px;
  border-radius: 12px;
  background: #1e1e1e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
}

.rebuy-title {
  margin: 0 0 12px;
  font-size: 18px;
  font-weight: 600;
  color: #ecf0f1;
}

.rebuy-desc {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #95a5a6;
}

.rebuy-desc strong {
  color: #f1c40f;
}

.rebuy-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.rebuy-btn {
  padding: 10px 18px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}

.rebuy-btn.secondary {
  background: transparent;
  color: #95a5a6;
}

.rebuy-btn.primary {
  background: rgba(46, 204, 113, 0.2);
  border-color: rgba(46, 204, 113, 0.35);
  color: #2ecc71;
}

.rebuy-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.table-stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 24px;
  min-height: 480px;
  position: relative;
}

.table-rail {
  width: 100%;
  max-width: 920px;
  aspect-ratio: 16 / 10;
  padding: 14px;
  border-radius: 50% / 44%;
  background: linear-gradient(180deg, #2a2a2a 0%, #1a1a1a 50%, #0d0d0d 100%);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.6),
    inset 0 2px 4px rgba(255, 255, 255, 0.06);
}

.table-felt {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50% / 44%;
  background: radial-gradient(ellipse at 50% 45%, #1f6b3a 0%, #1a4a2e 45%, #0f3320 100%);
  box-shadow: inset 0 0 80px rgba(0, 0, 0, 0.35);
  overflow: visible;
}

.felt-line {
  position: absolute;
  inset: 8% 10%;
  border-radius: 50% / 44%;
  border: 1px solid rgba(255, 255, 255, 0.06);
  pointer-events: none;
}

.table-center {
  position: absolute;
  top: 38%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  z-index: 1;
}

.pot-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-height: 52px;
}

.pot-info {
  font-size: 15px;
  font-weight: 600;
  color: #f1c40f;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

.community-row {
  display: flex;
  gap: 5px;
  align-items: center;
}

.card-slot {
  width: 58px;
  height: 82px;
  border-radius: 6px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
  background: rgba(0, 0, 0, 0.15);
}

.max-bet {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
}

.room-tag {
  position: absolute;
  top: 14%;
  right: 16%;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}

.seat-position,
.bet-position {
  position: absolute;
  transform: translate(-50%, -50%);
  z-index: 2;
}

.bet-position {
  z-index: 1;
}

.action-footer {
  padding: 16px 24px 24px;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.4));
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.my-chips {
  text-align: center;
  font-size: 13px;
  color: #95a5a6;
  margin-bottom: 12px;
}

.my-chips strong {
  color: #3498db;
  font-size: 15px;
}

.sit-hint {
  text-align: center;
  color: #7f8c8d;
  font-size: 14px;
  padding: 12px;
}

.ready-row {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.ready-btn {
  min-width: 140px;
  padding: 14px 32px;
  font-size: 15px;
}

.ready-waiting {
  text-align: center;
  padding: 12px;
}

.ready-waiting-text {
  color: #95a5a6;
  font-size: 14px;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(40, 40, 40, 0.9);
  color: #ecf0f1;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, transform 0.1s;
  min-width: 90px;
  justify-content: center;
}

.action-btn:hover:not(:disabled) {
  background: rgba(60, 60, 60, 0.95);
}

.action-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.action-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.action-btn.fold {
  color: #e74c3c;
  border-color: rgba(231, 76, 60, 0.25);
}

.action-btn.primary {
  background: rgba(52, 152, 219, 0.2);
  border-color: rgba(52, 152, 219, 0.35);
  color: #3498db;
}

.action-btn.allin-action {
  background: rgba(241, 196, 15, 0.18);
  border-color: rgba(241, 196, 15, 0.35);
  color: #f1c40f;
  min-width: 100px;
}

.action-btn.allin-action.secondary {
  background: rgba(241, 196, 15, 0.1);
  min-width: 72px;
}

.btn-icon {
  font-size: 12px;
  opacity: 0.7;
}

.raise-preset-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  max-width: 520px;
  margin-left: auto;
  margin-right: auto;
}

.raise-preset-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 52px;
  padding: 8px 6px;
  border-radius: 10px;
  border: 1px solid rgba(46, 204, 113, 0.28);
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.15s, transform 0.1s;
  touch-action: manipulation;
}

.raise-preset-btn:hover:not(:disabled) {
  background: rgba(46, 204, 113, 0.18);
}

.raise-preset-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.raise-preset-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.raise-preset-btn.allin {
  border-color: rgba(241, 196, 15, 0.35);
  background: rgba(241, 196, 15, 0.12);
  color: #f1c40f;
}

.preset-label {
  font-size: 12px;
  font-weight: 600;
}

.preset-amount {
  font-size: 11px;
  opacity: 0.85;
  font-variant-numeric: tabular-nums;
}

.turn-tip {
  text-align: center;
  margin: 10px 0 0;
  font-size: 13px;
  color: #7f8c8d;
}

.turn-tip.active {
  color: #2ecc71;
  font-weight: 600;
}

@media (max-width: 640px) {
  .table-stage {
    padding: 8px;
    min-height: 360px;
  }

  .table-rail {
    padding: 8px;
  }

  .action-btn {
    padding: 10px 16px;
    font-size: 13px;
    min-width: 72px;
  }

  .raise-preset-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .raise-preset-btn {
    min-height: 56px;
  }

  .card-slot {
    width: 46px;
    height: 64px;
  }
}
</style>
