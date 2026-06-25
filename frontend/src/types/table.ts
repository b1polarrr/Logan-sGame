export interface PlayerState {
  userId: string
  username: string
  seatIndex: number
  chips: number
  currentBet: number
  isFolded: boolean
  isAllIn: boolean
  isOnline: boolean
  holeCards: string[]
  sessionProfit: number
}

export interface TableSnapshot {
  roomId: string
  pot: number
  currentMaxBet: number
  dealerIndex: number
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
    isFolded: Boolean(raw.isFolded),
    isAllIn: Boolean(raw.isAllIn),
    isOnline: Boolean(raw.isOnline),
    holeCards: Array.isArray(holeCards) ? holeCards.map(String) : [],
    sessionProfit: Number(raw.sessionProfit ?? 0),
  }
}

export function parseTableSnapshot(raw: Record<string, unknown>): TableSnapshot {
  const players = raw.players
  return {
    roomId: String(raw.roomId ?? ''),
    pot: Number(raw.pot ?? 0),
    currentMaxBet: Number(raw.currentMaxBet ?? 0),
    dealerIndex: Number(raw.dealerIndex ?? -1),
    currentTurnIndex: Number(raw.currentTurnIndex ?? -1),
    communityCards: Array.isArray(raw.communityCards)
      ? raw.communityCards.map(String)
      : [],
    players: Array.isArray(players)
      ? players.map((player) => parsePlayerState(player as Record<string, unknown>))
      : [],
  }
}
