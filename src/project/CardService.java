package project;

import javacard.framework.*;

public class CardService extends Applet
{
	private static boolean pinCreated = false;
	private static byte[] pin;
	private static short pinCounter;

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new CardService().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		init();
	}
	
	// minh: init cac du lieu cua lop
	private static void init() {
		pin = new byte[Constant.PIN_LENGTH];
		pinCounter = 0;
		for (short i=0; i < pin.length; i++) {
			pin[i] = Constant.PIN_DEFAULT;
		}
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
				for (short i=0; i < Constant.PIN_LENGTH; i++) {
					short index = (short)(ISO7816.OFFSET_CDATA + i);
					pin[i] = buf[index];
				}
				JCSystem.commitTransaction();
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
				for (short i=0; i < Constant.PIN_LENGTH; i++) {
					if (pin[i] != buf[(short)(ISO7816.OFFSET_CDATA+i)]) {
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
			for (short i=0; i < Constant.PIN_LENGTH; i++) {
				pin[i] = buf[(short)(ISO7816.OFFSET_CDATA+i)];
			}
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
	
	private boolean isPinCreated() {
		return false;
	}
}
