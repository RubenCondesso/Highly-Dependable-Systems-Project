
import java.net.*;
import java.io.*;
import java.util.*;

import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

//import java.math.BigInteger;
import javax.crypto.*;
//import javax.crypto.spec.IvParameterSpec;


//The Client that can be run as a console
public class Client  {
	
	// notification
	private String notif = " *** ";

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private static ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;					
	
	private String server, clientID;	
	private int port;
	
	// id of the connection of the client socket
	private static String clientConnection;
	
	private Cipher cipher;
		
	MessageHandler msgEncrypt;
	MessageHandler message;
	
	//Sequence Number -> Guarantee freshness of messages
	private static int seqNumber;
	
	//time to expire message
	private static int expireTime;
	
	//real timestamp
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			
	//List of goods of the client
	private static HashMap<String, String> goodsList = new HashMap<String, String>();
	
	
	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	
	
	public static void setGoodsClient(String good, String clientID) {
		goodsList.put(good, clientID);
	}
	
	public static HashMap<String, String> getGoodsClient() {
		return goodsList;
	}
		

	/*
	 *  Constructor to set below things
	 *  server: the server address
	 *  port: the port number
	 *  clientID: the client identification
	*/
	Client(String server, int port, String clientID) {
		
		this.server = server;
		this.port = port;
		this.clientID = clientID;
				
	}
	
	/*
	 * To start the application
	 */
	
	public boolean start() {
		
		// try to connect to the server
		try {
			
			socket = new Socket(server, port);
									
			clientConnection = socket.getLocalAddress().getHostAddress().toString().replace("/","") + ":" + socket.getLocalPort();
			
			//30 seconds to message expire
			expireTime = 10;
						
		} 
		
		// exception handler if it failed
		catch(Exception ec) {
			
			display("Error connectiong to server:" + ec);
			return false;
		}
		
		String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
		
		display(msg);
	
		/* Creating both Data Stream */
		try {
			
			sInput  = new ObjectInputStream(socket.getInputStream());
			
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		
		catch (IOException eIO) {
			
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();	
					
		// success we inform the caller that it worked
		return true;
	}

		
	// Client wants to see the state of some specific good (available or not available)
		private static String getStateOfGood (String good){	
			return good;
		}
			
	
	// Client wants to sell some specific good
	private static String intentionToSell (String good){
				
		return good;
	
	}
	
	//Client wants to buy a specific good from another client
	private static String buyGood (String good) {
				
		return good;
	}
		
	
	//Ask the server to verify the transfer of goods between clients
	private static String transferGood (String message) {
				
		return message;
	}
	
	/*
	 * To send a message to the console
	 */
	
	private void display(String msg) {

		System.out.println(msg);
		
	}
			
	/*
	 * To send a message to the Notary
	 */
	void sendMessage(MessageHandler msg) throws UnrecoverableKeyException, KeyStoreException, CertificateException {
		
		try {
									
			msgEncrypt = null;
									
			//Client will send a normal message encrypted				
			msgEncrypt = new MessageHandler(msg.getType(), encryptMessage(new String(msg.getData())), encryptMessage(new String(msg.getSeq())), msg.getLocalDate());
							
			sOutput.writeObject(msgEncrypt);
			
			String count =  new String(msg.getSeq());
					
			seqNumber = Integer.parseInt(count) + 1; 
						
			socket.setSoTimeout(5000*100);  //set timeout to 500 seconds
										
		}
		
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
					
			e.printStackTrace();						
		}
		
		catch(IOException e) {
			display("Exception writing to server: " + e);
		}
	}
	
	
	/*
		* //===========  Encrypted message using the private key of the Client =================
		*	 
		* encryptMessage method
		* 
		* 		Takes the message string as input and encrypts the message.
		* 
		* 
	*/
	private byte[] encryptMessage(String s) throws NoSuchAlgorithmException, NoSuchPaddingException, 
						InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
										BadPaddingException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException{
	
				
		PrivateKey pK = getPrivateKey(clientConnection);
		
		cipher = null;
	
		byte[] cipherText = null;
	
		cipher = Cipher.getInstance("RSA");
			
		cipher.init(Cipher.ENCRYPT_MODE, pK);
	
		long time3 = System.nanoTime();
	
		cipherText = cipher.doFinal(s.getBytes());
	
		long time4 = System.nanoTime();
	
		//long totalRSA = time4 - time3;
				   
		return cipherText;
	}
	
	
	
	/*
	 * //=========== Decipher/decrypt the encrypted message using the public key of Notary =================
	 * 
	 * decryptMessage method.
	 * 
	 * 		Deciphers the encrypted message received from the Notary using his public Key.
	 * 		
	 * 		Takes byte array of the encrypted message as input.
	 *  
	*/
	
	private String decryptMessage(byte[] encryptedMessage) {
		
	        cipher = null;
	        
	        try {
	        	
	    		//String idConnection = socket.getLocalAddress().getHostAddress().toString() + ":" + socket.getPort();
	    		
	        	String idConnection = "0.0.0.0" + ":" + socket.getPort();
	    			        	
	        	PublicKey prK = readPublicKeyFromFile(idConnection + "public.key");
	        		            
	        	cipher = Cipher.getInstance("RSA");
	        		            	        	
	        	cipher.init(Cipher.DECRYPT_MODE, prK);
	             
	        	byte[] msg = cipher.doFinal(encryptedMessage);	
	        		             
	        	return new String(msg);
	             
	        }
	        
	        catch(Exception e)
	         {
	        	e.getCause();
	        
	        	e.printStackTrace();
	        	
	        	System.out.println ( "Exception genereated in decryptData method. Exception Name  :"  + e.getMessage() );
	          }
	        
	        return null;
	 }
	
	
	/*
		* //===========  Get the private key of the Client of the KeyStore =================
		*	 
		* getPrivateKey method
		* 
		* 		Takes the variable 'clientConnection' as input and retrieves the private key of the Client.
		* 		The private key of the client is saved in a JavaKey store.
		* 
		* 
	*/
	public PrivateKey getPrivateKey(String nome) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, IOException {
		
		FileInputStream is = new FileInputStream(nome);
		
	    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	    
	    keystore.load(is, "SEC".toCharArray());
	    
	    String alias = nome;
	
	    Key key = keystore.getKey(alias, "SEC".toCharArray());
	    
	    return (PrivateKey) key;
	 
	}
	

	/*
		* //===========  Get the public key of the Client of the serialize file =================
		*	 
		* readPublicKeyFromFile method
		* 
		* 		Takes the filename as input and retrieves the public key of the Client of the serialize file.
		* 
		* 
	*/
	PublicKey readPublicKeyFromFile(String fileName) throws IOException {
		
	 	FileInputStream in = new FileInputStream(fileName);
	  	ObjectInputStream oin =  new ObjectInputStream(new BufferedInputStream(in));

	  	try {
	  	  
	  		BigInteger m = (BigInteger) oin.readObject();
	  	  
	  		BigInteger e = (BigInteger) oin.readObject();
	  	  
	  		RSAPublicKeySpec keySpecifications = new RSAPublicKeySpec(m, e);
	  	  
	  		KeyFactory kF = KeyFactory.getInstance("RSA");
	  	  
	  		PublicKey pubK = kF.generatePublic(keySpecifications);
	  	  
	  		return pubK;
	  	
	  	} catch (Exception e) {
	  		  throw new RuntimeException("Some error in reading public key", e);
	  	
	  	} finally {
	 	   oin.close();
	 	}
		
	}
	 
		
	
	/*
	 * When something goes wrong
	 * Close the Input/Output streams and disconnect
	 */
	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {}
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {}
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {}
			
	}
	
	
	
	/*
	 * To start the Client in console mode use one of the following command
	 * > java Client
	 * > java Client clientID
	 * > java Client clientID portNumber
	 * > java Client clientID portNumber serverAddress
	 * at the console prompt
	 * If the portNumber is not specified 1500 is used
	 * If the serverAddress is not specified "localHost" is used
	 * If the clientID is not specified "Anonymous" is used
	 */
	public static void main(String[] args) throws IOException, GeneralSecurityException  {
		
		// default values if not entered
		int portNumber = 1500;
		String serverAddress = "localhost";
		String clientID = "Anonymous";
		Scanner scan = new Scanner(System.in);
				
		int i = 0;
		
		System.out.println("Enter the Client Name: ");
		clientID = scan.nextLine();
		
		
		// different case according to the length of the arguments.
		switch(args.length) {
		
			case 3:
				
				// for > javac Client portNumber serverAddr
				serverAddress = args[2];
				
			case 2:
				
				// for > javac Client portNumber
				try {
					
					portNumber = Integer.parseInt(args[1]);
				}
				
				catch(Exception e) {
					
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Client [clientID] [portNumber] [serverAddress]");
					return;
				}
				
			case 1: 
				
				// for > javac Client clientID
				clientID = args[0];
				
			case 0:
				
				// for > java Client
				break;
			
				// if number of arguments are invalid
			default:
				
				System.out.println("Usage is: > java Client [clientID] [portNumber] [serverAddress]");
				
			return;
		}
				
		
		// create the Client object
		Client client = new Client(serverAddress, portNumber, clientID);
									
		// try to connect to the server and return if not connected
		if(!client.start())
			
			return;
		
				
		RSA rsa = new RSA();
		rsa.createRSA(clientConnection);
		
		seqNumber = 0;
				
		System.out.println("Type 'ENTER' to enter in the application");
		
		// infinite loop to get the input from the user
		while(true) {
												
			System.out.print("> ");
			
			// read message from user
			String msg = scan.nextLine();
			
			if (i == 0){
				
				if (msg.equalsIgnoreCase("ENTER")){
					
					System.out.println("\nHello.! Welcome to HDS Notary Application");
					System.out.println("1. Type 'SELL' to inform the Notary that you want to sell some good");
					System.out.println("2. Type 'STATEGOOD' to see if some specific good is available for sell");
					System.out.println("3. Type 'BUYGOOD' to buy a good");
					System.out.println("4. Type 'TRANSFERGOOD'' to inform the Notary of some transaction");
					System.out.println("5. Type 'LOGOUT' to logoff from application");
					
					setGoodsClient(clientID + "Maca", clientID);
					setGoodsClient(clientID + "Banana", clientID);
					setGoodsClient(clientID + "Kiwi", clientID);
												
					String temp =getGoodsClient().toString();
					
					String tempSeq = Integer.toString(seqNumber);
					
					LocalDateTime time = LocalDateTime.now();
																										
					client.sendMessage(new MessageHandler(MessageHandler.ENTER, temp.getBytes(), tempSeq.getBytes(), time));
					
					i=1;
					
				}
				
				else{
					
					System.out.println("Wrong input! 1. Type 'ENTER' to enter in the application");
					
				}
				
			}
			
			
			else {
				
				String tempSeq = Integer.toString(seqNumber);
				
				LocalDateTime time = LocalDateTime.now();
				
				// logout if message is LOGOUT
				if(msg.equalsIgnoreCase("LOGOUT")) {
					
					String temp = "";
					
					byte[] tempBytes = temp.getBytes();
													
					client.sendMessage(new MessageHandler(MessageHandler.LOGOUT, tempBytes, tempSeq.getBytes(), time));
					
					break;
				}
									
				// message to inform server that client want to sell some good
				else if(msg.equalsIgnoreCase("SELL")) {
					
					System.out.println("Write the good you want to sell: ");
					
					String msgGoodToServer = scan.nextLine();
					
					msgGoodToServer=intentionToSell(msgGoodToServer);
							
					byte[] tempBytes = msgGoodToServer.getBytes();	
									
					client.sendMessage(new MessageHandler(MessageHandler.SELL, tempBytes, tempSeq.getBytes(), time));	
							
				}
				
				// message to the server to get the state of some good
				else if(msg.equalsIgnoreCase("STATEGOOD")) {
								
					System.out.println("Write the product of which the state you want to check: ");
								
					String msgGoodStateToServer = scan.nextLine();
								
					msgGoodStateToServer=getStateOfGood(msgGoodStateToServer);
					
					byte[] tempBytes =msgGoodStateToServer.getBytes();
																				
					client.sendMessage(new MessageHandler(MessageHandler.STATEGOOD, tempBytes, tempSeq.getBytes(), time));	
																							
				}
				
				// message to the server to buy some good
				else if(msg.equalsIgnoreCase("BUYGOOD")) {
					
					System.out.println("Write @Product Owner <space>" + " the goodID that you want to buy from him: ");
					
					String msgGoodToBuy = scan.nextLine();
					
					msgGoodToBuy = buyGood(msgGoodToBuy);
					
					byte[] tempBytes = msgGoodToBuy.getBytes();
										
					client.sendMessage(new MessageHandler(MessageHandler.BUYGOOD, tempBytes, tempSeq.getBytes(), time));
																										
				}
				
				// message to the server to transfer some good
				else if(msg.equalsIgnoreCase("TRANSFERGOOD")) {
					
					System.out.println("Write the goodID that will be transfer <space>" + " buyer ID: ");

					String msgTransfer = scan.nextLine();
									
					msgTransfer= transferGood(msgTransfer);
					
					byte[] tempBytes = msgTransfer.getBytes();
										
					client.sendMessage(new MessageHandler(MessageHandler.TRANSFERGOOD, tempBytes, tempSeq.getBytes(), time));	
																													
				}
				
				// regular text message
				else {
					
					//Wrong Input from the Client
					System.out.println("Wrong input! Try again please. ");
					
				}
				
			}
			
		}
		
		// close resource
		scan.close();
		
		// client completed its job. disconnect client.
		client.disconnect();	
	}

	
	
	/*
	 * a class that waits for the message from the server
	 */
	class ListenFromServer extends Thread {

		public void run() {
			
			while(true) {
				
				try {
					
					// read the message form the input datastream
					message = (MessageHandler) sInput.readObject();
					
					//get the message
					String msgDecrypt = decryptMessage(message.getData());
					
					//Get the sequence number of the message received
					String seqDecryt = decryptMessage(message.getSeq());
					
					LocalDateTime t0 = LocalDateTime.now();
									
					long diff = ChronoUnit.SECONDS.between(message.getLocalDate(), t0);
										
					//check if the message's time has expired 
					if (diff < expireTime ){
						
						// check if the message has the right sequence number
						if (seqNumber == Integer.parseInt(seqDecryt)){
							
							seqNumber ++ ;
							
							// print the message
							System.out.println("Mensagem recebida do servidor: " +  msgDecrypt);
							
							System.out.print("> ");
							
						}
						
						else {
							
							display("The Message hasn't the right sequence number. Won't accept it");
						}
						
					}
					
					//the message has expired
					else{
						
						display("The Message has expired. Won't accept it");
					}
													
				}
				
				catch(IOException e) {
					
					display(notif + "Server has closed the connection: " + e + notif);
					
					break;
				}
				
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
	
}