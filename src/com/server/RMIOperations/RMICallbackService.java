package com.server.RMIOperations;

import com.client.RMICallbackNotify;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author Davide Chen
 *
 * Server-side interface of the callback service
 */
public interface RMICallbackService extends Remote {

    /**
     * @param username of the client
     * @param client to register for the service
     *
     * @throws RemoteException if there are connection errors
     * */
    void registerForCallback(String username, RMICallbackNotify client) throws RemoteException;

    /**
     * @param username of the client to be de-registered to the service
     *
     * @throws RemoteException if there are connection errors
     * */
    void unregisterForCallback(String username) throws RemoteException;

    /**
     *
     * @throws RemoteException if there are connection errors
     * */
    void notifyServerDown() throws RemoteException;

    /**
     * @param projectName to which it refers
     * @param members to be notified
     *
     * @throws RemoteException if there are connection errors
     * */
    void terminateChat(String projectName, List<String> members) throws RemoteException;
}
