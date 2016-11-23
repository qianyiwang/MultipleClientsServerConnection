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
	int msgIdx = 0;

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
				}
			}
		}
	}

	private void storemsg(String s){
		
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
						response(handlers, msg);
						break;
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
