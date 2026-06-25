export function formatChips(amount: number, bigBlind: number): string {
  if (bigBlind <= 0) {
    return String(amount)
  }
  const inBigBlinds = amount / bigBlind
  if (Number.isInteger(inBigBlinds)) {
    return `${inBigBlinds} BB`
  }
  return `${inBigBlinds.toFixed(1)} BB`
}

/** 根据下注额决定筹码堆层数（对齐参考图：0.5BB 单枚，1BB 约 4～5 枚） */
export function stackLayers(amount: number, bigBlind: number): number {
  if (amount <= 0) {
    return 0
  }
  const ratio = bigBlind > 0 ? amount / bigBlind : amount
  if (ratio <= 0.75) return 1
  if (ratio <= 1.25) return 4
  if (ratio <= 2.5) return 5
  if (ratio <= 5) return 6
  return 7
}
