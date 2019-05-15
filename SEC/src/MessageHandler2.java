import java.io.Serializable;

public class MessageHandler2 implements Serializable {
	
	byte[] signature;

	MessageHandler2(byte[] Signature){
		
		this.signature = signature;
		
	}
	
	byte[] getSignature() {
		
		return signature;
	}

}
