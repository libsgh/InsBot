package com.ins.bot.bean;

public class MediaVariables {
	
	private String id;
	
	private Integer first;
	
	private String after;

	public MediaVariables(String id, String after) {
		super();
		this.id = id;
		this.first = 12;
		this.after = after;
	}
	public MediaVariables(String id) {
		super();
		this.id = id;
		this.first = 12;
		this.after = "";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getFirst() {
		return first;
	}

	public void setFirst(Integer first) {
		this.first = first;
	}

	public String getAfter() {
		return after;
	}

	public void setAfter(String after) {
		this.after = after;
	}
	
}
