package project;

public class Constant {
	// Danh sach cac instruction trong the ho tro
	// minh: ins tao ma pin cho the
	public static final byte INS_CREATE_PIN = (byte)0x00;
	// minh: ins thay doi ma pin the
	public static final byte INS_CHANGE_PIN = (byte)0x01;
	// minh: ins kiem tra ma pin
	public static final byte INS_CHECK_PIN = (byte)0x02;
	// minh: chuc nang unlock the
	public static final byte INS_UNLOCK_CARD = (byte)0x03;
	// minh: chuc nang nap du lieu
	public static final byte INS_FLUSH_DATA = (byte)0x04;
	// minh: chuc nang lay thong tin
    public static final byte INS_GET_DATA = (byte)0x05;
    // sua thong tin tren the
    public static final byte INS_EDIT_DATA = (byte)0x06;
    // reset trang the
    public static final byte INS_RESET_DATA = (byte)0x07;
    // cap nhat trang thai gui xe, lich su gui xe
    public static final byte INS_CHECK_IN_VEHICLE = (byte)0x08;
    // valid id the
    public static final byte INS_VALID_ID_CARD= (byte)0x09;
	
	//====================================================
	public static final byte CLA = (byte) 0x00;
	public static final short PIN_LENGTH = (short)4;
	public static final short PIN_WRAPPER_LENGTH = (short)16;
	public static final byte PIN_DEFAULT = (byte) 0x2E;
	public static final short MAX_PIN_COUNTER = (short)5;
	public static final byte[] SECRET_UNLOCK_KEY = {0x33, 0x33, 0x30, 0x32};
	public static final short AVATAR_LENGTH = (short)32767;
	public static final short MAX_SIZE_APDU = (short)255;
	public static final short MAX_SIZE_TEXT = (short) 1000;
	
	//====================================================
	public static final byte[] RESPONSE_SUCCESS = new byte[] {(byte)0x90, (byte)0x00};
	public static final byte[] RESPONSE_PIN_ALREADY_CREATED = new byte[] {(byte)0x2A};
	public static final byte[] RESPONSE_PIN_CREATE_SUCCESS = new byte[] {(byte)0x2B};
	public static final byte[] RESPONSE_PIN_CHECK_TRUE = new byte[] {(byte)0x2C}; 
	public static final byte[] RESPONSE_PIN_CHECK_FALSE = new byte[] {(byte)0x2D};
	public static final byte[] RESPONSE_PIN_CHECK_REACH_LIMIT = new byte[] {(byte)0x2E};
	public static final byte[] RESPONSE_RESET_INFO_SUCCESS = new byte[] {(byte)0x2F};
	public static final byte[] RESPONSE_RESET_INFO_FAIL = new byte[] {(byte)0x30};
	public static final byte[] RESPONSE_GUI_XE_OK = new byte[] {(byte)0x31};
	
	//====================================================
	// Khoa AES
	public static final byte[] KEY_AES = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};
	
	//================= Param truyen khi nap data card =========
	public static final byte PARAM_HO_TEN = (byte)0x00;
    public static final byte PARAM_GIOI_TINH = (byte)0x01;
    public static final byte PARAM_NGAY_SINH = (byte)0x02;
    public static final byte PARAM_DIEN_THOAI = (byte)0x03;
    public static final byte PARAM_MSV = (byte)0x04;
    public static final byte PARAM_LOP = (byte)0x05;
    public static final byte PARAM_AVATAR = (byte)0x06;
    public static final byte PARAM_ID = (byte)0x07;
    
}
