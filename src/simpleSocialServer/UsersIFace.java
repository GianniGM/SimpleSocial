package simpleSocialServer;

import java.util.ArrayList;
import java.util.UUID;

import org.json.simple.JSONObject;

import simpleSocialShared.SimpleSocialException;

public interface UsersIFace {

	//creating, login, logout users
	public boolean registerUser(String uname, String password);
	public UUID logUser(String uname, String password, String address, int port) throws SimpleSocialException;
	public boolean logOff(UUID id) throws SimpleSocialException;
	
	//social interaction, TCP activities
	public ArrayList<String> searchUser(UUID yourId, String name) throws SimpleSocialException;
	public boolean friend(UUID id, String name);
	public boolean acceptFriend(UUID id, String user);
	public JSONObject getFriends(UUID id);
	public boolean unFriend(UUID id, String name);
}
