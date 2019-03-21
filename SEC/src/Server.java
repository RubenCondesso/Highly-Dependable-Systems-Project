import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

// the server that can be run as a console
public class Server {

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
	
	// notification
	private String notif = " *** ";
			
	//constructor that receive the port to listen to for connection as parameter
	
	public Server(int port) {
		
		// the port
		this.port = port;
		
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		
		// an ArrayList to keep the list of the clients
		clientsList = new ArrayList<ClientThread>();
	}
			
	public void start() {
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
				for(int i = 0; i < clientsList.size(); ++i) {
					ClientThread tc = clientsList.get(i);
					
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
	private synchronized boolean broadcast(String message) {
		
		// add timestamp to the message
		String time = sdf.format(new Date());
		
		// to check if message is private i.e. client to client message
		String[] w = message.split(" ",3);
		
		boolean isPrivate = false;
		
		if(w[1].charAt(0)=='@') 
			isPrivate=true;
		
		
		// if private message, send message to mentioned clientID only
		if(isPrivate==true)
		{
			String tocheck=w[1].substring(1, w[1].length());
			
			message=w[0]+w[2];
			String messageLf = time + " " + message + "\n";
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
			for(int i = clientsList.size(); --i >= 0;) {
				
				ClientThread ct = clientsList.get(i);
				
				// try to write to the Client if it fails remove it from the list
				if(!ct.writeMsg(messageLf)) {
					
					clientsList.remove(i);
					display("Disconnected Client " + ct.clientID + " removed from list.");
				}
			}
		}
		return true;
		
		
	}

	// if client sent LOGOUT message to exit
	synchronized void remove(int id) {
		
		String disconnectedClient = "";
		
		// scan the array list until we found the Id
		for(int i = 0; i < clientsList.size(); ++i) {
			
			ClientThread ct = clientsList.get(i);
			
			// if found remove it
			if(ct.id == id) {
				
				disconnectedClient = ct.getClientID();
				clientsList.remove(i);
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
	public static void main(String[] args) {
		
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
		Server server = new Server(portNumber);
		
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
		ClientThread(Socket socket) {
			
			// a unique id
			id = ++connectionID;
			this.socket = socket;
					
			//Creating both Data Stream
			System.out.println("Thread trying to create Object Input/Output Streams");
			
			try
			{
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
								
				// read the goods list of the client and his clientID
			
				HashMap<String, String> temporaryList = (HashMap<String, String>) sInput.readObject();
				
				Object nomeCliente = temporaryList.keySet().toArray()[0];
								
				//clientID = (String) sInput.readObject();
				
				clientID= temporaryList .get(nomeCliente);
								
				broadcast(notif + clientID + " has joined the application " + notif);
				
				for (Map.Entry<String, String> item : temporaryList.entrySet()) {
					
					//the good of the client
					String key = item.getKey();
					
					//Client name
				    String value = item.getValue();
				    
				    //Add the new client and his goods to the all goods List
				    clientsGoodsList.put(key, value);
				    
				}
				
			}
			catch (IOException e) {
				
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			
			catch (ClassNotFoundException e) {
			}
			
            date = new Date().toString() + "\n";
		}
		
		public String getClientID() {
			return clientID;
		}

		public void setClientID(String clientID) {
			this.clientID = clientID;
		}
		
		
		// infinite loop to read and forward message
		public void run() {
			
			// to loop until LOGOUT
			boolean serverRunning = true;
			while(serverRunning) {
				
				// read a String (which is an object)
				try {
					
					msghandler = (MessageHandler) sInput.readObject();
				}
				
				catch (IOException e) {
					display(clientID + " Exception reading Streams: " + e);
					break;				
				}
				
				catch(ClassNotFoundException e2) {
					break;
				}
				
				// get the message from the MessageHandler object received
				String message = msghandler.getMessage();

				// different actions based on type message
				switch(msghandler.getType()) {

				case MessageHandler.MESSAGE:
					
					boolean confirmation =  broadcast(clientID + ": " + message);
					
					if(confirmation==false){
						
						String msg = notif + "Sorry. No such user exists." + notif;
						writeMsg(msg);
					}
					
					break;
					
				case MessageHandler.LOGOUT:
					
					display(clientID + " disconnected with a LOGOUT message.");
					serverRunning = false;
					
					break;
										
				case MessageHandler.SELL:
					
					display("The client " + clientID + " want to sell the following good: " + message);
										
					int c=0;
										
					for (Map.Entry<String, String> item : clientsGoodsList.entrySet()){
						
						String key = item.getKey();
					    String value = item.getValue();
					    
					    // Check if the client it is the owner of the good
					    if (value.equals(clientID) && key.equals(message)){
					    	
					    	c=1;
					    	
					    	//put the good on the list of produts to sell
					    	clientsGoodsToSell.put(key, value);
					    	
					    	display("The good you asked is for sale now. ");
					    	writeMsg("Yes" + "\n");
					 
					    }
					}
					//The ClientID and/or his good was not found in the clients goods list    
					if(c == 0){
						
						display("The ClientID and/or his good was not found in the clients goods list.  ");
						writeMsg("No" + "\n");
						
					}
					
					break;
											
				
				case MessageHandler.STATEGOOD:
					
					display("The client " + clientID + " want to check the state of the following good: " + message);
					
					int s=0;
					
					for (Map.Entry<String, String> item : clientsGoodsToSell.entrySet()){
						
						String key = item.getKey();
					    String value = item.getValue();
					    
					    //Verify if the requested good is on sale 
					    if (key.equals(message)){
					    	
					    	s=1;
					    	
					    	display("The good you asked is for sale. ");
					    	writeMsg(key + "," + value + "\n");
					    
					    }						
					}
					
					//This good is not for sale   
					if(s == 0){
						
						display("The good you asked is not for sale or does not exist. ");
						writeMsg("No" + "\n");
						
					}
					
					break;
					
				case MessageHandler.BUYGOOD:
					boolean cli =  broadcast(clientID + " wants to buy something from you");
					
					if(cli==false){
						
						String msg = notif + "Sorry. No such user exists." + notif;
						writeMsg(msg);
					}
					
					break;
					
					
				}
			}
			
			// if out of the loop then disconnected and remove from client list
			remove(id);
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
		private boolean writeMsg(String msg) {
			
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				
				close();
				return false;
			}
			
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				
				display(notif + "Error sending message to " + clientID + notif);
				display(e.toString());
			}
			return true;
		}
	}
}