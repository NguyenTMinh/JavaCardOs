package project;

public class LichSuGuiXe {
	// NOTE: trang thai gui xe hien tai thi lay tu thang cuoi cung
	public static final short MAX_RECORD = (short)5;
	private short currentIndex;
	
	private GuiXeRecord[] lichSu;
	
	public LichSuGuiXe() {
		lichSu = new GuiXeRecord[MAX_RECORD];
		currentIndex = (short)0;
	}
	
	public GuiXeRecord[] getLichSu() {
		return lichSu;
	}
	
	public void addLichSuMoi(GuiXeRecord record) {
		if (currentIndex >= MAX_RECORD - 1) {
			for (short i = 0; i < MAX_RECORD; i++) {
				if (i == (short)(MAX_RECORD - 1)) {
					lichSu[i] = record;
					break;
				} else {
					lichSu[i] = lichSu[(short)(i+1)];
				}
			}
		} else {
			lichSu[currentIndex] = record;
			currentIndex++;
		}
	}
	
	public short getTrangThaiXeHienTai() {
		return lichSu[currentIndex].getTrangThai();
	}
}
