# SimpleSocial

A Simple Social Network written in java, for my network project.

## SocialServer

Java Server for creating the SimpleSocial Network. 

### Usage

type on your shell

```
java -jar ./SocialServer.jar
```
the server is online!

### Configuration

for change your connection settings open the _./conf_ file and change right values:

```
IP_ADDRESS:localhost
IP_MULTICAST_GROUP:225.3.0.1
TCP_PORT:2000
MULTICAST_PORT:3000
RMI_PORT:4444
FRIEND_REQUEST_MIN:1
SAVE_TIME_SECONDS:20
```
* *FRIEND_REQUEST_MIN*: change validation time for friendship requests.
* *SAVE_TIME_SECONDS* : change time interval for saving server data.

## SocialClient 

Client for *SimpleSocial* network project. 

### Usage

#### Step 1 - User Registragion
type on your shell

```
java -jar ./SocialClient.jar register [username] [password]
```

you will receive a confirmation message, SocialClient will closed.

#### Step 2 - User Login
After registration type on your shell

```
java -jar ./SocialClient.jar login [username] [password]
```
you will logged into SimpleSocial Network

#### Step 3 - User commands

After login type one of this following commands

* *help*: receive help message
* *find [username]*: find users in SimpleSocial Network
* *friend [username]*: send friend request to user.
* *unfriend [username]*: remove friend request or friendship from user
* *friends*: get friends list and its status
* *getreq*: show received friendship requests (WARING: can expire if yuo don't accept it in time)
* *accept [username]*: accept friendship from user
* *follow [friend]*: see contents of your friend.
* *send [press ENTER] -> [type content]*: for write and send a content.
* *contents*: see contents of your following users
* *logout*: logging out from SimpleSocial Network

### Configuration

for change your connection settings open the _./confClient_ file and change right values:

```
IP_ADDRESS:localhost
IP_MULTICAST_GROUP:225.3.0.1
TCP_PORT:2000
MULTICAST_PORT:3000
RMI_PORT:4444
```

### Example

```   
>java -jar ./SocialClient.jar register darthVader bestFather 

 ______    ____    ___     _  _____    ____     ______ 
|   ___|  |    |  |    \  / ||     |  |    |   |   ___|
 `-.`-.   |    |  |     \/  ||    _|  |    |_  |   ___|
|______|  |____|  |__/\__/|_||___|    |______| |______|
 ______   _____    ______    ____    ____     ____   
|   ___| /     \  |   ___|  |    |  |    \   |    |  
 `-.`-.  |     |  |   |__   |    |  |     \  |    |_ 
|______| \_____/  |______|  |____|  |__|\__\ |______|

Server: DarthVader signed in

>java -jar ./SocialClient.jar login darthVader bestFather 

 ______    ____    ___     _  _____    ____     ______ 
|   ___|  |    |  |    \  / ||     |  |    |   |   ___|
 `-.`-.   |    |  |     \/  ||    _|  |    |_  |   ___|
|______|  |____|  |__/\__/|_||___|    |______| |______|
 ______   _____    ______    ____    ____     ____   
|   ___| /     \  |   ___|  |    |  |    \   |    |  
 `-.`-.  |     |  |   |__   |    |  |     \  |    |_ 
|______| \_____/  |______|  |____|  |__|\__\ |______|

logged in...
Welcome back darthvader

>find lu

Server:
luke

>friend luke

Server:
luke friends request sended

>logout

Server:
logged out

await closing... Bye!  Bye! 

```