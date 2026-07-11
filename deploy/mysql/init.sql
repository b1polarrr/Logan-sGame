-- Poker_AA：账号表（仅登录，不含筹码/战绩）
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       VARCHAR(64)  NOT NULL COMMENT '业务用户 ID，写入牌桌/Session',
  username      VARCHAR(32)  NOT NULL COMMENT '登录名，唯一',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_user_id (user_id),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
