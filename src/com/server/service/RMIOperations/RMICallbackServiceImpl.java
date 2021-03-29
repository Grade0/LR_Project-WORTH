package com.server.service.RMIOperations;

import com.server.data.UserStatus;
import com.client.RMICallbackNotify;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Davide Chen
 *
 * Implementation of the server-side callback service
 */
public class RMICallbackServiceImpl extends UnicastRemoteObject implements RMICallbackService {
    List<RMICallbackNotify> clients;

    /*
     * NOTE: when a client is abruptly interrupted it will never unregister
     * from the rmi service and, when a notification is sent,
     * the server reports connection errors with the aforementioned client
     * for this reason, if there are errors due to the notification
     * the client will be directly de-registered from the callback service
     */
    List<RMICallbackNotify> toDelete;

    public RMICallbackServiceImpl() throws RemoteException {
        clients = new ArrayList<>();
        toDelete = new ArrayList<>();
    }

    @Override
    public synchronized void registerForCallback(RMICallbackNotify client) throws RemoteException {
        if (!clients.contains(client)) {
            clients.add(client);
        }
    }

    @Override
    public synchronized void unregisterForCallback(RMICallbackNotify client) throws RemoteException {
        clients.remove(client);
    }

    /**
     * @param username to be notified to clients
     * @param status of the user to be notified
     *
     */
    public void notifyUsers(String username, UserStatus status) {
        doCallbacks(username, status);
    }

    private synchronized void doCallbacks(String username, UserStatus status) {
        for (RMICallbackNotify client : clients) {
            try {
                client.notifyUpdate(username, status);
            } catch (RemoteException e) {
                toDelete.add(client);
            }
        }
        while (!toDelete.isEmpty()) {
            clients.remove(toDelete.remove(0));
        }
    }
}