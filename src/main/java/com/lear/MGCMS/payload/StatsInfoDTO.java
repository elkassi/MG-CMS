package com.lear.MGCMS.payload;

public class StatsInfoDTO {

	private String info;
    private Long value1;
    private Long value2;

    public StatsInfoDTO(String info, Long value1, Long value2) {
        this.info = info;
        this.value1 = value1;
        this.value2 = value2;
    }

	public StatsInfoDTO(String info, Long value1) {
		super();
		this.info = info;
		this.value1 = value1;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Long getValue1() {
		return value1;
	}

	public void setValue1(Long value1) {
		this.value1 = value1;
	}

	public Long getValue2() {
		return value2;
	}

	public void setValue2(Long value2) {
		this.value2 = value2;
	}

	
}
