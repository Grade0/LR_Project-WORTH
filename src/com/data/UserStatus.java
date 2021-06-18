package com.data;

import java.io.Serializable;

/**
 * @author Davide Chen
 *
 * Represents the status of a registered user
 */
public enum UserStatus implements Serializable {
    ONLINE,
    OFFLINE
}
