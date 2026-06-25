<script setup lang="ts">
import { computed } from 'vue'
import { isHiddenCard, parseCard } from '../utils/cards'

const props = withDefaults(
  defineProps<{
    card?: string
    hidden?: boolean
    size?: 'sm' | 'md' | 'lg' | 'hero'
  }>(),
  {
    card: '',
    hidden: false,
    size: 'md',
  },
)

const showBack = computed(() => props.hidden || isHiddenCard(props.card))
const parsed = computed(() => (showBack.value ? null : parseCard(props.card)))
</script>

<template>
  <div
    class="playing-card"
    :class="[size, showBack ? 'back' : parsed?.isRed ? 'red' : 'black']"
  >
    <template v-if="showBack">
      <div class="back-inner">
        <div class="back-diamond" />
        <div class="back-diamond small" />
      </div>
    </template>
    <template v-else-if="parsed">
      <span class="corner top-left">
        <span class="rank">{{ parsed.rank }}</span>
        <span class="suit">{{ parsed.suit }}</span>
      </span>
      <span class="center-suit">{{ parsed.suit }}</span>
      <span class="corner bottom-right">
        <span class="rank">{{ parsed.rank }}</span>
        <span class="suit">{{ parsed.suit }}</span>
      </span>
    </template>
  </div>
</template>

<style scoped>
.playing-card {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #fafafa;
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.4),
    inset 0 0 0 1px rgba(0, 0, 0, 0.06);
  font-weight: 700;
  user-select: none;
  flex-shrink: 0;
}

.playing-card.sm {
  width: 34px;
  height: 48px;
  font-size: 10px;
  border-radius: 4px;
}

.playing-card.md {
  width: 46px;
  height: 64px;
  font-size: 12px;
}

.playing-card.lg {
  width: 58px;
  height: 82px;
  font-size: 14px;
}

.playing-card.hero {
  width: 72px;
  height: 100px;
  font-size: 18px;
  border-radius: 8px;
}

.playing-card.red {
  color: #c0392b;
}

.playing-card.black {
  color: #1a1a2e;
}

.playing-card.back {
  background: linear-gradient(145deg, #8b1a1a 0%, #c0392b 40%, #922b21 100%);
  overflow: hidden;
  border: 1px solid #6b1515;
}

.back-inner {
  position: relative;
  width: 78%;
  height: 86%;
  border-radius: 4px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  background:
    repeating-linear-gradient(
      45deg,
      rgba(255, 255, 255, 0.12) 0,
      rgba(255, 255, 255, 0.12) 3px,
      transparent 3px,
      transparent 6px
    ),
    repeating-linear-gradient(
      -45deg,
      rgba(255, 255, 255, 0.08) 0,
      rgba(255, 255, 255, 0.08) 3px,
      transparent 3px,
      transparent 6px
    );
}

.back-diamond {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 40%;
  height: 40%;
  transform: translate(-50%, -50%) rotate(45deg);
  border: 2px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.06);
}

.back-diamond.small {
  width: 22%;
  height: 22%;
  border-width: 1px;
}

.corner {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
  gap: 1px;
}

.top-left {
  top: 4px;
  left: 5px;
}

.bottom-right {
  bottom: 4px;
  right: 5px;
  transform: rotate(180deg);
}

.center-suit {
  font-size: 1.6em;
  line-height: 1;
}

.sm .center-suit {
  font-size: 1.3em;
}

.hero .center-suit {
  font-size: 2em;
}
</style>
