/*
 * ChildThread.java
 */


import java.io.*;
import java.net.Socket;
import java.util.Vector;
import java.util.*;

public class ChildThread extends Thread
{
  static  Vector<ChildThread> handlers = new Vector<ChildThread>(20);
  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;

	BufferedReader userInfoBuff = null, messageBuff = null;
	LinkedList<String> userList = new LinkedList();
	LinkedList<String> msgList = new LinkedList();
	// define commands String
	final String MSGGET = "MSGGET";
	final String MSGSTORE = "MSGSTORE";
	final String LOGIN = "LOGIN";
	final String LOGOUT = "LOGOUT";
	final String QUIT = "QUIT";
	final String WHO = "WHO";
	final String SEND = "SEND";
	static LinkedList<String> loginList = new LinkedList();
	static LinkedList<String> ipList = new LinkedList();
	// static HashMap<String, String> map = new HashMap();
	String currentIp, currentName, receiver;
	int msgIdx = 0;
	boolean msgStoreFlag = false, loginStatus = false, sendFlag = false;

  public ChildThread(Socket socket) throws IOException
  {
		this.socket = socket;
		in = new BufferedReader(
		    new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

		// reading txt files: user information and message of day into buffers
		userInfoBuff = new BufferedReader(new FileReader("userInfo.txt"));
		messageBuff = new BufferedReader(new FileReader("messageOfADay.txt"));
		String l;
		while((l = userInfoBuff.readLine()) != null)
		{
			userList.offer(l);
		}
		userInfoBuff.close();

		while((l = messageBuff.readLine()) != null)
		{
			msgList.offer(l);
		}
		messageBuff.close();

  }

	public void storeIp(String s){
		currentIp = s;
	}

	private void broadcast(Vector<ChildThread> handlers, String line){
		for(int i = 0; i < handlers.size(); i++)
		{
			synchronized(handlers)
			{
				ChildThread handler = (ChildThread)handlers.elementAt(i);
				if (handler != this)
				{
					handler.out.println(line); //broadcast to other clients
					handler.out.flush();
				}
			}
		}
	}

	private void response(Vector<ChildThread> handlers, String msg){
		for(int i = 0; i < handlers.size(); i++)
		{
			synchronized(handlers)
			{
				ChildThread handler = (ChildThread)handlers.elementAt(i);
				if (handler == this)
				{
					handler.out.println(msg); //broadcast to other clients
					handler.out.flush();
					break;
				}
			}
		}
	}

	private void storemsg(String s, Vector<ChildThread> handlers){
		msgStoreFlag = false;
		try{
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("messageOfADay.txt", true)));
			out.println(s);
			out.close();
			response(handlers,"200 OK");
		}
		catch(IOException ioe){
			response(handlers,"500 Server Error");
		}
	}

	private void login(String s, Vector<ChildThread> handlers){
		if(s.contains(" ")){
			String[] parts = s.split(" ");
			if(parts.length!=3){
				response(handlers, "410 Miss login information");
			}else{
				String userInfo = parts[1]+","+parts[2];
				for (int i=0; i<userList.size();i++){
					if(userInfo.equals(userList.get(i))){
						currentName = parts[1];
						if(loginList.contains(currentName)){
							response(handlers, currentName+", you have already login in");
						}
						else{
							response(handlers, "Welcome "+currentName);
							loginList.offer(currentName);
							ipList.offer(currentIp);
							loginStatus = true;
						}
						// if(isLoggedin(name)){
						// 	response(handlers, name+", you have already loggedin");
						// }else{
						// 	map.put(name, currentIp);
						// 	response(handlers, "Welcome "+name);
						// }
						break;
					}
					if(i==userList.size()-1){
						response(handlers, "410 No such user be found");
					}
				}
			}
		}
		else{
			response(handlers, "410 Wrong Login format");
		}
	}

	private void logout(String s, Vector<ChildThread> handlers){
		if(!loginStatus){
			response(handlers, "410 You are not loggedin yet.");
		}
		else{
			loginStatus = false;
			int idx = loginList.indexOf(currentName);
			loginList.remove(idx);
			ipList.remove(idx);
			currentName = "";
			loginStatus = false;
		}
	}

	private void who(Vector<ChildThread> handlers){
		response(handlers,"The list of the active users:");
		for(int i=0; i<loginList.size(); i++){
			String name = loginList.get(i);
			String ip = ipList.get(i);
			response(handlers,name+"---"+ip);
		}
	}

	private void send(String msg, ChildThread handler){
		handler.out.println("200 OK you have a new message from "+currentName);
		handler.out.println(currentName+": "+msg);
		handler.out.flush();
	}

  public void run()
  {
		String line;
		synchronized(handlers)
		{
	    // add the new client in Vector class
	    handlers.addElement(this);
		}

		try{
			while ((line = in.readLine()) != null)
	    {
				System.out.println(line); // print message from input client

				// check commands
				switch(line){
					case MSGGET:
						String msg = msgList.get(msgIdx);
						if(msgIdx==msgList.size()-1){
							msgIdx = 0;
						}
						else{
							msgIdx++;
						}
						response(handlers, "200 OK: "+msg);
						break;
					case MSGSTORE:
						msgStoreFlag = true;
						response(handlers, "200 OK");
						break;
					case WHO:
						who(handlers);
						break;
					case "SHOW":
						response(handlers, currentName+"---"+currentIp);
						break;
					default:
						if(msgStoreFlag){
							storemsg(line, handlers);
						}
						else if(sendFlag){
							int idx = loginList.indexOf(receiver);
							ChildThread handler = handlers.elementAt(idx);
							send(line, handler);
							sendFlag = false;
						}
						else if(line.contains(LOGIN)){
							login(line, handlers);
						}
						else if(line.contains(LOGOUT)){
							logout(line, handlers);
						}
						else if(line.contains(SEND)){
							if(line.contains(" ")){
								String[] parts = line.split(" ");
								String name = parts[1];
								if(!loginList.contains(name)){
									response(handlers, "420 either the user does not exist or is not logged in.");
								}
								else if(name.equals(currentName)){
									response(handlers, "420 cannot send to yourself.");
								}
								else{
									receiver = name;
									response(handlers, "200 OK");
									sendFlag = true;
								}
							}
							else{
								response(handlers, "410 Wrong send format.");
							}
						}
						else{
							response(handlers, "404 no such a command found");
						}

				}

				// Broadcast it to everyone!  You will change this.
				// Most commands do not need to broadcast
				// e.g. broadcast(handlers, line);
	    }
		}catch(IOException ioe)
    {}

    try
    {
			in.close();
			out.close();
			socket.close();
    }
    catch(IOException ioe)
    {
    }
    finally
    {
			synchronized(handlers)
			{
		    handlers.removeElement(this);
			}
    }
  }
}
