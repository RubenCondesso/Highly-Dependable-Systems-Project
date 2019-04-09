import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.Test;


public class Tests {

	Client client = new Client("localhost",1500,"test");
	
	Notary server = new Notary(1500);
	
	RSA rsa = new RSA();
	
	@Test
	public void testPrivateKeys() throws NoSuchAlgorithmException, GeneralSecurityException, IOException{
		
		KeyPair kPair = rsa.createKeyPairs("testKey");
		
		PrivateKey expectedPrivKey = rsa.checkPrivateKey(kPair);
		
		PublicKey pubKey = rsa.checkPublicKey("testKey", kPair);
		
		rsa.createCert("testKey", pubKey, expectedPrivKey);
		
		PrivateKey actualPrivKey = client.getPrivateKey("testKey");
		
		assertEquals(expectedPrivKey,actualPrivKey);
		
	}
	
	@Test
	public void testPublicKeys() throws NoSuchAlgorithmException, GeneralSecurityException, IOException{
		
		KeyPair kPair = rsa.createKeyPairs("testkey");
		
		PublicKey expectedPubKey = rsa.checkPublicKey("testkey", kPair);
		
		PublicKey actualPubKey = client.readPublicKeyFromFile("testkey" + "public.key");
		
		assertEquals(expectedPubKey,actualPubKey);
		
	}
	
	

}
