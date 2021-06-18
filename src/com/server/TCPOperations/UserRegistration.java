package com.server.TCPOperations;

import com.exceptions.UsernameNotAvailableException;


/**
 * @author Davide Chen
 *
 * Interface for a support operation for RMIRegistrationService
 */
public interface UserRegistration {

    /**
     * User registration on WORTH server
     *
     * @param username of the user
     * @param hash of the user's password
     * @param salt used to generate the password
     *
     * @throws UsernameNotAvailableException if the username is not available
     */
    void registerUser(String username, String hash, String salt) throws UsernameNotAvailableException;

}
