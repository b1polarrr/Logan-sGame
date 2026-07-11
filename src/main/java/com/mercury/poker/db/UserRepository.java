package com.mercury.poker.db;

import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
// import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 账号登录：按 user_id + 密码校验。不自动建号（账号需预先写入 users 表）。
 */
public class UserRepository {
    private static final UserRepository INSTANCE = new UserRepository();
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[\\w-]{1,64}$");
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_PASSWORD_LENGTH = 64;

    private final DataSource dataSource;

    private UserRepository() {
        this.dataSource = JdbcDataSourceFactory.getDataSource();
    }

    public static UserRepository getINSTANCE() {
        return INSTANCE;
    }

    /**
     * 仅登录：按 user_id 查库并校验密码。成功后展示用 username。
     *
     * @throws IllegalArgumentException 参数不合法、账号不存在或密码错误
     */
    public UserAccount login(String userId, String password) {
        String normalizedUserId = normalizeUserId(userId);
        validatePassword(password);

        Optional<UserAccount> existing = findByUserId(normalizedUserId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        UserAccount account = existing.get();
        if (!BCrypt.checkpw(password, account.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return account;
    }

    // /**
    //  * 原：登录或首次自动建号（已停用，账号请手工插入 users 表）。
    //  */
    // public UserAccount loginOrCreate(String username, String password) {
    //     String normalizedUsername = normalizeUsername(username);
    //     validatePassword(password);
    //     Optional<UserAccount> existing = findByUsername(normalizedUsername);
    //     if (existing.isPresent()) {
    //         UserAccount account = existing.get();
    //         if (!BCrypt.checkpw(password, account.getPasswordHash())) {
    //             throw new IllegalArgumentException("用户名或密码错误");
    //         }
    //         return account;
    //     }
    //     return create(normalizedUsername, password);
    // }

    public Optional<UserAccount> findByUserId(String userId) {
        String sql = "SELECT user_id, username, password_hash FROM users WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(
                        resultSet.getString("user_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("查询用户失败: " + exception.getMessage(), exception);
        }
    }

    // public Optional<UserAccount> findByUsername(String username) {
    //     String sql = "SELECT user_id, username, password_hash FROM users WHERE username = ?";
    //     try (Connection connection = dataSource.getConnection();
    //          PreparedStatement statement = connection.prepareStatement(sql)) {
    //         statement.setString(1, username);
    //         try (ResultSet resultSet = statement.executeQuery()) {
    //             if (!resultSet.next()) {
    //                 return Optional.empty();
    //             }
    //             return Optional.of(new UserAccount(
    //                     resultSet.getString("user_id"),
    //                     resultSet.getString("username"),
    //                     resultSet.getString("password_hash")
    //             ));
    //         }
    //     } catch (SQLException exception) {
    //         throw new IllegalStateException("查询用户失败: " + exception.getMessage(), exception);
    //     }
    // }

    // private UserAccount create(String username, String password) {
    //     String userId = UUID.randomUUID().toString().replace("-", "");
    //     String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    //     String sql = "INSERT INTO users (user_id, username, password_hash) VALUES (?, ?, ?)";
    //     try (Connection connection = dataSource.getConnection();
    //          PreparedStatement statement = connection.prepareStatement(sql)) {
    //         statement.setString(1, userId);
    //         statement.setString(2, username);
    //         statement.setString(3, passwordHash);
    //         statement.executeUpdate();
    //         return new UserAccount(userId, username, passwordHash);
    //     } catch (SQLException exception) {
    //         if (isDuplicateKey(exception)) {
    //             Optional<UserAccount> raced = findByUsername(username);
    //             if (raced.isPresent()) {
    //                 UserAccount account = raced.get();
    //                 if (!BCrypt.checkpw(password, account.getPasswordHash())) {
    //                     throw new IllegalArgumentException("用户名或密码错误");
    //                 }
    //                 return account;
    //             }
    //         }
    //         throw new IllegalStateException("创建用户失败: " + exception.getMessage(), exception);
    //     }
    // }

    private static String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("请输入账号 ID");
        }
        String trimmed = userId.trim();
        if (!USER_ID_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("账号 ID 需为 1–64 位字母/数字/下划线/连字符");
        }
        return trimmed;
    }

    // private static String normalizeUsername(String username) {
    //     if (username == null || username.isBlank()) {
    //         throw new IllegalArgumentException("请输入用户名");
    //     }
    //     String trimmed = username.trim();
    //     if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
    //         throw new IllegalArgumentException("用户名需为 2–32 位字母/数字/下划线/中文");
    //     }
    //     return trimmed;
    // }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码过长");
        }
    }

    // private static boolean isDuplicateKey(SQLException exception) {
    //     return "23000".equals(exception.getSQLState()) || exception.getErrorCode() == 1062;
    // }
}
