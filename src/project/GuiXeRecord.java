package project;

public class GuiXeRecord {
	
	public static final short GUI_XE_STATUS = (short)1;
	public static final short LAY_XE_STATUS = (short)0;
	
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