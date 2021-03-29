package com.client;

import com.server.data.UserStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Davide Chen
 *
 * Client-side interface of the RMI callback service
 */
public interface RMICallbackNotify extends Remote {

    /**
     *
     * @param username to be notified about
     * @param status the user new status
     * @throws RemoteException if there are errors with the rmi service
     */
    void notifyUpdate(String username, UserStatus status) throws RemoteException;
}
