package simpleSocialServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import simpleSocialShared.SimpleSocialException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import simpleSocialClient.ContentClient;

//TODO pulire codice e commenti subito dopo aver testato la parte RMI
public class User {

	public ContentClient callback;

	private String username, password;
	private boolean online;
	private long loginHours = -1;

	private InetSocketAddress address= null;
	private Set<String> friends = null; 
	private ConcurrentHashMap<String, Long> receivedFriendsRequests = null;
	private Set<String> followers = null;

	private Vector<String> pendingContents = null;

	private Lock lock;


	//registration
	public User(String username, String password) {
		this.username = username;
		this.password = password;

		this.online = false;

		this.friends =  Collections.synchronizedSet(new HashSet<>()); 
		this.followers = Collections.synchronizedSet(new HashSet<>());
		this.receivedFriendsRequests = new ConcurrentHashMap<>();
		this.pendingContents = new Vector<>();

		this.lock = new ReentrantLock();
	}

	//TODO JSON should be cyphered
	public User(String jsonString) throws ParseException{

		this.followers = Collections.synchronizedSet(new HashSet<>());
		this.friends =  Collections.synchronizedSet(new HashSet<>()); 	
		this.receivedFriendsRequests = new ConcurrentHashMap<>();
		this.pendingContents = new Vector<>();
		this.lock = new ReentrantLock();

		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(jsonString);

		this.username = (String) jsonObject.get("username");
		this.password = (String) jsonObject.get("password");

		this.online = false;

		JSONArray friendsarray = ((JSONArray) jsonObject.get("friendslist"));
		if(!friendsarray.isEmpty()){
			for (Object object : friendsarray) {
				this.friends.add((String) object);
			}
		}

		JSONArray followersarray = ((JSONArray) jsonObject.get("followers"));
		if(!followersarray.isEmpty()){
			for (Object object : followersarray) {
				this.followers.add((String) object);
			}
		}

		//		JSONArray pendingrequests = ((JSONArray) jsonObject.get("pendingfriends"));
		//		if(!pendingrequests.isEmpty()){
		//			for (Object o : pendingrequests) {
		//				this.receivedFriendsRequests.add((String) o);
		//			} 
		//		}

		JSONArray contents = ((JSONArray) jsonObject.get("contents"));
		if (contents != null) {
			for (Object object : contents) {
				this.pendingContents.add((String) object);
			} 
		}
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}

	public String getPassword() {
		return password;
	}

	public boolean setPassword(String password) {
		this.password = password;
		return true;
	}

	public String getUsername() {
		return username;
	}

	public boolean isOnline() {
		try{
			this.lock.lock();
			return online;
		}finally{
			this.lock.unlock();
		}
	}

	public boolean isFriend(String user){
		if(friends.contains(user)){
			return true;
		}
		return false;
	}

	public Set<String> getFriendsRequests() {
		return receivedFriendsRequests.keySet();
	}

	public Set<String> getFollowers() {
		return followers;
	}

	//returns false if is already online
	//else raise exception if wronged username or password
	public User login(String userName, String password) throws SimpleSocialException{

		if(this.username.compareTo(userName) != 0  && this.password.compareTo(password) != 0){
			throw new SimpleSocialException("Wrong Username");
		}

		this.setOnline();

		long hours = System.currentTimeMillis();
		TimeUnit unit = TimeUnit.MILLISECONDS;
		loginHours = unit.toHours(hours);

		return this;
	}

	public void logout(){
		callback = null;
		address = null;
		this.setOffline();
	}

	public boolean AcceptFriend(String user){

		if(receivedFriendsRequests.containsKey(user)){
			friends.add(user);
			receivedFriendsRequests.remove(user);
			return true;
		}
		return false;
	}

	public void addFriend(String user){
		friends.add(user);
	}


	public boolean unFriend(String user){
		if(friends.contains(user)){
			followers.remove(user);
			friends.remove(user);
			return true;
		}
		return false;
	}

	public Set<String> getFriends() {
		return friends;
	}

	public boolean addToPendingFriends(String u) {
		TimeUnit unit = TimeUnit.MILLISECONDS;
		long nowInMinutes =  unit.toMinutes(System.currentTimeMillis());
		return 	null !=	receivedFriendsRequests.put(u,nowInMinutes);
	}

	public boolean removeFromPendingFriends(String u) {

		if(receivedFriendsRequests.isEmpty())
			return false;

		if(receivedFriendsRequests.containsKey(u)){
			receivedFriendsRequests.remove(u);
			return true;
		}
		return false;
	}

	public boolean addToFollowers(String username) {
	
		if(friends.isEmpty())
			return false;
		
		if(!friends.contains(username)){
			return false;
		}

		return this.followers.add(username);
	}



	public boolean removeFromFollowers(ContentClient callback) {
		if(followers.isEmpty())
			return false;

		if(followers.contains(callback)){
			followers.remove(callback);
			return true;
		}
		return false;
	}

	public boolean addContents(String content, boolean enqueue){
		if(enqueue){
			return pendingContents.add(content);
		}else{
			pendingContents.add(0, content);
			return true;
		}
	}


	//garantire che questi contenuti siano stati inviati
	public ArrayList<String> getReceivedContents(){

		//TODO FORSE NON è UNA COPIA
		ArrayList<String> lista = new ArrayList<>(pendingContents);
		pendingContents.clear();
		return lista;

	}

	@SuppressWarnings("unchecked")
	public String toJSONString(){
		JSONObject user = new JSONObject();
		user.put("username", username.toLowerCase().trim());
		user.put("password", password.trim());

		JSONArray array = new JSONArray();
		for (String string : friends) {
			array.add(string.toLowerCase().trim());
		}
		user.put("friendslist", array);

		//		array=new JSONArray();
		//		for (String string : receivedFriendsRequests) {
		//			array.add(string.toLowerCase().trim());
		//		}
		//		user.put("pendingfriends", array);

		array=new JSONArray();
		for (String follower : followers) {
			array.add(follower.toLowerCase().trim());			
		}
		user.put("followers", array);


		array=new JSONArray();
		if(this.pendingContents != null){
			for (String string : pendingContents) {
				array.add(string);
			}
			user.put("contents", array);
		}else{
			user.put("contents", null);			
		}

		return user.toJSONString();
	}

	public boolean checkExpiration(long hourLogLimit, long friendReqLimit) {
		long nowMillis = System.currentTimeMillis();
		TimeUnit unit = TimeUnit.MILLISECONDS;

		long nowMinutes = unit.toMinutes(nowMillis);
		long nowHours = unit.toHours(nowMillis);

		if(!receivedFriendsRequests.isEmpty()){

			for(String user : receivedFriendsRequests.keySet()){
				long reqTime = receivedFriendsRequests.get(user);

				if(nowMinutes - reqTime > friendReqLimit){
					receivedFriendsRequests.remove(user);
				}
			}
		}		
		return nowHours - loginHours > hourLogLimit;
	}


	private void setOnline() {
		this.lock.lock();
		this.online = true;
		this.lock.unlock();
	}

	private void setOffline(){
		this.lock.lock();
		this.online = false;
		this.lock.unlock();
	}

}
