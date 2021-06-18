package com.server.RMIOperations;

import com.exceptions.CharactersNotAllowedException;
import com.exceptions.PasswordTooShortException;
import com.exceptions.UsernameNotAvailableException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Davide Chen
 *
 * RMI registration service interface
 */
public interface RMIRegistrationService extends Remote {

    /**
     * User registration on WORTH server
     *
     * @param username of the user
     * @param password of the user
     *
     * @throws RemoteException if there are connection problems
     * @throws CharactersNotAllowedException if the username contains spaces
     * @throws UsernameNotAvailableException if the username is not available
     * @throws PasswordTooShortException if the password size is shorter than MIN_PASS_LEN
     *
     * @return success code, if the operation went through
     */
    String register(String username, String password)
            throws RemoteException, PasswordTooShortException, CharactersNotAllowedException, UsernameNotAvailableException;
}
