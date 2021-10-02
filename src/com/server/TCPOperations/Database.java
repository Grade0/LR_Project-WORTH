package com.server.TCPOperations;

import com.CommunicationProtocol;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.data.*;
import com.exceptions.*;
import com.utils.MulticastAddressManager;
import com.utils.PasswordManager;
import com.utils.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Davide Chen
 *
 * Class that acts as a database, containing all application data
 */
public class Database implements UserRegistration, TCPOperations {
    private final static String STORAGE_FOLDER_PATH = "./database/";
    private final static String PROJECTS_FOLDER_PATH = STORAGE_FOLDER_PATH + "projects/";
    private final static String USERS_FOLDER_PATH = STORAGE_FOLDER_PATH + "users/";
    private final static String PROJECT_CONFIG_FILENAME = "info.json";
    // buffer allocation space
    private static final int BUFFER_SIZE = 1024*1024;
    private final Map<String, User> users;
    private final Map<String, Project> projects;
    private final Map<String, UserStatus> userStatus;
    private final ObjectMapper mapper;

    public Database() throws IOException {
        this.users = new ConcurrentHashMap<>();
        this.projects = new HashMap<>();
        this.userStatus = new ConcurrentHashMap<>();

        // Jackson object
        mapper = new MyObjectMapper();

        this.init();
    }

    private void init() throws IOException {
        System.out.println("Server data initialization - start");

        // folder management
        File directory = new File(STORAGE_FOLDER_PATH);
        if (!directory.exists()){
            directory.mkdirs();
            System.out.format("Folder %s created\n", STORAGE_FOLDER_PATH);
        }
        directory = new File(PROJECTS_FOLDER_PATH);
        if (!directory.exists()){
            directory.mkdirs();
            System.out.format("Folder %s created\n", PROJECTS_FOLDER_PATH);
        }
        directory = new File(USERS_FOLDER_PATH);
        if (!directory.exists()){
            directory.mkdirs();
            System.out.format("Folder %s created\n", USERS_FOLDER_PATH);
        }

        // buffer allocation
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        //FileFilter filters only files with .json extension
        FileFilter fileFilter =
                pathname -> pathname.isFile() && pathname.getPath().endsWith(".json");

        // loading of users
        int numOfUsers = 0;
        directory = new File(USERS_FOLDER_PATH);
        // get user list
        File[] usersList = directory.listFiles(fileFilter);
        if (usersList != null) {
            for (File userFile : usersList) {
                // load user in the data structure
                this.loadUser(userFile, buffer);

                numOfUsers++;
            }
        }

        // loading projects
        int numOfProjects = 0;
        directory = new File(PROJECTS_FOLDER_PATH);
        // get list of project directories
        File[] projectsList = directory.listFiles(File::isDirectory);
        if (projectsList != null) {
            for (File projectDir : projectsList) {
                // each project contains the project file and the card files
                File[] projectFileList = projectDir.listFiles(fileFilter);
                if (projectFileList != null) {
                    if (projectFileList.length == 0) {
                        System.out.format("Project %s doesn't have config file (%s)\n",
                                projectDir.getName(), PROJECT_CONFIG_FILENAME);
                    }
                    // list of project cards
                    List<CardImpl> projectCardList = new ArrayList<>();
                    File projectInfo = null;
                    // card processing
                    for (File cardFile : projectFileList) {
                        // if the file is the project file, I save it and I process it at the end
                        if (cardFile.getName().equals(PROJECT_CONFIG_FILENAME)) {
                            projectInfo = cardFile;
                            continue;
                        }

                        //loading the card into the project card list
                        this.loadCard(cardFile, buffer, projectCardList);
                    }


                    // now I can process the project and load it on the data structure
                    try {
                        this.loadProject(projectInfo, buffer, projectCardList);
                    } catch (NoSuchAddressException e) {
                        System.err.println("There are no more multicast addresses...");
                        throw new IOException();
                    } catch (AlreadyInitialedException e) {
                        System.err.println("Project seems to be already initialized...");
                        throw new IOException();
                    } catch (NoSuchPortException e) {
                        System.err.println("There are no more ports...");
                        throw new IOException();
                    }

                    numOfProjects++;
                }
            }
        }

        System.out.format(CommunicationProtocol.ANSI_YELLOW + "%d users retrieved\n", numOfUsers);
        System.out.format("%d projects retrieved\n", numOfProjects);
        System.out.println(CommunicationProtocol.ANSI_GREEN + "Server data initialization - successful");
    }

    @Override
    public void registerUser(String username, String hash, String salt) throws UsernameNotAvailableException {
        User newUser = new User(username, hash, salt);
        if (this.users.putIfAbsent(username, newUser) != null)
            throw new UsernameNotAvailableException();
        this.userStatus.put(username, UserStatus.OFFLINE);

        // saving user to file
        try {
            this.storeUser(newUser);
        } catch (IOException e) {
            e.printStackTrace();
            this.users.remove(username);
        }
    }

    @Override
    public void login(String username, String password)
            throws UserNotExistException, AlreadyLoggedInException, WrongPasswordException {
        User theUser = this.users.get(username);
        if (theUser == null) {
            throw new UserNotExistException();
        } else {
            UserStatus status = this.userStatus.get(username);
            if (status == UserStatus.ONLINE) {
                throw new AlreadyLoggedInException();
            }
            String hash = theUser.getHashPassword();
            String salt = theUser.getSalt();
            PasswordManager passwordManager = new PasswordManager();
            if (!passwordManager.isExpectedPassword(password, salt, hash))
                throw new WrongPasswordException();
            this.userStatus.replace(username, UserStatus.ONLINE);
        }
    }

    @Override
    public void logout(String username) throws UserNotExistException {
        User theUser = this.users.get(username);
        if (theUser == null) {
            throw new UserNotExistException();
        } else {
            this.userStatus.replace(username, UserStatus.OFFLINE);
        }
    }

    @Override
    public List<Project> listProjects(String username) throws UserNotExistException {
        if (!this.users.containsKey(username))
            throw new UserNotExistException();
        List<Project> toReturn = new ArrayList<>();
        for (String key : this.projects.keySet()) {
            Project p = this.projects.get(key);
            if (p.getMembers().contains(username))
                toReturn.add(p);
        }
        return toReturn;
    }

    @Override
    public void createProject(String projectName, String whoRequest)
            throws ProjectAlreadyExistException, NoSuchAddressException, IOException, NoSuchPortException {
        if (this.projects.containsKey(projectName))
            throw new ProjectAlreadyExistException();
        Project newProject = new Project(projectName, whoRequest);
        this.storeProject(newProject);
        this.projects.put(projectName, newProject);
    }

    @Override
    public void addMember(String projectName, String username, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, UserAlreadyMemberException, UserNotExistException, IOException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        if (!this.users.containsKey(username))
            throw new UserNotExistException();
        project.addMember(username);

        this.storeProject(project);
    }

    @Override
    public List<String> showMembers(String projectName, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        List<String> members = project.getMembers();
        if (!members.contains(whoRequest))
            throw new UnauthorizedUserException();
        return members;
    }

    @Override
    public Map<CardStatus, List<String>> showCards(String projectName, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        return project.getStatusLists();
    }

    @Override
    public CardImpl showCard(String projectName, String cardName, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException, CardNotExistException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        return project.getCard(cardName);
    }

    @Override
    public void addCard(String projectName, String cardName, String description, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException, CardAlreadyExistsException, IOException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        CardImpl newCard = new CardImpl(cardName, description);
        project.addCard(newCard);

        this.storeCard(newCard, projectName);
        this.storeProject(project);
    }

    @Override
    public void moveCard(String projectName, String cardName, CardStatus from, CardStatus to, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException, CardNotExistException, OperationNotAllowedException, IOException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        project.moveCard(cardName, from, to);
        CardImpl moved = project.getCard(cardName);

        this.storeCard(moved, projectName);
        this.storeProject(project);
    }

    @Override
    public List<Movement> getCardHistory(String projectName, String cardName, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException, CardNotExistException {
        Project project;
        if ((project = this.projects.get(projectName)) == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        CardImpl card = project.getCard(cardName);
        return card.getMovements();
    }

    @Override
    public void cancelProject(String projectName, String whoRequest) throws ProjectNotExistException, UnauthorizedUserException, ProjectNotCancelableException {
        Project project = this.projects.get(projectName);
        if (project == null)
            throw new ProjectNotExistException();
        if (!project.getMembers().contains(whoRequest))
            throw new UnauthorizedUserException();
        if (!project.isCancelable())
            throw new ProjectNotCancelableException();
        // free the multicast address
        MulticastAddressManager.freeAddress(project.getChatAddress());
        // remove the project
        this.projects.remove(projectName);

        // removing project files
        File projectDir = new File(PROJECTS_FOLDER_PATH + projectName);
        if (projectDir.exists() && projectDir.isDirectory()) {
            //elimino tutti i file al suo interno
            File[] files = projectDir.listFiles();
            for (File file : files) {
                file.delete();
            }
            // delete the directory
            projectDir.delete();
        }
    }

    @Override
    public Map<String, UserStatus> getUserStatus() {
        return this.userStatus;
    }

    @Override
    public String getProjectChatAddress(String projectName) throws ProjectNotExistException {
        Project project;
        project = this.projects.get(projectName);
        if (project == null)
            throw new ProjectNotExistException();
        return project.getChatAddress();
    }

    /**
     * Saving the user to storage by serializing it
     *
     * @param user to save
     *
     * @throws IOException if there are errors in saving the project
     */
    private void storeUser(User user) throws IOException {
        String fileName = USERS_FOLDER_PATH + user.getUsername() + ".json";

        byte[] byteUser = mapper.writeValueAsBytes(user);

        this.storeFile(new File(fileName), byteUser);
    }

    /**
     * Saving the card to storage of the project projectName
     *
     * @param card to save
     * @param projectName name of the project to which the card refers
     *
     * @throws IOException if there are errors in saving the project
     */
    private void storeCard(CardImpl card, String projectName) throws IOException {
        String fileName = PROJECTS_FOLDER_PATH + projectName + "/card_" + card.getName() + ".json";

        byte[] byteCard = mapper.writeValueAsBytes(card);

        this.storeFile(new File(fileName), byteCard);
    }

    /**
     * Salving the progect to storage
     *
     * @param project to save
     *
     * @throws IOException if there are errors in saving the project
     */
    private void storeProject(Project project) throws IOException {
        String projectFolder = PROJECTS_FOLDER_PATH + project.getName() + "/";
        String fileName = projectFolder + PROJECT_CONFIG_FILENAME;
        File projectFolderFile = new File(projectFolder);
        if (!projectFolderFile.exists()) {
            projectFolderFile.mkdir();
        }
        byte[] byteProject = mapper.writeValueAsBytes(project);

        this.storeFile(new File(fileName), byteProject);
    }

    /**
     * Deserializing the user defined by the userFile
     *
     * @param userFile file of the user
     * @param buffer used for loading
     *
     * @throws IOException if there are errors in the upload
     */
    private void loadUser(File userFile, ByteBuffer buffer) throws IOException {
        // loading the file on the buffer
        this.readFile(userFile, buffer);

        // reading from user's buffer
        User user = mapper.reader().forType(new TypeReference<User>() {})
                .readValue(buffer.array());

        // insert in the data structures
        this.users.put(user.getUsername(), user);
        this.userStatus.put(user.getUsername(), UserStatus.OFFLINE);
    }

    /**
     * Deserializing a card defined by the file cardFile
     *
     * @param cardFile file of the card
     * @param buffer used for loading
     * @param projectCardList list of the cards of the project to process
     *
     * @throws IOException if there are errors in the upload
     */
    private void loadCard(File cardFile, ByteBuffer buffer, List<CardImpl> projectCardList)
            throws IOException {
        // loading the file on the buffer
        this.readFile(cardFile, buffer);

        // reading from card's buffer
        CardImpl card = mapper.reader().forType(new TypeReference<CardImpl>() {})
                .readValue(buffer.array());
        projectCardList.add(card);

    }

    /**
     * Deserializing a project defined by the file projectInfo
     *
     * @param projectInfo file of the project
     * @param buffer used for loading
     * @param projectCardList list of the cards of the project to process
     *
     * @throws IOException if there are errors in the upload
     * @throws AlreadyInitialedException if there are fields of the project that have already been initialized
     * @throws NoSuchPortException if there are no more ports available
     * @throws NoSuchAddressException if there are no more addresses available
     */
    private void loadProject(File projectInfo, ByteBuffer buffer, List<CardImpl> projectCardList)
            throws IOException, NoSuchAddressException, AlreadyInitialedException, NoSuchPortException {
        // loading the file on the buffer
        this.readFile(projectInfo, buffer);

        // reading from project's buffer
        Project project = mapper.reader().forType(new TypeReference<Project>() {})
                .readValue(buffer.array());

        project.initCardList(projectCardList);
        project.initChatAddress(MulticastAddressManager.getAddress());

        this.projects.put(project.getName(), project);
    }

    /**
     Loading the buffer to the file
     *
     * @param file to upload
     * @param buffer used for loading
     *
     * @throws IOException if there are errors in the upload
     */
    private void readFile(File file, ByteBuffer buffer) throws IOException {
        if (file == null)
            // should not happen
            throw new IOException();
        buffer.clear();
        try (FileChannel inChannel = FileChannel.open(Paths.get(file.getAbsolutePath()), StandardOpenOption.READ)) {
            boolean stop = false;
            while (!stop) {
                if (inChannel.read(buffer) == -1) {
                    stop = true;
                }
            }
            buffer.flip();
        }
    }

    /**
     * Upload file to buffer
     *
     * @param file to upload
     * @param fileByte data to write to the file
     *
     * @throws IOException if there are errors in the upload
     *
     */
    private void storeFile(File file, byte[] fileByte) throws IOException {
        if (file == null)
            throw new IOException(); // impossibile non esista

        try (FileChannel outChannel = FileChannel.open(
                Paths.get(file.getAbsolutePath()),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            ByteBuffer bb = ByteBuffer.wrap(fileByte);

            while (bb.hasRemaining())
                outChannel.write(bb);
        }
    }
}

