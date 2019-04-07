import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.CertificateException;

import javax.crypto.*;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;

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
				
	// to display time
	private SimpleDateFormat sdf;
	
	// the port number to listen for connection
	private int port;
	
	// to check if server is running
	private boolean serverRunning;
	
	// id of the connection of the server socket
	private static String notaryConnection;
	
	// notification
	private String notif = " *** ";
		
	private Cipher ServerDecryptCipher;
	
	private Cipher ServerEncryptCipher;
	
	MessageHandler msgEncrypt;
		
	private MessageHandler message;
	
	
	//time to expire message
	private static int expireTime;
				
	//constructor that receive the port to listen to for connection as parameter
	public Notary(int port) {
		
		// the port
		this.port = port;
		
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		
		// an ArrayList to keep the list of the clients
		clientsList = new ArrayList<ClientThread>();
	}
			
	public void start() throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
		
		serverRunning = true;
		
		//create socket server and wait for connection requests 
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
						
			notaryConnection = serverSocket.getInetAddress().getHostAddress().toString().replace("/","") + ":" + serverSocket.getLocalPort();
			
			RSA rsa = new RSA();
			
			rsa.createRSA(notaryConnection);
			
			//30 seconds to message expire
			expireTime = 10;
			
			
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
	private synchronized boolean broadcast(String message) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
		
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
						
			String messageLf = time + " " + " The buyer " + w[0] + " will buy the following good from you: " + w[2] + ". Inform the Notary about the transfer. "  + "\n";
			
			boolean found=false; 
						
			// we loop in reverse order to find the mentioned clientID
			for(int y=clientsList.size(); --y>=0;){
				
				ClientThread ct1=clientsList.get(y);
				
				String check=ct1.getClientID();
				
				if(check.equals(tocheck)) {
					
					// try to write to the Client if it fails remove it from the list
					if(!ct1.writeMsg(messageLf)) {
						
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
				if(!ct.writeMsg(messageLf)) {
					
					clientsList.remove(x);
					
					display("Disconnected Client " + ct.clientID + " removed from list.");
				}
			}
		}
		return true;
		
		
	}

	// if client sent LOGOUT message to exit
	synchronized void remove(int id) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
		
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
	
	
	
	// to error message to a specific client
	private synchronized boolean sendErrorMsg(String clientName, String errorMsg) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
			
		for(int t = clientsList.size(); --t >= 0;) {
				
			ClientThread cT1 = clientsList.get(t);
			
			if(clientName.equals(cT1.getClientID())){
				
				cT1.writeMsg(errorMsg);
			}			
		}
			
		return true;
			
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
		
		// unique id (easier for disconnection)
		int id;
		
		// sequence number of the messages
		int seqNumber; 
		
		// the clientID of the Client
		String clientID;
		
		// message object to receive message and its type
		MessageHandler msghandler;
		
		// timestamp
		String date;
		
			

		// Constructor
		ClientThread(Socket socket) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
			
			// a unique id
			id = ++connectionID;
			
			seqNumber = 0;
			
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
		
		
		//check if the clientID is on the application
		public boolean checkClientID(String id) {
			
			return true;
			
		}
		


		//check if the good is on the application
		public boolean checkGood(String good) {
			
			for (Map.Entry<String, String> item : clientsGoodsList.entrySet()){
				
				String key = item.getKey();				
				
				if (key.equals(good)){
					
					return true;
				}
				
			}
	
			return false;
			
		}
		
		
		//check if the clientID is the owner of the good, and the good exists on the application
		public boolean checkGoodToSell(String id, String good) {
			
			
			for (Map.Entry<String, String> item : clientsGoodsList.entrySet()){
				
				String key = item.getKey();
				String value = item.getValue();
		    
				// Check if the client it is the owner of the good
				if (value.equals(id) && key.equals(good)){
					
					return true;
					
				}
			}
			
			return false;
			
		}
		
		
		
		// infinite loop to read and forward message
		public void run() {
			
			// to loop until LOGOUT
			boolean serverRunning = true;
			
			//int i = 0;
			
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
				
				// Server will decrypt the messages from the Client
				if(message.getData() != null){
												
					String idConnection = socket.getLocalAddress().getHostAddress().toString() + ":" + socket.getPort();
												
					String mensagemDecryt = decryptMessage(message.getData(), idConnection);
					
					String seqDecryt = decryptMessage(message.getSeq(), idConnection);	
					
					LocalDateTime t0 = LocalDateTime.now();
					
					long diff = ChronoUnit.SECONDS.between(message.getLocalDate(), t0);
					
					//check if the message's time has expired 
					if (diff < expireTime ){
						
						//Verify if message has the right sequence number
						if (seqNumber == Integer.parseInt(seqDecryt)){
							
							seqNumber ++;
							
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
								catch (IOException | GeneralSecurityException e1) {
									
									e1.printStackTrace();
								}
												
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
								
								// Check if the good exists on the application 
								if(checkGood(mensagemDecryt) == true){
									
									//check if the clientID is the owner of the good, and the good exists on the application
									if (checkGoodToSell(clientID, mensagemDecryt) == true){
										
										//put the good on the list of products to sell
								    	clientsGoodsToSell.put(clientID, mensagemDecryt);
								    	
								    	display("The good is now for sale.");
								    	

								    	try {
								    		
											writeMsg("Yes" + "\n");
											
										} catch (IOException | GeneralSecurityException  e) {
											
											e.printStackTrace();
										}
								    								
									}
									
									else{
										
										display("The client is not the owner of the good. ");
										
										try {
											
											writeMsg("No" + "\n");
											
											sendErrorMsg(clientID, "Your are not the owner of that good." + "\n");
											
										} catch (IOException | GeneralSecurityException  e) {
											
											e.printStackTrace();
										}
									}
									
								}
								
								else {
									
									display("The good was not found in the clients goods list. ");
									
									try {
										
										writeMsg("No" + "\n");
																
										sendErrorMsg(clientID, "The good was not found in the clients goods list." + "\n");
										
									} catch (IOException | GeneralSecurityException  e) {
										
										e.printStackTrace();
									}
								}
							
								break;
													
						
							case MessageHandler.STATEGOOD:
								
								display("The client " + clientID + " want to check the state of the following good: " + mensagemDecryt);
								
								int s= 0;
								
								int l = 0;
								
								// Check if the good exists on the application 
								if(checkGood(mensagemDecryt) == true){
									
									l = 1;
									
									for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
										
										String key = item.getKey();
									    String value = item.getValue();
									    							    									    
									    //Verify if the requested good is on sale 
									    if (value.equals(mensagemDecryt) && (s != 1)){
									    	
									    	s=1;
									    	
									    	display("The good is for sale.");
									    	
									    	try {
									    		
												writeMsg("Good: " + value + ", " + "Owner: " + key + "\n");
												
											} catch (IOException | GeneralSecurityException  e) {
											
												e.printStackTrace();
											}
									    
									    }						
									}
									
								}
								
								
								//This good is not for sale   
								if(s == 0 && l == 1){
									
									display("The good is not for sale. ");
									
									try {
										
										writeMsg("No" + "\n");
																
										sendErrorMsg(clientID, "The good is not for sale. " + "\n");
																			
									} catch (IOException | GeneralSecurityException  e) {
										
									
										e.printStackTrace();
									}
									
								}
								
								//The good does not exist on the application.
								else if(l == 0){
									
									display("The good does not exist on the application. ");
									
									try {
										
										writeMsg("No" + "\n");
																
										sendErrorMsg(clientID, "The good does not exist on the application. " + "\n");
																		
									} catch (IOException | GeneralSecurityException  e) {
										
									
										e.printStackTrace();
									}
									
								}
								
								break;
							
							case MessageHandler.BUYGOOD:
								
								int n=0;
								
								boolean cli;
								
								String[] w = (mensagemDecryt.toString()).split(" ",3);
														
								if (w.length == 2){
									
									try {
										
										// Check if the good exists on the application 
										if(checkGood(w[1]) == true){
											
											for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
												
												String key = item.getKey();
											    String value = item.getValue();
											    
											    //Verify if the requested good is for sale 
											    if (value.equals(w[1])){
											    	
											    	n=1;
											    	
											    	//The Buyer is the Seller
											    	if (key.equals(clientID)){
											    		
											    		display("A client can't buy his own goods. ");
											    		
											    		writeMsg("No" + "\n");
											    		
											    		sendErrorMsg(clientID, "You can't buy your own goods." + "\n");
														
											    	}
													
													else{
														
														cli = broadcast(clientID + ": " + mensagemDecryt);
														
														if(cli == false) {
															
															String msg = notif + "Sorry. No such user exists." + notif;
															
															writeMsg(msg);
														}
														
														else{
															
															//The operation was successful
															writeMsg("Yes" +"\n");
														}
														
													}
											    }						
											}
											
											//This good is not for sale   
											if(n == 0){
												
												display("The good is not for sale. ");
												
												writeMsg("No" + "\n");
												
												sendErrorMsg(clientID, "The good is not for sale." + "\n");
												
											}
											
										}
										
										else {
											
											display("The good does not exist on the application. ");
											
											writeMsg("No" + "\n");
																		
											sendErrorMsg(clientID, "The good does not exist on the application. " + "\n");
							
										}
																	
									} catch (IOException | GeneralSecurityException  e) {
									
										e.printStackTrace();
									}
									
								}
								
								
								else {
									
									try {
										
										sendErrorMsg(clientID, "Wrong Input. " + "\n");
										
									} catch (IOException | GeneralSecurityException e) {
									
										e.printStackTrace();
									}	
									
								}
								
								break;
								
							
							case MessageHandler.TRANSFERGOOD:
								
								String[] m = (mensagemDecryt.toString()).split(" ", 3);
														
								if (m.length == 2){
									
									//The goodID that will be transfer
									String good = m[0];
									
									//The BuyerID
									String buyer = m[1];
									
									int p = 0;
									int b = 0;
									
									// Check if the good exists on the application 
									if(checkGood(m[0]) == true){
									
										for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
											
											String key = item.getKey();
										    String value = item.getValue();
										    
										    //Verify if the requested good is for sale and if the client it's the owner of the good
										    if (key.equals(clientID) && value.equals(good)){
										    	
										    	b=1;
										    	
										    	//The Buyer is the Seller
										    	if(clientID.equals(buyer)){
										    	
										    		display("A client can't transfer his own goods to himself. ");
										    		
													try {
														
														writeMsg("No" + "\n");
														
														sendErrorMsg(clientID, "You can't transfer your own good to yourself. " + "\n");		
														
													} catch (IOException | GeneralSecurityException e) {
														
														e.printStackTrace();
													}
										    	}
										    	
										    	else{
										    		
										    		for(int y=clientsList.size(); --y>=0;){
														
														ClientThread ct1=clientsList.get(y);
														String check=ct1.getClientID();
														
														//Verify if the Buyer is a client on the list
														if (check.equals(buyer)){
															
															p=1;
															
															clientsGoodsList.put(value, buyer);
															
															clientsGoodsList.remove(value, key);
															
															clientsGoodsToSell.remove(key, value);	
															
															System.out.println("Lista de vendas: " + clientsGoodsToSell);
															
															System.out.println("Lista de goods: " + clientsGoodsList);
															
															display("The transfer was successful. ");
															
															//Tell the clients that the transfer was successful
															try {
																
																writeMsg("Yes" + "\n");
																												
																//inform the seller about the outcome of the transfer
													    		Boolean response1 = broadcast(clientID + ": " + "The transfer was successful. ");
													    		
													    		ct1.writeMsg("Yes" + "\n");
													    														
																//inform the buyer about the outcome of the transfer
													    		Boolean response2 = broadcast(ct1.getClientID() + ": " + "The transfer was successful. ");									    		
																
															} catch (IOException | GeneralSecurityException  e) {
																
																e.printStackTrace();
															}
															
														}
											    	}
										    		
										    		
										    		
										    	}
										    									
										    }						
										}
										
										// The ID of the buyer does not correspond to any of the ID clients of the Clients's List
										if(p == 0 && b == 1){
											
											display("The transfer was unsuccessful. ");
											
											try {
												
												writeMsg("No" + "\n");
												
												sendErrorMsg(clientID, "The ID of the buyer does not correspond to any of the ID clients of the Clients's List" + "\n");
												
											} catch (IOException | GeneralSecurityException  e) {
												
												e.printStackTrace();
											}
											
										}
										
										//The good is not for sale
										else if (b == 0){
											
											display("The good is not for sale. ");
											
											try {
												
												writeMsg("No" + "\n");
												
												sendErrorMsg(clientID, "The good is not for sale." + "\n");
																					
											} catch (IOException | GeneralSecurityException e) {
											
												e.printStackTrace();
											}
											
										}
										
									}
									
									else {
										
										display("The good does not exist on the application. ");
										
										try {
											
											writeMsg("No" + "\n");
											
											sendErrorMsg(clientID, "The good does not exist on the application. " + "\n");
											
										} catch (IOException | GeneralSecurityException e) {
										
											e.printStackTrace();
										}
									
									}					
								}
								
								else {
									
									try {
										
										sendErrorMsg(clientID, "Wrong Input. " + "\n");
										
									} catch (IOException | GeneralSecurityException e) {
									
										e.printStackTrace();
									}	
								}
								
								break;
							}
							
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
			}
			
			// if out of the loop then disconnected and remove from client list
			try {
				
				remove(id);
				
			} catch (IOException | GeneralSecurityException e) {
				
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
		private boolean writeMsg(String msg) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException   {
			
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
						
				close();
				return false;
			}
			
			// write the message to the stream
			try {
												
				msgEncrypt = null;
				
				String tempSeq = Integer.toString(seqNumber);
				
				LocalDateTime time = LocalDateTime.now();
												
				msgEncrypt = new MessageHandler(5, encryptMessage(msg), encryptMessage(tempSeq), time);
								
				sOutput.writeObject(msgEncrypt);
				
				seqNumber ++;
				
				socket.setSoTimeout(5000*100);  //set timeout to 500 seconds
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
	 * //=========== Decipher/decrypt the encrypted message using the public key of Client =================
	 * 
	 * decryptMessage method.
	 * 
	 * 		Deciphers the encrypted message received from the client, using his public Key.
	 * 		
	 * 		Takes byte array of the encrypted message as input.
	 *  
	*/
		
	private String decryptMessage(byte[] encryptedMessage, String id) {
		
		ServerDecryptCipher = null;
		
		try
	        {		
				PublicKey pK = readPublicKeyFromFile(id + "public.key");
			
	            ServerDecryptCipher = Cipher.getInstance("RSA");
	           	            
	            ServerDecryptCipher.init(Cipher.DECRYPT_MODE, pK);
	            
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
 		* //===========  Encrypted message using the private key of the Notary =================
 		*	 
 		* encryptMessage method
 		* 
 		* 		Takes the message string as input and encrypts the message.
 		* 
 		* 
	*/
	
	private byte[] encryptMessage(String s) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
		
		ServerEncryptCipher = null;
				
		byte[] cipherText = null;
				
		PrivateKey prK = getPrivateKey(notaryConnection);
		
		ServerEncryptCipher = Cipher.getInstance("RSA");  
				
		ServerEncryptCipher.init(Cipher.ENCRYPT_MODE, prK);
		
		cipherText = ServerEncryptCipher.doFinal(s.getBytes());
	
		return cipherText;
	   
	}
	
	
	
	
	/*
		* //===========  Get the private key of the Notary of the KeyStore =================
		*	 
		* getPrivateKey method
		* 
		* 		Takes the variable 'clientConnection' as input and retrieves the private key of the Notary.
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
		* //===========  Get the public key of the Notary of the serialize file =================
		*	 
		* readPublicKeyFromFile method
		* 
		* 		Takes the filename as input and retrieves the public key of the Notary of the serialize file.
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
}