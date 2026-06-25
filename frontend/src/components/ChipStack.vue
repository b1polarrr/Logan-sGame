<script setup lang="ts">
import { computed } from 'vue'
import { formatChips, stackLayers } from '../utils/chips'

const props = withDefaults(
  defineProps<{
    amount: number
    bigBlind?: number
    size?: 'sm' | 'md'
    showLabel?: boolean
  }>(),
  {
    bigBlind: 20,
    size: 'md',
    showLabel: true,
  },
)

const layers = computed(() => stackLayers(props.amount, props.bigBlind))
const label = computed(() => formatChips(props.amount, props.bigBlind))
const pileHeight = computed(() => {
  const layerStep = props.size === 'sm' ? 3 : 4
  const base = props.size === 'sm' ? 22 : 30
  return base + (layers.value - 1) * layerStep
})
</script>

<template>
  <div v-if="amount > 0" class="chip-bet" :class="size">
    <div class="chip-pile" :style="{ height: pileHeight + 'px' }">
      <div
        v-for="layerIndex in layers"
        :key="layerIndex"
        class="chip-unit"
        :style="{ '--stack-index': layerIndex - 1 }"
      >
        <div class="chip-cylinder">
          <div class="chip-face" />
          <div class="chip-side" />
        </div>
      </div>
      <div class="chip-shadow" />
    </div>

    <div v-if="showLabel" class="chip-label">{{ label }}</div>
  </div>
</template>

<style scoped>
.chip-bet {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  pointer-events: none;
  filter: drop-shadow(0 3px 6px rgba(0, 0, 0, 0.45));
}

.chip-pile {
  position: relative;
  width: 40px;
  perspective: 120px;
}

.chip-bet.sm .chip-pile {
  width: 30px;
}

.chip-unit {
  position: absolute;
  left: 50%;
  bottom: calc(var(--stack-index) * 4px);
  transform: translateX(-50%) rotateX(28deg);
  transform-style: preserve-3d;
}

.chip-bet.sm .chip-unit {
  bottom: calc(var(--stack-index) * 3px);
  transform: translateX(-50%) rotateX(32deg) scale(0.78);
  transform-origin: center bottom;
}

.chip-cylinder {
  position: relative;
  width: 34px;
  height: 34px;
}

.chip-face {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 28%, #ff7b7b 0%, #e74c3c 35%, #c0392b 70%, #922b21 100%);
  border: 2.5px solid transparent;
  background-clip: padding-box;
  box-shadow:
    inset 0 2px 4px rgba(255, 255, 255, 0.35),
    inset 0 -2px 4px rgba(0, 0, 0, 0.25);
}

.chip-face::before {
  content: '';
  position: absolute;
  inset: -2.5px;
  border-radius: 50%;
  background: repeating-conic-gradient(
    from 0deg,
    #ffffff 0deg 14deg,
    #c0392b 14deg 28deg
  );
  z-index: -1;
}

.chip-face::after {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: radial-gradient(circle at 40% 35%, rgba(255, 255, 255, 0.08), transparent 60%);
}

.chip-side {
  position: absolute;
  left: 3px;
  right: 3px;
  bottom: -4px;
  height: 7px;
  border-radius: 0 0 50% 50% / 0 0 100% 100%;
  background: linear-gradient(180deg, #a93226 0%, #7b241c 55%, #641e16 100%);
  box-shadow: 0 2px 3px rgba(0, 0, 0, 0.35);
}

.chip-side::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: repeating-linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.35) 0,
    rgba(255, 255, 255, 0.35) 2px,
    transparent 2px,
    transparent 5px
  );
  opacity: 0.5;
}

.chip-shadow {
  position: absolute;
  left: 50%;
  bottom: -2px;
  width: 70%;
  height: 6px;
  transform: translateX(-50%);
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(0, 0, 0, 0.5) 0%, transparent 70%);
}

.chip-label {
  padding: 3px 12px;
  border-radius: 20px;
  background: rgba(0, 42, 18, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
  letter-spacing: 0.02em;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.chip-bet.sm .chip-label {
  padding: 2px 9px;
  font-size: 10px;
  border-radius: 14px;
}
</style>
