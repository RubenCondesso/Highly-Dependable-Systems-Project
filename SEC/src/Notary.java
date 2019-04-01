import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// the server that can be run as a console
public class Notary {

	// a unique ID for each connection
	private static int connectionID;
	
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> clientsList;
	
	// HashMap to keep the goods of each Client
	private HashMap<String, String> clientsGoodsList = new HashMap<String,String>();
	
	// HashMap to keep the goods to sell of each Client
	private HashMap<String, String> clientsGoodsToSell = new HashMap<String,String>();
	
	// HashMap with the AESKeys of each client of the application
	private HashMap <SecretKey, Integer> listAESKeys = new HashMap <SecretKey, Integer>();
	
	//
	private HashMap <String, Integer> listRSAOwners = new HashMap <String, Integer>();
	
	// to display time
	private SimpleDateFormat sdf;
	
	// the port number to listen for connection
	private int port;
	
	// to check if server is running
	private boolean serverRunning;
	
	// notification
	private String notif = " *** ";
	
	private Cipher keyDecipher;
	
	private Cipher ServerDecryptCipher;
	
	private Cipher ServerEncryptCipher;
	
	SecretKey AESKey;
		
	static String IV = "AAAAAAAAAAAAAAAA";
		
	MessageHandler msgEncrypt;
	
	private MessageHandler message;
				
	//constructor that receive the port to listen to for connection as parameter
	
	public Notary(int port) {
		
		// the port
		this.port = port;
		
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		
		// an ArrayList to keep the list of the clients
		clientsList = new ArrayList<ClientThread>();
	}
			
	public void start() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		serverRunning = true;
		
		//create socket server and wait for connection requests 
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
					
			// infinite loop to wait for connections ( till server is active )
			while(serverRunning) 
			{
				display("Server waiting for Clients on port " + port + ".");
				
				// accept connection if requested from client
				Socket socket = serverSocket.accept();
				
				// break if server stoped
				if(!serverRunning)
					
					break;
				
				// if client is connected, create its thread
				ClientThread t = new ClientThread(socket);
				
				//add this client to arraylist
				clientsList.add(t);
								
				t.start();
								
			}
			
			// try to stop the server
			try {
				serverSocket.close();
				
				for(int p = 0; p < clientsList.size(); ++p) {
					
					ClientThread tc = clientsList.get(p);
					
					try {
						
						// close all data streams and socket
						tc.sInput.close();
						tc.sOutput.close();
						tc.socket.close();
					}
					
					catch(IOException ioE) {
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		catch (IOException e) {
			
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}
	
	
	// to stop the server
	protected void stop() {
		
		serverRunning = false;
		
		try {
			
			new Socket("localhost", port);
		}
		
		catch(Exception e) {
		}
	}
	
	// Display an event to the console
	private void display(String msg) {
		
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// to broadcast a message to all Clients
	private synchronized boolean broadcast(String message) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		// add timestamp to the message
		String time = sdf.format(new Date());
		
		// to check if message is private i.e. client to client message
		String[] w = message.split(" ",3);
		
		boolean isPrivate = false;
		
		if(w[1].charAt(0)=='@') 
			
			isPrivate=true;
		
		
		// if private message, send message to mentioned clientID only. (When the option BuyGood is called)
		if(isPrivate==true)
		{
			String tocheck=w[1].substring(1, w[1].length());
			
			message=w[0]+w[2];
			
			String messageLf = time + " " + " The buyer " + w[0] + " will buy the following good from you: " + w[2] + "\n";
			
			boolean found=false; 
						
			// we loop in reverse order to find the mentioned clientID
			for(int y=clientsList.size(); --y>=0;){
				
				ClientThread ct1=clientsList.get(y);
				
				String check=ct1.getClientID();
				
				if(check.equals(tocheck)) {
					
					// try to write to the Client if it fails remove it from the list
					if(!ct1.writeMsg(messageLf, ct1.getConnectionID())) {
						
						clientsList.remove(y);
						display("Disconnected Client " + ct1.clientID + " removed from list.");
					}
					
					// clientID found and delivered the message
					found=true;
					
					break;
				}
				
			}
			
			// mentioned user not found, return false
			if(found!=true)
			{
				return false; 
			}
		}
		
		// if message is a broadcast message
		else
		{
			String messageLf = time + " " + message + "\n";
			
			// display message
			System.out.print(messageLf);
			
			// we loop in reverse order in case we would have to remove a Client
			// because it has disconnected
			for(int x = clientsList.size(); --x >= 0;) {
				
				ClientThread ct = clientsList.get(x);
				
				
				// try to write to the Client if it fails remove it from the list
				if(!ct.writeMsg(messageLf, ct.getConnectionID())) {
					
					clientsList.remove(x);
					
					display("Disconnected Client " + ct.clientID + " removed from list.");
				}
			}
		}
		return true;
		
		
	}

	// if client sent LOGOUT message to exit
	synchronized void remove(int id) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		String disconnectedClient = "";
		
		// scan the array list until we found the Id
		for(int k = 0; k < clientsList.size(); ++k) {
			
			ClientThread ct = clientsList.get(k);
			
			// if found remove it
			if(ct.id == id) {
				
				disconnectedClient = ct.getClientID();
				
				clientsList.remove(k);
				
				break;
			}
		}
		broadcast(notif + disconnectedClient + " has left the application. " + notif);
	}
	
	
	/*
	 *  To run as a console application
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	public static void main(String[] args) throws IOException, GeneralSecurityException {
		
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		
		switch(args.length) {
		
			case 1:
				
				try {
					
					portNumber = Integer.parseInt(args[0]);
				}
				
				catch(Exception e) {
					
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
				
			case 0:
				
				break;
				
			default:
				
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
		
		RSA rsa = new RSA();
		rsa.createRSA("Notary");
			
		// create a server object and start it
		Notary server = new Notary(portNumber);
		
		server.start();
		
	}

	// One instance of this thread will run for each client
	class ClientThread extends Thread {
		
		// the socket to get messages from client
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		
		// unique id (easier for desconnection)
		int id;
		
		// the clientID of the Client
		String clientID;
		
		// message object to receive message and its type
		MessageHandler msghandler;
		
		// timestamp
		String date;
		
			

		// Constructor
		ClientThread(Socket socket) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
			
			// a unique id
			id = ++connectionID;
			
			this.socket = socket;
					
			//Creating both Data Stream
			System.out.println("Thread trying to create Object Input/Output Streams");
			
			try
			{
				
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());								
				
			}
			catch (IOException e) {
				
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			
            date = new Date().toString() + "\n";
		}
		
		public String getClientID() {
			return clientID;
		}

		public void setClientID(String clientID) {
			this.clientID = clientID;
		}
		
		public Integer getConnectionID() {
			return id;
		}
		
		
		// infinite loop to read and forward message
		public void run() {
			
			// to loop until LOGOUT
			boolean serverRunning = true;
			
			int i = 0;
			
			while(serverRunning) {
				
				// read a String (which is an object)
				try {
											
					message = (MessageHandler) sInput.readObject(); 
										
				}
				
				catch (IOException e) {
					
					display(clientID + " Exception reading Streams: " + e);
					break;				
				}
				
				catch(ClassNotFoundException e2) {
					break;
				}
				
				//In the first message received by the client, the server will decrypt the AES Key from the client
				if (i == 0){
										
					if(message.getData() != null){	
												
											
						decryptFirstMessage(message.getData(), id);
																	
						i++;
					}
					
					else{
						
						display("Error in decrypting AES key in clientThread.run()"); 
						
						System.exit(1);
					}
					
				}
				
				else {
					
					// Server will decrypt the normal messages from the Client
					if(message.getData() != null){
						
						
						/*
						for (Entry<SecretKey, Integer> entry : listAESKeys.entrySet()){
							
							if((entry.getValue()).equals(getConnectionID())){
								
								AESKey = entry.getKey();
							}
							
						}
						*/
													
						String mensagemDecryt = decryptMessage(message.getData());
												
						
						// different actions based on type message
						switch(message.getType()) {
						
						case MessageHandler.ENTER:
														
							//Convert the message received to a HashMap
							mensagemDecryt = mensagemDecryt.substring(1, mensagemDecryt.length()-1); //remove curly brackets
							
							String[] keyValuePairs = mensagemDecryt.split(",");              //split the string to creat key-value pairs
							
							Map <String,String> map = new HashMap<>();               

							for(String pair : keyValuePairs)                        //iterate over the pairs
							{
							    String[] entry = pair.split("=");                   //split the pairs to get key and value 
							    map.put(entry[0].trim(), entry[1].trim());          //add them to the hashmap and trim whitespaces
							}
														
							// read the goods list of the client and his clientID
							HashMap<String, String> temporaryList = (HashMap<String, String>) map;
							
							Object nomeCliente = temporaryList.keySet().toArray()[0];
																		
							clientID= temporaryList.get(nomeCliente);
																									
							try {
								
								broadcast(notif + clientID + " has joined the application " + notif);
							} 
							catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
									| InvalidAlgorithmParameterException | IllegalBlockSizeException
									| BadPaddingException e1) {
								
								e1.printStackTrace();
							}
							
							i++;
							
							for (Map.Entry<String, String> item : temporaryList.entrySet()) {
								
								//the good of the client
								String key = item.getKey();
								
								//Client name
							    String value = item.getValue();
							    
							    //Add the new client and his goods to the all goods List
							    clientsGoodsList.put(key, value);
							    
							}
														
							break;
							
						case MessageHandler.LOGOUT:
							
							display(clientID + " disconnected from the application.");
							serverRunning = false;
							
							break;
												
						case MessageHandler.SELL:
							
							display("The client " + clientID + " want to sell the following good: " + mensagemDecryt);
												
							int c=0;
												
							for (Map.Entry<String, String> item : clientsGoodsList.entrySet()){
								
								String key = item.getKey();
							    String value = item.getValue();
							    
							    // Check if the client it is the owner of the good
							    if (value.equals(clientID) && key.equals(mensagemDecryt)){
							    	
							    	c=1;
							    	
							    	//put the good on the list of products to sell
							    	clientsGoodsToSell.put(key, value);
							    	
							    	display("The good is now for sale.");
							    	
							    	try {
							    		
										writeMsg("Yes" + "\n", getConnectionID());
										
									} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
											| InvalidAlgorithmParameterException | IllegalBlockSizeException
											| BadPaddingException e) {
										
										e.printStackTrace();
									}
							 
							    }
							}
							//The ClientID and/or his good was not found in the clients goods list    
							if(c == 0){
								
								display("The ClientID and/or his good was not found in the clients goods list.");
								
								try {
									
									writeMsg("No" + "\n", getConnectionID());
									
								} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
										| InvalidAlgorithmParameterException | IllegalBlockSizeException
										| BadPaddingException e) {
									
									e.printStackTrace();
								}
								
							}
							
							break;
													
						
						case MessageHandler.STATEGOOD:
							
							display("The client " + clientID + " want to check the state of the following good: " + mensagemDecryt);
							
							int s=0;
							
							for (Map.Entry<String, String> item0 : clientsGoodsList.entrySet()){
							
							    String checkCliente = item0.getValue();
							    
							    // Check if the client exists on the application 
							    if (checkCliente.equals(clientID)){
							    	
							    	for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
										
										String key = item.getKey();
									    String value = item.getValue();
									    									    
									    //Verify if the requested good is on sale 
									    if (key.equals(mensagemDecryt) && (s != 1)){
									    	
									    	s=1;
									    	
									    	display("The good is for sale.");
									    	
									    	try {
									    		
												writeMsg("Good: " + key + ", " + "Owner: " + value + "\n", getConnectionID());
												
											} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
													| InvalidAlgorithmParameterException | IllegalBlockSizeException
													| BadPaddingException e) {
											
												e.printStackTrace();
											}
									    
									    }						
									}
							    	
							    }
							}
							
																	
							//This good is not for sale   
							if(s == 0){
								
								display("The good you asked is not for sale or does not exist or your ID does not exist in the application");
								
								try {
									
									writeMsg("No" + "\n", getConnectionID());
									
								} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
										| InvalidAlgorithmParameterException | IllegalBlockSizeException
										| BadPaddingException e) {
									
								
									e.printStackTrace();
								}
								
							}
							
							break;
							
						case MessageHandler.BUYGOOD:
							
							int n=0;
							
							boolean cli;
							
							try {
								
								cli = broadcast(clientID + ": " + mensagemDecryt);
								
								String[] w = (mensagemDecryt.toString()).split(" ",3);			
								
								for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
									
									String key = item.getKey();
								    String value = item.getValue();
								    
								    //Verify if the requested good is for sale 
								    if (key.equals(w[1])){
								    	
								    	n=1;
										
										if(cli == false) {
											
											String msg = notif + "Sorry. No such user exists." + notif;
											writeMsg(msg, getConnectionID());
										}
										
										else{
											
											writeMsg("Yes" +"\n", getConnectionID());
											
										}
								    }						
								}
								
								//This good is not for sale   
								if(n == 0){
									
									display("The good you asked is not for sale or does not exist. ");
									writeMsg("No" + "\n", getConnectionID());
									
								}
								
								break;
								
							} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
									| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
							
								e.printStackTrace();
							}
							
							
						case MessageHandler.TRANSFERGOOD:
							
							String[] m = (mensagemDecryt.toString()).split(" ",3);
							
							//The goodID that will be transfer
							String good = m[0];
							
							//The BuyerID
							String buyer = m[1];
							
							int p = 0;
							
							for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
								
								String key = item.getKey();
							    String value = item.getValue();
							    
							    //Verify if the requested good is for sale and if the client it's the owner of the good
							    if (value.equals(clientID) && key.equals(good)){
							    		
							    	for(int y=clientsList.size(); --y>=0;){
										
										ClientThread ct1=clientsList.get(y);
										String check=ct1.getClientID();
										
										//Verify if the Buyer is a client on the list
										if (check.equals(buyer)){
											
											p=1;
											
											clientsGoodsList.put(key, buyer);
											
											clientsGoodsToSell.remove(key, value);					
											
											display("The transfer was successful. ");
											
											//Tell the seller that the transfer was successful
											try {
												
												writeMsg("Yes" + "\n", getConnectionID());
												
											} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
													| InvalidAlgorithmParameterException | IllegalBlockSizeException
													| BadPaddingException e) {
												
												e.printStackTrace();
											}
											
											//Tell the buyer that the transfer was successful
											try {
												
												ct1.writeMsg("Yes" + "\n", ct1.getConnectionID());
												
											} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
													| InvalidAlgorithmParameterException | IllegalBlockSizeException
													| BadPaddingException e) {
												
												e.printStackTrace();
											}
											
										}
							    	}
								
							    }						
							}
							
							if(p == 0){
								
								display("The transfer was unsuccessful. ");
								
								try {
									
									writeMsg("No" + "\n", getConnectionID());
									
								} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
										| InvalidAlgorithmParameterException | IllegalBlockSizeException
										| BadPaddingException e) {
									
									e.printStackTrace();
								}
								
							}
							
							
							break;
							
						}
					}
					
				}
							
						
			}
			
			// if out of the loop then disconnected and remove from client list
			try {
				
				remove(id);
				
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
					| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
				
				e.printStackTrace();
			}
			close();
		}
		
		
		
		// close everything
		private void close() {
					
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		// write a String to the Client output stream
		private boolean writeMsg(String msg, int id) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException  {
			
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
						
				close();
				return false;
			}
			
			// write the message to the stream
			try {
												
				msgEncrypt = null;
				
				msgEncrypt = new MessageHandler(5, encryptMessage(msg, id));
				
								
				sOutput.writeObject(msgEncrypt);
			}
			
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				
				display(notif + "Error sending message to " + clientID + notif);
				display(e.toString());
				
			} 
			
			return true;
		}
	}
	
	
	 /*
	   * // ====== Receive the encrypted AES key from server and decipher it
	   * decryptFirstMessage method
	   * 					will use RSA private key from the public - private key pair to
	   * 					decipher the AES key encrypted using public key and sent by the client.
	   * 
	   * @param byte[] encryptedData
	   * 							The encrypted key as byte array.
	   * 
	   */
	
	 
	private void decryptFirstMessage (byte[] encryptedMessage, Integer id) {
		
		SecretKey key = null; 
		
		PrivateKey privKey = null; 
		
		keyDecipher = null;
		
	    try {

	    	PrivateKey prK = readPrivateKeyFromFile("private.key"+ "Notary");
		
            ServerDecryptCipher = Cipher.getInstance("RSA");
            
            //ServerDecryptCipher.init(Cipher.DECRYPT_MODE, prK , new IvParameterSpec(IV.getBytes()));
            
            ServerDecryptCipher.init(Cipher.DECRYPT_MODE, prK);
            
            byte[] msg = ServerDecryptCipher.doFinal(encryptedMessage);
	            	                      	
            listRSAOwners.put(new String(msg), id);
						            
	        }
	    
	        catch(Exception e){  
	        	
	        	e.printStackTrace(); 
	         
	        	System.out.println ( "exception decrypting the aes key: "  + e.getMessage() );
	        }
	       
	    }
	
	
	/*
	 * //=========== Decipher/decrypt the encrypted message using AES key =================
	 * 
	 * decryptMessage method.
	 * 						Deciphers the encrypted message received from the client.
	 * 						Takes byte array of the encrypted message as input.
	 * 
	 * 
	 */
		
	private String decryptMessage(byte[] encryptedMessage) {
		
		ServerDecryptCipher = null;
	        
		try
	        {
				
				PrivateKey prK = readPrivateKeyFromFile("private.key"+ "Notary");
			
	            ServerDecryptCipher = Cipher.getInstance("RSA");
	            
	            //ServerDecryptCipher.init(Cipher.DECRYPT_MODE, prK , new IvParameterSpec(IV.getBytes()));
	            
	            ServerDecryptCipher.init(Cipher.DECRYPT_MODE, prK);
	            
	            byte[] msg = ServerDecryptCipher.doFinal(encryptedMessage);
	            	            	            
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
	 * //===========  Encrypted message using AES key =================
	 * 
	 * encryptMessage method
	 * 						Takes the message string as input and encrypts it.
	 * 
	 * 
	 */
	private byte[] encryptMessage(String s, int id) throws NoSuchAlgorithmException, NoSuchPaddingException, 
						InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
										BadPaddingException, IOException{
		
		ServerEncryptCipher = null;
		
		String owner = null;
		
		byte[] cipherText = null;
		
		
		for (Entry<String, Integer> entry : listRSAOwners.entrySet()){
			
			if((entry.getValue()).equals(id)){
				
				owner = entry.getKey();
			}
		}
		
		
		PublicKey pK = readPublicKeyFromFile("public.key"+ owner);
		
		ServerEncryptCipher = Cipher.getInstance("RSA");  
		
		//ServerEncryptCipher.init(Cipher.ENCRYPT_MODE, pK, new IvParameterSpec(IV.getBytes()) );
		
		ServerEncryptCipher.init(Cipher.ENCRYPT_MODE, pK);
		
		cipherText = ServerEncryptCipher.doFinal(s.getBytes());
		
		
	
	   return cipherText;
	   
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
	
	
}