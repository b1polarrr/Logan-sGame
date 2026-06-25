export interface ParsedCard {
  raw: string
  suit: string
  rank: string
  isRed: boolean
}

const RED_SUITS = new Set(['♥', '♦', 'H', 'D'])

export function parseCard(raw: string): ParsedCard | null {
  const trimmed = raw.trim()
  if (!trimmed) {
    return null
  }

  const suit = trimmed.charAt(0)
  const rank = trimmed.slice(1)
  if (!rank) {
    return null
  }

  return {
    raw: trimmed,
    suit,
    rank,
    isRed: RED_SUITS.has(suit),
  }
}

export function isHiddenCard(raw: string): boolean {
  return !raw || raw.trim() === '' || raw === '??'
}
