package kr.ac.kaist.mms_client;

import java.io.BufferedOutputStream;

/* -------------------------------------------------------- */
/** 
File name : MMSSndHandler.java
Author : Jaehyun Park (jae519@kaist.ac.kr)
	Haeun Kim (hukim@kaist.ac.kr)
	Jaehee Ha (jaehee.ha@kaist.ac.kr)
Creation Date : 2016-12-03
Version : 0.3.01

Rev. history : 2017-02-01
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)
	Added setting header field features. 
	Added locator registering features.

Rev. history : 2017-04-20 
Version : 0.5.0
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-04-25
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)

Rev. history : 2017-06-18
Version : 0.5.6
	Changed the variable Map<String,String> headerField to Map<String,List<String>>
Modifier : Jaehee Ha (jaehee.ha@kaist.ac.kr)
*/
/* -------------------------------------------------------- */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import sun.misc.BASE64Decoder;

class MMSSndHandler {
	
	private String TAG = "[MMSSndHandler] ";
	private final String USER_AGENT = "MMSClient/0.6.0";
	private String clientMRN = null;
	private boolean isRgstLoc = false;
	private MMSClientHandler.ResponseCallback myCallback;
	MMSSndHandler (String clientMRN){
		this.clientMRN = clientMRN;
	}

	@Deprecated
	void registerLocator(int port) throws Exception {
		isRgstLoc = true;
		sendHttpPost("urn:mrn:smart-navi:device:mms1", "/registering", port+":2", null);
	}
	
	void setResponseCallback (MMSClientHandler.ResponseCallback callback){
		this.myCallback = callback;
	}
	
	void sendHttpPost(String dstMRN, String loc, String data, Map<String,List<String>> headerField) throws Exception{
		String url = "http://"+MMSConfiguration.MMS_URL; // MMS Server
		if (!loc.startsWith("/")) {
			loc = "/" + loc;
		}
		url += loc;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		//add request header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Charset", "UTF-8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("srcMRN", clientMRN);
		con.setRequestProperty("dstMRN", dstMRN);
		//con.addRequestProperty("Connection","keep-alive");
		
		if (headerField != null) {
			con = addCustomHeaderField(con, headerField);
		} 
		
		//load contents
		String urlParameters = data;
		

		if(MMSConfiguration.LOGGING)System.out.println(TAG+"urlParameters: "+urlParameters);
		
		// Send post request
		con.setDoOutput(true);
		BufferedWriter wr = new BufferedWriter(
				new OutputStreamWriter(con.getOutputStream(),Charset.forName("UTF-8")));
		
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"Trying to send message");
		wr.write(urlParameters);
		wr.flush();
		wr.close();

		Map<String,List<String>> inH = con.getHeaderFields();
		
		
		inH = getModifiableMap(inH);
		int responseCode = 0;
		InputStream inStream = null;
		if (con.getResponseCode() != HttpURLConnection.HTTP_ENTITY_TOO_LARGE) {
			responseCode = con.getResponseCode();
			inStream = con.getInputStream();
		} else {
		     /* error from server */
			responseCode = con.getResponseCode();
			inStream = new ByteArrayInputStream("HTTP 413 Error: HTTP Entity Too Large".getBytes());
		}
		List<String> responseCodes = new ArrayList<String>();
		responseCodes.add(responseCode+"");
		inH.put("Response-code", responseCodes);
		
		if(MMSConfiguration.LOGGING){
			System.out.println("\n"+TAG+"Sending 'POST' request to URL : " + url);
			System.out.println(TAG+"Post parameters : " + urlParameters);
			System.out.println(TAG+"Response Code : " + responseCode);
		}
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(inStream,Charset.forName("UTF-8")));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		in.close();
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"Response: " + response.toString() + "\n");
		receiveResponse(inH, response.toString());
		
		return;
	}
	
	//OONI
	String sendHttpGetFile(String dstMRN, String fileName, Map<String,List<String>> headerField) throws Exception {

		String url = "http://"+MMSConfiguration.MMS_URL; // MMS Server
		if (!fileName.startsWith("/")) {
			fileName = "/" + fileName;
		}
		url += fileName;
		URL obj = new URL(url);
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Charset", "UTF-8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("srcMRN", clientMRN);
		con.setRequestProperty("dstMRN", dstMRN);
		if (headerField != null) {
			con = addCustomHeaderField(con, headerField);
		}
		//con.addRequestProperty("Connection","keep-alive");

		int responseCode = con.getResponseCode();
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"\nSending 'GET' request to URL : " + url);
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"Response Code : " + responseCode + "\n");
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer inputMsg = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			inputMsg.append(inputLine+"\n");
		}
		
		BASE64Decoder base64Decoder = new BASE64Decoder();
        InputStream encoded = new ByteArrayInputStream(inputMsg.toString().getBytes("UTF-8"));
        BufferedOutputStream decoded = new BufferedOutputStream(new FileOutputStream(System.getProperty("user.dir")+fileName));

        base64Decoder.decodeBuffer(encoded, decoded);      

        encoded.close();
        decoded.close();
		in.close();
		return fileName + " is saved";
	}
	//OONI end
	
	//HJH
	void sendHttpGet(String dstMRN, String loc, String params, Map<String,List<String>> headerField) throws Exception {

		String url = "http://"+MMSConfiguration.MMS_URL; // MMS Server
		if (!loc.startsWith("/")) {
			loc = "/" + loc;
		}
		url += loc;
		if (params != null) {
			if (params.equals("")) {
				
			}
			else if (params.startsWith("?")) {
				url += params;
			} else {
				url += "?" + params;
			}
		}
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		
		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Charset", "UTF-8");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("srcMRN", clientMRN);
		con.setRequestProperty("dstMRN", dstMRN);
		if (headerField != null) {
			con = addCustomHeaderField(con, headerField);
		}
		//con.addRequestProperty("Connection","keep-alive");

		Map<String,List<String>> inH = con.getHeaderFields();
		inH = getModifiableMap(inH);
		int responseCode = con.getResponseCode();
		List<String> responseCodes = new ArrayList<String>();
		responseCodes.add(responseCode+"");
		inH.put("Response-code", responseCodes);
		
		if(MMSConfiguration.LOGGING)System.out.println("\n"+TAG+"Sending 'GET' request to URL : " + url);
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"Response Code : " + responseCode);
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream(),Charset.forName("UTF-8")));
		String inputLine;
		StringBuffer response = new StringBuffer();
		
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		
		
		
		in.close();
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"Response: " + response.toString() + "\n");
		
		receiveResponse(inH, response.toString());
		return;
	}
	
	void receiveResponse (Map<String,List<String>> headerField, String message) {
		if (!isRgstLoc) {
			isRgstLoc = false;
			try {
				myCallback.callbackMethod(headerField, message);
			} catch (NullPointerException e) {
				System.out.println(TAG+"NullPointerException : Have to set response callback interface! MMSClientHandler.setSender()");
			}
		}
			
		return;
	}
	
	private Map<String, List<String>> getModifiableMap (Map<String, List<String>> map) {
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		Set<String> resHeaderKeyset = map.keySet(); 
		for (Iterator<String> resHeaderIterator = resHeaderKeyset.iterator();resHeaderIterator.hasNext();) {
			String key = resHeaderIterator.next();
			List<String> values = map.get(key);
			ret.put(key, values);
		}
	
		return ret;
	}
	
	private HttpURLConnection addCustomHeaderField (HttpURLConnection con, Map<String,List<String>> headerField) {
		HttpURLConnection retCon = con;
		if(MMSConfiguration.LOGGING)System.out.println(TAG+"set headerfield[");
		for (Iterator keys = headerField.keySet().iterator() ; keys.hasNext() ;) {
			String key = (String) keys.next();
			List<String> valueList = (List<String>) headerField.get(key);
			for (String value : valueList) {
				if(MMSConfiguration.LOGGING)System.out.println(key+":"+value);
				retCon.addRequestProperty(key, value);
			}
		}
		if(MMSConfiguration.LOGGING)System.out.println("]");
		return retCon;
	}
	//HJH end
}
