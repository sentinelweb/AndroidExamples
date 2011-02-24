package com.illposed.osc;

import java.net.*;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.illposed.osc.utility.OSCByteArrayToJavaConverter;

/**
 * OSCPortOut is the class that sends OSC messages to a specific address and port.
 *
 * To send an OSC message, call send().
 * <p>
 * An example based on com.illposed.osc.test.OSCPortTest::testMessageWithArgs() :
 * <pre>
	OSCPort sender = new OSCPort();
	Object args[] = new Object[2];
	args[0] = new Integer(3);
	args[1] = "hello";
	OSCMessage msg = new OSCMessage("/sayhello", args);
	 try {
		sender.send(msg);
	 } catch (Exception e) {
		 showError("Couldn't send");
	 }
 * </pre>
 * <p>
 * Copyright (C) 2004-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 * <p>
 * See license.txt (or license.rtf) for license information.
 *
 * @author Chandrasekhar Ramakrishnan
 * @version 1.0
 */
public class OSCPortOut extends OSCPort {

	protected InetAddress address;

	/**
	 * Create an OSCPort that sends to newAddress, newPort
	 * @param newAddress InetAddress
	 * @param newPort int
	 */
	public OSCPortOut(InetAddress newAddress, int newPort)
		throws SocketException {
		//socket = new DatagramSocket();
		address = newAddress;
		port = newPort;
			//socket = new ServerSocket(newPort);
			try { 
				SocketAddress sockaddr = new InetSocketAddress(address, port); 
				// Create an unbound socket 
				socket = new Socket(); 
				// This method will block no more than timeoutMs. 
				// If the timeout occurs, SocketTimeoutException is thrown.
				//int timeoutMs = 2000; // 2 seconds 
				//socket.connect(sockaddr, timeoutMs); 
				socket.connect(sockaddr);
				
			} catch (UnknownHostException e) { 
				Log.d("osc", "connect:",e);
			} catch (SocketTimeoutException e) { Log.d("osc", "connect:",e);
			} catch (IOException e) {Log.d("osc", "connect:",e); } 
			Log.d("osc", "connect:open finished");
		
		
	}

	/**
	 * Create an OSCPort that sends to newAddress, on the standard SuperCollider port
	 * @param newAddress InetAddress
	 *
	 * Default the port to the standard one for SuperCollider
	 */
	public OSCPortOut(InetAddress newAddress) throws SocketException {
		this(newAddress, defaultSCOSCPort());
	}

	/**
	 * Create an OSCPort that sends to localhost, on the standard SuperCollider port
	 * Default the address to localhost
	 * Default the port to the standard one for SuperCollider
	 */
	public OSCPortOut() throws UnknownHostException, SocketException {
		this(InetAddress.getLocalHost(), defaultSCOSCPort());
	}
	
	/**
	 * Send an osc packet (message or bundle) to the receiver I am bound to.
	 * @param aPacket OSCPacket
	 */
	public void send(OSCPacket aPacket) throws IOException {
		byte[] byteArray = aPacket.getByteArray();
		// have to still do send
		// Create a socket with a timeout 
	
	}
	
	public void send(byte[] byteArray) throws IOException {
		try { 
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write(byteArray);
			outputStream.flush();
			Log.d("osc", "send:ok");
		}
		catch (UnknownHostException e) {
			Log.d("osc", "send:",e);
			
		} catch (SocketTimeoutException e) {
			Log.d("osc", "send:",e);
		} catch (IOException e) {
			if (!(e instanceof SocketException)) {
				Log.d("osc", "send:",e);
			} else {
				Log.d("osc", "send:"+e.getClass().getSimpleName()+":"+e.getMessage());
			}
		} 
		
		//old.
		//socket.
		//DatagramPacket packet =
		//	new DatagramPacket(byteArray, byteArray.length, address, port);
		//socket.send(packet);
	}
}
