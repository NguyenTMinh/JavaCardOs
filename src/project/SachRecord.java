package project;
public class SachRecord {
	public static final short TRA_SACH_STATUS = (short)1;
	public static final short MUON_SACH_STATUS = (short)0;
	
	private short trangThai;
	private byte[] thoiGian;
	private short idSach;
	
	public SachRecord(byte[] time, short status, short id) {
		trangThai = status;
		thoiGian = time;
		idSach = id;
	}
	
	public byte[] getThoiGian() {
		return thoiGian;
	}
	
	public short getTrangThai() {
		return trangThai;
	}
	
	public short getIdSach() {
		return idSach;
	}
}
