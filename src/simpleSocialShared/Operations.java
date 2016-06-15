package simpleSocialShared;

public class Operations {

	public final static int OK = 1;
	public final static int BAD_REQUEST = -1;
	
	public final static int REG_USER = 10;
	public final static int LOGIN_USER = 11;
	public final static int LOGOUT_USER = 12;
	public final static int SYNC_USER = 13;
	
	public final static int SEARCH = 20;
	
	public final static int FRIEND_REQ = 30;
	public final static int FRIEND_ACCPT = 31;
	public final static int FRIEND_SEARCH = 32;
	public final static int FRIENDS_LIST = 33;
	public final static int GET_FRIENDS_REQS = 34;
	public final static int UNFRIEND = 35;
	public static final int FRIEND_DENY = 36;

	
	public final static int GENERIC_ERROR = 50;
	public final static int USER_NOT_FOUND_ERROR = 51;
	public final static int CREDENTIALS_ERROR = 52;
	public final static int ACCESS_DENIED = 53;
	public final static int CONNECTION_ERROR = 54;
	public static final int ALREADY_EXISTS = 55;

}
