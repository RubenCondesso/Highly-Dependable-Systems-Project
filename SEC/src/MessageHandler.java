
import java.io.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 */

public class MessageHandler implements Serializable {

	// The different types of message sent by the Client
		
	static final int MESSAGE = 0, LOGOUT = 1, SELL = 2, STATEGOOD = 3, BUYGOOD = 4, TRANSFERGOOD = 5;
	private int type;
	private String message;
	
	// constructor
	MessageHandler(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	int getType() {
		return type;
	}

	String getMessage() {
		return message;
	}
}
