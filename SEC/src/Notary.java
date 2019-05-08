import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import java.security.cert.X509Certificate;

import javax.crypto.*;
import java.util.concurrent.ConcurrentHashMap;


// the server that can be run as a console
public class Notary {

	/*
	 *  
	 *  All variables and objets used in the server side
	 *  
	*/	

	// to display time
	private SimpleDateFormat sdf;
	
	//time to expire message
	private static int expireTime;

	// a unique ID for each connection
	private static int connectionID;
				
	// the port number to listen for connection
	private int port;
	
	//max number of clients on the application
	private int maxNumberClients;
	
	// to check if server is running
	private boolean serverRunning;
	
	// id of the connection of the server socket
	private static String notaryConnection;
	
	// notification
	private String notif = " *** ";
		
	private Cipher ServerDecryptCipher;
	
	private Cipher ServerEncryptCipher;
	
	MessageHandler msgEncrypt;
	
	// message received from the clients
	private MessageHandler message;
	


	/*
	 *  
	 *  All hashmaps and lists used in the server side
	 *  
	*/
	
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> clientsList;
	
	// HashMap to keep the goods of each Client
	private ConcurrentHashMap <String, String> clientsGoodsList = new ConcurrentHashMap <String,String>();
	
	// HashMap to keep the goods to sell of each Client
	private HashMap<String, String> clientsGoodsToSell = new HashMap<String,String>();

	// save the transactions's history
	private Map<LocalDateTime, String[]> transactionsHistory = new HashMap<LocalDateTime, String[]>();    	
	
	// HashMap to keep the ports that will be used by each client in theirs privates connections
	private HashMap<String, Integer> portsList = new HashMap<String, Integer>();



				
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
		
		// example of max number of clients (to have a control on number of clients)
		maxNumberClients = 6;
		
		//create socket server and wait for connection requests 
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);
						
			notaryConnection = serverSocket.getInetAddress().getHostAddress().toString().replace("/","") + ":" + serverSocket.getLocalPort();
			
			RSA rsa = new RSA();
			
			KeyPair keys = rsa.createKeyPairs(notaryConnection);
			
			PublicKey pubKey = rsa.checkPublicKey(notaryConnection,keys);
			
			PrivateKey privKey = rsa.checkPrivateKey(keys);
			
			rsa.createCert(notaryConnection,pubKey,privKey);
			
			//10 seconds to message expire
			expireTime = 10;
			
			
			try {
				
				//check if there is any old information about the application
				File file = new File ("clientsGoodsList.ser");
				
				if (file.exists()){
					
					FileInputStream fileIn = new FileInputStream(file);
					ObjectInputStream in = new ObjectInputStream(fileIn);
					
					//if there is, recover that information to the server's list
					ConcurrentHashMap <String, String> clientsGoodsListTemp  = (ConcurrentHashMap) in.readObject();

					for (Map.Entry<String, String> item : clientsGoodsListTemp.entrySet()){

						//the good of the client
						String key = item.getKey();
									
						//Client name
					    String value = item.getValue();
					    
					    //Add the new client and his goods to the all goods List
					    clientsGoodsList.put(key, value);
					}

				}
		        
			} catch (ClassNotFoundException e1) {
				
				e1.printStackTrace();
			}
			
			
			// infinite loop to wait for connections ( till server is active )
			while(serverRunning) 
			{
		
				// accept connection if requested from client if the number of client its bellow max number
				if (clientsList.size() < maxNumberClients)  {
					
					display("Server waiting for Clients on port " + port + ".");
					
					Socket socket = serverSocket.accept();
					
					// break if server stopped
					if(!serverRunning)
						
						break;
					
					// if client is connected, create its thread
					ClientThread t = new ClientThread(socket);
					
					//add this client to arraylist
					clientsList.add(t);
														
					t.start();
				}
								
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
	private synchronized boolean broadcast(Integer typeMessage, String message) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
		
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
					if(!ct1.writeMsg(typeMessage, messageLf)) {
						
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
				if(!ct.writeMsg(typeMessage, messageLf)) {
					
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
		broadcast(5, notif + disconnectedClient + " has left the application. " + notif);
	}
	
	
	
	// to error message to a specific client
	private synchronized boolean sendErrorMsg(Integer typeMessage, String clientName, String errorMsg) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
			
		for(int t = clientsList.size(); --t >= 0;) {
				
			ClientThread cT1 = clientsList.get(t);
			
			if(clientName.equals(cT1.getClientID())){
				
				cT1.writeMsg(typeMessage, errorMsg);
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

					Integer realPort = socket.getPort() - message.getNumber();
					
					//number of the connection
					String idConnection = socket.getLocalAddress().getHostAddress().toString() + ":" + realPort;
												
					//message received 
					String mensagemDecryt = decryptMessage(message.getData(), idConnection);
					
					//sequence number of the message
					String seqDecryt = decryptMessage(message.getSeq(), idConnection);	
					
					//Get the time of the message received
					String timeReceived = decryptMessage(message.getLocalDate(), idConnection);
					
					//format that is gone be used
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					
					//convert to LocalDateTime type
					LocalDateTime localDateReceived = LocalDateTime.parse(timeReceived, formatter);
					
					//current time
					LocalDateTime tAtual = LocalDateTime.now();        
			        
					//do the difference between times
					long diff = ChronoUnit.SECONDS.between(localDateReceived, tAtual);
					
					//check if the message's time has expired 
					if (diff < expireTime ){
						
						//Verify if message has a right sequence number
						if (seqNumber <= Integer.parseInt(seqDecryt)){
							
							seqNumber = Integer.parseInt(seqDecryt) + 1;
							
							// different actions based on type message
							switch(message.getType()) {
								
							case MessageHandler.ENTER:
																
								//Convert the message received to a HashMap
								mensagemDecryt = mensagemDecryt.substring(1, mensagemDecryt.length()-1); //remove curly brackets
																
								String[] keyValuePairs = mensagemDecryt.split(",");              //split the string to create key-value pairs
										
								Map <String,String> map = new HashMap<>();               
			
								for(String pair : keyValuePairs)                        //iterate over the pairs
								{
									String[] entry = pair.split("=");                   //split the pairs to get key and value 
								    map.put(entry[0].trim(), entry[1].trim());          //add them to the hashmap and trim whitespaces
								}
																	
								// read the goods list of the client and his clientID
								HashMap<String, String> temporaryList = (HashMap<String, String>) map;
										
								Object nomeCliente = temporaryList.keySet().toArray()[0];
																					
								clientID = temporaryList.get(nomeCliente);
																										
								try {
									
									//broadcast(notif + clientID + " has joined the application " + notif);
									
									int tempPort = 1500 + clientsList.size();
									
									String nomeTemp = clientID;
									
									portsList.put(nomeTemp, tempPort);
																		
									updateClientsPortsTables(portsList.toString());
									
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
								
								FileOutputStream fos;
									
							    try {
							    	
							    									
							    	fos = new FileOutputStream("clientsGoodsList.ser");
							    	
							    	synchronized(fos){
							    		
							    		ObjectOutputStream oos = new ObjectOutputStream(fos);
										
										//save information of the application to file, in case of server crash
										oos.writeObject(clientsGoodsList);
										
										oos.close();
										fos.close();
							    	}
									
								    
								} catch (FileNotFoundException e) {
									
									e.printStackTrace();
									
								} catch (IOException e) {
									
									e.printStackTrace();
								}
							    														
								break;
							
							case MessageHandler.LOGOUT:
							
								display(clientID + " disconnected from the application.");
								
								serverRunning = false;
							
								break;
												
							case MessageHandler.SELL:

								// the good that the client wants to sell + the wts
								String[] msgReceivedSell = mensagemDecryt.split(" ");
							
								display("The client " + clientID + " want to sell the following good: " + msgReceivedSell[0]);
								
								// Check if the good exists on the application 
								if(checkGood(msgReceivedSell[0]) == true){
									
									//check if the clientID is the owner of the good, and the good exists on the application
									if (checkGoodToSell(clientID, msgReceivedSell[0]) == true){
										
										//put the good on the list of products to sell
								    	clientsGoodsToSell.put(clientID, msgReceivedSell[0]);
								    	
								    	display("The good is now for sale.");
								    	
								    	try {
								    		
								    		//All Conditions passed -> Return a ACK to the Client + the wts received
											writeMsg(message.getType(), "ACK" + " " + msgReceivedSell[1]);
											
										} catch (IOException | GeneralSecurityException  e) {
											
											e.printStackTrace();
										}
								    								
									}
									
									else{
										
										display("The client is not the owner of the good. ");
										
										try {
																						
											sendErrorMsg(message.getType(), clientID, "No. Your are not the owner of that good." + " " + msgReceivedSell[1]);
											
										} catch (IOException | GeneralSecurityException  e) {
											
											e.printStackTrace();
										}
									}
									
								}
								
								else {
									
									display("The good was not found in the clients goods list. ");
									
									try {
																										
										sendErrorMsg(message.getType(), clientID, "No. The good was not found in the clients goods list." + " " + msgReceivedSell[1]);
										
									} catch (IOException | GeneralSecurityException  e) {
										
										e.printStackTrace();
									}
								}
							
								break;
													
						
							case MessageHandler.STATEGOOD:
																
								int s= 0;
								
								int l = 0;

								// the good that the client wants to know its state + the id of the operation
								String[] msgReceivedState = mensagemDecryt.split(" ");

								display("The client " + clientID + " want to check the state of the following good: " + msgReceivedState[0]);
								
								// Check if the good exists on the application 
								if(checkGood(msgReceivedState[0]) == true){
									
									l = 1;
									
									for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
										
										String key = item.getKey();
									    String value = item.getValue();
									    							    									    
									    //Verify if the requested good is on sale 
									    if (value.equals(msgReceivedState[0]) && (s != 1)){
									    	
									    	s=1;
									    	
									    	display("The good is for sale.");
									    	
									    	try {
									    		
												writeMsg(message.getType(), "Good: " + value + ", " + "Owner: " + key +  " " + msgReceivedState[1]);
												
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
																										
										sendErrorMsg(message.getType(), clientID, "No. The good is not for sale." +  " " + msgReceivedState[1]);
																			
									} catch (IOException | GeneralSecurityException  e) {
										
										e.printStackTrace();
									}
								}
								
								//The good does not exist on the application.
								else if(l == 0){
									
									display("The good does not exist on the application. ");
									
									try {
																									
										sendErrorMsg(message.getType(), clientID, "The good doesn't exist on the application." + " " + msgReceivedState[1]);
																		
									} catch (IOException | GeneralSecurityException  e) {
										
									
										e.printStackTrace();
									}
								}
								
								break;
								
							
							case MessageHandler.TRANSFERGOOD:
								
								String[] m = (mensagemDecryt.toString()).split(" ", 3);
																						
								if (m.length == 2){
																																				
									//The BuyerID
									String buyer = m[0];
									
									//The goodID that will be transfer
									String good = m[1];
									
									int b = 0;
									
									int k = 0;
									
									// Check if the good exists on the application 
									if(checkGood(good) == true){
									
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
														
														sendErrorMsg(message.getType(), clientID, "No. You can't transfer your own good to yourself.");		
														
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
															
															k = 1;
															
															try {
																																
																clientsGoodsList.put(value, buyer);
																
																clientsGoodsList.remove(value, key);
																
																clientsGoodsToSell.remove(key, value);	
																																
																display("The transfer was successful. ");
																
																//inform the seller about the outcome of the transfer
													    		writeMsg(4, "Yes. The transfer was successful.");
													    													
																//inform the buyer about the outcome of the transfer
													    		ct1.writeMsg(4, "Yes. The transfer was successful.");		
																																
															    try {
																
															    	FileOutputStream fos1 = new FileOutputStream("clientsGoodsList.ser");

															    	synchronized(fos1){

																		ObjectOutputStream oos = new ObjectOutputStream(fos1);
																		
																		//save information of the application to file, in case of server crash
																		oos.writeObject(clientsGoodsList);
																		
																		oos.close();
																		fos1.close();
																	}

																	// save the information of the transaction
																	String[] transactionInformation = new String[]{ct1.getClientID(), clientID, good}; 

																	
																	// save this information in the 
																	transactionsHistory.put(tAtual, transactionInformation);

																	FileOutputStream fos2 = new FileOutputStream("transactionsHistory.ser");

															    	synchronized(fos2){

																		ObjectOutputStream oos1 = new ObjectOutputStream(fos2);
																		
																		//save information of the application to file, in case of server crash
																		oos1.writeObject(transactionsHistory);
																		
																		oos1.close();
																		fos2.close();
																	}

																    
																} catch (FileNotFoundException e) {
																	
																	e.printStackTrace();
																	
																} catch (IOException e) {
																	
																	e.printStackTrace();
																}
																
															} catch (IOException | GeneralSecurityException  e) {
																
																e.printStackTrace();
															}
															
														}
														
											    	}
										    		
										    		// the Buyer is not on the application
										    		if (k == 0){
										    			
										    			try {
															
															sendErrorMsg(message.getType(), clientID, "No. The Buyer is not on the application.");
																															
														} catch (IOException | GeneralSecurityException e) {
															
															e.printStackTrace();
														}
										    		}
										    	
										    	}
										    									
										    }						
										}
									
										//The good is not for sale
										if (b == 0){
											
											display("The good is not for sale. ");
											
											try {
																								
												sendErrorMsg(message.getType(), clientID, "No. The good is not for sale.");
																					
											} catch (IOException | GeneralSecurityException e) {
											
												e.printStackTrace();
											}
											
										}
										
									}
									
									else {
										
										display("The good does not exist on the application. ");
										
										try {
																						
											sendErrorMsg(message.getType(), clientID, "No. The good does not exist on the application. ");
											
										} catch (IOException | GeneralSecurityException e) {
										
											e.printStackTrace();
										}
									
									}					
								}
								
								else {
									
									try {
										
										sendErrorMsg(message.getType(), clientID, "No. Wrong Input. ");
										
									} catch (IOException | GeneralSecurityException e) {
									
										e.printStackTrace();
									}	
								}
								
								break;
							}
							
						}
						
						//Wrong sequence number received
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
		private boolean writeMsg(Integer typeMessage, String msg) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException   {
			
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
						
				close();
				return false;
			}
			
			// write the message to the stream
			try {
												
				msgEncrypt = null;
				
				//get the current sequence number
				String tempSeq = Integer.toString(seqNumber);
				
				//current time
				LocalDateTime timeCurrent = LocalDateTime.now();
				
				//format that is gone be used
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				
				//convert to string
				String time = timeCurrent.format(formatter);
				
				//secure the current message
				msgEncrypt = new MessageHandler(typeMessage, encryptMessage(msg,notaryConnection), encryptMessage(tempSeq,notaryConnection),  encryptMessage(time,notaryConnection), port, clientsList.size());
								
				//send the final message
				sOutput.writeObject(msgEncrypt);
				
				//increase the sequence number
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
		
		// send the portsList list to a client
		private boolean updateMsg(String msg) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException   {
			
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
						
				close();
				return false;
			}
			
			// write the message to the stream
			try {
												
				msgEncrypt = null;
				
				//get the current sequence number
				String tempSeq = Integer.toString(seqNumber);
				
				//current time
				LocalDateTime timeCurrent = LocalDateTime.now();
				
				//format that is gone be used
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				
				//convert to string
				String time = timeCurrent.format(formatter);
				
				//secure the current message
				msgEncrypt = new MessageHandler(6, encryptMessage(msg,notaryConnection), encryptMessage(tempSeq,notaryConnection),  encryptMessage(time,notaryConnection), port, clientsList.size());
								
				//send the final message
				sOutput.writeObject(msgEncrypt);
				
				//increase the sequence number
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
		
		
		// write a String to the Client output stream
		private boolean updateClientsPortsTables(String msg) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException   {
			
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
						
				close();
				return false;
			}
			
			try {
			
				for(int y=clientsList.size(); --y>=0;){
					
					ClientThread ct1=clientsList.get(y);
					
					// send the updated table of portsList to all clients
					ct1.updateMsg(msg);
					
				}
												
			}
			
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				
				display(notif + "Error sending message to " + clientID + notif);
				
				display(e.toString());
				
			} 
			
			return true;
		}
	}





	// =============================================================================================================================================================================

	/*
		*	 
		* 		Bellow are standing the methods related to the RSA keys (private and public keys) and to the decryption and encryption of messages exchange on the application 
		* 
	*/

	// =============================================================================================================================================================================

	
	
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
	public String decryptMessage(byte[] encryptedMessage, String id) {
		
		ServerDecryptCipher = null;
		
		try
	        {	
						
				PublicKey pK = readPublicKeyFromFile(id);
						
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
	public byte[] encryptMessage(String s, String nome) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
		
		ServerEncryptCipher = null;
				
		byte[] cipherText = null;
				
		PrivateKey prK = getPrivateKey(nome);
		
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
	PublicKey readPublicKeyFromFile(String id) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
		
		java.io.FileInputStream is = new java.io.FileInputStream("publicKeys");
		
	    KeyStore keystore = KeyStore.getInstance("JKS");
	    
	    keystore.load(is, "SECpass".toCharArray());
	    
	    String alias = id;
	    
	    X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
	    
	    PublicKey pubKey = cert.getPublicKey();
	    
	    return pubKey;
		
	}	
}