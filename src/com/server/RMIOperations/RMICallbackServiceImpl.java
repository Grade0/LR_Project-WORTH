package com.server.RMIOperations;

import com.data.UserStatus;
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
        super();
        // HashTable is preffered here because
        // it doesn't allow any null key or value
        clients = new Hashtable<>();
        toDelete = new ArrayList<>();
    }

    @Override
    public synchronized void registerForCallback(String username, RMICallbackNotify client) throws RemoteException {
        clients.put(username, client);
    }

    @Override
    public synchronized void unregisterForCallback(String username) throws RemoteException {
        clients.remove(username);
    }

    /**
     * @param username to be notified to clients
     * @param status of the user to be notified
     *
     */
    public synchronized void notifyUsers(String username, UserStatus status) {
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

    public synchronized void notifyProject(String username, String projectName, String chatAddress) {
        RMICallbackNotify client = clients.get(username);

        // client == null when the user is offline
        // thus I don't have to notify anything
        if(client != null) {
            try {
                client.notifyNewProject(projectName, chatAddress);
            } catch (RemoteException e) {
                toDelete.add(username);
            }
        }
    }


    public synchronized void notifyServerDown() {
        for (RMICallbackNotify client : clients.values()) {
            try {
                client.notifyCloseClient();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public synchronized void terminateChat(String projectName, List<String> members) {
        for(String username : members) {
            RMICallbackNotify client = clients.get(username);

            // client == null when the user is offline
            // thus I don't have to notify anything
            if(client != null) {
                try {
                    client.leaveMulticastGroup(projectName);
                } catch (RemoteException e) {
                    toDelete.add(username);
                }
            }
        }
    }
}