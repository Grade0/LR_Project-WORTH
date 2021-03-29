package com.client;

import com.server.data.UserStatus;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * @author Davide Chen
 *
 * Implementation of the client-side callback service
 */
public class RMICallbackNotifyImpl extends UnicastRemoteObject implements RMICallbackNotify {
    private final Map<String, UserStatus> userStatus;

    public RMICallbackNotifyImpl(Map<String, UserStatus> userStatus) throws RemoteException {
        super();
        this.userStatus = userStatus;
    }

    @Override
    public synchronized void notifyUpdate(String username, UserStatus status) throws RemoteException {
        this.userStatus.put(username, status);
    }


}
