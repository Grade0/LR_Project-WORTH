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
    void notifyUserStatus(String username, UserStatus status) throws RemoteException;

    /**
     *
     * @param projectName of the new project
     * @param chatAddressAndPort of the new project chat
     * @throws RemoteException if there are errors with the rmi service
     */
    void notifyNewProject(String projectName, String chatAddressAndPort) throws RemoteException;
}
