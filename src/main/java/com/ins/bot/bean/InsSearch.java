package com.ins.bot.bean;

import java.util.List;

public class InsSearch {
	
	private String username;//用户名
	
	private String fullname;//全名
	
	private String pic_url;//头像地址
	
	private String display_url;//卡片展示地址
	
	List<Child> children;//多图
	
	List<NodeSearch> nodes;//下载图片节点
	
	private Boolean is_video;//是不是视频
	
	private String video_url;//视频下载地址
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getPic_url() {
		return pic_url;
	}

	public void setPic_url(String pic_url) {
		this.pic_url = pic_url;
	}

	public String getDisplay_url() {
		return display_url;
	}

	public void setDisplay_url(String display_url) {
		this.display_url = display_url;
	}

	public List<NodeSearch> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeSearch> nodes) {
		this.nodes = nodes;
	}

	public Boolean getIs_video() {
		return is_video;
	}

	public void setIs_video(Boolean is_video) {
		this.is_video = is_video;
	}

	public String getVideo_url() {
		return video_url;
	}

	public void setVideo_url(String video_url) {
		this.video_url = video_url;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}

	@Override
	public String toString() {
		return "InsSearch [username=" + username + ", fullname=" + fullname + ", pic_url=" + pic_url + ", display_url="
				+ display_url + ", nodes=" + nodes + ", is_video=" + is_video + ", video_url=" + video_url + "]";
	}
}
