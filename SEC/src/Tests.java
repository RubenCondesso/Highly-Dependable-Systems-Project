import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
	public void encryptDecript() throws NoSuchAlgorithmException, IOException, GeneralSecurityException {
		
		byte[] ciphertext = client.encryptMessage("mensagem","testKey");
		
		String plaintext = server.decryptMessage(ciphertext, "testKey");
		
		assertEquals("mensagem",plaintext);
		
		assertNotEquals("mensagemAlterada",plaintext);
		
	}
	
	@Test
	public void testPrivateKeys() throws NoSuchAlgorithmException, GeneralSecurityException, IOException{
		
		KeyPair kPair = rsa.createKeyPairs("testKey");
		
		KeyPair kPair1 = rsa.createKeyPairs("testKey1");
		
		PrivateKey expectedPrivKey = rsa.checkPrivateKey(kPair);
		
		PrivateKey expectedPrivKey1 = rsa.checkPrivateKey(kPair1);
		
		PublicKey pubKey = rsa.checkPublicKey("testKey", kPair);
		
		PublicKey pubKey1 = rsa.checkPublicKey("testKey1", kPair1);
		
		rsa.createCert("testKey", pubKey, expectedPrivKey);
		
		rsa.createCert("testKey1", pubKey1, expectedPrivKey1);
		
		PrivateKey actualPrivKey = client.getPrivateKey("testKey");
		
		PrivateKey actualPrivKey1 = client.getPrivateKey("testKey1");

		assertEquals(expectedPrivKey,actualPrivKey);
		
		assertNotEquals(expectedPrivKey,actualPrivKey1);
		
	}
	
	@Test
	public void testPublicKeys() throws NoSuchAlgorithmException, GeneralSecurityException, IOException{
		
		KeyPair kPair = rsa.createKeyPairs("testkey");
		
		PublicKey expectedPubKey = rsa.checkPublicKey("testkey", kPair);
		
		PublicKey actualPubKey = client.readPublicKeyFromFile("testkey" + "public.key");
		
		assertEquals(expectedPubKey,actualPubKey);
		
	}
	
	
	

}
