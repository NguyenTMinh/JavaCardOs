package project;

public class Constant {
	// Danh sach cac instruction trong the ho tro
	// minh: ins tao ma pin cho the
	public static final byte INS_CREATE_PIN = (byte)0x00;
	// minh: ins thay doi ma pin the
	public static final byte INS_CHANGE_PIN = (byte)0x01;
	// minh: ins kiem tra ma pin
	public static final byte INS_CHECK_PIN = (byte)0x02;
	
	//====================================================
	public static final byte CLA = (byte) 0x00;
	public static final short PIN_LENGTH = (short)4;
	public static final short PIN_WRAPPER_LENGTH = (short)16;
	public static final byte PIN_DEFAULT = (byte) 0x2E;
	public static final short MAX_PIN_COUNTER = (short)5;
	
	//====================================================
	public static final byte[] RESPONSE_SUCCESS = new byte[] {(byte)0x90, (byte)0x00};
	public static final byte[] RESPONSE_PIN_ALREADY_CREATED = new byte[] {(byte)0x2A};
	public static final byte[] RESPONSE_PIN_CREATE_SUCCESS = new byte[] {(byte)0x2B};
	public static final byte[] RESPONSE_PIN_CHECK_TRUE = new byte[] {(byte)0x2C}; 
	public static final byte[] RESPONSE_PIN_CHECK_FALSE = new byte[] {(byte)0x2D};
	public static final byte[] RESPONSE_PIN_CHECK_REACH_LIMIT = new byte[] {(byte)0x2E};
	
	//====================================================
	// Khoa AES
	public static final byte[] KEY_AES = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};
}
