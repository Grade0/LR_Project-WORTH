package com.server.data;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;

/**
 * @author Davide Chen
 *
 * Represents the user of the service
 */

public class User implements Serializable {
    private String username;
    private String encPassword;
    private String salt;

    public User(String user, String encPsswd, String salt) {
        this.username = user;
        this.encPassword = encPsswd;
        this.salt = salt;
    }

    @JsonCreator
    private User() {}

    public String getUsername() {
        return this.username;
    }

    public String getEncPassword() {
        return this.encPassword;
    }

    public String getSalt() {
        return this.salt;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        return this.username.equals(((User)o).getUsername());
    }
}
