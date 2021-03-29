package com.server.service.RMIOperations;

import com.server.data.UserStatus;
import com.utils.CommunicationProtocol;
import com.exceptions.PasswordTooShortException;
import com.exceptions.CharactersNotAllowedException;
import com.exceptions.UsernameNotAvailableException;
import com.server.service.TCPOperations.LocalRegistration;
import com.utils.SuccessMSG;
import com.utils.PasswordManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Davide Chen
 *
 * Implementation of the RMI registration service
 */
public class RMIRegistrationServiceImpl extends UnicastRemoteObject implements RMIRegistrationService {
    private LocalRegistration registration;
    private RMICallbackServiceImpl callbackService;
    private PasswordManager passwordManager;

    public RMIRegistrationServiceImpl(LocalRegistration registration, RMICallbackServiceImpl callbackService) throws RemoteException {
        super();
        this.callbackService = callbackService;
        this.registration = registration;
        passwordManager = new PasswordManager();
    }

    @Override
    public synchronized String register (String username, String password)
            throws RemoteException, CharactersNotAllowedException, UsernameNotAvailableException, PasswordTooShortException {
        if (!username.matches(CommunicationProtocol.STRING_REGEX))
            throw new CharactersNotAllowedException();
        if (password.length() < CommunicationProtocol.MIN_PASSWORD_LEN)
            throw new PasswordTooShortException();

        String salt = passwordManager.getSalt();
        String hash = passwordManager.hash(password, salt);

        registration.registerUser(username, hash, salt);

        // notifies users that the user 'username' has registered
        callbackService.notifyUsers(username, UserStatus.OFFLINE);

        return SuccessMSG.REGISTRATION_SUCCESSFUL;
    }
}
