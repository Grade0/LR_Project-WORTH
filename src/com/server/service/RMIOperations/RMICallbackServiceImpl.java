package com.server.service.RMIOperations;

import com.server.data.UserStatus;
import com.client.RMICallbackNotify;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * @author Davide Chen
 *
 * Implementation of the server-side callback service
 */
public class RMICallbackServiceImpl extends UnicastRemoteObject implements RMICallbackService {
    Map<String, RMICallbackNotify> clients;

    /*
     * NOTE: when a client is abruptly interrupted it will never unregister
     * from the rmi service and, when a notification is sent,
     * the server reports connection errors with the aforementioned client
     * for this reason, if there are errors due to the notification
     * the client will be directly de-registered from the callback service
     */
    List<String> toDelete;

    public RMICallbackServiceImpl() throws RemoteException {
        // hashtable is preferred because is thread-safe
        // and doesn't allow any null key or value
        clients = new Hashtable<>();
        toDelete = new ArrayList<>();
    }

    @Override
    public void registerForCallback(String username, RMICallbackNotify client) throws RemoteException {
        clients.put(username, client);
    }

    @Override
    public void unregisterForCallback(String username) throws RemoteException {
        clients.remove(username);
    }

    /**
     * @param username to be notified to clients
     * @param status of the user to be notified
     *
     */
    public void notifyUsers(String username, UserStatus status) {
        doCallbacks(username, status);
    }

    public synchronized void notifyProject(String username, String projectName, String chatAddressAndPort) {
        RMICallbackNotify client = clients.get(username);

        // client == null when the user is offline
        // thus I don't have to notify anything
        if(client != null) {
            try {
                client.notifyNewProject(projectName, chatAddressAndPort);
            } catch (RemoteException e) {
                toDelete.add(username);
            }
        }
    }

    private synchronized void doCallbacks(String username, UserStatus status) {
        for (Map.Entry<String, RMICallbackNotify> entry : clients.entrySet()) {
            try {
                entry.getValue().notifyUserStatus(username, status);
            } catch (RemoteException e) {
                toDelete.add(entry.getKey());
            }
        }
        while (!toDelete.isEmpty()) {
            clients.remove(toDelete.remove(0));
        }
    }
}