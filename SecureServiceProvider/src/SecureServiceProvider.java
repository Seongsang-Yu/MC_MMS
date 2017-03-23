import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import kr.ac.kaist.mms_client.*;

/* -------------------------------------------------------- */
/** 
File name : SecureServiceProvider.java
	HTTPS Service Provider only forwards messages to SC having urn:mrn:imo:imo-no:1000009 by HTTPS
Author : 
	Jaehee Ha (jaehee.ha@kaist.ac.kr)
Creation Date : 2017-03-21
Version : 0.4.0
*/
/* -------------------------------------------------------- */

public class SecureServiceProvider {
	public static void main(String args[]) throws Exception{
		String myMRN = "urn:mrn:smart-navi:device:secure-tm-server";
		int port = 8902;
		String jksDirectory = System.getProperty("user.dir")+"/testkey.jks";
		String jksPassword = "lovesm13";

		//MMSConfiguration.MMS_URL="winsgkwogml.iptime.org:444";
		
		SecureMMSClientHandler sch = new SecureMMSClientHandler(myMRN);
		sch.setPort(port, "/forwarding", jksDirectory, jksPassword); //sch has a context '/forwarding'
		/* It is not same with:
		 * sch.setPort(port); //It sets default context as '/'
		 * sch.addContext("/forwarding"); //Finally sch has two context '/' and '/forwarding'
		 */
		
		sch.setCallback(new SecureMMSClientHandler.Callback() {
			
			//it is called when client receives a message
			@Override
			public String callbackMethod(Map<String,List<String>> headerField, String message) {
				try {
					Iterator<String> iter = headerField.keySet().iterator();
					while (iter.hasNext()){
						String key = iter.next();
						System.out.println(key+":"+headerField.get(key).toString());
					}
					System.out.println(message);
					JSONParser Jpar = new JSONParser();
					String httpBody = (String)((JSONObject) Jpar.parse(message)).get("HTTP Body");
					//it only forwards messages to sc having urn:mrn:imo:imo-no:1000009
					String res = sch.sendPostMsg("urn:mrn:imo:imo-no:1000009", httpBody);
					System.out.println(res);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return "OK";
			}
		});
	}
}