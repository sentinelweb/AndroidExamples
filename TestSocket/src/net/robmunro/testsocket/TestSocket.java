package net.robmunro.testsocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;

public class TestSocket extends Activity {
 //public static String SOCKET_ADDRESS = "192.148.1.4:523";
	    /** Called when the activity is first created. */
	    @Override
	    public void onCreate(Bundle icicle) {
	        super.onCreate(icicle);
	        setContentView(R.layout.main);
	       
	        Thread sThread = new Thread(new TCPServer());
	        //Thread cThread = new Thread(new TCPClient());
	       
	        sThread.start();
	        try {
	               Thread.sleep(1000);
	          } catch (InterruptedException e) { }
	       
	         // cThread.start();
	    }
    
	    public class TCPServer implements Runnable{
	        
	        public static final String SERVERIP = "192.168.1.4";
	        public static final int SERVERPORT = 4444;
	             
	        public void run() {
	             try {
	                  Log.d("TCP", "S: Connecting...");
	                 
	                  ServerSocket serverSocket = new ServerSocket(SERVERPORT);
	                  while (true) {
	                     Socket client = serverSocket.accept();
	                     Log.d("TCP", "S: Receiving...");
	                     try {
	                          BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	                          String str = "";
	                          String path = null;
	                          while (str.indexOf("Connection:")==-1) {
	                        	  str = in.readLine();
	                        	  if (str!=null &&str.indexOf("GET /")!=-1) {
	                        		  path = str.substring("GET /".length(),str.length()-" HTTP/1.1".length());
	                        		  Log.d("TCP", "S: Path: '" + path + "'");
	                        	  }
	                        	  Log.d("TCP", "S: Received: '" + str + "'");
	                          }
	                          BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
	                          out.write("HTTP/1.0 200 OK"+"\n");
	                          out.write("Date: Fri, 20 Nov 2009 02:44:52 GMT"+"\n"+"\n");
	                          
	                          if ("".equals(path)) {
	                        	  out.write("Expires: -1"+"\n");
	                        	  out.write("Cache-Control: private, max-age=0"+"\n");
	                        	  out.write("Content-Type: text/html; charset=utf-8"+"\n");
	                        	  out.write("<html><head><body>"+path+"<br/><img src=\"drawable-hdpi/butt_play_on.png\"></body></head></html>"+"\n");
		                          out.flush();
	                          }
	                          
	                          if (path.endsWith(".png")||path.endsWith(".gif")||path.endsWith(".js")||path.endsWith(".html")) {
	                        	  Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(),R.drawable.icon);
	                        	  new BitmapDrawable(bitmapOrg).getBitmap().compress(Bitmap.CompressFormat.PNG, 90, client.getOutputStream()); 
	                          }
	                        } catch(Exception e) {
	                            Log.e("TCP", "S: Error", e);
	                        } finally {
	                        	client.close();
	                        	Log.d("TCP", "S: Done.");
	                        }
	                  }
	             } catch (Exception e) {
	               Log.e("TCP", "S: Error", e);
	             }
	        }
	    } 
    
    
}