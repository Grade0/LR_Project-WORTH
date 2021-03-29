package com;

import com.server.service.TCPOperations.Database;
import com.server.service.RMIOperations.RMITask;
import com.server.service.TCPOperations.SelectionTask;
import com.server.service.RMIOperations.RMICallbackServiceImpl;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * @author Davide Chen
 */
public class ServerMain {

    public static void main (String[] args) {

        // Upload of all persistent data
        Database data;
        try {
            data = new Database();
        } catch (IOException e) {
            System.out.println("Data initialization failed. Stack Trace: ");
            e.printStackTrace();
            return;
        }

        // creation object for callback
        RMICallbackServiceImpl callbackService;
        try {
            callbackService = new RMICallbackServiceImpl();
        } catch (RemoteException e) {
            System.out.println("RMI callback service initialization failed. Stack Trace: ");
            e.printStackTrace();
            return;
        }

        // run task user registration
        RMITask registrationTask = new RMITask(data, callbackService);
        new Thread(registrationTask).start();

        // TCP connection management
        SelectionTask selectionTask = new SelectionTask(data, callbackService);
        new Thread(selectionTask).start();

    }

}
