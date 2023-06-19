package project;
public class LichSuMuonSach {
	// NOTE: trang thai gui xe hien tai thi lay tu thang cuoi cung
	public static final short MAX_RECORD = (short)5;
	private short currentIndex;
	private boolean isEmpty;
	
	private SachRecord[] lichSu;
	
	public LichSuMuonSach() {
		lichSu = new SachRecord[MAX_RECORD];
		currentIndex = (short)0;
		isEmpty = true;
	}
	
	public SachRecord[] getLichSu() {
		return lichSu;
	}
	
	public void addLichSuMoi(SachRecord record) {
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
		isEmpty = false;
	}
	
	public short getTrangThaiXeHienTai() {
		return lichSu[currentIndex].getTrangThai();
	}
	
	public short getCurrentIndex() {
		return currentIndex;
	}
	
	public boolean isEmpty() {
		return isEmpty;
	}
}
