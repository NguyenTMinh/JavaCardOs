package project;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

public class CardService extends Applet
{
	// Cac truong thong tin luu tru
	private static byte[] pin;
	
	// Cac bien ho tro logic
	private static boolean pinCreated = false;
	private static short pinCounter;
	private AESKey aesKey;
	private Cipher cipher;
	
	private CardService() {
		aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
		cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new CardService().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		init();
	}
	
	// minh: init cac du lieu cua lop
	private static void init() {
		pin = new byte[Constant.PIN_WRAPPER_LENGTH];
	}

	public void process(APDU apdu)
	{
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		short len = apdu.setIncomingAndReceive();
		
		if (buf[ISO7816.OFFSET_CLA] != Constant.CLA) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
		switch (buf[ISO7816.OFFSET_INS]){
		case Constant.INS_CREATE_PIN: {
			if (!pinCreated) {
				JCSystem.beginTransaction();
				pin = encryptAES(buf);
				JCSystem.commitTransaction();
				pin = pin;
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)1);
				apdu.sendBytesLong(Constant.RESPONSE_PIN_CREATE_SUCCESS, (short)0, (short)1);
				pinCreated = true;
			} else {
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)1);
				apdu.sendBytesLong(Constant.RESPONSE_PIN_ALREADY_CREATED, (short)0, (short)1);
			}
			
			break;
		}
		case Constant.INS_CHECK_PIN: {
			if (pinCounter <= Constant.MAX_PIN_COUNTER) {
				boolean check = true;
				byte[] temp = encryptAES(buf);
				for (short i=0; i < Constant.PIN_LENGTH; i++) {
					if (pin[i] != temp[i]) {
						check = false;
						break;
					}
				}
				
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)1);
				if (check) {
					apdu.sendBytesLong(Constant.RESPONSE_PIN_CHECK_TRUE, (short)0, (short)1);
				} else {
					apdu.sendBytesLong(Constant.RESPONSE_PIN_CHECK_FALSE, (short)0, (short)1);
					pinCounter++;
				}
			} else {
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)1);
				apdu.sendBytesLong(Constant.RESPONSE_PIN_CHECK_REACH_LIMIT, (short)0, (short)1);
			}
			
			break;
		}
		case Constant.INS_CHANGE_PIN: {
			JCSystem.beginTransaction();
			pin = encryptAES(buf);
			JCSystem.commitTransaction();
			
			apdu.setOutgoing();
			apdu.setOutgoingLength((short)1);
			apdu.sendBytesLong(Constant.RESPONSE_PIN_CREATE_SUCCESS, (short)0, (short)1);
			break;
		}
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	private byte[] encryptAES(byte[] buffer) {
		byte[] input = new byte[Constant.PIN_WRAPPER_LENGTH];
		byte[] output = new byte[Constant.PIN_WRAPPER_LENGTH];
		
		for (short i=0; i < Constant.PIN_LENGTH; i++) {
			input[i] = buffer[(short)(ISO7816.OFFSET_CDATA+i)];
		}
		
		aesKey.setKey(Constant.KEY_AES, (short)0);
		cipher.init(aesKey, Cipher.MODE_ENCRYPT);
		cipher.doFinal(input, (short)0, (short)input.length, output, (short)0);
		
		return output;
	}
}
