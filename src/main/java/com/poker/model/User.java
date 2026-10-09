package com.poker.model;

import com.poker.model.auth.AccountStatus;
import com.poker.model.auth.Role;

public record User(long id, String username, String displayName, String email,
                   Role role, AccountStatus status, long accountChips, String avatarUrl) { }
