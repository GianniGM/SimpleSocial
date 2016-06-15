package simpleSocialServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import simpleSocialShared.SocialMessage;
import simpleSocialShared.Operations;
import simpleSocialShared.SimpleSocialException;

public class TCPHandler implements Runnable {
	Users users;
	Socket socket;

	public TCPHandler(Socket socket, Users s) {
		this.users = s;
		this.socket = socket;
	}

	@Override
	public void run() {
		SocialMessage response = new SocialMessage();
		SocialMessage req = new SocialMessage();

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-16"));
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-16"))){

			//CONTROLLARE SE L'AMICO è ONLINE
			req.parseMessage(reader.readLine().trim());
			System.out.println("request received " + req.toJSONString());

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
				if (params.length != 4) {
					response.createMessage(Operations.BAD_REQUEST, params);
				} else {
					try {
						UUID id = users.logUser(params[0], params[1], params[2], Integer.parseInt(params[3]));
						response.createMessage(Operations.OK, id.toString());
					} catch (SimpleSocialException e) {
						response.createMessage(Operations.CREDENTIALS_ERROR, e.getMessage());
					}
				}

				writer.write(response.toJSONString());
				writer.newLine();
				writer.flush();
				
				req.parseMessage(reader.readLine().trim());
				System.out.println("request received " + req.toJSONString());

				if(req.getOpCode() == Operations.SYNC_USER){
					params = req.getParameters();
					if (params.length != 1) {
						response.createMessage(Operations.BAD_REQUEST, params);
					} else {
						User s = users.getUser(UUID.fromString(params[0]));
						if(s != null)
							response.createMessage(Operations.OK, s.toJSONString());
						else
							response.createMessage(Operations.USER_NOT_FOUND_ERROR, "user not logged");
					}
				}else{
					response.createMessage(Operations.BAD_REQUEST, "sync error");

				}

				break;

			case Operations.SEARCH:
				params = req.getParameters();
				if (params.length != 2) {
					response.createMessage(Operations.BAD_REQUEST, "wrong values");
				} else {
					try {
						ArrayList<String> userslist = users.searchUser(UUID.fromString(params[0]), params[1]);
						if (userslist != null && !userslist.isEmpty()) {
							response.createMessage(Operations.OK, userslist.toArray(new String[userslist.size()]));
						} else {
							response.createMessage(Operations.USER_NOT_FOUND_ERROR, "user not found");
						}
					} catch (SimpleSocialException e) {
						response.createMessage(Operations.ACCESS_DENIED, e.getMessage());
						e.printStackTrace();
					}
				}
				break;

			case Operations.GET_FRIENDS_REQS:
				params = req.getParameters();
				if (params.length != 1) {
					response.createMessage(Operations.BAD_REQUEST, "BAD REQUEST");
				} else {
					Set<String> list = users.getUser(UUID.fromString(params[0])).getFriendsRequests();

					if (list != null && !list.isEmpty()) {
						response.createMessage(Operations.OK, list.toArray(new String[list.size()]));
						System.out.println("RECEIVED FRIENDS REQ LIST, SENDING " + response.toJSONString());
					} else {
						response.createMessage(Operations.USER_NOT_FOUND_ERROR, "friends reqeusts empty");
					}
				}
				break;

			case Operations.FRIEND_REQ:
				params = req.getParameters();
				if (params.length != 2) {
					response.createMessage(Operations.BAD_REQUEST, params);

				} else if(users.getUser(UUID.fromString(params[0])).getUsername().compareTo(params[1]) == 0 ) {
					response.createMessage(Operations.BAD_REQUEST, "Trust me: you are already friend of yourself");

				}else{
					User u = users.getUser(params[1]); 
					if(u == null || u.getAddress() == null){
						response.createMessage(Operations.CONNECTION_ERROR, "friend is offline or not exists");
					}else{
						InetSocketAddress address = users.getUser(params[1]).getAddress();

						Socket s = new Socket(address.getAddress(), address.getPort());
						s.setSoTimeout(2000);

						if(s.isConnected()){
							users.friend(UUID.fromString(params[0]), params[1]);
							response.createMessage(Operations.OK, params[1] + " friends request sended");
						}else{
							response.createMessage(Operations.CONNECTION_ERROR, "friend is offline");
						}

						s.close();
					}
				}
				break;

			case Operations.FRIEND_DENY:
				params = req.getParameters();
				if (params.length != 2) {
					response.createMessage(Operations.BAD_REQUEST, params);

				} else if(users.getUser(UUID.fromString(params[0])).getUsername().compareTo(params[1]) == 0 ) {
					response.createMessage(Operations.BAD_REQUEST, "Trust me: don't hate yourself");

				}else{
					if(users.unFriend(UUID.fromString(params[0]), params[1])){
						response.createMessage(Operations.OK, params[1] + " friends removed");
					}else{
						response.createMessage(Operations.CONNECTION_ERROR, "friend is not removed");
					}
				}
				break;	

			case Operations.FRIENDS_LIST:
				params = req.getParameters();
				if (params.length != 1) {
					response.createMessage(Operations.BAD_REQUEST, params);
				} else {
					JSONObject fList = users.getFriends(UUID.fromString(params[0])); 
					if( fList != null && fList.size() > 0){
						response.createMessage(Operations.OK, fList.toJSONString());
					}else {
						response.createMessage(Operations.USER_NOT_FOUND_ERROR,	"NO friends in the list");
					}
				}
				break;

			case Operations.FRIEND_ACCPT:
				params = req.getParameters();
				if (params.length != 2) {
					response.createMessage(Operations.BAD_REQUEST, params);
				} else {
					if (users.acceptFriend(UUID.fromString(params[0]), params[1])) {

						String me = users.getUser(UUID.fromString(params[0])).getUsername();

						InetSocketAddress address = users.getUser(params[1]).getAddress();
						try(Socket s = new Socket(address.getHostName(), address.getPort());
								BufferedWriter notify = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-16"))){

							notify.write("friendship accepted from " + me);
							notify.newLine();
							notify.flush();
							notify.close();

						}catch(IOException e){
							System.out.println("conenction refused");
						}

						response.createMessage(Operations.OK, params[1] + " friendzoned" );
					} else {
						response.createMessage(Operations.USER_NOT_FOUND_ERROR, "user not found or request has expired");
					}
				}
				break;



			case Operations.UNFRIEND:
				params = req.getParameters();
				if (params.length != 2) {
					response.createMessage(Operations.BAD_REQUEST, "error on unfriending");
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
					response.createMessage(Operations.BAD_REQUEST, "error on logout");
				} else {
					try {
						if (users.logOff(UUID.fromString(params[0]))) {
							response.createMessage(Operations.LOGOUT_USER, "logged out");
						} else {
							response.createMessage(Operations.LOGOUT_USER, "already logged out");
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

			System.out.println("sending: " + response.toJSONString());

			writer.write(response.toJSONString().trim());
			writer.newLine();
			writer.flush();	

			req.parseMessage(reader.readLine());
			if(req.getOpCode() == Operations.OK){
				System.out.println("client received data, closing TCP connection");
			}else{
				System.out.println("Error!!!");
			}


		} catch (UnsupportedEncodingException e1) {
			System.out.println(new StringBuilder().append("Encoding problem ").append(e1.getMessage()));
			response.createMessage(Operations.ACCESS_DENIED, "Wrong command");
		} catch (IOException e1) {
			System.out.println(new StringBuilder().append("Connection refused ").append(e1.getMessage()));
		} catch (ParseException e1) {
			e1.printStackTrace();
			response.createMessage(Operations.ACCESS_DENIED, "Wrong command");
		}finally{
			try {
				socket.close();
			} catch (IOException e) {}
		}
	}
}