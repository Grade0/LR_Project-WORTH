package com.server.RMIOperations;

import com.server.TCPOperations.UserRegistration;
import com.utils.CommunicationProtocol;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Davide Chen
 *
 * Task for setting up and publishing the RMI Regitry
 * of the registration and callback services
 */
public class RMITask implements Runnable {
    private Registry registry;
    private RMIRegistrationService registrationService;
    private final UserRegistration registration;
    private final RMICallbackServiceImpl callbackService;

    public RMITask(UserRegistration registration, RMICallbackServiceImpl callbackService) {
        this.registration = registration;
        this.callbackService = callbackService;
    }

    @Override
    public void run() {
        try {
            // creation of the registry on the REG_PORT port
            registry = LocateRegistry.createRegistry(CommunicationProtocol.REGISTRY_PORT);

            // publishing the callback stub in the registry
            registry.bind(CommunicationProtocol.CALLBACK_SERVICE_NAME, callbackService);
            System.out.println("Callback service is now available");

            // Initialization of the RMIRegistrationService
            registrationService = new RMIRegistrationServiceImpl(registration, callbackService);

            // publish the registration stub in the registry
            registry.bind(CommunicationProtocol.REGISTRATION_SERVICE_NAME, registrationService);
            System.out.println("Registration service is now available");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }


    public void unbindRegistry() {
        try {
            registry.unbind(CommunicationProtocol.CALLBACK_SERVICE_NAME);
            registry.unbind(CommunicationProtocol.REGISTRATION_SERVICE_NAME);
            UnicastRemoteObject.unexportObject(registrationService, true);
            UnicastRemoteObject.unexportObject(callbackService, true);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

}
