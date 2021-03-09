# LR_Project-WORTH
"Network Programming" Class Project - University of Pisa

## WORTH: WORk TogetHer

End of Course Project A.A. 2020/21 &nbsp; **-** &nbsp; **(Click [[here](/ProgettoWORTH_IT.pdf)] for Italian version)**

### 1. Problem description
In recent years, numerous collaborative applications have been created for content sharing, messaging, videoconferencing, project management, etc. 
In this educational project, WORTH(WORkTogetHer), we will focus on organizing and managing projects in a collaborative way. 
The collaboration and project management applications (e.g. [Trello](https://trello.com/), [Asana](https://asana.com/)) help people get organized and coordinate in carrying out common projects.
These can be professional projects, or in general, any activity can be organized into a series of tasks (e.g. to-do list) which are carried out by members of a group:
the applications of interest are of different types, such as organizing a software development project with colleagues from the development team, but also organizing a party with a group of friends.    
    
Some of these tools (eg Trello) implement the Kanban method (sign or billboard, in Japanese), an “agile” management method. 
The Kanban board provides an overview of the activities and displays their evolution, for example from creation and subsequent progress to completion, after the review phase has been successfully passed.
A person in the working group may take charge of one activity when it has the possibility, moving the activity on the blackboard.    
    
The project consists in the implementation of WORkTogetHer (WORTH): a tool for managing collaborative projects inspired by some principles of the Kanban methodology.

<br>

### 2. Specification of operations

Users can access WORTH after registration and login. In WORTH, a **project**, identified by a unique name, consists of a series of "cards", which represent the tasks to be performed to complete it, and provides a series of services. 
Each project is associated with a list of members, that is users who have permission to change cards and access the services associated with the project (e.g. chat).    
    

A **card** consists of a name and a text description. The name assigned to the card must be unique within a project.
Each project has four associated lists that define the flow of work as a passage of cards from one list to the next: TODO, INPROGRESS, TOBEREVISED, DONE.
Any member of the project can move the card from one list to another, respecting the constraints illustrated in the diagram in FIG. 1.    
    
The newly created cards are automatically added to the TODO list. Any member can move one card from one list to another.
When all the cards are in the DONE list the project can be deleted, from any member participating in the project.    
    
Each project is associated with a **group chat**, and all members of that project, if online (after having after logging in), can receive and send messages on the chat.
On the chat, the system also sends automatically notifications of events related to moving a project card from a list to the other.

<br>

![FIG 1](/Fig1.jpg)
<p align="center"><b>FIG. 1</b></p>
<br>

A **registered** and successful **logged-in** user has permissions for:
* retrieve the list of all registered users of the service;
* retrieve the list of all users registered for the service and connected to the service (in online status);
* create a project;
* retrieve the list of projects of which it is a member.

A user who has created a project automatically becomes a member of it. Can add other users registered as project members.
All members of the project have the same rights (the creator himself is a member like the others), in particular:
* add other registered users as project members;
* retrieve the list of project members;
* create card in the project;
* retrieve the list of cards associated with a project;
* retrieve the information of a specific project card;
* recover the “history” of a specific card of the project (see below for details);
* move any card of the project (respecting the constraints of FIG.1);
* send a message on the project chat;
* read messages from the group chat;
* delete the project.

<br>

The operations offered by the service are specified below. During implementation it is possible add additional parameters as needed.    
    
***register(nickUtente, password):*** to insert a new user, the server provides a user registration operation. The server responds with a code that can indicate successful registration, or, if the nickname is already present, or if the password is empty, it returns an error message. As specified below, records are among the information to be persisted.

***login(nickUtente, password):*** login of an already registered user to access the service. The server responds with a code that can indicate successful login, or, if the user has already logged-in or the password is incorrect, it returns an error message. 

***logout(nickUtente):*** logout the user from the service.

***listUsers():*** used by a user to view the list of user nicknames registered to the service and their status (online or offline).

***listOnlineusers():*** used by a user to view the list of user nicknames registered for the service and online at that time.

***listProjects() :*** operation to retrieve the list of projects of which the user is a member.

***createProject(projectName):*** operation to require the creation of a new project. If the operation is successful, the project is created and has as a member the user who requested the creation.

***addMember(projectName, nickUtente):*** operation to add the user *nickUtente* to the project *projectName*. If the user is registered, the addition as a member is performed without asking for the consent of *nickUtente*, if the user is not registered, the operation cannot be completed and the service returns an error message.

***showMembers(projectName):*** operation to retrieve the list of project members.

***showCards(projectName):*** operazione per recuperare la lista di card associate ad un progetto *projectName*.

***showCard(projectName, cardName):*** operation to retrieve the information (name, text description, list in which it is at that time) of the card *cardName* associated with a project *projectName*.

***addCard(projectName, cardName, descrizione):*** operation to request the card named *cardName* to be added to the *projectName* project. The card must be accompanied by a short descriptive text. The card is automatically added to the TODO list.

***moveCard(projectName, cardName, listaPartenza, listaDestinazione):*** operation to request the move of the card named cardName to the project *projectName* from the list *listDeparture* to the list *listDestination*.

***getCardHistory(projectName, cardName):*** operation to request the “history” of the card, that is the sequence of moving events of the card, from creation to the most recent move.

***readChat(projectName)<sup>1</sup>:*** operation to view *projectName* project chat messages.

***cancelProject(projectName):*** a project member asks to delete a project. The operation can be completed successfully only if all the cards are in the DONE list.

<br>

| &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Permission &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;      | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; User &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;           | &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Project member &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; |
| :-------------: | :-------------: | :-------------: |
| listUsers       | X               |                 |
| listOnlineusers | X               |                 |
| listProjects    | X               |                 |
| createProject   | X               |                 |
| addMember       |                 | X               |
| showMembers     |                 | X               |
| showCards       |                 | X               |
| showCard        |                 | X               |
| addCard         |                 | X               |
| moveCard        |                 | X               |
| getCardHistory  |                 | X               |
| readChat        |                 | X               |
| sendChatMsg     |                 | X               |
| cancelProject   |                 | X               |

<br>
<br>

### 3. Implementation specifications.
Many of the technologies illustrated during the course must be used in the realization of the project. In particular:
* the registration phase is implemented through **RMI**.   
* The login phase must be performed as first operation after establishing a **TCP connection** to the server. In response to the login operation, the server also sends the list of registered users and their status (online, offline)<sup>2</sup>. Following login, the client registers to a server notification service to receive updates on the status of registered users (online / offline). The notification service must be implemented with the **RMI callback** mechanism. The client maintains a data structure to keep track of the list of registered users and their status (online / offline), the list is then updated following the receipt of a callback, through which the server sends updates: new registrations, change status of registered users (online / offline). 
* after successful login, the user interacts, according to the **client-server model** (requests/replies), with the server on the TCP connection created, by sending the commands listed above. All operations are performed on this TCP connection, except the registration (RMI), the operations of retrieving the list of users (listUsers and listOnlineusers) that use the client's local data structure updated via the RMI callback mechanism (as described in previous point) and chat operations.
* The server can be **multithreaded** or **multiplexing channels via NIO**.
* The user interacts with WORTH through a client that can use a simple graphical interface, or a command line interface, defining a set of commands, presented in a menu.
* A separate data structure must be implemented for each project list (i.e. four lists per project).
* The project chat must be done using **multicast UDP** (a client can send messages directly to other clients). Each project chat has a different **multicast** IP address, chosen by the server when creating the project. The way in which the server communicates to the clients the references to join the chat is chosen by the student (to be motivated in the report).
* **Implementation of the chat:** in case you decide to implement the graphical interface, it will provide two simple text areas where respectively insert/ receive text messages sent to the chat. In this case, messages are immediately presented to the user as they are received. Instead, if you prefer a command line interaction with WORTH, two commands will be defined to send new messages to the chat/receive all messages received since the last execution of the message display command. In this case, the messages are presented to the user asynchronously, at his request.
* The server persists system status, in particular: the registration information, the project list (including members, cards and lists status). **The status of the projects must be persistent on the file system** as described below: a directory for each project and a file for each card of the project (the move events related to the card are appended to the file). Chat messages must not be persisted. When the server is restarted this information is used to rebuild the system state.
