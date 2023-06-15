package project;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

public class CardService extends Applet
{
	//TODO them id cua the, 
	private static final byte[] idCard = new byte[] {(byte)0x30, (byte)0x32, (byte)0x35, (byte)0x36, (byte)0x34};
	//TODO rsa sinh 2 key public key, private key. 2key nay sinh ra tai card va publick key duoc gui cho server, private giu lai
	
	// Cac truong thong tin luu tru
	private static byte[] pin;
	// Thong tin sinh vine
	private static byte id;
	private static byte[] avatar;
	private static byte[] name;
	private static byte gender;
	private static byte[] date;
	private static byte[] phone;
	private static byte[] studentId;
	private static byte[] classSV;
	// Thong tin lich su gui xe
	private static LichSuGuiXe historyVehicle;
	
	// Cac bien ho tro logic
	private static boolean pinCreated = false; // trang thai pin da duoc tao hay chua
	private static short pinCounter; // so lan nhap pin sai
	private AESKey aesKey;
	private Cipher cipher;
	private static boolean infoInit = false; // check rang thong tin da duoc nap xuong hay chua
	private static short offsetIndexReceiveData = (short)0;
	private static short offsetIndexSendData = (short)0;
	private static short avatarSize;
	private static short avatarLengthRemain = (short)0;
	private static byte[] temp; // temp tam de tra avatar
	
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
		pinCounter = 1;
		avatar = new byte[Constant.AVATAR_LENGTH];
		historyVehicle = new LichSuGuiXe();
	}

	public void process(APDU apdu)
	{
		if (selectingApplet()) {
			return;
		}

		byte[] buf = apdu.getBuffer();
		short receiveLen = apdu.setIncomingAndReceive();
		
		if (buf[ISO7816.OFFSET_CLA] != Constant.CLA) {
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
		
		switch (buf[ISO7816.OFFSET_INS]){
		case Constant.INS_VALID_ID_CARD: {
			sendResponse(apdu, idCard);
			break;
		}
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
				infoInit = true;
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
				checkPin(buf, apdu);
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
		case Constant.INS_UNLOCK_CARD: {
			boolean check = true;
			for (short i=0; i < Constant.PIN_LENGTH; i++) {
				if (Constant.SECRET_UNLOCK_KEY[i] != buf[(short)(ISO7816.OFFSET_CDATA+i)]) {
					check = false;
					break;
				}
			}
			
			apdu.setOutgoing();
			if (check) {
				pinCounter = 1;
				byte[] uPin = new byte[Constant.PIN_LENGTH];
				byte[] temp = decryptAES(pin);
				for (short i=0; i < uPin.length; i++) {
					uPin[i] = temp[i];
				}
				
				apdu.setOutgoingLength((short)4);
				apdu.sendBytesLong(uPin, (short)0, (short)4);
			} else {
				apdu.setOutgoingLength((short)1);
				apdu.sendBytesLong(Constant.RESPONSE_PIN_CHECK_FALSE, (short)0, (short)1);
			}
			break;
		}
		case Constant.INS_FLUSH_DATA: {
			if (infoInit) {
				sendResponse(apdu, Constant.RESPONSE_PIN_ALREADY_CREATED);
				return;
			}
			
			flushInfo(apdu, buf);
			
			break;
		}
		case Constant.INS_GET_DATA: {
			byte choice = buf[ISO7816.OFFSET_P1];
			switch(choice) {
			case Constant.PARAM_ID:{
				byte[] resp = new byte[]{id};
				sendResponse(apdu, resp);
				break;
			}
			case Constant.PARAM_AVATAR: {
				if (avatarLengthRemain > Constant.MAX_SIZE_APDU) {
					if (temp == null) {
						temp = new byte[Constant.MAX_SIZE_APDU];
					}
				} else {
					temp = new byte[(short)(avatarLengthRemain)];
				}
				Util.arrayCopy(avatar, offsetIndexSendData, temp, (short)0, (short)temp.length);
				offsetIndexSendData += (short)temp.length;
				avatarLengthRemain = (short)(avatarLengthRemain-temp.length);
				
				sendResponse(apdu, temp);
				break;
			}
			case Constant.PARAM_HO_TEN: {
				temp = null;
				avatarLengthRemain = avatarSize;
				offsetIndexSendData = (short)0;
				
				sendResponse(apdu, name);
				break;
			}
			case Constant.PARAM_GIOI_TINH: {
				sendResponse(apdu, new byte[]{gender});
				break;
			}
			case Constant.PARAM_NGAY_SINH: {
				sendResponse(apdu, date);
				break;
			}
			case Constant.PARAM_DIEN_THOAI: {
				sendResponse(apdu, phone);
				break;
			}
			case Constant.PARAM_MSV: {
				sendResponse(apdu, studentId);
				break;
			}
			case Constant.PARAM_LOP: {
				sendResponse(apdu, classSV);
				break;
			}
			}
			break;
		}
		case Constant.INS_EDIT_DATA: {
			flushInfo(apdu, buf);
			break;
		}
		case Constant.INS_RESET_DATA: {
			byte[] pass = new byte[Constant.PIN_LENGTH];
			boolean check = true;
			for (short i=0; i < Constant.PIN_LENGTH; i++) {
				if (Constant.SECRET_UNLOCK_KEY[i] != buf[(short)(ISO7816.OFFSET_CDATA+i)]) {
					check = false;
					break;
				}
			}
			
			if (check) {
				resetInfo();
				sendResponse(apdu, Constant.RESPONSE_RESET_INFO_SUCCESS);
			} else {
				sendResponse(apdu, Constant.RESPONSE_PIN_CHECK_FALSE);
			}
			break;
		}
		case Constant.INS_CHECK_IN_VEHICLE: {
			JCSystem.beginTransaction();
			short trangThai = buf[ISO7816.OFFSET_P1];
			byte[] thoiGian = new byte[7];
			Util.arrayCopy(buf, (short)ISO7816.OFFSET_CDATA, thoiGian, (short)0, (short)7);
			historyVehicle.addLichSuMoi(new GuiXeRecord(thoiGian, trangThai));
			JCSystem.commitTransaction();
			
			sendResponse(apdu, Constant.RESPONSE_GUI_XE_OK);
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
	
	private byte[] decryptAES(byte[] buffer) {
		byte[] output = new byte[Constant.PIN_WRAPPER_LENGTH];
		
		
		aesKey.setKey(Constant.KEY_AES, (short)0);
		cipher.init(aesKey, Cipher.MODE_DECRYPT);
		cipher.doFinal(buffer, (short)0, (short)buffer.length, output, (short)0);
		
		return output;
	}
	
	public byte[] shortToByteArray(short value, short offset) {
		byte[] byteArray = new byte[3];
		byteArray[offset] = (byte) (value >> 8);
		byteArray[(short)(offset + 1)] = (byte) value;
		return byteArray;
	}
	
	private void sendResponse(APDU apdu, byte[] data) {
		apdu.setOutgoing();
		apdu.setOutgoingLength((short)data.length);
		apdu.sendBytesLong(data, (short)0, (short)data.length);
	}
	
	private void flushInfo(APDU apdu, byte[] buf) {
		JCSystem.beginTransaction();
		byte choice = buf[ISO7816.OFFSET_P1];
		switch (choice) {
			case Constant.PARAM_ID: {
				id = buf[ISO7816.OFFSET_P2];
				JCSystem.commitTransaction();
				
				apdu.setOutgoing();
				apdu.setOutgoingLength((short)1);
				apdu.sendBytesLong(new byte[]{(byte)apdu.getBuffer().length},(short)0, (short)1);
				break;
			}
			case Constant.PARAM_AVATAR: {
				short dataLength = apdu.getIncomingLength();
				short dataOffset = apdu.getOffsetCdata();
				Util.arrayCopy(buf, dataOffset, avatar, offsetIndexReceiveData, dataLength);
				offsetIndexReceiveData += dataLength;
				
				JCSystem.commitTransaction();
				
				byte[] leg = shortToByteArray((short)avatar.length, (short)0);
				
				sendResponse(apdu, leg);
				break;
			}
			case Constant.PARAM_HO_TEN: {
				avatarSize = (short)(offsetIndexReceiveData);
				avatarLengthRemain = avatarSize;
				offsetIndexReceiveData = (short)0;
				
				short nameLength = apdu.getIncomingLength();
				name = new byte[nameLength];
				Util.arrayCopy(buf,(short)(apdu.getOffsetCdata()),name,(short)0,nameLength);
				JCSystem.commitTransaction();
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_GIOI_TINH: {
				gender = buf[ISO7816.OFFSET_P2];
				JCSystem.commitTransaction();
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_NGAY_SINH: {
				short ngaySinhLength = apdu.getIncomingLength();
				date = new byte[ngaySinhLength];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,date,(short)0,ngaySinhLength);
				JCSystem.commitTransaction();
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_DIEN_THOAI: {
				short dienThoaiLength = apdu.getIncomingLength();
				phone = new byte[dienThoaiLength];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,phone,(short)0,dienThoaiLength);
				JCSystem.commitTransaction();
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_MSV: {
				short msvLength= apdu.getIncomingLength();
				studentId = new byte[msvLength];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,studentId,(short)0,msvLength);
				JCSystem.commitTransaction();
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_LOP: {
				short lopLength = apdu.getIncomingLength();
				classSV = new byte[lopLength];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,classSV,(short)0,lopLength);
				JCSystem.commitTransaction();
				
				sendResponse(apdu, new byte[1]);
				break;
			}
		}
	}
	
	private void checkPin(byte[] buf, APDU apdu) {
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
	}
	
	/** 
	*/
	private void resetInfo() {
		JCSystem.beginTransaction();
		// reset bien
		pinCreated = false;
		pinCounter = 1;
		infoInit = false;
		offsetIndexReceiveData = (short)0;
		offsetIndexSendData = (short)0;
		avatarLengthRemain = (short)0;
		temp = null;
		// reset thong tin 
		pin = new byte[Constant.PIN_WRAPPER_LENGTH];
		avatar = new byte[Constant.AVATAR_LENGTH];
		id = (byte)0x00;
		name = null;
		date = null;
		phone = null;
		studentId = null;
		classSV = null;
		JCSystem.commitTransaction();
	}
}
