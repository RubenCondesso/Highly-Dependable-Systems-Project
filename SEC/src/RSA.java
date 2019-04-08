
/**
 * RSA.java
 * 
 * This program will be called by Server program. It is not required to individually.
 * 
 * Compile	 	$javac RSA.java 
 * 
 * 
 */

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.sql.Date;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

	/*
	 * RSA class
	 * 			will generate RSA key pair and save in files locally.
	 * 
	 * 
	 */

public class RSA {
		
		Key publicKey;
		Key privateKey;
		
		static String nome;
		
		private static final String CERTIFICATE_ALIAS = "certificate";
	    private static final String CERTIFICATE_ALGORITHM = "RSA";
	    private static final String CERTIFICATE_DN = "CN=cn, O=o, L=L, ST=il, C= c";
	    private static final String CERTIFICATE_NAME = "keystore.test";
	    private static final int CERTIFICATE_BITS = 1024;
	    
	    static {
	        // adds the Bouncy castle provider to java security
	        Security.addProvider(new BouncyCastleProvider());
	    }
		
		
		
		/*
		 * main method
		 * 			will instantiate an object of RSA class and call the createRSA method.
		 * 
		 */
	
		public static void main(String[] args) throws NoSuchAlgorithmException, GeneralSecurityException, IOException{
			
			// System.out.println("Creating RSA class");
			
			RSA rsa = new RSA();
			
			rsa.createRSA(nome);
			
		}
		
		
		
		@SuppressWarnings("deprecation")
		// ============ Generating key pair =======
		
		/*
		 * createRSA method
		 * 					will create RSA key pair.
		 * 					the keys will be saved as object in two separate files.
		 */
				
		void createRSA(String nome) throws NoSuchAlgorithmException, GeneralSecurityException, IOException {
			
			System.out.println("Identificação da Ligação: "+ nome);
			
			X509Certificate cert = null;
			
			KeyPairGenerator kPairGen = KeyPairGenerator.getInstance("RSA");
			
			kPairGen.initialize(1024);
			
			KeyPair kPair = kPairGen.genKeyPair();
			
			publicKey = kPair.getPublic();
						
			privateKey = kPair.getPrivate();
	 
			KeyFactory fact = KeyFactory.getInstance("RSA");
			
			RSAPublicKeySpec pub = fact.getKeySpec(kPair.getPublic(), RSAPublicKeySpec.class);
			
			RSAPrivateKeySpec priv = fact.getKeySpec(kPair.getPrivate(), RSAPrivateKeySpec.class);
			
			serializeToFile(nome + "public.key", pub.getModulus(), pub.getPublicExponent()); 				// this will give public key file
			
			X509V3CertificateGenerator v3CertGen =  new X509V3CertificateGenerator();
			
	        v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
	        
	        v3CertGen.setIssuerDN(new X509Principal(CERTIFICATE_DN));
	        
	        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24));
	        
	        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365*10)));
	        
	        v3CertGen.setSubjectDN(new X509Principal(CERTIFICATE_DN));
	        
	        v3CertGen.setPublicKey(kPair.getPublic());
	        
	        v3CertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
	        
	        cert = v3CertGen.generateX509Certificate(kPair.getPrivate());
	        
	     
	        try {
	        	
				saveCert(cert,kPair.getPrivate(), nome);
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		
		}
		
		void saveCert(X509Certificate cert, PrivateKey key, String nome) throws Exception {
			
	        KeyStore keyStore = KeyStore.getInstance("JKS");    
	        
	        keyStore.load(null, null);
	        
	        keyStore.setKeyEntry(nome, key, "SEC".toCharArray(),  new java.security.cert.Certificate[]{cert});
	        
	        File file = new File(".", nome);
	        
	        keyStore.store( new FileOutputStream(file), "SEC".toCharArray() );
	    }
			
		// ===== Save the keys with  specifications into files ==============
		/*
		 * serializeToFile method
		 * 						will create an ObjectOutput Stream and 
		 * 						save the elements of key pairs into files locally.
		 * 
		 */

		void serializeToFile(String fileName, BigInteger mod, BigInteger exp) throws IOException {
			
		  	ObjectOutputStream ObjOut = new ObjectOutputStream( new BufferedOutputStream(new FileOutputStream(fileName)));

		  	try {
		  		
		  		ObjOut.writeObject(mod);
		  		
		  		ObjOut.writeObject(exp);
		  		
		  		// System.out.println("Key File Created: " + fileName);
		  		
		 	 } catch (Exception e) {
		 		 
		 	   throw new IOException(" Error while writing the key object", e);
		 	   
		 	 } finally {
		 		 
		 	   ObjOut.close();
		 	 }
			}
			
}