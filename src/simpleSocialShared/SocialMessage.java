package simpleSocialShared;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SocialMessage {
	private long ops;
	private String[] parameters;
		
	public static HashMap<String, Boolean> parseFriends(String jsonString) throws ParseException{
		HashMap<String, Boolean> friends = new HashMap<>();
		
//		System.out.println("friend received " + jsonString);
		JSONParser parser = new JSONParser();
		JSONObject jsonFriends = (JSONObject) parser.parse(jsonString);
		JSONArray array = (JSONArray)jsonFriends.get("friends");
		
		for (int i = 0; i < array.size(); i++) {
			JSONObject friend = (JSONObject) array.get(i);
			friends.put((String) friend.get("name"),(Boolean) friend.get("status"));
		}
				
		return friends;
	}
	
//	@SuppressWarnings("unchecked")
//	public static String friendsToJSON(Set<String> friends){
//
//		JSONObject jsonFriends = new JSONObject();	
//		JSONArray array = new JSONArray();
//		for(String s : friends){
//			JSONObject friend = new JSONObject();
//			friend.put("name", s);
//			array.add(friend);
//		}
//		jsonFriends.put("friends", array);
//		
//		return jsonFriends.toJSONString();
//	}
	
	public SocialMessage parseMessage(String jsonString) throws ParseException{
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(jsonString);
		JSONArray array = (JSONArray) obj.get("parameters");
		
		this.ops = (long) obj.get("operationcode");
		this.parameters = new String[array.size()];
				
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = (String) array.get(i);
		}
				
		return this;
	}
	
	
	public SocialMessage createMessage(int operation, String...params){

		this.ops = operation;
		this.parameters = params;
		return this;
	}
	
	public int getOpCode() {
		return (int) ops;
	}
	
	public String[] getParameters() {
		return parameters;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("opcode: ").append(ops).append(" params:");
		for (String string : parameters) {
			sb.append(" ").append(string);
		}
		
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	public String toJSONString(){
		JSONObject obj = new JSONObject();
		obj.put("operationcode", ops);

		JSONArray array = new JSONArray();
		for (String s : parameters) {
			array.add(s);
		}
		
		obj.put("parameters", array);
		
		return obj.toJSONString();
	}
}
