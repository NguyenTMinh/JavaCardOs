package project;

public class GuiXeRecord {
	// Bieu dien thoi gian theo quy tac mang 7 byte, theo quy tac
	// byte dau tuong trung cho ngay,
	// byte 2 tuong trung cho thang
	// byte 3,4 tuong trung cho nam
	// byte 5 tuong trung cho gio
	// byte 6 tuong trung cho phut
	// byte 7 tuong trung cho giay
	public static final short GUI_XE_STATUS = (short)1;
	public static final short LAY_XE_STATUS = (short)0;
	public static final short INDEX_NGAY = (short)0;
	public static final short INDEX_THANG = (short)1;
	public static final short INDEX_NAM_0 = (short)2;
	public static final short INDEX_NAM_1 = (short)3;
	public static final short INDEX_GIO = (short)4;
	public static final short INDEX_PHUT = (short)5;
	public static final short INDEX_GIAY = (short)6;
	
	private byte[] thoiGian;
	private short trangThai;
	
	public GuiXeRecord(byte[] time, short status) {
		thoiGian = time;
		trangThai = status;
	}
	
	public void setThoiGian(byte[] time) {
		thoiGian = time;
	}
	
	public void setTrangThai(short status) {
		trangThai = status;
	}
	
	public byte[] getThoiGian() {
		return thoiGian;
	}
	
	public short getTrangThai() {
		return trangThai;
	}
}