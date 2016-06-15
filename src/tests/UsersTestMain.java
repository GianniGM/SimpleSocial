package tests;

import java.util.HashMap;
import java.util.UUID;

import org.json.simple.parser.ParseException;

import simpleSocialServer.User;
import simpleSocialServer.Users;
import simpleSocialShared.SocialMessage;
import simpleSocialShared.SimpleSocialException;

public class UsersTestMain {

	public static void main(String[] args) {
		String[] user1 = {"Gianni","password"};
		String[] user2 = {"Davide","davide"};
		String[] user3 = {"Giovanni","traitor"};
		
		UUID idUser1 = null;
		UUID idUser2 = null;
		UUID idUser3 = null;
		
		Users users = new Users();
		
		users.registerUser(user1[0], user1[1]);
		users.registerUser(user2[0], user2[1]);
		users.registerUser(user3[0], user3[1]);
		
		try {
			idUser1 = users.logUser(user1[0], user1[1], "cacca", 5);
			idUser2 = users.logUser(user2[0], user2[1], "cacca", 5);
//			idUser3 = users.logUser("pinko", "palla");		
			idUser3 = users.logUser(user3[0], user3[1], "cacca", 5);
			
			System.out.println("primo tentativo di following");
			
			//searching user
			System.out.println(users.searchUser(idUser2, "Gi"));

			//adding follower			
//			if(!users.follow(idUser3, user2[0])){
//				System.out.println("no follow");
//			}
			
			System.out.println(".................amicizia.1..........................");
			System.out.println(users.getUser(idUser2).toJSONString());
			System.out.println(users.getUser(idUser3).toJSONString());
			
			//adding friendship
			users.friend(idUser2, user3[0]);
			System.out.println(".................amicizia..2.........................");
			System.out.println(users.getUser(idUser2).toJSONString());
			System.out.println(users.getUser(idUser3).toJSONString());

			users.acceptFriend(idUser3, user2[0]);
			System.out.println(".................amicizia...3........................");
			System.out.println(users.getUser(idUser2).toJSONString());
			System.out.println(users.getUser(idUser3).toJSONString());

			try {
				HashMap<String , Boolean>  map = SocialMessage.parseFriends(users.getFriends(idUser2).toJSONString());
				if(!map.isEmpty()){
					System.out.println("MAPPA AMICI: " + map.toString());
				}else{
					System.out.println("no one");
				}
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
	
			//adding follower
//			if(!users.follow(idUser3, user2[0])){
//				System.out.println("no follow");
//			}
			
			System.out.println("primo tentativo contenuto");
			//sending content
			
			//setting offline			
			System.out.println("secondo tentativo di contenuto");
			
			System.out.println(users.getUser(idUser1).toJSONString());
			System.out.println(users.getUser(idUser2).toJSONString());
			System.out.println(users.getUser(idUser3).toJSONString());
		
			
//			caricare contenuti
//			users.logOff(idUser1);
//			users.sendContent(user1[0], "lam rossa");
//			users.sendContent(user1[0], "lam rossa ciao ciao vi voglio bene tutti");			
//			idUser1 = users.logUser(user1[0],user1[1]);
//			users.sendContent(user1[0], "lam due parte seconda");
//			users.sendContent(user1[0], "sei online e non dovresti ricevere questo m essaggio perché si invia solo se sei offline");
//			System.out.println(users.getUser(idUser1).getReceivedContents());
			
			users.unFriend(idUser3, user2[0]);
			System.out.println("friendship removed.....................");
			System.out.println(users.getUser(idUser3).toJSONString());
			System.out.println(users.getUser(idUser2).toJSONString());
		
			
			try {
				System.out.println(new User(users.getUser(idUser1).toJSONString()).toJSONString());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
		} catch (SimpleSocialException e) {
			System.out.println(e.getMessage());
		}
		
		
	}

}
