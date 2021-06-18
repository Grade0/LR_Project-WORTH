package com.client;

import com.data.UserStatus;

import java.io.IOException;
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
    private final Map<String, ProjectChatTask> projectChats;

    public RMICallbackNotifyImpl(Map<String, UserStatus> userStatus,
                                 Map<String, ProjectChatTask> projectChats) throws RemoteException {
        super();
        this.userStatus = userStatus;
        this.projectChats = projectChats;
    }

    @Override
    public synchronized void notifyUserStatus(String username, UserStatus status) throws RemoteException {
        // If the map previously contained a mapping for the key,
        // the old value is replaced by the specified value.
        this.userStatus.put(username, status);
    }

    public synchronized void notifyNewProject(String projectName, String chatAddressAndPort) throws RemoteException {
        String[] tokens = chatAddressAndPort.split(":");
        String chatAddress = tokens[0];
        int port = Integer.parseInt(tokens[1]);

        try {
            ProjectChatTask newChat = new ProjectChatTask(chatAddress, port);
            this.projectChats.put(projectName, newChat);
            new Thread(newChat).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("System ntf: You have been added to a new project: " + projectName);
    }

    // non ha bisogno della keyword synchronized poiché
    // è chiamato una singola volta dal server
    public void notifyCloseClient() {
        System.err.println("System ntf: Server is offline, please close the application");
    }
}
