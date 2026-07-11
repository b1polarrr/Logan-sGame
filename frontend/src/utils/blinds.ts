import type { TableSnapshot } from '../types/table'
import { isParticipating } from '../types/table'

/** 从庄家位起找下一个仍在局内的座位（与后端 getNextActiveSeat 一致） */
function nextSeatInHand(
  startSeatIndex: number,
  seatsInHand: number[],
  maxSeats: number,
): number {
  for (let step = 1; step <= maxSeats; step++) {
    const seatIndex = (startSeatIndex + step) % maxSeats
    if (seatsInHand.includes(seatIndex)) {
      return seatIndex
    }
  }
  return -1
}

function seatsInHand(snapshot: TableSnapshot): number[] {
  return snapshot.players
    .filter((player) => isParticipating(player))
    .map((player) => player.seatIndex)
}

/** 后端未下发盲位时 proto 默认为 0，需结合庄位与在座玩家推算 */
function backendBlindIndicesLookUnset(snapshot: TableSnapshot): boolean {
  const { dealerIndex, smallBlindIndex, bigBlindIndex } = snapshot
  if (smallBlindIndex < 0 || bigBlindIndex < 0) {
    return true
  }
  if (smallBlindIndex === 0 && bigBlindIndex === 0 && dealerIndex > 0) {
    return true
  }
  const occupied = new Set(snapshot.players.map((player) => player.seatIndex))
  if (!occupied.has(smallBlindIndex) || !occupied.has(bigBlindIndex)) {
    return true
  }
  return false
}

export function resolveBlindSeatIndices(
  snapshot: TableSnapshot,
  maxSeats: number,
): { smallBlindIndex: number; bigBlindIndex: number } {
  const inHand = seatsInHand(snapshot)
  if (snapshot.dealerIndex < 0 || inHand.length < 2) {
    return { smallBlindIndex: -1, bigBlindIndex: -1 }
  }

  if (!backendBlindIndicesLookUnset(snapshot)) {
    return {
      smallBlindIndex: snapshot.smallBlindIndex,
      bigBlindIndex: snapshot.bigBlindIndex,
    }
  }

  const smallBlindIndex = nextSeatInHand(snapshot.dealerIndex, inHand, maxSeats)
  const bigBlindIndex = nextSeatInHand(smallBlindIndex, inHand, maxSeats)
  return { smallBlindIndex, bigBlindIndex }
}
