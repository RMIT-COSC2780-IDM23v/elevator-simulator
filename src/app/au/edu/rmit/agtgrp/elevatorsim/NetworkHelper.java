package au.edu.rmit.agtgrp.elevatorsim;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import au.edu.rmit.agtgrp.elevatorsim.ui.ControllerDialogCreator;

/**
 * Abstracts networking operations so other classes only need to work with JSON.
 * Read and write operations are synchronized on the input and output streams respectively
 * @author Joshua Richards
 */
public class NetworkHelper
{
	private int port;
	private ServerSocket ss;
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;

	private List<Listener> listeners = new ArrayList<>();
	private ControllerDialogCreator cdc;
	
	private boolean closed = true;

	/**
	 * Starts a ServerSocket, accepts one client and retrieves input and
	 * output streams to the client. This constructor will block until a
	 * client connects to the ServerSocket or an Exception if thrown.
	 * @param port The port to listen on
	 * @throws IOException If the ServerSocket times out while waiting
	 * for a client to connect to it or there is a problem retrieving the
	 * input and output streams
	 */
	public NetworkHelper(int port) throws IOException
	{
		this.port = port;
		initSocket(0);
	}
	
	private void initSocket(int serverTimeoutSeconds) throws IOException
	{
		ss = new ServerSocket(port);
		try
		{
			ss.setSoTimeout(serverTimeoutSeconds * 1000);
			socket = ss.accept();
			socket.setSoTimeout(ElsimSettings.get().getTimeout() * 1000);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			closed = false;
		}
		finally
		{
			ss.close();
		}
	}
	
	/**
	 * Reads a message from the client and returns it as JSON.
	 * This method is synchronized on the input stream and will
	 * block until a message is received.
	 * @return The message from the client
	 * @throws IOException If there is a connection problem
	 */
	public JSONObject receive() throws IOException
	{
		String messageStr;
		synchronized (in)
		{
			try
			{
				messageStr = in.readUTF();
			}
			catch (SocketException | EOFException e)
			{
				// connection reset
				// probably client closed
				if (!closed)
				{
					handleConnectionClose("Connection closed by client");
				}
				throw e;
			}
			catch (SocketTimeoutException e)
			{
				for (Listener listener : listeners)
				{
					// call on litener(s) to send heartbeat
					listener.onTimeout();
				}
				
				try
				{
					// try to retrieve reply
					messageStr = in.readUTF();
				}
				catch (SocketTimeoutException e1)
				{
					handleConnectionClose("Connection lost");
					throw e1;
				}
			}
		}
		return new JSONObject(messageStr);
	}
	
	/**
	 * Sends a message to the client
	 * @param message The message to send
	 * @throws IOException If there is a connection problem
	 */
	public void transmit(JSONObject message) throws IOException
	{
		synchronized (out)
		{
			out.writeUTF(message.toString());
		}
	}
	
	public void close()
	{
		closed = true;
		try
		{
			socket.close();
			in.close();
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void handleConnectionClose(String message)
	{
		cdc.showErrorDialog(message);
		close();
		for (Listener l : listeners)
		{
			l.onConnectionClosed();
		}
	}
	
	public void setControllerDialogCreator(ControllerDialogCreator cdc)
	{
		this.cdc = cdc;
	}
	
	public void addListener(Listener l)
	{
		listeners.add(l);
	}
	
	public void removeListener(Listener l)
	{
		listeners.remove(l);
	}
	
	public interface Listener
	{
		public void onTimeout();
		public void onConnectionClosed();
	}
}
