
import java.net.*;
import java.io.*;
import java.util.*;

import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.crypto.*;

// import Notary.ClientThread;


//The Client that can be run as a console
public class Client  {

	/*
	 *  
	 *  All variables and objets used in the client side
	 *  
	*/
	
	// notification
	private String notif = " *** ";
	
	// the server's and client's name	
	private String server, clientID;
	
	// port of server's socket
	private int port;

	// port used to originate the name used to create RSA keys
	private int firstPort;
		
	// id of the connection of the client socket
	private static String clientConnection;

	// socket to talk to server
	private Socket socket;

	// to read from the socket connected to Notary
	private ObjectInputStream sInput;

	// to write on the socket connected to Notary		
	private static ObjectOutputStream sOutput;	
	
	private Cipher cipher;
	
	// encrypted message received
	MessageHandler msgEncrypt;
	
	// message received from server
	MessageHandler message;
	
	// message received from other clients
	MessageHandler messageClient;

	// message sent to other clients
	MessageHandler msgToClient;
	
	//Sequence Number -> Guarantee freshness of messages
	private static int seqNumber;
	
	//time to expire message
	private static int expireTime;
	
	// max number of trys to connect to server
	private static int threshold;
	
	//real timestamp
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			
	//List of goods of the client
	private static HashMap<String, String> goodsList = new HashMap<String, String>();
	
	// socket to talk to other clients
	private ServerSocket clientSocket;
	
	// port to client to client connection
	private static int clientPort;
	
	// to check if client is running
	private boolean clientRunning;

	// total number of Servers in system
	private static int numberOfServers;

	//max number of faults that system tolerate
	private static int maxFaults;

	// number of faults counted 
	private static int countFaults;

	

	
	/*
	 *  
	 *  All hashmaps and lists used in the client side
	 *  
	*/

	// HashMap to keep the ports that will be used by each client in theirs privates connections
	private static HashMap<String, Integer> portsList = new HashMap<String, Integer>();

	// list of all sockets created to talk to all servers (Notary)
	private static ArrayList<Socket> socketsList = new ArrayList<Socket>();

	// list of all input's objects to send messages to all servers
	private static ArrayList<ObjectInputStream> objInputList = new ArrayList<ObjectInputStream>();

	// list of all output's objects to receive messages to all servers
	private static ArrayList<ObjectOutputStream> objOutputList = new ArrayList<ObjectOutputStream>();
	

	//To store all responses received by the servers -> allresponses: (message, number of votes of that message)
	private static Map<String, Integer>  serverResponses = new HashMap<String, Integer>(); 


	
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
		
		int retryCounter = 0;
		
		threshold = 100;
		
		socket = null; 
		
		clientRunning = true;

		Socket socketToClient;

		ObjectInputStream sInputTemp;		
		
		ObjectOutputStream sOutputToClient;

		numberOfServers = 3;

		// The system follows the following rule: N - f > (N + f)/2; N=Number of Servers, f=Number of faults tolerated
		maxFaults = numberOfServers / 3;

		while (retryCounter < threshold) {
			
			retryCounter ++ ;
			
			// try to connect to the server
			try {	

				for (int l = 1; l < (numberOfServers + 1); l ++){

					socket = null;

					// get the port that will be used in this socket
					Integer portCalculated = 1490 + 10*l;

					socket = new Socket(server, portCalculated);

					// add this socket (with that name and port) to list of sockets
					socketsList.add(socket);
			
					// we only need to make this one time
					if (l == 1){

						// its the unique id that is gone be used to create the RSA keys and Certificate
						clientConnection = socket.getLocalAddress().getHostAddress().toString().replace("/","") + ":" + socket.getLocalPort();

						firstPort = socket.getLocalPort();				
					}												
					
					//10 seconds to message expire
					expireTime = 10;
				
					String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
				
					display(msg);

					/* Creating both Data Stream */
					try {

						sInputTemp = null; 
						
						sInputTemp  = new ObjectInputStream(socket.getInputStream());

						// add this obj (related to the that socket) to the list
						objInputList.add(sInputTemp);

						sOutputToClient = null;
						
						sOutputToClient = new ObjectOutputStream(socket.getOutputStream());

						// add this obj (related to the that socket) to the list
						objOutputList.add(sOutputToClient);
					}
					
					catch (IOException eIO) {
						
						display("Exception creating new Input/output Streams: " + eIO);
						return false;
					}

				}
								
				// creates the Thread to listen from the server 
				new ListenFromServer().start();	
											
				// success we inform the caller that it worked
				return true;
							
			} 
			
			// exception handler if it failed
			catch(Exception ec) {
				
				display("Error connectiong to server:" + ec);
				
				display("I will keep trying.");
			}	
		}
		
		if (socket == null){
			
			display("Failed to connect to server");
			
			return false;
		}
		
		return false;
		
		
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
		
	
	
	/*
	 * To send a message to the console
	*/
	private void display(String msg) {

		System.out.println(msg);
		
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
		
		//generate private and public keys
		KeyPair keys = rsa.createKeyPairs(clientConnection);
						
		//get the client's public key from the file
		PublicKey pubKey = rsa.checkPublicKey(clientConnection, keys);
		
		//get the client's private key 
		PrivateKey privKey = rsa.checkPrivateKey(keys);
		
		//get the certificate
		X509Certificate cert = rsa.createCert(clientConnection,pubKey,privKey);
		
		//sequence number is initialized
		seqNumber = 0;

		// number of faults counted is initialized
		countFaults = 0;
		
		//format that is gone be used
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				
		System.out.println("Type 'ENTER' to enter in the application");
		
		// infinite loop to get the input from the user
		while(true) {
												
			System.out.print("> ");
			
			// read message from user
			String msg = scan.nextLine();
			
			//is the first message between client and server
			if (i == 0){
				
				if (msg.equalsIgnoreCase("ENTER")){
					
					
					setGoodsClient(clientID + "Maca", clientID);
					setGoodsClient(clientID + "Banana", clientID);
					setGoodsClient(clientID + "Kiwi", clientID);
						
					//the message that is gone be sent
					String temp =getGoodsClient().toString();
					
					//sequence number that is gone be sent in message
					String tempSeq = Integer.toString(seqNumber);
					
					//get the current time
					LocalDateTime dateTime = LocalDateTime.now();
					
					//convert to string
			        String time = dateTime.format(formatter);
			        	
			        //send message
					client.sendMessage(new MessageHandler(MessageHandler.ENTER, temp.getBytes(), tempSeq.getBytes(), time.getBytes(), clientPort, 0));
					
					i=1;
					
				}
				
				else{
					
					System.out.println("Wrong input! 1. Type 'ENTER' to enter in the application");
					
				}
				
			}
			
			
			else {
				
				String tempSeq = Integer.toString(seqNumber);
				
				// logout if message is LOGOUT
				if(msg.equalsIgnoreCase("LOGOUT")) {
					
					LocalDateTime dateTime = LocalDateTime.now();
					
			        String time = dateTime.format(formatter);
					
					String temp = "";
					
					byte[] tempBytes = temp.getBytes();
													
					client.sendMessage(new MessageHandler(MessageHandler.LOGOUT, tempBytes, tempSeq.getBytes(), time.getBytes(), clientPort, 0));
					
					break;
				}
									
				// message to inform server that client want to sell some good
				else if(msg.equalsIgnoreCase("SELL")) {
					
					System.out.println("Write the good you want to sell: ");
					
					String msgGoodToServer = scan.nextLine();
					
					msgGoodToServer=intentionToSell(msgGoodToServer);
							
					byte[] tempBytes = msgGoodToServer.getBytes();	
					
					LocalDateTime dateTime = LocalDateTime.now();
					
			        String time = dateTime.format(formatter);
									
					client.sendMessage(new MessageHandler(MessageHandler.SELL, tempBytes, tempSeq.getBytes(), time.getBytes(), clientPort, 0));	
							
				}
				
				// message to the server to get the state of some good
				else if(msg.equalsIgnoreCase("STATEGOOD")) {
								
					System.out.println("Write the product of which the state you want to check: ");
								
					String msgGoodStateToServer = scan.nextLine();
								
					msgGoodStateToServer=getStateOfGood(msgGoodStateToServer);
					
					byte[] tempBytes =msgGoodStateToServer.getBytes();
					
					LocalDateTime dateTime = LocalDateTime.now();
					
			        String time = dateTime.format(formatter);
																				
					client.sendMessage(new MessageHandler(MessageHandler.STATEGOOD, tempBytes, tempSeq.getBytes(), time.getBytes(), clientPort, 0));	
																							
				}
				
				// message to the server to buy some good
				else if(msg.equalsIgnoreCase("BUYGOOD")) {
					
					System.out.println("Write Product Owner <space>" + " the goodID that you want to buy from him: ");
					
					String msgGoodToBuy = scan.nextLine();
					
					msgGoodToBuy = buyGood(msgGoodToBuy);
					
					LocalDateTime dateTime = LocalDateTime.now();
					
			        String time = dateTime.format(formatter);
												        
			        String[] w = msgGoodToBuy.split(" ",3);
			     		
			        // the good that will be sold
			        String tempGood = w[1];
			        
			        // final msg that will be sent
			        String temp = clientID + " " + tempGood;
			        
			        byte[] tempBytes = temp.getBytes();
			        			        			        
			        for (Map.Entry<String, Integer> item : portsList.entrySet()) {
			        			
			        	// get the port of the socket of the seller
			        	if (item.getKey().equals(w[0])){
			        		
			        		// port of the seller
			        		Integer tempPort = item.getValue();
			        					        					        					        		
			        		client.sendMessageToClients(new MessageHandler(MessageHandler.BUYGOOD, tempBytes, tempSeq.getBytes(), time.getBytes(), tempPort, 0));
			        	}
			        	
			        }																					
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
			
			int contador = 0;

			while(true) {
				
				try {

					// get the messages received from all servers
					for (int v = 0; v < objInputList.size(); v ++){

						ObjectInputStream objTemp = objInputList.get(v);

						message = null;

						// message received by the server
						message = (MessageHandler) objTemp.readObject();

						//Get the sequence number of the message received
						String seqDecryt = decryptMessage(message.getSeq(), message.getPort());

						// its the first message received from the server
						if( contador == 0){

							// check if the message has a right sequence number
							if (seqNumber <= Integer.parseInt(seqDecryt)){
								
								contador ++;
								
								// port that will be used by this client to create a socket to receive messagem from other clients 
								clientPort = port + message.getNumber();

								// we only need to make one time
								if (v == 0){

									// creates the Thread to listen from the clients
									new ListenFromClients().start();

								}
																
								System.out.println("\nHello.! Welcome to HDS Notary Application");
								System.out.println("1. Type 'SELL' to inform the Notary that you want to sell some good");
								System.out.println("2. Type 'STATEGOOD' to see if some specific good is available for sell");
								System.out.println("3. Type 'BUYGOOD' to buy a good");
								System.out.println("4. Type 'LOGOUT' to logoff from application");
								
							}
							
							else {
								
								display("The Message hasn't the right sequence number. Won't accept it");
							}
							
					
						}
					
						// update the client's portsList list
						if (message.getType() == 6){
													
							//get the message
							String msgDecrypt = decryptMessage(message.getData(), message.getPort());
							
							//Convert the message received to a HashMap
							msgDecrypt = msgDecrypt.substring(1, msgDecrypt.length()-1); //remove curly brackets
															
							String[] keyValuePairs = msgDecrypt.split(",");              //split the string to create key-value pairs
									
							Map <String,String> map = new HashMap<>();               
		
							for(String pair : keyValuePairs)                        //iterate over the pairs
							{
								String[] entry = pair.split("=");                   //split the pairs to get key and value 
							    map.put(entry[0].trim(), entry[1].trim());          //add them to the hashmap and trim whitespaces
							}
																
							// read all clientIDs and theirs private ports numbers
							HashMap<String, String> temporaryList = (HashMap<String, String>) map;
									
							Object nomeCliente = temporaryList.keySet().toArray()[0];
																				
							String nomeTemp = temporaryList.get(nomeCliente);
							
							// reset the portsList
							portsList.clear();
							
							//update the portsList
							for (Map.Entry<String, String> item : temporaryList.entrySet()) {
								
								//the good of the client
								String key = item.getKey();
								
								//Client name
							    String value = item.getValue();
							    
							    //Add the client name and his private port connection
							    portsList.put(key, Integer.parseInt(value));
							    									
							}

							if (v == (objInputList.size()-1)){

								// basta aumentar uma vez o número sequencial, dado que a mesma mensagem vai ser recebida de n servers
								seqNumber = Integer.parseInt(seqDecryt) + 1 ;

							}

						}
					
						else  {

							//get the message
							String msgDecrypt = decryptMessage(message.getData(), message.getPort());
							
							//Get the time of the message received
							String timeReceived = decryptMessage(message.getLocalDate(), message.getPort());
														
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
							
							//convert to LocalDateTime type
							LocalDateTime localDateReceived = LocalDateTime.parse(timeReceived, formatter);
							
							//current time
							LocalDateTime tAtual = LocalDateTime.now();
											
							long diff = ChronoUnit.SECONDS.between(localDateReceived, tAtual);
												
							//check if the message's time has expired 
							if (diff < expireTime ){
								
								// check if the message has a right sequence number
								if (seqNumber <= Integer.parseInt(seqDecryt)){


									if (v == (objInputList.size()-1)){

										// basta aumentar uma vez o número sequencial, dado que a mesma mensagem vai ser enviada para os n servers
										seqNumber = Integer.parseInt(seqDecryt) + 1 ;

									}

									// process message received from the server
									String result = processResponses(message.getType(), msgDecrypt);

									// we have a final result
									if (result != null) {

										display(result);

										System.out.print("> ");

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
																		
				}
				
				catch(IOException e) {
					
					display(notif + "Can´t connect to server. Connection was closed. " + e + notif);
					
					break;
										
				}
				
				catch(ClassNotFoundException e2) {
				}
			}
		}
	}
	
	/*
	 * a class that waits for the message from the other clients
	*/
	class ListenFromClients extends Thread {

		public void run() {		
			
			try {

				// the socket used by the server
				clientSocket = new ServerSocket(clientPort);

				// infinite loop to wait for connections ( till this client is active )
				while(clientRunning) 
				{
			
					display("Client waiting for other clients on port " + clientPort + ".");			
					
					Socket clientToClientSocket = clientSocket.accept();
					
					// break if client stopped
					if(!clientRunning)
						
						break;
					
					// if client is connected, create its thread
					ClientThread t = new ClientThread(clientToClientSocket);
														
					t.start();
										
				}
				
				// try to stop the client
				try {
									
					clientSocket.close();
				
				}
				catch(Exception e) {
					
					display("Exception in closing. " + e);
				}
				
			} catch (IOException e) {
				
				e.printStackTrace();
				
			} catch (NoSuchAlgorithmException e) {
				
				e.printStackTrace();
				
			} catch (GeneralSecurityException e) {
				
				e.printStackTrace();
			}
			
		}
	}
	
	
	
	// One instance of this thread will run for each client connected to this client
	class ClientThread extends Thread {
		
		// the socket to get messages from client
		Socket clientToClientSocket ;
		
		ObjectInputStream sInputClient;
		ObjectOutputStream sOutputClient;
		
			
		// the clientID of the Client
		String clientID;
				
	
		// Constructor
		ClientThread(Socket clientToClientSocket) throws NoSuchAlgorithmException,  IOException, GeneralSecurityException {
						
			this.clientToClientSocket = clientToClientSocket;
					
			//Creating both Data Stream
			System.out.println("Thread trying to create Object Input/Output Streams");
			
			try
			{
				
				sOutputClient = new ObjectOutputStream(clientToClientSocket.getOutputStream());
				sInputClient  = new ObjectInputStream(clientToClientSocket.getInputStream());								
				
			}
			catch (IOException e) {
				
				display("Exception creating new Input/output Streams: " + e);
				
				return;
			}
			
		}
		
			
		// infinite loop to read and forward message
		public void run() {
			
			// to loop until LOGOUT
			boolean clientRunning = true;
			
			
			while(clientRunning) {
								
				// it will be always a buygood from other client
				try {
					
					// message received from the client
					messageClient = (MessageHandler) sInputClient.readObject(); 
					
					// get the id connection of that the client that sent the message							
					String idClientReceived= clientToClientSocket.getLocalAddress().getHostAddress().toString() + ":" + messageClient.getPort();
					
					//get the message received from the client
					String msgDecryptOfClient = decryptMessageOfClients(messageClient.getData(), idClientReceived);
										
					LocalDateTime dateTime = LocalDateTime.now();
					
					DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					
			        String time = dateTime.format(formatter1);
			        
			        String tempSeq = Integer.toString(seqNumber);

					try {
												
						sendMessage(new MessageHandler(MessageHandler.TRANSFERGOOD, msgDecryptOfClient.getBytes(), tempSeq.getBytes(), time.getBytes(), messageClient.getPort(), 0));
						
					} catch (UnrecoverableKeyException | KeyStoreException | CertificateException e) {
						
						e.printStackTrace();
					}
										
				}
				
				catch (IOException e) {
					
					break;				
				}
				
				catch(ClassNotFoundException e2) {
					break;
				}
						
				close();
			}
		
		}
		
		// close everything
		private void close() {
								
			try {
				if(sOutputClient != null) sOutputClient.close();
			}
			catch(Exception e) {}
			
			try {
				if(sInputClient != null) sInputClient.close();
			}
			catch(Exception e) {};
			
			try {
				
				if(clientToClientSocket != null) clientToClientSocket.close();
			}
			catch (Exception e) {}
		}
		
	}
	
	
	
	
	/*
	 * To send a message to the Notary
	*/
	void sendMessage(MessageHandler msg) throws UnrecoverableKeyException, KeyStoreException, CertificateException {
		
		try {

			Socket socketToServer;
		
			ObjectOutputStream sOutputToServer;	

			for (int p = 0; p < objOutputList.size(); p ++){

				socketToServer = null;

				// get the socket connected to that server from the socket's list
				socketToServer = socketsList.get(p);

				sOutputToServer = null;

				// get the output object connected to the socket connected to that server from the list
				sOutputToServer = objOutputList.get(p);				

				msgEncrypt = null;
									
				//Client will send a normal message encrypted				
				msgEncrypt = new MessageHandler(msg.getType(), encryptMessage(new String(msg.getData()), clientConnection), encryptMessage(new String(msg.getSeq()),clientConnection),  encryptMessage(new String(msg.getLocalDate()), clientConnection), clientPort, p);
						
				// send the final message
				sOutputToServer.writeObject(msgEncrypt);
				
				//convert to string
				String count =  new String(msg.getSeq());
					
				// basta aumentar uma vez o número sequencial, dado que a mesma mensagem vai ser enviada para os n servers
				if (p == 0){

					//increase the sequence number
					seqNumber = Integer.parseInt(count) + 1;

				} 
							
				socketToServer.setSoTimeout(5000*100);  //set timeout to 500 seconds

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
	 * To send a message to other Clients: only used in goods transfers
	*/
	void sendMessageToClients(MessageHandler msg) throws UnrecoverableKeyException, KeyStoreException, CertificateException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		//socket used to send message to other Client
		Socket socketToClient;
		
		ObjectOutputStream sOutputToClient;			
		
		int port = msg.getPort();
		
		String clientAddress = "localhost";
		
		try {

			socketToClient = null;
			
			// connect to seller's socket
			socketToClient = new Socket (clientAddress, port);	
			
			sOutputToClient = new ObjectOutputStream(socketToClient.getOutputStream());

			msgToClient = new MessageHandler(msg.getType(), encryptMessage(new String(msg.getData()), clientConnection), encryptMessage(new String(msg.getSeq()), clientConnection),  encryptMessage(new String(msg.getLocalDate()), clientConnection), firstPort, 0);
					
			sOutputToClient.writeObject(msgToClient);
			
			socketToClient.setSoTimeout(5000*100);  //set timeout to 500 seconds
			
			sOutputToClient.close();
										
		}
		
				
		catch(IOException e) {
			
			display("Exception writing to other client: " + e);
		}
	}



	/*
		* //===========  Process messages received by all servers =================
		*	 
		* processResponses method
		* 
		* 		Takes the server's port, the message's type, the message string, and the timestamp as input and implements a specific couting vote.
		*
		*		Returns the final response that will be shown to the client 
		* 
	*/
	public String processResponses(int typeMessage, String messageReceived){

			String finalResponse = null;

			// its a response of a SELL message
			if (typeMessage == 1){

				// Possible message received by the server: the good is now for sale
				if(messageReceived.equals("Yes")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);
				        	}
			        	
			        	}

					}
				}

				// Other possible message received by the server -> the operation of selling the good was not successful
				else if(messageReceived.equals("No. Your are not the owner of that good.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);
				        	}
			        	
			        	}

					}
				}

				// Other possible message received by the server -> the operation of selling the good was not successful
				else if(messageReceived.equals("No. The good was not found in the clients goods list.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);

				        	}
			        	
			        	}

					}
				}


			}

			// its a response of a STATEGOOD message
			else if (typeMessage == 2){

				// possible message received by the server -> the good is not for sale
				if(messageReceived.equals("No. The good is not for sale.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);
				        	}
			        	
			        	}

					}
				}

				// Other possible message received by the server -> the good does not exist on the application
				else if(messageReceived.equals("No. The good does not exist on the application.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);

				        	}
			        	
			        	}

					}
				}


				// the good is now for sale
				else {

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);
				        	}
			        	
			        	}

					}
				}


			}

			// its a response of a TRANSFERGOOD message
			else if (typeMessage == 4){

				// the transfer was successfull
				if(messageReceived.equals("Yes. The transfer was successful.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);
				        	}
			        	
			        	}

					}
				}

				// Other possible message received by the server -> the operation of transfer the good was not successful
				else if(messageReceived.equals("No. You can't transfer your own good to yourself.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);
				        	}
			        	
			        	}

					}
				}

				// Other possible message received by the server -> the buyer ID does not exist
				else if(messageReceived.equals("No. The Buyer is not on the application.")){

					// the list is empty
					if (serverResponses.size() == 0){

						serverResponses.put(messageReceived, 1);

					}

					else {

						for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {
			        			
				        	// check if this message was already in list
				        	if (item.getKey().equals(messageReceived)){
				        		
				        		// if so, increase the number of votes
				        		serverResponses.put(messageReceived, item.getValue() + 1);
				        					        					        					        		
				        	}

				        	// the list is not empty -> there was at least one fault
				        	else {

				        		countFaults ++;

				        		// if not, add it to the list with one vote
				        		serverResponses.put(messageReceived, 1);

				        	}
			        	
			        	}

					}
				}

			}

			// message's type its incorrect
			else {

				display("The Message has a invalid type. ");
			}


			// check if we have already a final response
			for (Map.Entry<String, Integer> item : serverResponses.entrySet()) {


				if(item.getValue() >= (socketsList.size() - maxFaults)){

					finalResponse = item.getKey();

					serverResponses.clear();

					countFaults = 0;
				}

			}

		return finalResponse;		

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
	public byte[] encryptMessage(String s, String nome) throws NoSuchAlgorithmException, NoSuchPaddingException, 
						InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
										BadPaddingException, IOException, UnrecoverableKeyException, KeyStoreException, CertificateException{
		
		PrivateKey prK = getPrivateKey(nome);
		
		cipher = null;
	
		byte[] cipherText = null;
	
		cipher = Cipher.getInstance("RSA");
			
		cipher.init(Cipher.ENCRYPT_MODE, prK);
	
		cipherText = cipher.doFinal(s.getBytes());
							   
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
	public String decryptMessage (byte[] encryptedMessage, Integer portServer) {
		
	        cipher = null;
	        
	        try {
	        	
	    		//String idConnection = socket.getLocalAddress().getHostAddress().toString() + ":" + socket.getPort();
	    		
	        	String idConnection = "0.0.0.0" + ":" + portServer;
	    			        	
	        	PublicKey pK = readPublicKeyFromFile(idConnection);
	        		            
	        	cipher = Cipher.getInstance("RSA");
	        		            	        	
	        	cipher.init(Cipher.DECRYPT_MODE, pK);
	             
	        	byte[] msg = cipher.doFinal(encryptedMessage);
	        		        	
	        	return new String(msg);
	             
	        }
	        
	        catch(Exception e) {
	        	
	        	e.getCause();
	        
	        	e.printStackTrace();
	        	
	        	System.out.println ( "Exception genereated in decryptData method. Exception Name  :"  + e.getMessage() );
	          }
	        
	        return null;
	 }
	
	
	/*
	 * //=========== Decipher the encrypted message using the public key of the Client that sent the message =================
	 * 
	 * decryptMessage method.
	 * 
	 * 		Deciphers the encrypted message received from the Client using his public Key.
	 * 		
	 * 		Takes byte array of the encrypted message as input.
	 *  
	*/
	public String decryptMessageOfClients (byte[] encryptedMessage, String id) {
		
	        cipher = null;
	        
	        try {
	    			        		    			        	
	        	PublicKey pK = readPublicKeyFromFile(id);
	        		        		        		        		            
	        	cipher = Cipher.getInstance("RSA");
	        		            	        	
	        	cipher.init(Cipher.DECRYPT_MODE, pK);
	        		             
	        	byte[] msg = cipher.doFinal(encryptedMessage);
	        		        	
	        	return new String(msg);
	             
	        }
	        
	        catch(Exception e) {
	        	
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