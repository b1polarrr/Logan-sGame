import { ref } from 'vue'
import {
  ACTION_MAP,
  GAME_TYPE_MAP,
  type ActionName,
  initGameProtocol,
} from '../proto/gameProtocol'
import { parseTableSnapshot, parseShowdownResult, type ShowdownResult, type TableSnapshot } from '../types/table'

export interface RoomInfo {
  roomId: string
  gameType: number
  maxSeats: number
  seatedCount: number
  smallBlind: number
  bigBlind: number
}

export interface SendActionOptions {
  roomId?: string
  seatIndex?: number
  amount?: number
  maxSeats?: number
  smallBlind?: number
  bigBlind?: number
  sessionToken?: string
  username?: string
  password?: string
  userId?: string
}

function buildWebSocketUrl(): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${location.host}/ws`
}

const SESSION_STORAGE_KEY = 'poker_aa_session_token'
const ROOM_STORAGE_KEY = 'poker_aa_room_id'
const SEAT_STORAGE_KEY = 'poker_aa_seat_index'

function persistSessionState(token: string, roomId?: string, seatIndex?: number) {
  if (token) {
    localStorage.setItem(SESSION_STORAGE_KEY, token)
  }
  if (roomId) {
    localStorage.setItem(ROOM_STORAGE_KEY, roomId)
  }
  if (seatIndex !== undefined && seatIndex >= 0) {
    localStorage.setItem(SEAT_STORAGE_KEY, String(seatIndex))
  }
}

function readStoredSeatIndex(): number {
  const raw = localStorage.getItem(SEAT_STORAGE_KEY)
  if (!raw) {
    return -1
  }
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : -1
}

function clearStoredSessionState() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
  localStorage.removeItem(ROOM_STORAGE_KEY)
  localStorage.removeItem(SEAT_STORAGE_KEY)
}

function parseRoomInfo(raw: Record<string, unknown>): RoomInfo {
  return {
    roomId: String(raw.roomId ?? ''),
    gameType: Number(raw.gameType ?? 0),
    maxSeats: Number(raw.maxSeats ?? 0),
    seatedCount: Number(raw.seatedCount ?? 0),
    smallBlind: Number(raw.smallBlind ?? 0),
    bigBlind: Number(raw.bigBlind ?? 0),
  }
}

export function useGameSocket() {
  const rooms = ref<RoomInfo[]>([])
  const currentRoomId = ref('')
  const tableSnapshot = ref<TableSnapshot | null>(null)
  const inTable = ref(false)
  const mySeatIndex = ref(-1)
  const connected = ref(false)
  const authenticated = ref(false)
  const username = ref('')
  const userId = ref('')
  const sessionToken = ref('')
  const loginBusy = ref(false)
  const loginError = ref('')
  const logs = ref<string[]>([])
  const showdownResult = ref<ShowdownResult | null>(null)

  let websocket: WebSocket | null = null
  let protocolTypes: Awaited<ReturnType<typeof initGameProtocol>> | null = null
  let pendingReconnect = false
  let showdownClearTimer: ReturnType<typeof setTimeout> | null = null

  function appendLog(message: string) {
    logs.value = [...logs.value, message]
  }

  async function ensureProtocol() {
    if (!protocolTypes) {
      protocolTypes = await initGameProtocol()
    }
    return protocolTypes
  }

  function updateMySeatFromSnapshot(snapshot: TableSnapshot) {
    if (userId.value) {
      const selfPlayer = snapshot.players.find((player) => player.userId === userId.value)
      if (selfPlayer) {
        mySeatIndex.value = selfPlayer.seatIndex
        return
      }
    }
    if (mySeatIndex.value >= 0) {
      const stillSeated = snapshot.players.some(
        (player) => player.seatIndex === mySeatIndex.value,
      )
      if (!stillSeated) {
        mySeatIndex.value = -1
      }
    }
  }

  async function connect() {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
      appendLog('[提示] 连接已存在')
      return
    }

    await ensureProtocol()

    websocket = new WebSocket(buildWebSocketUrl())
    websocket.binaryType = 'arraybuffer'

    websocket.onopen = () => {
      connected.value = true
      appendLog('[成功] WebSocket 已连接')
      const storedToken = localStorage.getItem(SESSION_STORAGE_KEY)
      if (storedToken) {
        reconnectWithToken(storedToken)
      }
    }

    websocket.onerror = () => {
      appendLog('[错误] WebSocket 连接失败，请确认 PokerNettyServer 已在 8888 启动')
    }

    websocket.onclose = () => {
      connected.value = false
      authenticated.value = false
      appendLog('[关闭] 连接已断开')
    }

    websocket.onmessage = (event: MessageEvent) => {
      if (!(event.data instanceof ArrayBuffer)) {
        appendLog('[文本] ' + String(event.data))
        return
      }

      handleBinaryMessage(new Uint8Array(event.data))
    }
  }

  function handleBinaryMessage(bytes: Uint8Array) {
    if (!protocolTypes) {
      appendLog('[错误] 协议尚未初始化')
      return
    }

    try {
      const serverMessage = protocolTypes.ServerMessage.decode(bytes)
      const messageObject = protocolTypes.ServerMessage.toObject(serverMessage, {
        defaults: true,
      }) as {
        tableSnapshot?: Record<string, unknown>
        roomList?: { rooms?: Record<string, unknown>[] }
        roomCreated?: Record<string, unknown>
        sessionConnected?: {
          sessionToken?: string
          userId?: string
          username?: string
          authenticated?: boolean
        }
        error?: { code?: string; message?: string }
        showdown?: Record<string, unknown>
      }

      if (messageObject.sessionConnected) {
        const newToken = String(messageObject.sessionConnected.sessionToken ?? '')
        const storedToken = localStorage.getItem(SESSION_STORAGE_KEY)
        const isAuthenticated = Boolean(messageObject.sessionConnected.authenticated)
        if (!pendingReconnect || newToken === storedToken || isAuthenticated) {
          sessionToken.value = newToken
          persistSessionState(newToken)
        }
        userId.value = String(messageObject.sessionConnected.userId ?? '')
        username.value = String(messageObject.sessionConnected.username ?? '')
        authenticated.value = isAuthenticated
        loginBusy.value = false
        if (isAuthenticated) {
          loginError.value = ''
          appendLog('[登录] ' + username.value)
        } else {
          appendLog('[会话] 访客 token ' + (newToken.slice(0, 8) || '(空)') + '…')
        }
        return
      }
      if (messageObject.tableSnapshot) {
        const snapshot = parseTableSnapshot(messageObject.tableSnapshot)
        tableSnapshot.value = snapshot
        inTable.value = true
        currentRoomId.value = snapshot.roomId
        pendingReconnect = false
        updateMySeatFromSnapshot(snapshot)
        persistSessionState(sessionToken.value, snapshot.roomId, mySeatIndex.value)
        const playerStatusLog = snapshot.players
          .map(
            (player) =>
              'seat' + player.seatIndex + ':' + player.username + '=' + player.handStatus,
          )
          .join(' ')
        appendLog(
          '[快照] 房间 ' +
            snapshot.roomId +
            ' 底池 ' +
            snapshot.pot +
            (playerStatusLog ? ' | ' + playerStatusLog : ''),
        )
        return
      }
      if (messageObject.roomList) {
        const rawRooms = messageObject.roomList.rooms ?? []
        rooms.value = rawRooms.map(parseRoomInfo)
        pendingReconnect = false
        appendLog('[房间列表] 共 ' + rooms.value.length + ' 个房间')
        return
      }
      if (messageObject.roomCreated) {
        const createdRoom = parseRoomInfo(messageObject.roomCreated)
        if (createdRoom.roomId) {
          currentRoomId.value = createdRoom.roomId
        }
        appendLog('[创建房间] 房间号 ' + createdRoom.roomId)
        refreshRoomList()
        return
      }
      if (messageObject.error) {
        const code = String(messageObject.error.code ?? 'ERROR')
        const message = String(messageObject.error.message ?? '')
        appendLog('[错误] ' + code + ' ' + message)
        if (code === 'LOGIN_FAILED' || code === 'AUTH_REQUIRED') {
          loginBusy.value = false
          loginError.value = message || '登录失败'
          if (code === 'LOGIN_FAILED') {
            authenticated.value = false
          }
        }
        if (code === 'ROOM_ON_OTHER_POD' || code === 'ROOM_NOT_FOUND') {
          leaveTable()
          localStorage.removeItem(ROOM_STORAGE_KEY)
          localStorage.removeItem(SEAT_STORAGE_KEY)
          if (authenticated.value) {
            refreshRoomList()
          }
          appendLog('[提示] 请刷新房间列表，加入本页可见的房间或新建房间')
        }
        return
      }
      if (messageObject.showdown) {
        const result = parseShowdownResult(messageObject.showdown)
        showdownResult.value = result
        if (showdownClearTimer) {
          clearTimeout(showdownClearTimer)
        }
        showdownClearTimer = setTimeout(() => {
          showdownResult.value = null
        }, 10000)
        const winnerNames = result.players
          .filter((player) => player.isWinner)
          .map((player) => player.username)
          .join('、')
        appendLog(
          '[摊牌] ' +
            (result.reason === 'fold' ? '收池' : '比牌') +
            ' 赢家 ' +
            (winnerNames || '(无)') +
            ' 底池 ' +
            result.potTotal,
        )
        return
      }
      appendLog('[未知消息] ' + JSON.stringify(messageObject, null, 2))
    } catch (decodeError) {
      const message =
        decodeError instanceof Error ? decodeError.message : String(decodeError)
      appendLog('[解码失败] ' + message)
    }
  }

  function disconnect() {
    if (websocket) {
      websocket.close()
      websocket = null
    }
    leaveTable()
    clearStoredSessionState()
    sessionToken.value = ''
    authenticated.value = false
    username.value = ''
    userId.value = ''
    loginBusy.value = false
    loginError.value = ''
    showdownResult.value = null
    if (showdownClearTimer) {
      clearTimeout(showdownClearTimer)
      showdownClearTimer = null
    }
  }

  async function reconnectWithToken(token: string) {
    if (!websocket || websocket.readyState !== WebSocket.OPEN) {
      appendLog('[错误] 请先连接')
      return
    }

    const storedRoomId = localStorage.getItem(ROOM_STORAGE_KEY) ?? ''
    const storedSeatIndex = readStoredSeatIndex()
    if (storedRoomId) {
      currentRoomId.value = storedRoomId
    }
    if (storedSeatIndex >= 0) {
      mySeatIndex.value = storedSeatIndex
    }

    pendingReconnect = true
    sessionToken.value = token
    persistSessionState(token)

    const types = await ensureProtocol()
    const payload = {
      actionType: ACTION_MAP.RECONNECT,
      sessionToken: token,
      roomId: storedRoomId,
      seatIndex: storedSeatIndex >= 0 ? storedSeatIndex : 0,
      amount: 0,
    }

    const verifyError = types.PlayerActionRequest.verify(payload)
    if (verifyError) {
      appendLog('[重连校验失败] ' + verifyError)
      return
    }

    const message = types.PlayerActionRequest.create(payload)
    const bytes = types.PlayerActionRequest.encode(message).finish()
    const payloadBuffer = bytes.buffer.slice(
      bytes.byteOffset,
      bytes.byteOffset + bytes.byteLength,
    ) as ArrayBuffer
    websocket.send(payloadBuffer)
    appendLog('[重连] 已发送 RECONNECT token=' + token.slice(0, 8) + '…')
  }

  async function sendAction(actionName: ActionName, options: SendActionOptions = {}) {
    if (!websocket || websocket.readyState !== WebSocket.OPEN) {
      appendLog('[错误] 请先连接')
      return
    }

    const types = await ensureProtocol()

    const roomId = options.roomId ?? currentRoomId.value
    const seatIndex =
      options.seatIndex !== undefined ? options.seatIndex : mySeatIndex.value
    const amount = actionName === 'RAISE' ? (options.amount ?? 0) : 0

    const payload: Record<string, unknown> = {
      actionType: ACTION_MAP[actionName],
      roomId,
      seatIndex: seatIndex >= 0 ? seatIndex : 0,
      amount,
    }

    if (actionName === 'CREATE_ROOM') {
      payload.gameType = GAME_TYPE_MAP.TEXAS_HOLDEM
      payload.maxSeats = options.maxSeats ?? 6
      payload.smallBlind = options.smallBlind ?? 10
      payload.bigBlind = options.bigBlind ?? 20
    }

    if (actionName === 'SIT_DOWN' && options.seatIndex !== undefined) {
      payload.seatIndex = options.seatIndex
    }

    if (actionName === 'RECONNECT' && options.sessionToken) {
      payload.sessionToken = options.sessionToken
    }

    if (actionName === 'LOGIN') {
      payload.userId = options.userId ?? ''
      payload.password = options.password ?? ''
    }

    const verifyError = types.PlayerActionRequest.verify(payload)
    if (verifyError) {
      appendLog('[编码校验失败] ' + verifyError)
      return
    }

    const message = types.PlayerActionRequest.create(payload)
    const bytes = types.PlayerActionRequest.encode(message).finish()
    const payloadBuffer = bytes.buffer.slice(
      bytes.byteOffset,
      bytes.byteOffset + bytes.byteLength,
    ) as ArrayBuffer
    websocket.send(payloadBuffer)

    appendLog(
      actionName === 'LOGIN'
        ? '[已发送] LOGIN userId=' + (options.userId ?? '')
        : '[已发送] ' +
            actionName +
            ' room=' +
            roomId +
            ' seat=' +
            payload.seatIndex +
            ' amount=' +
            amount,
    )
  }

  async function login(loginUserId: string, loginPassword: string) {
    if (!websocket || websocket.readyState !== WebSocket.OPEN) {
      appendLog('[错误] 请先连接')
      loginError.value = '请先连接服务器'
      return
    }
    loginBusy.value = true
    loginError.value = ''
    await sendAction('LOGIN', {
      userId: loginUserId,
      password: loginPassword,
    })
  }

  async function refreshRoomList() {
    if (!authenticated.value) {
      return
    }
    await sendAction('LIST_ROOMS')
  }

  async function createRoom(options: {
    maxSeats: number
    smallBlind: number
    bigBlind: number
  }) {
    await sendAction('CREATE_ROOM', options)
  }

  async function joinRoom(roomId: string) {
    currentRoomId.value = roomId
    mySeatIndex.value = -1
    persistSessionState(sessionToken.value, roomId, -1)
    await sendAction('JOIN_ROOM', { roomId })
  }

  async function sitDown(seatIndex: number) {
    mySeatIndex.value = seatIndex
    persistSessionState(sessionToken.value, currentRoomId.value, seatIndex)
    await sendAction('SIT_DOWN', { seatIndex })
  }

  async function standUp() {
    await sendAction('STAND_UP')
  }

  async function fold() {
    await sendAction('FOLD')
  }

  async function check() {
    await sendAction('CHECK')
  }

  async function call() {
    await sendAction('CALL')
  }

  async function raiseBet(targetTotalBet: number) {
    await sendAction('RAISE', { amount: targetTotalBet })
  }

  async function rebuy(amount?: number) {
    await sendAction('REBUY', { amount: amount ?? 1000 })
  }

  async function declineRebuy() {
    await sendAction('DECLINE_REBUY')
  }

  async function ready() {
    await sendAction('READY')
  }

  function leaveTable() {
    inTable.value = false
    tableSnapshot.value = null
    mySeatIndex.value = -1
    localStorage.removeItem(SEAT_STORAGE_KEY)
  }

  function backToLobby() {
    leaveTable()
    refreshRoomList()
  }

  return {
    connected,
    authenticated,
    username,
    userId,
    sessionToken,
    loginBusy,
    loginError,
    logs,
    showdownResult,
    rooms,
    currentRoomId,
    tableSnapshot,
    inTable,
    mySeatIndex,
    connect,
    disconnect,
    login,
    sendAction,
    refreshRoomList,
    createRoom,
    joinRoom,
    sitDown,
    standUp,
    fold,
    check,
    call,
    raiseBet,
    rebuy,
    declineRebuy,
    ready,
    leaveTable,
    backToLobby,
  }
}
