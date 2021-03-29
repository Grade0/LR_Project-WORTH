package com.server.service.RMIOperations;

import com.client.RMICallbackNotify;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Davide Chen
 *
 * Server-side interface of the callback service
 */
public interface RMICallbackService extends Remote {

    /**
     * @param client to register for the service
     *
     * @throws RemoteException if there are connection errors
     * */
    void registerForCallback(RMICallbackNotify client) throws RemoteException;

    /**
     * @param client to be de-registered to the service
     *
     * @throws RemoteException if there are connection errors
     * */
    void unregisterForCallback(RMICallbackNotify client) throws RemoteException;

}
