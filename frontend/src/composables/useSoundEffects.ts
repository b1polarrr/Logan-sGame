import type { TableSnapshot } from '../types/table'

/** 当前启用的音效（public/sounds/） */
const SOUND_FILES = {
  call: '/sounds/call.mp3',
  check: '/sounds/check.wav',
  flop: '/sounds/flop.mp3',
  turnAndRiver: '/sounds/turnAndRiver.mp3',
  // deal: '/sounds/deal.mp3',
  // fold: '/sounds/fold.mp3',
  // raise: '/sounds/raise.mp3',
  // win: '/sounds/win.mp3',
  // chip: '/sounds/chip.mp3',
} as const

export type SoundName = keyof typeof SOUND_FILES

const MUTED_KEY = 'poker_aa_sound_muted'

let unlocked = false
let muted = localStorage.getItem(MUTED_KEY) === '1'

function createAudio(src: string): HTMLAudioElement {
  const audio = new Audio(src)
  audio.preload = 'auto'
  return audio
}

/** 浏览器需用户手势后才能播音；在坐下/准备/操作时调用一次即可 */
export function unlockAudio() {
  // 音效暂时全部关闭
  // if (unlocked) {
  //   return
  // }
  // unlocked = true
  // const probe = createAudio(SOUND_FILES.check)
  // probe.volume = 0
  // void probe.play().then(() => {
  //   probe.pause()
  // }).catch(() => {
  //   unlocked = false
  // })
}

export function isSoundMuted(): boolean {
  return muted
}

export function setSoundMuted(next: boolean) {
  muted = next
  localStorage.setItem(MUTED_KEY, next ? '1' : '0')
}

export function playSound(_name: SoundName) {
  // 音效暂时全部关闭
  // if (muted) {
  //   return
  // }
  // const src = SOUND_FILES[name]
  // if (!src) {
  //   return
  // }
  // const audio = createAudio(src)
  // void audio.play().catch(() => {
  //   // 未解锁时静默失败，等下次用户手势再 unlock
  // })
}

/**
 * 对比相邻两帧快照，触发对应音效。
 * 只启用：flop / turnAndRiver / check / call；其余保留注释占位。
 */
export function playSoundsForSnapshotDiff(
  previous: TableSnapshot | null,
  next: TableSnapshot,
) {
  if (previous == null || previous.roomId !== next.roomId) {
    return
  }

  const previousCardCount = previous.communityCards.length
  const nextCardCount = next.communityCards.length
  if (nextCardCount > previousCardCount) {
    // if (nextCardCount === 3) {
    //   playSound('flop')
    // } else if (nextCardCount === 4 || nextCardCount === 5) {
    //   playSound('turnAndRiver')
    // }
    return
  }

  const previousTurnIndex = previous.currentTurnIndex
  const nextTurnIndex = next.currentTurnIndex
  if (previousTurnIndex < 0 || nextTurnIndex === previousTurnIndex) {
    return
  }

  const previousActor = previous.players.find(
    (player) => player.seatIndex === previousTurnIndex,
  )
  const nextActor = next.players.find((player) => player.seatIndex === previousTurnIndex)
  if (previousActor == null || nextActor == null) {
    return
  }

  // if (nextActor.handStatus === 'FOLDED' && previousActor.handStatus !== 'FOLDED') {
  //   playSound('fold')
  //   return
  // }

  if (nextActor.currentBet > previousActor.currentBet) {
    if (next.currentMaxBet > previous.currentMaxBet) {
      // playSound('raise')
      return
    }
    // playSound('call')
    return
  }

  // if (
  //   nextActor.currentBet === previousActor.currentBet &&
  //   nextActor.handStatus !== 'FOLDED'
  // ) {
  //   playSound('check')
  // }
}
