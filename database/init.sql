-- Poker Java thuần: schema tổng hợp từ V1 đến V12.
CREATE DATABASE IF NOT EXISTS poker_java CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE poker_java;

-- ===== V1__create_users_and_profiles.sql =====
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    account_chips BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6) NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('PLAYER', 'ADMIN')),
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT chk_users_account_chips_non_negative CHECK (account_chips >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(2048) NULL,
    online_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_player_profiles PRIMARY KEY (id),
    CONSTRAINT uk_player_profiles_user_id UNIQUE (user_id),
    CONSTRAINT chk_player_profiles_online_status CHECK (online_status IN ('ONLINE', 'IN_GAME', 'OFFLINE')),
    CONSTRAINT fk_player_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_player_profiles_online_status (online_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V2__create_refresh_tokens.sql =====
CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V3__create_rooms.sql =====
CREATE TABLE rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    room_type VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NULL,
    max_players INT NOT NULL,
    small_blind BIGINT NOT NULL,
    big_blind BIGINT NOT NULL,
    buy_in BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_activity_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_rooms PRIMARY KEY (id),
    CONSTRAINT fk_rooms_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_rooms_room_type CHECK (room_type IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT chk_rooms_public_password CHECK (room_type = 'PRIVATE' OR password_hash IS NULL),
    CONSTRAINT chk_rooms_max_players CHECK (max_players BETWEEN 6 AND 9),
    CONSTRAINT chk_rooms_small_blind_positive CHECK (small_blind > 0),
    CONSTRAINT chk_rooms_big_blind_greater CHECK (big_blind > small_blind),
    CONSTRAINT chk_rooms_buy_in_positive CHECK (buy_in > 0),
    CONSTRAINT chk_rooms_status CHECK (status IN ('WAITING', 'PLAYING', 'FINISHED', 'CLOSED')),
    INDEX idx_rooms_owner_user_id (owner_user_id),
    INDEX idx_rooms_status_type (status, room_type),
    INDEX idx_rooms_last_activity_at (last_activity_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V4__create_room_players.sql =====
CREATE TABLE room_players (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    seat_number INT NULL,
    player_state VARCHAR(20) NOT NULL,
    table_chips BIGINT NOT NULL DEFAULT 0,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at DATETIME(6) NULL,
    CONSTRAINT pk_room_players PRIMARY KEY (id),
    CONSTRAINT uk_room_players_room_user UNIQUE (room_id, user_id),
    CONSTRAINT uk_room_players_room_seat UNIQUE (room_id, seat_number),
    CONSTRAINT fk_room_players_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_players_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_players_seat_number CHECK (seat_number IS NULL OR seat_number BETWEEN 1 AND 9),
    CONSTRAINT chk_room_players_state CHECK (
        player_state IN ('NOT_READY', 'READY', 'PLAYING', 'SPECTATING', 'DISCONNECTED', 'LEAVING')
    ),
    CONSTRAINT chk_room_players_table_chips_non_negative CHECK (table_chips >= 0),
    INDEX idx_room_players_user_id (user_id),
    INDEX idx_room_players_room_state (room_id, player_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V5__create_gameplay_history_tables.sql =====
CREATE TABLE game_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_game_sessions PRIMARY KEY (id),
    CONSTRAINT fk_game_sessions_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT,
    CONSTRAINT chk_game_sessions_status CHECK (status IN ('ACTIVE', 'FINISHED', 'ABORTED')),
    CONSTRAINT chk_game_sessions_time_order CHECK (ended_at IS NULL OR ended_at >= started_at),
    INDEX idx_game_sessions_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE poker_hands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_session_id BIGINT NOT NULL,
    hand_number BIGINT NOT NULL,
    dealer_seat INT NOT NULL,
    small_blind_seat INT NOT NULL,
    big_blind_seat INT NOT NULL,
    small_blind_amount BIGINT NOT NULL,
    big_blind_amount BIGINT NOT NULL,
    started_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    final_phase VARCHAR(20) NOT NULL,
    board_cards VARCHAR(32) NOT NULL DEFAULT '',
    end_reason VARCHAR(30) NULL,
    CONSTRAINT pk_poker_hands PRIMARY KEY (id),
    CONSTRAINT uk_poker_hands_session_number UNIQUE (game_session_id, hand_number),
    CONSTRAINT fk_poker_hands_session FOREIGN KEY (game_session_id) REFERENCES game_sessions (id) ON DELETE CASCADE,
    CONSTRAINT chk_poker_hands_number_positive CHECK (hand_number > 0),
    CONSTRAINT chk_poker_hands_dealer_seat CHECK (dealer_seat BETWEEN 1 AND 9),
    CONSTRAINT chk_poker_hands_small_blind_seat CHECK (small_blind_seat BETWEEN 1 AND 9),
    CONSTRAINT chk_poker_hands_big_blind_seat CHECK (big_blind_seat BETWEEN 1 AND 9),
    CONSTRAINT chk_poker_hands_small_blind_positive CHECK (small_blind_amount > 0),
    CONSTRAINT chk_poker_hands_big_blind_greater CHECK (big_blind_amount > small_blind_amount),
    CONSTRAINT chk_poker_hands_final_phase CHECK (
        final_phase IN ('PRE_FLOP', 'FLOP', 'TURN', 'RIVER', 'SHOWDOWN', 'FINISHED')
    ),
    CONSTRAINT chk_poker_hands_end_reason CHECK (
        end_reason IS NULL OR end_reason IN ('SHOWDOWN', 'ALL_OTHERS_FOLDED', 'ABORTED')
    ),
    CONSTRAINT chk_poker_hands_time_order CHECK (ended_at IS NULL OR ended_at >= started_at),
    INDEX idx_poker_hands_session_id (game_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE hand_players (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poker_hand_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    seat_number INT NOT NULL,
    starting_table_chips BIGINT NOT NULL,
    ending_table_chips BIGINT NOT NULL,
    total_committed BIGINT NOT NULL,
    participation_state VARCHAR(20) NOT NULL,
    connected_at_end BOOLEAN NOT NULL,
    leaving_at_end BOOLEAN NOT NULL,
    hole_cards VARCHAR(8) NULL,
    CONSTRAINT pk_hand_players PRIMARY KEY (id),
    CONSTRAINT uk_hand_players_hand_user UNIQUE (poker_hand_id, user_id),
    CONSTRAINT uk_hand_players_hand_seat UNIQUE (poker_hand_id, seat_number),
    CONSTRAINT fk_hand_players_hand FOREIGN KEY (poker_hand_id) REFERENCES poker_hands (id) ON DELETE CASCADE,
    CONSTRAINT fk_hand_players_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_hand_players_seat CHECK (seat_number BETWEEN 1 AND 9),
    CONSTRAINT chk_hand_players_starting_chips CHECK (starting_table_chips >= 0),
    CONSTRAINT chk_hand_players_ending_chips CHECK (ending_table_chips >= 0),
    CONSTRAINT chk_hand_players_committed CHECK (total_committed >= 0),
    CONSTRAINT chk_hand_players_participation CHECK (participation_state IN ('ACTIVE', 'FOLDED', 'ALL_IN')),
    CONSTRAINT chk_hand_players_leaving_folded CHECK (leaving_at_end = FALSE OR participation_state = 'FOLDED'),
    INDEX idx_hand_players_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_actions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poker_hand_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action_sequence BIGINT NOT NULL,
    phase VARCHAR(20) NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    amount_committed_by_action BIGINT NOT NULL,
    resulting_player_current_bet BIGINT NOT NULL,
    resulting_game_current_bet BIGINT NOT NULL,
    resulting_table_chips BIGINT NOT NULL,
    turn_id VARCHAR(36) NULL,
    client_action_id VARCHAR(36) NULL,
    acted_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_player_actions PRIMARY KEY (id),
    CONSTRAINT uk_player_actions_hand_sequence UNIQUE (poker_hand_id, action_sequence),
    CONSTRAINT fk_player_actions_hand FOREIGN KEY (poker_hand_id) REFERENCES poker_hands (id) ON DELETE CASCADE,
    CONSTRAINT fk_player_actions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_player_actions_sequence_positive CHECK (action_sequence > 0),
    CONSTRAINT chk_player_actions_phase CHECK (phase IN ('PRE_FLOP', 'FLOP', 'TURN', 'RIVER')),
    CONSTRAINT chk_player_actions_type CHECK (action_type IN ('FOLD', 'CHECK', 'CALL', 'BET', 'RAISE', 'ALL_IN')),
    CONSTRAINT chk_player_actions_amount CHECK (amount_committed_by_action >= 0),
    CONSTRAINT chk_player_actions_player_bet CHECK (resulting_player_current_bet >= 0),
    CONSTRAINT chk_player_actions_game_bet CHECK (resulting_game_current_bet >= 0),
    CONSTRAINT chk_player_actions_table_chips CHECK (resulting_table_chips >= 0),
    INDEX idx_player_actions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poker_hand_id BIGINT NOT NULL,
    pot_index INT NOT NULL,
    pot_type VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    contribution_cap BIGINT NOT NULL,
    CONSTRAINT pk_pots PRIMARY KEY (id),
    CONSTRAINT uk_pots_hand_index UNIQUE (poker_hand_id, pot_index),
    CONSTRAINT fk_pots_hand FOREIGN KEY (poker_hand_id) REFERENCES poker_hands (id) ON DELETE CASCADE,
    CONSTRAINT chk_pots_index CHECK (pot_index >= 0),
    CONSTRAINT chk_pots_type CHECK (pot_type IN ('MAIN', 'SIDE')),
    CONSTRAINT chk_pots_type_index CHECK (
        (pot_type = 'MAIN' AND pot_index = 0) OR (pot_type = 'SIDE' AND pot_index > 0)
    ),
    CONSTRAINT chk_pots_amount CHECK (amount > 0),
    CONSTRAINT chk_pots_contribution_cap CHECK (contribution_cap > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pot_awards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pot_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount_awarded BIGINT NOT NULL,
    odd_chip_amount BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_pot_awards PRIMARY KEY (id),
    CONSTRAINT uk_pot_awards_pot_user UNIQUE (pot_id, user_id),
    CONSTRAINT fk_pot_awards_pot FOREIGN KEY (pot_id) REFERENCES pots (id) ON DELETE CASCADE,
    CONSTRAINT fk_pot_awards_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_pot_awards_amount CHECK (amount_awarded > 0),
    CONSTRAINT chk_pot_awards_odd_chip CHECK (odd_chip_amount >= 0 AND odd_chip_amount <= amount_awarded),
    INDEX idx_pot_awards_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE uncalled_bet_returns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poker_hand_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    CONSTRAINT pk_uncalled_bet_returns PRIMARY KEY (id),
    CONSTRAINT uk_uncalled_returns_hand_user UNIQUE (poker_hand_id, user_id),
    CONSTRAINT fk_uncalled_returns_hand FOREIGN KEY (poker_hand_id) REFERENCES poker_hands (id) ON DELETE CASCADE,
    CONSTRAINT fk_uncalled_returns_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_uncalled_returns_amount CHECK (amount > 0),
    INDEX idx_uncalled_returns_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V6__create_player_statistics.sql =====
CREATE TABLE player_statistics (
    user_id BIGINT NOT NULL,
    total_games BIGINT NOT NULL DEFAULT 0,
    total_hands BIGINT NOT NULL DEFAULT 0,
    total_wins BIGINT NOT NULL DEFAULT 0,
    total_losses BIGINT NOT NULL DEFAULT 0,
    win_rate DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    total_chips_won BIGINT NOT NULL DEFAULT 0,
    total_chips_lost BIGINT NOT NULL DEFAULT 0,
    net_chip BIGINT NOT NULL DEFAULT 0,
    largest_pot_won BIGINT NOT NULL DEFAULT 0,
    average_playing_seconds BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_player_statistics PRIMARY KEY (user_id),
    CONSTRAINT fk_player_statistics_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_player_statistics_counts CHECK (
        total_games >= 0 AND total_hands >= 0 AND total_wins >= 0 AND total_losses >= 0
    ),
    CONSTRAINT chk_player_statistics_win_rate CHECK (win_rate >= 0 AND win_rate <= 100),
    CONSTRAINT chk_player_statistics_chips CHECK (
        total_chips_won >= 0 AND total_chips_lost >= 0 AND largest_pot_won >= 0
    ),
    CONSTRAINT chk_player_statistics_playing_time CHECK (average_playing_seconds >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V7__create_player_rankings.sql =====
CREATE TABLE player_rankings (
 user_id BIGINT NOT NULL, rating INT NOT NULL DEFAULT 1000, games_rated BIGINT NOT NULL DEFAULT 0,
 peak_rating INT NOT NULL DEFAULT 1000, updated_at DATETIME(6) NOT NULL,
 CONSTRAINT pk_player_rankings PRIMARY KEY(user_id),
 CONSTRAINT fk_player_rankings_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT chk_player_rankings_rating CHECK(rating >= 0),
 CONSTRAINT chk_player_rankings_games CHECK(games_rated >= 0),
 CONSTRAINT chk_player_rankings_peak CHECK(peak_rating >= 0),
 INDEX idx_player_rankings_board(rating DESC,games_rated DESC,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ranking_history (
 id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, game_session_id BIGINT NOT NULL,
 old_rating INT NOT NULL, new_rating INT NOT NULL, rating_delta INT NOT NULL, session_net BIGINT NOT NULL,
 placement INT NOT NULL, participant_count INT NOT NULL, created_at DATETIME(6) NOT NULL,
 CONSTRAINT pk_ranking_history PRIMARY KEY(id),
 CONSTRAINT uk_ranking_history_user_session UNIQUE(user_id,game_session_id),
 CONSTRAINT fk_ranking_history_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT fk_ranking_history_session FOREIGN KEY(game_session_id) REFERENCES game_sessions(id) ON DELETE CASCADE,
 CONSTRAINT chk_ranking_history_ratings CHECK(old_rating >= 0 AND new_rating >= 0),
 CONSTRAINT chk_ranking_history_placement CHECK(placement > 0 AND participant_count >= 2 AND placement <= participant_count),
 INDEX idx_ranking_history_user_created(user_id,created_at), INDEX idx_ranking_history_session(game_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V8__create_daily_and_weekly_statistics.sql =====
CREATE TABLE daily_statistics (
    user_id BIGINT NOT NULL, stat_date DATE NOT NULL,
    hands_played BIGINT NOT NULL DEFAULT 0, hands_won BIGINT NOT NULL DEFAULT 0,
    hands_lost BIGINT NOT NULL DEFAULT 0, hands_tied BIGINT NOT NULL DEFAULT 0,
    chips_won BIGINT NOT NULL DEFAULT 0, chips_lost BIGINT NOT NULL DEFAULT 0,
    net_chips BIGINT NOT NULL DEFAULT 0, largest_pot_won BIGINT NOT NULL DEFAULT 0,
    playing_time_seconds BIGINT NOT NULL DEFAULT 0, sessions_participated BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_daily_statistics PRIMARY KEY (user_id, stat_date),
    CONSTRAINT fk_daily_statistics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_daily_statistics_counts CHECK (hands_played >= 0 AND hands_won >= 0 AND hands_lost >= 0 AND hands_tied >= 0),
    CONSTRAINT chk_daily_statistics_chips CHECK (chips_won >= 0 AND chips_lost >= 0 AND largest_pot_won >= 0),
    CONSTRAINT chk_daily_statistics_time_sessions CHECK (playing_time_seconds >= 0 AND sessions_participated >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE weekly_statistics (
    user_id BIGINT NOT NULL, week_start_date DATE NOT NULL,
    hands_played BIGINT NOT NULL DEFAULT 0, hands_won BIGINT NOT NULL DEFAULT 0,
    hands_lost BIGINT NOT NULL DEFAULT 0, hands_tied BIGINT NOT NULL DEFAULT 0,
    chips_won BIGINT NOT NULL DEFAULT 0, chips_lost BIGINT NOT NULL DEFAULT 0,
    net_chips BIGINT NOT NULL DEFAULT 0, largest_pot_won BIGINT NOT NULL DEFAULT 0,
    playing_time_seconds BIGINT NOT NULL DEFAULT 0, sessions_participated BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_weekly_statistics PRIMARY KEY (user_id, week_start_date),
    CONSTRAINT fk_weekly_statistics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_weekly_statistics_counts CHECK (hands_played >= 0 AND hands_won >= 0 AND hands_lost >= 0 AND hands_tied >= 0),
    CONSTRAINT chk_weekly_statistics_chips CHECK (chips_won >= 0 AND chips_lost >= 0 AND largest_pot_won >= 0),
    CONSTRAINT chk_weekly_statistics_time_sessions CHECK (playing_time_seconds >= 0 AND sessions_participated >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V9__create_admin_audit_log.sql =====
CREATE TABLE admin_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    request_id VARCHAR(100) NULL,
    metadata_json JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_admin_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_admin_audit_log_admin_user FOREIGN KEY (admin_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_admin_audit_log_action_type CHECK (action_type IN (
        'USER_SUSPENDED', 'USER_REACTIVATED', 'PLAYER_REMOVED_FROM_ROOM', 'ROOM_CLOSED', 'GAME_TERMINATED'
    )),
    CONSTRAINT chk_admin_audit_log_target_type CHECK (target_type IN ('USER', 'ROOM', 'GAME_SESSION')),
    INDEX idx_admin_audit_log_admin_created (admin_user_id, created_at),
    INDEX idx_admin_audit_log_target_created (target_type, target_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V10__create_friendships.sql =====
CREATE TABLE friendships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_user_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    lower_user_id BIGINT GENERATED ALWAYS AS (LEAST(requester_user_id, recipient_user_id)) STORED,
    higher_user_id BIGINT GENERATED ALWAYS AS (GREATEST(requester_user_id, recipient_user_id)) STORED,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_friendships PRIMARY KEY (id),
    CONSTRAINT uk_friendships_canonical_pair UNIQUE (lower_user_id, higher_user_id),
    CONSTRAINT chk_friendships_distinct_users CHECK (requester_user_id <> recipient_user_id),
    CONSTRAINT chk_friendships_canonical_order CHECK (lower_user_id < higher_user_id),
    CONSTRAINT chk_friendships_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT fk_friendships_requester FOREIGN KEY (requester_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_friendships_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_friendships_lower_user FOREIGN KEY (lower_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_friendships_higher_user FOREIGN KEY (higher_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_friendships_recipient_status_created (recipient_user_id, status, created_at),
    INDEX idx_friendships_requester_status_created (requester_user_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V11__create_chat_messages.sql =====
CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    client_message_id CHAR(36) NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT uk_chat_messages_client_command UNIQUE (room_id, sender_user_id, client_message_id),
    CONSTRAINT chk_chat_messages_content_length CHECK (CHAR_LENGTH(content) BETWEEN 1 AND 500),
    CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_chat_messages_room_id (room_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ===== V12__add_game_session_runtime_identity.sql =====
ALTER TABLE game_sessions
    ADD COLUMN game_id CHAR(36) NULL AFTER id,
    ADD CONSTRAINT uk_game_sessions_game_id UNIQUE (game_id);

