import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.HashMap;

import javax.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;



public class Tests {

	Client client = new Client("localhost",1500,"test");
	Notary server = new Notary(1500);
	RSA rsa = new RSA();
	
	@Test
	public void test() throws NoSuchAlgorithmException, GeneralSecurityException, IOException{
		
		java.security.cert.X509Certificate testCert = rsa.createRSA("testC");
		java.security.cert.X509Certificate testKey1 = rsa.createRSA("testF");
		
		FileInputStream is = new FileInputStream("testC");
		
	    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	    
	    keystore.load(is, "SEC".toCharArray());
	    
	    String alias = "testC";
	
	    Key key = keystore.getKey(alias, "SEC".toCharArray());
	    
	    PrivateKey testKey = (PrivateKey) key;
	    
		PrivateKey checkKey = client.getPrivateKey("testC");
		
		assertEquals(testKey,checkKey);
		//assertNotEquals(testKey1,checkKey);
		
	

	}

}
