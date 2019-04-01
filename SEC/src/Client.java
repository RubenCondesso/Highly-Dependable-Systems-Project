
import java.net.*;
import java.io.*;
import java.util.*;

import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Scanner;

//import java.math.BigInteger;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;


//The Client that can be run as a console
public class Client  {
	
	// notification
	private String notif = " *** ";

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private static ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;					
	
	private String server, clientID, good;	
	private int port;	
	
	private Cipher cipher1;
	private Cipher cipher2;
	
	SecretKey AESkey;
	
	MessageHandler msgEncrypt;
	
	MessageHandler message;
	
	static String IV = "AAAAAAAAAAAAAAAA";
	
	int i = 0;
	
	
	
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
		
		//check if the good in hand are in the list of goods of this client
		for (Map.Entry<String, String> item : goodsList.entrySet()) {
			
			//the good of the client
			String key = item.getKey();
			
			if (key.equals(good)){
				
				return good;
			}
		}
		
		return null;
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
	 * To send a message to the server
	 */
	void sendMessage(MessageHandler msg) {
		
		try {
			
			if(i==0){
						
				msgEncrypt = null;
				
				//Client will encrypt his AEK key   
				//msgEncrypt = new MessageHandler(5, encryptAESKey());
				
				msgEncrypt = new MessageHandler(msg.getType(), encryptMessage(new String(msg.getData())));
							 
				sOutput.writeObject(msgEncrypt);
				
			}
			
			else {
				
				msgEncrypt = null;
				
				//Client will send a normal message encrypted				
				msgEncrypt = new MessageHandler(msg.getType(), encryptMessage(new String(msg.getData())));
								
				sOutput.writeObject(msgEncrypt);
				
			}
						
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
	 * //============== Create AES Key =================================
	 * 
	 * generateAESkey method
	 * 
	 * 						Called by main method, generates the AES key for encryption / decryption of the messages exchanged between client and server.
	 */
	
	/*
	void generateAESkey() throws NoSuchAlgorithmException{
		
		AESkey = null;
	
		KeyGenerator Gen = KeyGenerator.getInstance("AES");
	
		Gen.init(128);
	
		AESkey = Gen.generateKey();
	
		//System.out.println("Genereated the AES key : " + AESkey);
	}
	
	 */
	
	/*
	 * // ====== Read RSA Public key to Encrypt the AES key  ==================
	 * 
	 * encryptAESKey method.
	 * 
	 * 						Will encrypt the AES key generated by generateAESkey method. It will also calculate the time taken for encrypting the AES key using RSA encryption method.
	 * 
	 * 						To encrypt the AES key, this method will read RSA public key from the RSA public = private key pairs saved in the same directory.
	 * 							
	 * 						Dependency: the public key  file "public.key" should be saved in the same directory. (Performed by server.java class)
	 * 	
	 */
		

	
	/*
	private byte[] encryptAESKey (){
		
		cipher1 = null;
    	
		byte[] key = null;
  	  	
		try {
		 
			PublicKey pK = readPublicKeyFromFile("public.key");
	 	  
			// System.out.println("Encrypting the AES key using RSA Public Key" + pK);
   	     
			// initialize the cipher with the user's public key
			cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
   	    
   	     	cipher1.init(Cipher.ENCRYPT_MODE, pK );
   	     
   	     	long time1 = System.nanoTime();
   	     
   	     	key = cipher1.doFinal(AESkey.getEncoded());   // this encrypted key will be sent to the server.
   	     
   	     	long time2 = System.nanoTime();
   	     
   	     	long totalRSA = time2 - time1;
   	     
   	     	// System.out.println("Time taken by RSA Encryption (Nano Seconds) : " + totalRSA);
   	     
   	     	i = 1;
   	 	}
  	  
		catch(Exception e ) {
   		 
    	    System.out.println ( "exception encoding key: " + e.getMessage() );
    	    e.printStackTrace();
   	 	}
  	  
		return key;
  	  } 
	 */
	
	
	
	
	/*
	 * //============= Encrypt Data to send =================
	 * 
	 * encryptMessage method
	 * 						Encrypts the string input using AES encryption with AES key generated by generateAESkey method.
	 * 
	 * @param 	String s 
	 * 					Input string to encrypt
	 * 
	 * Returns byte array as output.
	 * 
	 */


	private byte[] encryptMessage(String s) throws NoSuchAlgorithmException, NoSuchPaddingException, 
						InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
										BadPaddingException, IOException{
	
		
		PublicKey pK = readPublicKeyFromFile("public.key"+"Notary");
		
		cipher2 = null;
	
		byte[] cipherText = null;
	
		cipher2 = Cipher.getInstance("RSA");
	
		//cipher2.init(Cipher.ENCRYPT_MODE, AESKey, new IvParameterSpec(IV.getBytes()) );
		
		cipher2.init(Cipher.ENCRYPT_MODE, pK);
	
		long time3 = System.nanoTime();
	
		cipherText = cipher2.doFinal(s.getBytes());
	
		long time4 = System.nanoTime();
	
		long totalAES = time4 - time3;
	
		// System.out.println("Time taken by AES Encryption (Nano Seconds) " + totalAES);
			   
		return cipherText;
	}
	
	
	
	/*
	 * //=========== Decipher the received message with AES key =================
	 * 
	 * decryptMessage method, will decrypt the cipher text received from server. Currently disabled, can be enabled for two way communication.
	 * 
	 * @param byte[] data
	 * 					takes the byte array of encrypted message as input. Returns plain text.
	 * 
	 * 	
	 */
	
		
	private String decryptMessage(byte[] encryptedMessage) {
		
	        cipher2 = null;
	        
	        try {
	        	
	        	PrivateKey prK = readPrivateKeyFromFile("private.key"+ getClientID());
	            
	        	cipher2 = Cipher.getInstance("RSA");
	            
	        	//cipher2.init(Cipher.DECRYPT_MODE, prK , new IvParameterSpec(IV.getBytes()));
	        	
	        	cipher2.init(Cipher.DECRYPT_MODE, prK);
	             
	        	byte[] msg = cipher2.doFinal(encryptedMessage);		            
	             
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
	  	
	PrivateKey readPrivateKeyFromFile(String fileName) throws IOException {
			
		FileInputStream in = new FileInputStream(fileName);
		ObjectInputStream oin =  new ObjectInputStream(new BufferedInputStream(in));

		try {
		  	  
			BigInteger m = (BigInteger) oin.readObject();
		  	  
		  	BigInteger e = (BigInteger) oin.readObject();
		  	  
		  	RSAPrivateKeySpec keySpecifications = new RSAPrivateKeySpec(m, e);
		  	  
		  	KeyFactory kF = KeyFactory.getInstance("RSA");
		  	  
		  	PrivateKey privK = kF.generatePrivate(keySpecifications);
		  	  
		  	return privK;
		  	
		} catch (Exception e) {
			
				throw new RuntimeException("Some error in reading private key", e);
		  	
		  } finally {
		 	   
			  oin.close();
		 	
		  }
			
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
		
		RSA rsa = new RSA();
		rsa.createRSA(clientID);
		
		// create the Client object
		Client client = new Client(serverAddress, portNumber, clientID);
		
		//client.generateAESkey();
							
		// try to connect to the server and return if not connected
		if(!client.start())
			
			return;
		
		//First Message between server and client			
		byte[] owner = (client.getClientID()).getBytes();
		
		//Send AES Key of Client to server
		client.sendMessage(new MessageHandler(MessageHandler.Encrypt, owner));
		
		System.out.println("1. Type 'ENTER' to enter in the application");
		
		// infinite loop to get the input from the user
		while(true) {
												
			System.out.print("> ");
			
			// read message from user
			String msg = scan.nextLine();
			
			if (i == 0){
				
				if (msg.equalsIgnoreCase("ENTER")){
					
					System.out.println("\nHello.! Welcome to HDS Notary Application");
					System.out.println("1. Type the message to send broadcast to all active clients");
					System.out.println("2. Type '@clientID<space>yourmessage' to send message to desired client");
					System.out.println("3. Type 'SELL' to inform the server that you want to sell some good");
					System.out.println("4. Type 'STATEGOOD' to see if some specific good is available for sell");
					System.out.println("5. Type 'BUYGOOD' to buy a good");
					System.out.println("6. Type 'LOGOUT' to logoff from server");
					
					setGoodsClient(clientID + "Maça", clientID);
					setGoodsClient(clientID + "Banana", clientID);
					setGoodsClient(clientID + "Kiwi", clientID);
												
					String temp =getGoodsClient().toString();
																					
					client.sendMessage(new MessageHandler(MessageHandler.ENTER, temp.getBytes()));
					
					i=1;
					
				}
				
				else{
					
					System.out.println("Wrong, type again!! 1. Type 'ENTER' to enter in the application");
					
				}
				
			}
			
			
			else {
				
				// logout if message is LOGOUT
				if(msg.equalsIgnoreCase("LOGOUT")) {
					
					String temp = "";
					
					byte[] tempBytes = temp.getBytes();
													
					client.sendMessage(new MessageHandler(MessageHandler.LOGOUT, tempBytes));
					
					break;
				}
									
				// message to inform server that client want to sell some good
				else if(msg.equalsIgnoreCase("SELL")) {
					
					System.out.println("Write the good you want to sell: ");
					
					String msgGoodToServer = scan.nextLine();
					
					msgGoodToServer=intentionToSell(msgGoodToServer);
					
					//The good was found in the good's list
					if(msgGoodToServer != null){
						
						byte[] tempBytes = msgGoodToServer.getBytes();
						
						client.sendMessage(new MessageHandler(MessageHandler.SELL, tempBytes));	
						
					}
					
					else{
						
						System.out.println("Something went wrong. The good you typed is not in your good's list. ");
					}
					
				}
				
				// message to the server to get the state of some good
				else if(msg.equalsIgnoreCase("STATEGOOD")) {
								
					System.out.println("Write the product of which the state you want to check: ");
								
					String msgGoodStateToServer = scan.nextLine();
								
					msgGoodStateToServer=getStateOfGood(msgGoodStateToServer);
					
					byte[] tempBytes =msgGoodStateToServer.getBytes();
															
					client.sendMessage(new MessageHandler(MessageHandler.STATEGOOD, tempBytes));	
																							
				}
				
				// message to the server to buy some good
				else if(msg.equalsIgnoreCase("BUYGOOD")) {
					
					System.out.println("Write @Product Owner <space>" + " the goodID that you want to buy from him: ");
					
					String msgGoodToBuy = scan.nextLine();
					
					msgGoodToBuy = buyGood(msgGoodToBuy);
					
					byte[] tempBytes = msgGoodToBuy.getBytes();
					
					client.sendMessage(new MessageHandler(MessageHandler.BUYGOOD, tempBytes));
																										
				}
				
				// message to the server to transfer some good
				else if(msg.equalsIgnoreCase("TRANSFERGOOD")) {
					
					System.out.println("Write the goodID that will be transfer <space>" + " buyer ID: ");

					String msgTransfer = scan.nextLine();
									
					msgTransfer= transferGood(msgTransfer);
					
					byte[] tempBytes = msgTransfer.getBytes();
					
					client.sendMessage(new MessageHandler(MessageHandler.TRANSFERGOOD, tempBytes));	
																													
				}
				
				// regular text message
				else {
					
					
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
					
					String msgDecrypt = decryptMessage(message.getData());
					
					// print the message
					System.out.println("Mensagem recebida do servidor: " +  msgDecrypt);
					
					System.out.print("> ");
															
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