import protobuf from 'protobufjs'

export const ACTION_MAP = {
  JOIN_ROOM: 1,
  SIT_DOWN: 2,
  FOLD: 3,
  CHECK: 4,
  CALL: 5,
  RAISE: 6,
  CREATE_ROOM: 7,
  LIST_ROOMS: 8,
  RECONNECT: 9,
  REBUY: 10,
  READY: 11,
  DECLINE_REBUY: 12,
  STAND_UP: 13,
  LOGIN: 14,
  LEAVE_TABLE: 15,
} as const

export const GAME_TYPE_MAP = {
  TEXAS_HOLDEM: 1,
} as const

export type ActionName = keyof typeof ACTION_MAP

const PROTO = `
syntax = "proto3";
package mercury.poker;
enum GameType {
  GAME_UNKNOWN = 0;
  TEXAS_HOLDEM = 1;
}
enum ActionType {
  ACTION_UNKNOWN = 0;
  JOIN_ROOM = 1;
  SIT_DOWN = 2;
  FOLD = 3;
  CHECK = 4;
  CALL = 5;
  RAISE = 6;
  CREATE_ROOM = 7;
  LIST_ROOMS = 8;
  RECONNECT = 9;
  REBUY = 10;
  READY = 11;
  DECLINE_REBUY = 12;
  STAND_UP = 13;
  LOGIN = 14;
  LEAVE_TABLE = 15;
}
message PlayerActionRequest {
  ActionType action_type = 1;
  string room_id = 2;
  int32 seat_index = 3;
  int32 amount = 4;
  GameType game_type = 5;
  int32 max_seats = 6;
  int32 small_blind = 7;
  int32 big_blind = 8;
  string session_token = 9;
  string user_id = 10;
  string password = 11;
}
message RoomInfo {
  string room_id = 1;
  GameType game_type = 2;
  int32 max_seats = 3;
  int32 seated_count = 4;
  int32 small_blind = 5;
  int32 big_blind = 6;
}
message RoomListResponse {
  repeated RoomInfo rooms = 1;
}
message SessionConnectedResponse {
  string session_token = 1;
  string user_id = 2;
  string username = 3;
  bool authenticated = 4;
}
message ErrorResponse {
  string code = 1;
  string message = 2;
}
message ShowdownPlayerResult {
  int32 seat_index = 1;
  string username = 2;
  repeated string hole_cards = 3;
  int32 chips_won = 4;
  bool is_winner = 5;
  string hand_type = 6;
}
message ShowdownResult {
  string room_id = 1;
  int32 pot_total = 2;
  string reason = 3;
  repeated ShowdownPlayerResult players = 4;
}
message ServerMessage {
  TableSnapshotResponse table_snapshot = 1;
  RoomListResponse room_list = 2;
  RoomInfo room_created = 3;
  SessionConnectedResponse session_connected = 4;
  ErrorResponse error = 5;
  ShowdownResult showdown = 6;
}
enum HandStatus {
  HAND_STATUS_UNKNOWN = 0;
  SITTING_OUT = 1;
  IN_HAND = 2;
  FOLDED = 3;
  ALL_IN = 4;
  STOOD_UP = 5;
}
message PlayerState {
  string user_id = 1;
  string username = 2;
  int32 seat_index = 3;
  int32 chips = 4;
  int32 current_bet = 5;
  HandStatus hand_status = 6;
  bool is_online = 8;
  repeated string hole_cards = 9;
  int32 session_profit = 10;
  bool is_ready = 11;
  bool will_rebuy = 12;
  int32 locked_chips = 14;
}
message SessionProfitEntry {
  string user_id = 1;
  string username = 2;
  int32 session_profit = 3;
  int32 last_seat_index = 4;
}
message TableSnapshotResponse {
  string room_id = 1;
  int32 pot = 2;
  int32 current_max_bet = 3;
  int32 dealer_index = 4;
  int32 current_turn_index = 5;
  repeated string community_cards = 6;
  repeated PlayerState players = 7;
  int32 small_blind_index = 8;
  int32 big_blind_index = 9;
  repeated SessionProfitEntry departed_profits = 10;
}
`

export interface GameProtocolTypes {
  PlayerActionRequest: protobuf.Type
  ServerMessage: protobuf.Type
}

let cachedTypes: GameProtocolTypes | null = null

export async function initGameProtocol(): Promise<GameProtocolTypes> {
  if (cachedTypes) {
    return cachedTypes
  }

  const root = protobuf.parse(PROTO).root
  cachedTypes = {
    PlayerActionRequest: root.lookupType('PlayerActionRequest'),
    ServerMessage: root.lookupType('ServerMessage'),
  }
  return cachedTypes
}
