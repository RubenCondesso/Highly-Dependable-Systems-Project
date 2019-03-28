
import java.io.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 */

public class MessageHandler implements Serializable {

	// The different types of message sent by the Client
		
	static final int SETLIST = 0, SELL = 1, STATEGOOD = 2, BUYGOOD = 3, TRANSFERGOOD = 4, Encrypt = 5, LOGOUT=6;
	
	private int type;
	
	private static final long serialVersionUID = 1L;
	
	byte[] message;

	
	// constructor
	MessageHandler(int type, byte [] message) {
		
		this.type = type;
		this.message = message;
	}
	
	int getType() {
		
		return type;
	}
	
	byte[] getData(){
		
		return message;
}

	
}
