package com.ins.bot.bean;

import org.springframework.data.annotation.Id;

public class GhCache {
	
	@Id
	private String name;
	
	private Integer size;
	
	private String user;
	
	private Boolean isValid;//size大于850M就设置为false

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public Boolean getIsValid() {
		return isValid;
	}

	public void setIsValid(Boolean isValid) {
		this.isValid = isValid;
	}
	
}
