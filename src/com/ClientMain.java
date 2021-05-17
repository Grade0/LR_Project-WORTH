package com;

import com.client.ClientService;
import com.exceptions.*;
import com.server.data.CardStatus;
import com.utils.ErrorMSG;
import com.utils.CommunicationProtocol;

import javax.naming.CommunicationException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.Scanner;
import static java.lang.System.exit;

/**
 * @author Davide Chen
 */

public class ClientMain {
    public static void main(String[] args) {

        System.out.println("Welcome on WORTH (WORkTogetHer) Service");
        System.out.println("(enter the 'help' command for the list of available operations)");
        avvioMenu();
        System.out.println("Client Shutdown, bye!");

        exit(0);
    }

    public static void avvioMenu() {
        try {
            ClientService serviceOp = new ClientService();
            Scanner in = new Scanner(System.in);
            boolean finish = false;

            while(!finish) {
                try {
                    System.out.println("\nInsert next command: ");
                    String command = in.nextLine();
                    String[] splittedCommand = command.split(" ");

                    switch (splittedCommand[0].toLowerCase()) {
                        case CommunicationProtocol.REGISTER_CMD:
                            if(splittedCommand.length < 3) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.register(splittedCommand[1], splittedCommand[2]);
                            break;
                        case CommunicationProtocol.LOGIN_CMD:
                            if(splittedCommand.length < 3) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.login(splittedCommand[1], splittedCommand[2]);
                            break;
                        case CommunicationProtocol.LOGOUT_CMD:
                            if(splittedCommand.length < 2) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.logout(splittedCommand[1]);
                            break;
                        case CommunicationProtocol.LIST_USERS_CMD:
                            serviceOp.listUsers();
                            break;
                        case CommunicationProtocol.LIST_ONLINE_USERS_CMD:
                            serviceOp.listOnlineUsers();
                            break;
                        case CommunicationProtocol.LIST_PROJECTS_CMD:
                            serviceOp.listProjects();
                            break;
                        case CommunicationProtocol.CREATE_PROJECT_CMD:
                            if(splittedCommand.length < 2) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.createProject(splittedCommand[1]);
                            break;
                        case CommunicationProtocol.ADD_MEMBER_CMD:
                            if(splittedCommand.length < 3) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.addMember(splittedCommand[1], splittedCommand[2]);
                            break;
                        case CommunicationProtocol.SHOW_MEMBERS_CMD:
                            if(splittedCommand.length < 2) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.showMembers(splittedCommand[1]);
                            break;
                        case CommunicationProtocol.SHOW_CARDS_CMD:
                            if(splittedCommand.length < 2) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.showCards(splittedCommand[1]);
                            break;
                        case CommunicationProtocol.SHOW_CARD_CMD:
                            if(splittedCommand.length < 3) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.showCard(splittedCommand[1], splittedCommand[2]);
                            break;
                        case CommunicationProtocol.ADD_CARD_CMD:
                            if(splittedCommand.length < 4) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else {
                                String desc = "";
                                for (int i = 3; i < splittedCommand.length; i++) {
                                    desc = desc + splittedCommand[i] + " ";
                                }
                                serviceOp.addCard(splittedCommand[1], splittedCommand[2], desc);
                            }
                            break;
                        case CommunicationProtocol.MOVE_CARD_CMD:
                            if(splittedCommand.length < 5) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.moveCard(splittedCommand[1], splittedCommand[2],
                                    CardStatus.retriveFromString(splittedCommand[3]), CardStatus.retriveFromString(splittedCommand[4]));
                            break;
                        case CommunicationProtocol.CARD_HISTORY_CMD:
                            if(splittedCommand.length < 3) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.getCardHistory(splittedCommand[1], splittedCommand[2]);
                            break;
                        case CommunicationProtocol.READ_CHAT_CMD:
                            if(splittedCommand.length < 2) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.readChat(splittedCommand[1]);
                            break;
                        case CommunicationProtocol.SEND_CHAT_CMD:
                            if(splittedCommand.length < 3) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else {
                                String message = "";
                                for (int i = 2; i < splittedCommand.length; i++) {
                                    message = message + splittedCommand[i] + " ";
                                }
                                serviceOp.sendChatMsg(splittedCommand[1], message);
                            }
                            break;
                        case CommunicationProtocol.CANCELPROJECT_CMD:
                            if(splittedCommand.length < 2) System.err.println(ErrorMSG.EMPTY_FIELD);
                            else serviceOp.cancelProject(splittedCommand[1]);
                            break;
                        case "help":
                            System.out.println("\nList of available commands: ");
                            System.out.println("- register           username        password");
                            System.out.println("- login              username        password");
                            System.out.println("- logout             username");
                            System.out.println("- list_users");
                            System.out.println("- list_online_users");
                            System.out.println("- list_projects");
                            System.out.println("- create_project     projectName");
                            System.out.println("- add_member         projectName     username");
                            System.out.println("- show_members       projectName");
                            System.out.println("- show_cards         projectName");
                            System.out.println("- show_card          projectName     cardName");
                            System.out.println("- add_card           projectName     cardName     description");
                            System.out.println("- move_card          projectName     cardName     originList     destinationList");
                            System.out.println("- get_card_history   projectName     cardName");
                            System.out.println("- send_chat          projectName     message");
                            System.out.println("- read_chat          projectName");
                            System.out.println("- cancel_project     projectName");
                            break;
                        case "exit":
                            finish = true;
                            break;
                        default:
                            System.err.println("wrong command, please try again...");
                    }
                } catch (NotBoundException e) {
                    System.out.println(ErrorMSG.REGISTRY_NOT_BOUND);
                } catch (UsernameNotAvailableException e) {
                    System.err.println(ErrorMSG.USERNAME_NOT_AVAILABLE);
                } catch (PasswordTooShortException e) {
                    System.err.println(ErrorMSG.PASSWORD_TOO_SHORT);
                } catch (CharactersNotAllowedException e) {
                    System.err.println(ErrorMSG.CHARACTERS_NOT_ALLOWED);
                } catch (CommunicationException e) {
                    System.err.println(ErrorMSG.CONNECTION_ERROR);
                } catch (AlreadyLoggedInException e) {
                    System.err.println(ErrorMSG.USER_ALREADY_LOGGED);
                } catch (UserNotExistException e) {
                    System.err.println(ErrorMSG.USERNAME_NOT_EXISTS);
                } catch (WrongPasswordException e) {
                    System.err.println(ErrorMSG.PASSWORD_WRONG);
                } catch (NoSuchPortException e) {
                    System.err.println(ErrorMSG.NO_SUCH_PORT);
                } catch (NoSuchAddressException e) {
                    System.err.println(ErrorMSG.NO_SUCH_ADDRESS);
                } catch (ProjectAlreadyExistException e) {
                    System.err.println(ErrorMSG.PROJECT_ALREADY_EXISTS);
                } catch (ProjectNotExistException e) {
                    System.err.println(ErrorMSG.PROJECT_NOT_EXISTS);
                } catch (UnauthorizedUserException e) {
                    System.err.println(ErrorMSG.UNAUTHORIZED_USER);
                } catch (UserAlreadyMemberException e) {
                    System.err.println(ErrorMSG.USER_ALREADY_MEMBER);
                } catch (CardNotExistException e) {
                    System.err.println(ErrorMSG.CARD_NOT_EXISTS);
                } catch (CardAlreadyExistsException e) {
                    System.err.println(ErrorMSG.CARD_ALREADY_EXISTS);
                } catch (OperationNotAllowedException e) {
                    System.err.println(ErrorMSG.OPERATION_NOT_ALLOWED);
                } catch (DatagramTooBigException e) {
                    System.err.println(ErrorMSG.DATAGRAM_TOO_BIG);
                } catch (ChatAddressException e) {
                    System.err.println(ErrorMSG.UNOBTAINABLE_ADDRESS);
                } catch (ProjectNotCancelableException e) {
                    System.err.println(ErrorMSG.PROJECT_NOT_CANCELABLE);
                } catch (UserNotLoggedException e) {
                    System.err.println(ErrorMSG.NOT_LOGGED);
                }

            }

        } catch (IOException e) {
            System.out.println(ErrorMSG.GENERIC_ERROR);
        }
    }
}
