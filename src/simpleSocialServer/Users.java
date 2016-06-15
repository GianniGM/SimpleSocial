package simpleSocialServer;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import simpleSocialClient.ContentClient;
import simpleSocialShared.SimpleSocialException;

public class Users extends RemoteObject implements UsersIFace, ContentsServer{

	private static final long serialVersionUID = 1L;

	private ConcurrentHashMap<String, User> usersByName;
	private ConcurrentHashMap<UUID, User> usersByID;

	private Lock lock;

	public Users() {
		this.usersByName = new ConcurrentHashMap<>();
		this.usersByID = new ConcurrentHashMap<>();
		this.lock = new ReentrantLock();
	}

	@Override
	public boolean acceptFriend(UUID id, String user) {

		if(getUser(user) == null){
			return false;
		}

		if(getUser(id).AcceptFriend(user)){
			getUser(user).addFriend(getUser(id).getUsername());
			return true;
		}
		return false;
	}

	@Override
	public boolean follow(UUID id, String following) throws RemoteException{
		
		if(!usersByName.containsKey(following))
			return false;
		
		if(!usersByID.containsKey(id))
			return false;
	
		return	getUser(following).addToFollowers(getUser(id).getUsername());
	
	}

	@Override
	public boolean friend(UUID id, String name){

		if(!usersByName.containsKey(name))
			return false;
		
		if(!usersByID.containsKey(id))
			return false;

		return	getUser(name).addToPendingFriends(getUser(id).getUsername());
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject getFriends(UUID id) {

		JSONObject friends = new JSONObject();

		JSONArray array = new JSONArray();
		for(String s : getUser(id).getFriends()){
			JSONObject friend = new JSONObject();
			friend.put("name", s);
			friend.put("status", getUser(s).isOnline());
			array.add(friend);
		}
		friends.put("friends", array);

		return friends;
	}

	public ConcurrentHashMap<UUID, User> getOnlineUsers() {
		return usersByID;
	}

	public Set<String> getRegisteredUsers() {
		if(!usersByName.isEmpty())
			return  usersByName.keySet();
		else 
			return null;
	}

	public User getUser(String name){
		return usersByName.get(name);
	}


	public User getUser(UUID id){
		return usersByID.get(id);
	}



	public boolean loadUser(String jsonString) throws ParseException{
		User u = new User(jsonString);
		String name = u.getUsername();
		usersByName.put(name, u);
		return true;
	}

	// id wants to be follower of name

	@Override	
	public boolean logOff(UUID id) throws SimpleSocialException{
		if(!usersByID.containsKey(id)){
			return false;
		}

		System.out.println("logoff");
		getUser(id).logout();
		usersByID.remove(id);
		return true;
	}


	@Override
	public UUID logUser(String uname, String password, String address, int port) throws SimpleSocialException{

		if(!usersByName.containsKey(uname)){
			throw new SimpleSocialException("unable to find " + uname + " please register first");
		}

		UUID u;

		//according to this http://bugs.java.com/view_bug.do?bug_id=6611830
		//UUID is not thread safe		
		this.lock.lock();
		u = UUID.randomUUID();
		this.lock.unlock();

		//login
		User user = usersByName.get(uname).login(uname, password);

		//concurrent hash map
		usersByID.put(u, user);

		//adding socket
		getUser(uname).setAddress(new InetSocketAddress(address, port));
		return u;
	}

	@Override
	public boolean registerUser(String uname, String password){

		if(usersByName.containsKey(uname.trim().toLowerCase())){
			return false;
		}

		User u = new User(uname, password);
		usersByName.put(uname, u);
		return true;
	}




	@Override
	public ArrayList<String> searchUser(UUID yourId, String name) throws SimpleSocialException {
		ArrayList<String> list = new ArrayList<>();

		//FIXME ERA COMMENTATO PER QUALCHE MOTIVO MA BOH TESTARE
		if( yourId == null || !usersByID.containsKey(yourId)){
			throw new SimpleSocialException("you're not logged");
		}		

		if(name.compareTo("***") == 0){
			for (String s : usersByName.keySet()){
				list.add(s);
			}

		}else{

			for (String s : usersByName.keySet()){
				if (s.contains(name)){
					list.add(s);
				}
			}
		}
		return list;
	}

	@Override
	public boolean sendContent(UUID id, String content) throws RemoteException{
		for(String followerName : getUser(id).getFollowers()){

			User follower = getUser(followerName); 
			String message = new StringBuilder().append(getUser(id).getUsername()).append(" typed: ").append(content).toString();
			if(follower.isOnline())
				//send remotely	
				follower.callback.pushContent(follower.getUsername(), message);
			else
				//send locally
				follower.addContents(message, true);
		}
		return	false;
	}

	@Override
	public boolean unfollow(UUID id, String following) throws RemoteException{
		if(usersByID.containsKey(id))
			return	getUser(following).removeFromFollowers(getUser(id).callback);
		else
			return false;
	}

	//id wan't regret his pending request
	@Override
	public boolean unFriend(UUID id, String name){

		//se è nelle richieste pendenti allora
		if(!getUser(name).removeFromPendingFriends(getUser(id).getUsername())){
			boolean val = true;
			val = val && getUser(name).unFriend(getUser(id).getUsername());
			val = val && getUser(id).unFriend(name);
			return val;
		}

		return true;
	}

	@Override
	public boolean exit(UUID id) throws RemoteException {
		getUser(id).callback = null;
		return false;
	}

	@Override
	public boolean registerCallback(UUID id, ContentClient callback) throws RemoteException {
		getUser(id).callback = callback;
		return false;
	}

}
