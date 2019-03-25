
import java.net.*;
import java.io.*;
import java.util.*;


//The Client that can be run as a console
public class Client  {
	
	// notification
	private String notif = " *** ";

	// for I/O
	private ObjectInputStream sInput;		// to read from the socket
	private ObjectOutputStream sOutput;		// to write on the socket
	private Socket socket;					
	
	private String server, clientID, good;	
	private int port;	
	
	//List of goods of the client
	private static HashMap<String, String> goodsList = new HashMap<String, String>();
	
	
	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	
	
	public void setGoodsClient(String good, String clientID) {
		goodsList.put(good, clientID);
	}
	
	public HashMap<String, String> getGoodsClient() {
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
		try
		{
			
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		
		catch (IOException eIO) {
			
			display("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
		
		// Send our clientID to the server this is the only message that we
		// will send as a String. All other messages will be messageHandler objects
		try
		{
			
			setGoodsClient(clientID + "Maça", clientID);
			setGoodsClient(clientID + "Banana", clientID);
			setGoodsClient(clientID + "Kiwi", clientID);
									
			sOutput.writeObject(getGoodsClient());
		}
		catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		
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
			sOutput.writeObject(msg);
		}
		
		catch(IOException e) {
			display("Exception writing to server: " + e);
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
	public static void main(String[] args) {
		
		// default values if not entered
		int portNumber = 1500;
		String serverAddress = "localhost";
		String clientID = "Anonymous";
		Scanner scan = new Scanner(System.in);
		
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
		
		System.out.println("\nHello.! Welcome to HDS Notary Application");
		System.out.println("1. Type the message to send broadcast to all active clients");
		System.out.println("2. Type '@clientID<space>yourmessage' to send message to desired client");
		System.out.println("3. Type 'SELL' to inform the server that you want to sell some good");
		System.out.println("4. Type 'STATEGOOD' to see if some specific good is available for sell");
		System.out.println("5. Type 'BUYGOOD' to buy a good");
		System.out.println("6. Type 'LOGOUT' to logoff from server");
		
		
		// infinite loop to get the input from the user
		while(true) {
			System.out.print("> ");
			
			// read message from user
			String msg = scan.nextLine();
						
			// logout if message is LOGOUT
			if(msg.equalsIgnoreCase("LOGOUT")) {
				client.sendMessage(new MessageHandler(MessageHandler.LOGOUT, ""));
				break;
			}
						
			// message to inform server that client want to sell some good
			else if(msg.equalsIgnoreCase("SELL")) {
				
				System.out.println("Write the good you want to sell: ");
				
				String msgGoodToServer = scan.nextLine();
				
				msgGoodToServer=intentionToSell(msgGoodToServer);
				
				//The good was found in the good's list
				if(msgGoodToServer != null){
					
					client.sendMessage(new MessageHandler(MessageHandler.SELL, msgGoodToServer));	
					
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
														
				client.sendMessage(new MessageHandler(MessageHandler.STATEGOOD, msgGoodStateToServer));	
																						
			}
			
			// message to the server to buy some good
			else if(msg.equalsIgnoreCase("BUYGOOD")) {
				
				System.out.println("Write @Product Owner <space>" + " the goodID that you want to buy from him: ");
				
				String msgGoodToBuy = scan.nextLine();
				
				msgGoodToBuy = buyGood(msgGoodToBuy);
				
				client.sendMessage(new MessageHandler(MessageHandler.BUYGOOD, msgGoodToBuy));
																									
			}
			
			// message to the server to transfer some good
			else if(msg.equalsIgnoreCase("TRANSFERGOOD")) {
				
				System.out.println("Write the goodID that will be transfer <space>" + " buyer ID: ");

				String msgTransfer = scan.nextLine();
								
				msgTransfer= transferGood(msgTransfer);
				
				client.sendMessage(new MessageHandler(MessageHandler.TRANSFERGOOD, msgTransfer));	
																												
			}
			
			// regular text message
			else {
				
				
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
					String msg = (String) sInput.readObject();
					
					// print the message
					System.out.println(msg);
					
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