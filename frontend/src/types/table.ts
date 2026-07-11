export type HandStatus = 'SITTING_OUT' | 'IN_HAND' | 'FOLDED' | 'ALL_IN' | 'STOOD_UP'

export interface PlayerState {
  userId: string
  username: string
  seatIndex: number
  chips: number
  currentBet: number
  handStatus: HandStatus
  isOnline: boolean
  holeCards: string[]
  sessionProfit: number
  isReady: boolean
  willRebuy: boolean
}

export interface TableSnapshot {
  roomId: string
  pot: number
  currentMaxBet: number
  dealerIndex: number
  smallBlindIndex: number
  bigBlindIndex: number
  currentTurnIndex: number
  communityCards: string[]
  players: PlayerState[]
}

export interface ShowdownPlayerResult {
  seatIndex: number
  username: string
  holeCards: string[]
  chipsWon: number
  isWinner: boolean
  handTypeName: string
}

export interface ShowdownResult {
  roomId: string
  potTotal: number
  reason: string
  players: ShowdownPlayerResult[]
}

const HAND_STATUS_BY_NUMBER: Record<number, HandStatus> = {
  1: 'SITTING_OUT',
  2: 'IN_HAND',
  3: 'FOLDED',
  4: 'ALL_IN',
  5: 'STOOD_UP',
}

const HAND_STATUS_NAMES = new Set<HandStatus>([
  'SITTING_OUT',
  'IN_HAND',
  'FOLDED',
  'ALL_IN',
  'STOOD_UP',
])

export function parseHandStatus(raw: unknown): HandStatus {
  if (typeof raw === 'number') {
    return HAND_STATUS_BY_NUMBER[raw] ?? 'SITTING_OUT'
  }
  if (typeof raw === 'string') {
    const normalized = raw.replace(/^HAND_STATUS_/, '') as HandStatus
    if (HAND_STATUS_NAMES.has(normalized)) {
      return normalized
    }
    if (HAND_STATUS_NAMES.has(raw as HandStatus)) {
      return raw as HandStatus
    }
  }
  return 'SITTING_OUT'
}

/** 是否参与当前局（含弃牌、全下；不含起身/旁观） */
export function isParticipating(player: PlayerState): boolean {
  return (
    player.handStatus === 'IN_HAND' ||
    player.handStatus === 'FOLDED' ||
    player.handStatus === 'ALL_IN'
  )
}

export function isFolded(player: PlayerState): boolean {
  return player.handStatus === 'FOLDED'
}

export function isAllIn(player: PlayerState): boolean {
  return player.handStatus === 'ALL_IN'
}

export function isStoodUp(player: PlayerState): boolean {
  return player.handStatus === 'STOOD_UP'
}

function parseShowdownPlayer(raw: Record<string, unknown>): ShowdownPlayerResult {
  const holeCards = raw.holeCards
  return {
    seatIndex: Number(raw.seatIndex ?? 0),
    username: String(raw.username ?? ''),
    holeCards: Array.isArray(holeCards) ? holeCards.map(String) : [],
    chipsWon: Number(raw.chipsWon ?? 0),
    isWinner: Boolean(raw.isWinner),
    handTypeName: String(raw.handType ?? raw.handTypeName ?? ''),
  }
}

export function parseShowdownResult(raw: Record<string, unknown>): ShowdownResult {
  const players = raw.players
  return {
    roomId: String(raw.roomId ?? ''),
    potTotal: Number(raw.potTotal ?? 0),
    reason: String(raw.reason ?? ''),
    players: Array.isArray(players)
      ? players.map((player) => parseShowdownPlayer(player as Record<string, unknown>))
      : [],
  }
}

function parsePlayerState(raw: Record<string, unknown>): PlayerState {
  const holeCards = raw.holeCards
  return {
    userId: String(raw.userId ?? ''),
    username: String(raw.username ?? ''),
    seatIndex: Number(raw.seatIndex ?? 0),
    chips: Number(raw.chips ?? 0),
    currentBet: Number(raw.currentBet ?? 0),
    handStatus: parseHandStatus(raw.handStatus ?? raw.hand_status),
    isOnline: Boolean(raw.isOnline),
    holeCards: Array.isArray(holeCards) ? holeCards.map(String) : [],
    sessionProfit: Number(raw.sessionProfit ?? 0),
    isReady: Boolean(raw.isReady ?? raw.is_ready),
    willRebuy: Boolean(raw.willRebuy ?? raw.will_rebuy ?? true),
  }
}

export function parseTableSnapshot(raw: Record<string, unknown>): TableSnapshot {
  const players = raw.players
  return {
    roomId: String(raw.roomId ?? ''),
    pot: Number(raw.pot ?? 0),
    currentMaxBet: Number(raw.currentMaxBet ?? 0),
    dealerIndex: Number(raw.dealerIndex ?? -1),
    smallBlindIndex: Number(raw.smallBlindIndex ?? raw.small_blind_index ?? -1),
    bigBlindIndex: Number(raw.bigBlindIndex ?? raw.big_blind_index ?? -1),
    currentTurnIndex: Number(raw.currentTurnIndex ?? -1),
    communityCards: Array.isArray(raw.communityCards)
      ? raw.communityCards.map(String)
      : [],
    players: Array.isArray(players)
      ? players.map((player) => parsePlayerState(player as Record<string, unknown>))
      : [],
  }
}
