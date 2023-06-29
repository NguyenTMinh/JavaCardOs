package project;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class CardService extends Applet
{
	// id the mac dinh
	private static final byte[] idCard = new byte[] {(byte)0x30, (byte)0x32, (byte)0x35, (byte)0x36, (byte)0x34};
	// Khoa rsa 
	private RSAPrivateKey rsaPrivKey;
	private RSAPublicKey rsaPubKey;
	private Signature rsaSig;
	private byte[] s1, s2, s3, sig_buffer;
	private short sigLen;
	
	// Ham bam
	private MessageDigest sha;
	
	// Cac truong thong tin luu tru
	private static byte[] pin;
	// Thong tin sinh vine
	private static byte id;
	private static byte[] avatar;
	private static short avatarLen;
	private static byte[] name;
	private static short nameLen;
	private static byte gender;
	private static byte[] date;
	private static short dateLen;
	private static byte[] phone;
	private static short phoneLen;
	private static byte[] studentId;
	private static short studentIdLen;
	private static byte[] classSV;
	private static short classSVLen;
	
	// Thong tin lich su gui xe
	private static LichSuGuiXe historyVehicle;
	private static short indexLSX = (short)0;
	private static short indexLSS = (short)0;
	private static LichSuMuonSach historyBook;
	
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
	private static boolean doneGetData = true;
	private static byte[] temp; // temp tam de tra avatar
	private static byte[] avatarDecryptTemp; // mang chua avatar duoc giai ma tam thoi, chi dung khi gui thong tin len app, xoa ngay lap tuc khi khong dung
	
	private CardService() {
		aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
		cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		pin = new byte[Constant.PIN_WRAPPER_LENGTH];
		pinCounter = 1;
		avatar = new byte[Constant.AVATAR_LENGTH];
		historyVehicle = new LichSuGuiXe();
		historyBook = new LichSuMuonSach();
		// RSA
		createRsaVariables();
		// HASH
		sha = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
	}
	
	private void createRsaVariables() {
		// Khoi tao cac bien rsa
		sigLen = (short)(KeyBuilder.LENGTH_RSA_1024/8);
		sig_buffer = new byte[sigLen];
		
		rsaSig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
		rsaPrivKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, (short)(8*sigLen), false);
		rsaPubKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, (short)(8*sigLen), false);
		
		KeyPair keyPair = new KeyPair(KeyPair.ALG_RSA,(short)(8*sigLen));
		keyPair.genKeyPair();
		rsaPrivKey = (RSAPrivateKey)keyPair.getPrivate();
		rsaPubKey = (RSAPublicKey)keyPair.getPublic();
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new CardService().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
			// decrypt avatar truoc da
			if (avatarDecryptTemp == null && doneGetData) {
				avatarDecryptTemp = aesDecrypt(avatar);
			}
			switch(choice) {
			case Constant.PARAM_ID:{
				doneGetData = false;
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
				Util.arrayCopy(avatarDecryptTemp, offsetIndexSendData, temp, (short)0, (short)temp.length);
				offsetIndexSendData += (short)temp.length;
				avatarLengthRemain = (short)(avatarLengthRemain-temp.length);
				
				sendResponse(apdu, temp);
				break;
			}
			case Constant.PARAM_HO_TEN: {
				avatarDecryptTemp = null;
				temp = null;
				avatarLengthRemain = avatarSize;
				offsetIndexSendData = (short)0;
				// giai ma thong tin bi ma hoa roi moi gui len
				byte[] temp = aesDecrypt(name);
				
				sendResponse(apdu, temp, nameLen);
				temp = null;
				break;
			}
			case Constant.PARAM_GIOI_TINH: {
				sendResponse(apdu, new byte[]{gender});
				
				break;
			}
			case Constant.PARAM_NGAY_SINH: {
				// giai ma thong tin bi ma hoa roi moi gui len
				byte[] temp = aesDecrypt(date);
				sendResponse(apdu, temp, dateLen);
				temp = null;
				break;
			}
			case Constant.PARAM_DIEN_THOAI: {
				byte[] temp = aesDecrypt(phone);
				sendResponse(apdu, temp, phoneLen);
				temp = null;
				break;
			}
			case Constant.PARAM_MSV: {
				byte[] temp = aesDecrypt(studentId);
				sendResponse(apdu, temp, studentIdLen);
				temp = null;
				break;
			}
			case Constant.PARAM_LOP: {
				byte[] temp = aesDecrypt(classSV);
				sendResponse(apdu, temp, classSVLen);
				temp = null;
				doneGetData = true;
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
			short trangThai = buf[ISO7816.OFFSET_P1];
			byte[] thoiGian = new byte[7];
			Util.arrayCopy(buf, (short)ISO7816.OFFSET_CDATA, thoiGian, (short)0, (short)7);
			historyVehicle.addLichSuMoi(new GuiXeRecord(thoiGian, trangThai));
			
			sendResponse(apdu, Constant.RESPONSE_GUI_XE_OK);
			break;
		}
		case Constant.INS_BOOK: {
			short trangThai = buf[ISO7816.OFFSET_P1];
			short id = buf[ISO7816.OFFSET_P2];
			byte[] thoiGian = new byte[7];
			Util.arrayCopy(buf, (short)ISO7816.OFFSET_CDATA, thoiGian, (short)0, (short)7);
			historyBook.addLichSuMoi(new SachRecord(thoiGian, trangThai, id));
			
			sendResponse(apdu, Constant.RESPONSE_GUI_XE_OK);
			break;
		}
		case Constant.INS_GET_PUB_KEY_RSA: {
			byte choice = buf[ISO7816.OFFSET_P1];
			switch (choice) {
			case Constant.PARAM_MODULUS: {
				byte[] pubKey = new byte[(short) (sigLen * 2)];
				short pubKeyLen = rsaPubKey.getModulus(pubKey, (short)0);
				sendResponse(apdu, pubKey, pubKeyLen);
				break;
			}
			case Constant.PARAM_EXPONENT: {
				byte[] pubKey = new byte[(short) (sigLen * 2)];
				short pubKeyLen = rsaPubKey.getExponent(pubKey, (short)0);
				sendResponse(apdu, pubKey, pubKeyLen);
				break;
			}
			}
			
			break;
		}
		case Constant.INS_CHALLENGE_CARD: {
			short len = apdu.getIncomingLength();
			byte[] data = new byte[len];
			byte[] hash = new byte[255];
			
			Util.arrayCopy(buf, (short)(apdu.getOffsetCdata()), data, (short)0, len);
			short ret = sha.doFinal(data, (short)0, len, hash, (short)0);
			// sendResponse(apdu, hash, ret);
			rsaSign(apdu, hash, ret);
			break;
		}
		case Constant.INS_GET_LS_XE: {
			byte[] respone = new byte[9];
			if (historyVehicle.isEmpty()) {
				sendResponse(apdu, new byte[]{(byte)0x00});
				break;
			}
			GuiXeRecord rc = historyVehicle.getLichSu()[indexLSX];
			if (rc == null) {
				indexLSX = (short)0;
				sendResponse(apdu, new byte[]{(byte)0x00});
				break;
			}
			if (historyVehicle.getCurrentIndex() <= indexLSX) {
				indexLSX = (short)0;
				respone[0] = (byte)0x00;
				respone[1] = (byte)(rc.getTrangThai());
				respone[2] = rc.getThoiGian()[Constant.INDEX_NGAY];
				respone[3] = rc.getThoiGian()[Constant.INDEX_THANG];
				respone[4] = rc.getThoiGian()[Constant.INDEX_NAM_0];
				respone[5] = rc.getThoiGian()[Constant.INDEX_NAM_1];
				respone[6] = rc.getThoiGian()[Constant.INDEX_GIO];
				respone[7] = rc.getThoiGian()[Constant.INDEX_PHUT];
				respone[8] = rc.getThoiGian()[Constant.INDEX_GIAY];
				
				sendResponse(apdu, respone);
			} else {
				indexLSX++;
				
				respone[0] = Constant.RESPONSE_HAS_NEXT;
				respone[1] = (byte)rc.getTrangThai();
				respone[2] = rc.getThoiGian()[Constant.INDEX_NGAY];
				respone[3] = rc.getThoiGian()[Constant.INDEX_THANG];
				respone[4] = rc.getThoiGian()[Constant.INDEX_NAM_0];
				respone[5] = rc.getThoiGian()[Constant.INDEX_NAM_1];
				respone[6] = rc.getThoiGian()[Constant.INDEX_GIO];
				respone[7] = rc.getThoiGian()[Constant.INDEX_PHUT];
				respone[8] = rc.getThoiGian()[Constant.INDEX_GIAY];
				
				sendResponse(apdu, respone);
			}
			break;
		}
		case Constant.INS_GET_LS_SACH: {
			byte[] respone = new byte[10];
			if (historyBook.isEmpty()) {
				sendResponse(apdu, new byte[]{(byte)0x00});
				break;
			}
			SachRecord rc = historyBook.getLichSu()[indexLSS];
			if (rc == null) {
				indexLSS = (short)0;
				sendResponse(apdu, new byte[]{(byte)0x00});
				break;
			}
			if (historyBook.getCurrentIndex() <= indexLSS) {
				indexLSS = (short)0;
				respone[0] = (byte)0x00;
				respone[1] = (byte)(rc.getTrangThai());
				respone[2] = rc.getThoiGian()[Constant.INDEX_NGAY];
				respone[3] = rc.getThoiGian()[Constant.INDEX_THANG];
				respone[4] = rc.getThoiGian()[Constant.INDEX_NAM_0];
				respone[5] = rc.getThoiGian()[Constant.INDEX_NAM_1];
				respone[6] = rc.getThoiGian()[Constant.INDEX_GIO];
				respone[7] = rc.getThoiGian()[Constant.INDEX_PHUT];
				respone[8] = rc.getThoiGian()[Constant.INDEX_GIAY];
				respone[9] = (byte)(rc.getIdSach());
				
				sendResponse(apdu, respone);
			} else {
				indexLSS++;
				
				respone[0] = Constant.RESPONSE_HAS_NEXT;
				respone[1] = (byte)rc.getTrangThai();
				respone[2] = rc.getThoiGian()[Constant.INDEX_NGAY];
				respone[3] = rc.getThoiGian()[Constant.INDEX_THANG];
				respone[4] = rc.getThoiGian()[Constant.INDEX_NAM_0];
				respone[5] = rc.getThoiGian()[Constant.INDEX_NAM_1];
				respone[6] = rc.getThoiGian()[Constant.INDEX_GIO];
				respone[7] = rc.getThoiGian()[Constant.INDEX_PHUT];
				respone[8] = rc.getThoiGian()[Constant.INDEX_GIAY];
				respone[9] = (byte)(rc.getIdSach());
				
				sendResponse(apdu, respone);
			}
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
	
	private void sendResponse(APDU apdu, byte[] data, short len) {
		apdu.setOutgoing();
		apdu.setOutgoingLength(len);
		apdu.sendBytesLong(data, (short)0, len);
	}
	
	private void flushInfo(APDU apdu, byte[] buf) {
		byte choice = buf[ISO7816.OFFSET_P1];
		switch (choice) {
			case Constant.PARAM_ID: {
				id = buf[ISO7816.OFFSET_P2];
				
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
				
				byte[] leg = shortToByteArray((short)avatar.length, (short)0);
				
				sendResponse(apdu, leg);
				break;
			}
			case Constant.PARAM_HO_TEN: {
				// Chuyen het avatar xuong roi moi ma hoa - start
				avatarSize = (short)(offsetIndexReceiveData);
				avatarLengthRemain = avatarSize;
				offsetIndexReceiveData = (short)0;
				
				byte[] temp2 = aesEncrypt(avatar);
				copy(temp2, avatar);
				temp2 = null;
				avatarLen = avatarSize;
				// ma hoa avatar - end
				
				nameLen = apdu.getIncomingLength();
				name = new byte[getLengthForEncrypt(nameLen)];
				Util.arrayCopy(buf,(short)(apdu.getOffsetCdata()),name,(short)0,nameLen);
				// Luu du lieu da duoc ma hoa
				name = aesEncrypt(name);
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_GIOI_TINH: {
				gender = buf[ISO7816.OFFSET_P2];
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_NGAY_SINH: {
				dateLen = apdu.getIncomingLength();
				date = new byte[getLengthForEncrypt(dateLen)];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,date,(short)0,dateLen);
				// Luu thong tin ma hoa
				date = aesEncrypt(date);
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_DIEN_THOAI: {
				phoneLen = apdu.getIncomingLength();
				phone = new byte[getLengthForEncrypt(phoneLen)];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,phone,(short)0,phoneLen);
				// Luu thong tin ma hoa
				phone = aesEncrypt(phone);
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_MSV: {
				studentIdLen = apdu.getIncomingLength();
				studentId = new byte[getLengthForEncrypt(studentIdLen)];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,studentId,(short)0,studentIdLen);
				// Luu thong tin ma hoa
				studentId = aesEncrypt(studentId);
				
				sendResponse(apdu, new byte[1]);
				break;
			}
			case Constant.PARAM_LOP: {
				classSVLen = apdu.getIncomingLength();
				classSV = new byte[getLengthForEncrypt(classSVLen)];
				Util.arrayCopy(buf,(short)ISO7816.OFFSET_CDATA,classSV,(short)0,classSVLen);
				// Luu thong tin ma hoa
				classSV = aesEncrypt(classSV);
				
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
	* Xoa sach thong tin trong the
	*/
	private void resetInfo() {
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
		Util.arrayFillNonAtomic(avatar, (short) 0, (short) avatar.length, (byte) 0x00);
		id = (byte)0x00;
		name = null;
		date = null;
		phone = null;
		studentId = null;
		classSV = null;
		//
		historyBook = new LichSuMuonSach();
		historyVehicle = new LichSuGuiXe();
	}
	
	/** 
	* Ki doan du lieu data bang rsa va gui di
	*/
	private void rsaSign(APDU apdu, byte[] data, short dataLen) {
		rsaSig.init(rsaPrivKey, Signature.MODE_SIGN);
		short ret = rsaSig.sign(data, (short)0, dataLen, sig_buffer, (short)0);
		apdu.setOutgoing();
		apdu.setOutgoingLength(ret);
		apdu.sendBytesLong(sig_buffer, (short)0, ret);
	}
	
	private short getLengthForEncrypt(short initLeng) {
		short st = (short)16;
		if (initLeng % st == (short)0) {
			return initLeng;
		} else {
			return (short)(((int)initLeng / st) * st + st);
		}
	}
	
	// Ma hoa aes dau vao
	private byte[] aesEncrypt(byte[] input) {
		byte[] output = new byte[(short)input.length];
		
		aesKey.setKey(Constant.KEY_AES, (short)0);
		cipher.init(aesKey, Cipher.MODE_ENCRYPT);
		cipher.doFinal(input, (short)0, (short)input.length, output, (short)0);
		
		return output;
	}
	
	// Giai ma aes
	private byte[] aesDecrypt(byte[] input) {
		byte[] output = new byte[(short)input.length];
		
		aesKey.setKey(Constant.KEY_AES, (short)0);
		cipher.init(aesKey, Cipher.MODE_DECRYPT);
		cipher.doFinal(input, (short)0, (short)input.length, output, (short)0);
		
		return output;
	}
	
	private void copy(byte[] src, byte[] dest) {
		for (short i = 0; i < dest.length; i++) {
			dest[i] = src[i];
		}
	}
}
