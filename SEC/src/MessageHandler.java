
import java.io.*;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 */

public class MessageHandler implements Serializable {

	// The different types of message sent by the Client
		
	static final int ENTER = 0, SELL = 1, STATEGOOD = 2, BUYGOOD = 3, TRANSFERGOOD = 4, LOGOUT = 5, UPDATE = 6;
	
	private int type;
	
	private int port;

	private int number;

	private int randomNumber;

	private String hashResult;
	
	private static final long serialVersionUID = 1L;
	
	X509Certificate cert;
	
	byte[] message;
	
	byte[] seq;
	
	byte[] time;
	
	byte[] dataSignature;
	
	byte[] seqSignature;
	
	byte[] dateSignature;


	
	// constructor
	MessageHandler(int type, byte [] message, byte[] seq,  byte[] time, int port, int number, byte[] dataSignature,byte[] seqSignature,byte[] dateSignature, int randomNumber, String hashResult ) {
		
		//Type of message
		this.type = type;
		
		//message
		this.message = message;
		
		//sequence number of message
		this.seq=seq;
		
		//time now
		this.time = time;
		
		// the certificate
		this.cert = cert;
		
		// port used in connection
		this.port = port;

		// number used to do some calculations (in port's numbers)
		this.number= number;
		
		this.dataSignature = dataSignature;
		
		this.seqSignature = seqSignature;
		
		this.dateSignature = dateSignature;

		// random Number to prevent spam messaging of clients
		this.randomNumber = randomNumber;	

		// hash of the result of the factorial of the random number
		this.hashResult = hashResult;

	}


	int getType() {
		
		return type;
	}
	
	byte[] getData(){
		
		return message;
	}
	
	byte[] getSeq() {
		
		return seq;
	}
	
	 byte[] getLocalDate(){
		
		return time;
	}
	 
	 int getPort() {
			
		return port;
	}

	 int getNumber(){

	 	return number;
	 }
	 
	 byte[] getDataSignature() {
		 return dataSignature;
		 
	 }
	 
	 byte[] getSeqSignature() {
		 return seqSignature;
		 
	 }
	 
	 byte[] getDateSignature() {
		 return dateSignature;
		 
	 }

	 int getRandomNumber(){

	 	return randomNumber;
	 }

	 String getHashResult(){

	 	return hashResult;
	 }
	
}
