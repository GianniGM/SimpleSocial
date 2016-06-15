package tests;

import java.util.ArrayList;
import java.util.UUID;

import org.json.simple.parser.ParseException;

import simpleSocialServer.Users;
import simpleSocialShared.SocialMessage;
import simpleSocialShared.Operations;
import simpleSocialShared.SimpleSocialException;

public class RequestResponseTest {

	public static void main(String[] args) {
		Users users = new Users();

		ArrayList<SocialMessage> messages = new ArrayList<>();
		ArrayList<String> parsedMessages = new ArrayList<>();

		messages.add(new SocialMessage().createMessage(Operations.REG_USER, "zeno","zenolo"));
		messages.add(new SocialMessage().createMessage(Operations.REG_USER, "zeno","zenolo"));
		messages.add(new SocialMessage().createMessage(Operations.REG_USER, "Gianni","zenolo"));
		messages.add(new SocialMessage().createMessage(Operations.REG_USER, "Giannoide","pwd"));
		messages.add(new SocialMessage().createMessage(Operations.LOGIN_USER, "zeno","zenolo"));
		messages.add(new SocialMessage().createMessage(Operations.SEARCH, "zeno","zenolo"));
		messages.add(new SocialMessage().createMessage(Operations.LOGOUT_USER,"zenolo"));

		//virtual sending message
		for (SocialMessage message : messages) {
			parsedMessages.add(message.toJSONString());
		}

		//Server simulation--------------------HANDLER----------FARE SOLO QUALCHE PICCOLA MODIFICA PERFETTO COME-------
		SocialMessage req = new SocialMessage();
		SocialMessage response = new SocialMessage();
		for(int i = 0; i < parsedMessages.size(); i++){
			String s = parsedMessages.get(i);
			//while(true){
			try {
				//reading from socket parsed in s
				Thread.sleep(1000);
				req.parseMessage(s);
				System.out.println("received " + req.toJSONString());

				String[] params;
				switch (req.getOpCode()) {

				case Operations.REG_USER:
					params = req.getParameters();
					if (params.length != 2) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						if (users.registerUser(params[0], params[1])) {
							response.createMessage(Operations.OK, params[0] + " signed in");
						} else {
							response.createMessage(Operations.ALREADY_EXISTS, "Username already exists");
						}
					}
					break;

				case Operations.LOGIN_USER:
					params = req.getParameters();
					if (params.length != 2) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						try {
							UUID id = users.logUser(params[0], params[1], "cacca", 5);
							// begin test
							parsedMessages.add(5, new SocialMessage().createMessage(Operations.SEARCH, id.toString(), "Gia").toJSONString());
							// end test
							response.createMessage(Operations.OK, id.toString());
						} catch (SimpleSocialException e) {
							response.createMessage(Operations.CREDENTIALS_ERROR, e.getMessage());
						}
					}

					break;

				case Operations.SEARCH:
					params = req.getParameters();
					if (params.length != 2) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						try {
							ArrayList<String> list = users.searchUser(UUID.fromString(params[0]), params[1]);
							if (list != null) {
								response.createMessage(Operations.OK, list.toArray(new String[list.size()]));
							} else {
								response.createMessage(Operations.USER_NOT_FOUND_ERROR, "user not found");
							}
						} catch (SimpleSocialException e) {
							response.createMessage(Operations.ACCESS_DENIED, e.getMessage());
							e.printStackTrace();
						}
					}
					break;

//				case Operations.FOLLOW:
//					params = req.getParameters();
//					if (params.length != 2) {
//						response.createMessage(Operations.BAD_REQUEST, params);
//					} else {
//						if (users.follow(UUID.fromString(params[0]), params[1])) {
//							response.createMessage(Operations.OK, params[1] +  " followed");
//						} else {
//							response.createMessage(Operations.USER_NOT_FOUND_ERROR,
//									"not could follow " + params[1] + "must be friend first");
//						}
//					}
//					break;
//
//				case Operations.UNFOLLOW:
//					params = req.getParameters();
//					if (params.length != 2) {
//						response.createMessage(Operations.BAD_REQUEST, params);
//					} else {
//						if (users.unfollow(UUID.fromString(params[0]), params[1])) {
//							response.createMessage(Operations.OK, params[1] + " unfollowed");
//						} else {
//							response.createMessage(Operations.USER_NOT_FOUND_ERROR, "not could unfollow " + params[1]);
//						}
//					}
//					break;

				case Operations.FRIEND_REQ:
					params = req.getParameters();
					if (params.length != 2) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						if (users.friend(UUID.fromString(params[0]), params[1])) {
							response.createMessage(Operations.OK, params[1] + " friends request sended");
						} else {
							response.createMessage(Operations.USER_NOT_FOUND_ERROR, "not could friend " + params[1]);
						}
					}
					break;

				case Operations.FRIEND_ACCPT:
					params = req.getParameters();
					if (params.length != 2) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						if (users.acceptFriend(UUID.fromString(params[0]), params[1])) {
							response.createMessage(Operations.OK, params[1] + " friendzoned" );
						} else {
							response.createMessage(Operations.USER_NOT_FOUND_ERROR,
									"not could accept friendship with " + params[1] + "must be friend first");
						}
					}
					break;

				case Operations.UNFRIEND:
					params = req.getParameters();
					if (params.length != 2) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						if (users.unFriend(UUID.fromString(params[0]), params[1])) {
							response.createMessage(Operations.OK, params[1] + " unfriended");
						} else {
							response.createMessage(Operations.USER_NOT_FOUND_ERROR,
									"not could unfriend " + params[1] + "must be friend first");
						}
					}
					break;

				case Operations.LOGOUT_USER:
					params = req.getParameters();
					if (params.length != 1) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						try {
							if (users.logOff(UUID.fromString(params[0]))) {
								response.createMessage(Operations.OK, "logged out");
							} else {
								response.createMessage(Operations.ACCESS_DENIED, "access denied");
							}
						} catch (SimpleSocialException e) {
							response.createMessage(Operations.USER_NOT_FOUND_ERROR, e.getMessage());
						}
					}
					break;

				default:
					response.createMessage(Operations.ACCESS_DENIED, "Wrong command");
					break;
				}


			} catch (InterruptedException e) {
				response.createMessage(Operations.GENERIC_ERROR, e.getMessage());
			} catch (ParseException e) {
				response.createMessage(Operations.GENERIC_ERROR, e.getMessage());
			} catch (IllegalArgumentException e){
				response.createMessage(Operations.BAD_REQUEST, "illegal Argument");
			} finally{
				System.out.println(response.toJSONString() + "\n" + response.toString());
			}

		}
	}
}
