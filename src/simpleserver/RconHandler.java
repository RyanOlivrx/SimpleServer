/*******************************************************************************
 * Copyright (C) 2010 Charles Wagner Jr..
 * spiegalpwns@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package simpleserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import simpleserver.files.EOFWriter;


public class RconHandler implements Runnable {
	protected final int BUF_SIZE = 8192;
	protected int BYTE_THRESHOLD = 32;
	protected final int SERVERDATA_AUTH=3;
	protected final int SERVERDATA_EXECCOMMAND=2;
	protected final int SERVERDATA_AUTH_RESPONSE = 2;
	protected final int SERVERDATA_RESPONSE_VALUE = 0;
	protected final int INT = 4;
	protected final int BB_DEFAULT = 128;
	protected byte[] buf;
	protected int r;
	protected int a;
	//protected boolean auth=false;
	protected boolean done=false;
	protected InputStream in;
	protected OutputStream out;
	protected Semaphore lock;
	protected LinkedList<byte[]> packetBuffer=new LinkedList<byte[]>();
	public static final int IDLE_TIME=60*1000;
	protected ByteBuffer bb = ByteBuffer.allocate(BB_DEFAULT);
	protected ByteBuffer bbSend = ByteBuffer.allocate(4096);
	
	
	private Socket s;
	private RconTCP parent;
	private Server server;
	
	public RconHandler(Socket s, RconTCP r) throws IOException {
		this.s=s;
		this.in=s.getInputStream();
		this.out=s.getOutputStream();
		parent=r;
		server=r.parent;
		buf = new byte[BUF_SIZE];
		lock = new Semaphore(1);
		bb.order(ByteOrder.LITTLE_ENDIAN);
	}

	public void setByteThreshold(int n) {
		if (n>BUF_SIZE)
			BYTE_THRESHOLD=BUF_SIZE;
		if (n<=0)
			BYTE_THRESHOLD=32;
		BYTE_THRESHOLD=n;
	}
	
	
	public void run() {
		//int packetid=0;
		try {
			s.setSoTimeout(IDLE_TIME);
			parent.lastRead=System.currentTimeMillis();
		    while (!parent.isClosed() && !Thread.interrupted()) {
		    	//int read = in.read(buf, 0, 4);
		    	int packetSize = readInt();
		    	//int read = in.read(buf, 4, packetSize);
		    	ensureRead(packetSize);
		    	
		    	/*
		    	if (System.currentTimeMillis()-lastRead>IDLE_TIME) {
		    		if (!parent.isRobot)
		    			System.out.println("[SimpleServer] Disconnecting " + parent.getIPAddress() + " due to inactivity.");
		    		try {in.close();} catch (IOException e1) {}
					try {out.close();} catch (IOException e1) {}
		    		parent.close();
		    	}
		    	*/
		    	int requestID = readInt();
		    	int requestType = readInt();
		    	String s1 = readString2();
		    	String s2 = readString2();
		    	if (a-r>0) {
		    		System.out.println(a-r);
					byte[] cpy = new byte[BUF_SIZE];
					System.arraycopy(buf, r, cpy, 0, a-r);
					System.arraycopy(cpy, 0, buf, 0, a-r);
				}
		    	a-=r;
		    	r=0;
		    	int responseType=0;
		    	String response;
		    	if (requestType==SERVERDATA_EXECCOMMAND && parent.auth) {
		    		response = parsePacket(s1);
		    		done=true;
		    	}
		    	else if (requestType==SERVERDATA_AUTH) {
		    		response = auth(s1);
		    		responseType=SERVERDATA_AUTH_RESPONSE;
		    		if (response==null) {
		    			requestID=-1;
		    			response = "";
		    		}
		    		junkResponse();
		    	}
		    	else if (!parent.auth) {
		    		responseType=SERVERDATA_AUTH_RESPONSE;
		    		requestID=-1;
		    		response = "";
		    	}
		    	else {
		    		responseType=SERVERDATA_RESPONSE_VALUE;
		    		requestID=-1;
		    		response = "Error";
		    	}
		    	
		    	if (!response.equals("")) {	   
		    		int i=0;
			    	for (i=0;i+4096<bbSend.capacity();i+=4096) {
			    		out.write(assemblePacket(response.substring(i, i+4096),requestID,responseType));
			    		
			    	}
			    	out.write(assemblePacket(response.substring(i),requestID,responseType));
		    		
		    	}
		    	else {
		    		out.write(assemblePacket("",requestID,responseType));
		    	}
	    		out.flush();
	    		s.setSendBufferSize(10);
	    		//s.sendUrgentData(0);
	    		if (done) {
	    			out.close();
	    			break;
	    		}
		    	
		    	
		    	
		    	if (parent.isClosed()) {
		    		throw new InterruptedException();
		    	}
		    	bb = ByteBuffer.allocate(BB_DEFAULT);
		    	bb.order(ByteOrder.LITTLE_ENDIAN);
		    	Thread.sleep(20);
		    }
		}
		catch(InterruptedException e1) {
			//We don't care about an Interrupted Exception.
			//Don't even print out to the console that we received the exception.
			//We are only interrupted if we are closing.
			//e1.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		//try {s.shutdownInput();} catch (IOException e2) {}
		//try {s.shutdownOutput();} catch (IOException e2) {}
		try {
			out.close();
		} catch (IOException e1) {

		}
		try {
			in.close();
		} catch (IOException e) {

		}
		in=null;
		out=null;
		try {
			s.close();
		} catch (IOException e) {
		}
		parent=null;
		lock=null;
	}
	private byte[] assemblePacket(String command, int requestID, int responseType) {
		byte[] strBytes = command.getBytes();
		return assemblePacket(strBytes,requestID,responseType);
	}
	private byte[] assemblePacket(byte[] command, int requestID, int responseType) {
		ByteBuffer bbuf;
		
		int packetSize = INT * 2 + (command.length) + 1 + 1;
		bbuf = ByteBuffer.allocate(packetSize + 4);
		bbuf.order(ByteOrder.LITTLE_ENDIAN);
		bbuf.putInt(packetSize);
		bbuf.putInt(requestID);
		bbuf.putInt(responseType);
		bbuf.put(command);
		bbuf.put((byte)0);
		bbuf.put((byte)0);
		byte[] send = bbuf.array();
		return send;
	}
	private void junkResponse() throws IOException {
		int packetSize= INT * 2 + 1 + 1;
		bb = ByteBuffer.allocate(INT + packetSize);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(packetSize);
		bb.putInt(-1);
		bb.putInt(SERVERDATA_RESPONSE_VALUE);
		bb.put((byte) 0);
		bb.put((byte) 0);
		out.write(bb.array());
	}
	private void printStream(byte[] stream) {
		for(int i=0;i<64;i++) {
			System.out.print(Byte.toString(stream[i]) + " ");
		}
		System.out.println();
	}
	private String getConsole() {
		String console = "";
		String[] consolearray = server.getOutputLog();
		for (String i: consolearray) {
			console += i;
		}
		return console;
	}
	protected String parsePacket(String command) throws IOException, InterruptedException {
		String[] tokens = command.split(" ");
		if (tokens.length>0) {
			if (tokens[0].equalsIgnoreCase("rcon")) {
				if (tokens.length>1) {
					int idx = command.indexOf(tokens[1]);
					server.runCommand(command.substring(idx));
					return command;
				}
				else
					return "Error: No Command";
			}
			if (tokens[0].equalsIgnoreCase("help")) {
				if (tokens.length>1) {
					if (tokens[1].equalsIgnoreCase("get")) {
						return "Resources:\r\n" +
						"console		Shows console output\r\n";
					}
				}
				return "Commands:\r\n" +
					"help		Shows this message\r\n" +
					"rcon		Execute Command\r\n" +
					"get		Get a resource";
			}
			if (tokens[0].equalsIgnoreCase("get")) {
				if (tokens.length>1) {
					if (tokens[1].equalsIgnoreCase("console")) {
						return getConsole();
					}
				}
				return "Error: No Command";
			}
		}
		return "Error: Unrecognized Command";
	}
	protected String auth(String passwd) {
		if (server.options.rconPassword.equals("")) {
			System.out.println("[SimpleServer] RCON Auth Attempt from " + s.getInetAddress().getHostAddress() + "! (rconPassword is blank)");
			return null;
		}
		if (passwd.equals(server.options.rconPassword)) {
			parent.auth=true;
			return "";
		}
		else {
			System.out.println("[SimpleServer] RCON Authentication Failed from " + s.getInetAddress().getHostAddress() + "!");
			return null;
		}
	}
	
	
	private int readMore() throws InterruptedException, IOException {
		int tmp=0;
		int avail;
		try {
			lock.acquire();
			avail = in.available();	
			if (avail==0) {
				lock.release();
				Thread.sleep(20);
				return a;
			}
			if (a+avail>buf.length)
				avail=buf.length-a;
			if (avail>0)
				tmp =in.read(buf, a, avail);
			if (tmp>0) {
				a+=tmp;
				parent.lastRead=System.currentTimeMillis();
			}
			lock.release();
			return a;
		} catch (IOException e) {
			lock.release();
			throw e;
		} catch (InterruptedException e) {
			lock.release();
			throw e;
		}
		
	}
	
	protected boolean ensureRead(int n) throws InterruptedException, IOException {
		
		if (r+n>BUF_SIZE)
			return false;
		if (a>=r+n)
			return true;
		while(a<r+n) {
			if (parent.isClosed() || Thread.interrupted())
				throw new InterruptedException();
			
			readMore();
		}
		return true;
	}
	protected String readString2() throws IOException, InterruptedException {
		bb.clear();
		byte b=0;
		int i=0;
		while(true) {
			ensureRead(1);
			b = readByte();
			if (b!=0)
				bb.put(b);
			else
				break;
			i++;
		}
		if (bb.get(0)==0)
			return "";
		byte[] string = new byte[i];
		bb.position(0);
		bb.get(string, 0, i);
		return new String(string);
	}
	protected int readInt() throws IOException, InterruptedException {
		ensureRead(4);
		byte[] cpy = new byte[4];
		cpy[0] = buf[r];
		cpy[1] = buf[r+1];
		cpy[2] = buf[r+2];
		cpy[3] = buf[r+3];
		r+=4;
		return bytesToInt(cpy);
	}
	protected double readDouble() throws IOException, InterruptedException {
		ensureRead(8);
		byte[] cpy = new byte[8];
		cpy[0] = buf[r];
		cpy[1] = buf[r+1];
		cpy[2] = buf[r+2];
		cpy[3] = buf[r+3];
		cpy[4] = buf[r+4];
		cpy[5] = buf[r+5];
		cpy[6] = buf[r+6];
		cpy[7] = buf[r+7];
		r+=8;
		return bytesToDouble(cpy);
	}
	protected short readShort() throws IOException, InterruptedException {
		ensureRead(2);
		byte[] cpy = new byte[2];
		cpy[0] = buf[r];
		cpy[1] = buf[r+1];
		r+=2;
		return bytesToShort(cpy);
	}
	protected byte[] readBytes(int n) throws IOException, InterruptedException {
		ensureRead(n);
		byte[] cpy = new byte[n];
		System.arraycopy(buf, r, cpy, 0, n);
		r+=n;
		return cpy;
	}
	protected byte readByte() throws IOException, InterruptedException {
		ensureRead(1);
		return buf[r++];
	}
	protected void skipBytes(int n) {
		r+=n;
	}
	protected void removeBytes(int n) throws IOException, InterruptedException {
		ensureRead(0);
		lock.acquire();
		if (a-r!=0) {
			//byte[] cpy = new byte[a-r];
			System.arraycopy(buf, r, buf, r-n, a-r);
			//System.arraycopy(cpy, 0, buf, r-n, a-r);
		}
		r-=n;
		a-=n;
		lock.release();
	}
	private short bytesToShort(byte[] data) 
	{       
		bb.clear();
		bb.put(data[0]);
		bb.put(data[1]);
		short s = bb.getShort(0);
		return s;
	}
	private int bytesToInt(byte[] data) 
	{       
		bb.clear();
		bb.put(data[0]);
		bb.put(data[1]);
		bb.put(data[2]);
		bb.put(data[3]);
		int s = bb.getInt(0);
		return s;
	}
	private double bytesToDouble(byte[] data) 
	{       
		bb.clear();
		bb.put(data[0]);
		bb.put(data[1]);
		bb.put(data[2]);
		bb.put(data[3]);
		bb.put(data[4]);
		bb.put(data[5]);
		bb.put(data[6]);
		bb.put(data[7]);
		double s = bb.getDouble(0);
		return s;
	}
	

}