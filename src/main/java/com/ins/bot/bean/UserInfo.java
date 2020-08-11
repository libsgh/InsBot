package com.ins.bot.bean;

import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.annotation.Id;

import cn.hutool.core.util.NumberUtil;

public class UserInfo {
	
	private String id;//id
	
	private String full_name;//全称
	
	private String edge_followed_by;//粉丝
	
	private String edge_follow;//关注
	
	private String profile_pic_url_hd;//高清头像
	
	@Id
	private String username;//用户名
	
	private String tiez;//帖子数
	
	private boolean is_verified;//是否认证
	
	private String biography;//自我描述
	
	private String external_url;//三方链接
	
	private String lastTimeF;//最近一次更新时间
	
	private Long lastTime;//最近一次更新时间

	private String query_hash;//查询hash
	
	private String query_hash2;//查询hash2
	
	private String query_hash3;//查询hash3(快拍)
	
	private List<Reel> reels;//快拍
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFull_name() {
		return full_name;
	}

	public void setFull_name(String full_name) {
		this.full_name = full_name;
	}

	public String getEdge_followed_by() {
		return edge_followed_by;
	}

	public void setEdge_followed_by(Long edge_followed_by) {
		this.edge_followed_by = this.formatNumber(edge_followed_by);
	}

	public String getEdge_follow() {
		return edge_follow;
	}

	public void setEdge_follow(Long edge_follow) {
		this.edge_follow = this.formatNumber(edge_follow);
	}

	public String getProfile_pic_url_hd() {
		return profile_pic_url_hd;
	}

	public void setProfile_pic_url_hd(String profile_pic_url_hd) {
		this.profile_pic_url_hd = profile_pic_url_hd;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getTiez() {
		return tiez;
	}

	public void setTiez(Long tiez) {
		this.tiez = this.formatNumber(tiez);
	}


	public boolean getIs_verified() {
		return is_verified;
	}

	public void setIs_verified(boolean is_verified) {
		this.is_verified = is_verified;
	}

	public String getBiography() {
		return biography;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public String getQuery_hash() {
		return query_hash;
	}

	public void setQuery_hash(String query_hash) {
		this.query_hash = query_hash;
	}

	public String getQuery_hash2() {
		return query_hash2;
	}

	public void setQuery_hash2(String query_hash2) {
		this.query_hash2 = query_hash2;
	}

	public String getExternal_url() {
		return external_url;
	}

	public void setExternal_url(String external_url) {
		this.external_url = external_url;
	}
	
	
	public String getQuery_hash3() {
		return query_hash3;
	}

	public void setQuery_hash3(String query_hash3) {
		this.query_hash3 = query_hash3;
	}

	public List<Reel> getReels() {
		return reels;
	}

	public void setReels(List<Reel> reels) {
		this.reels = reels;
	}

	public String getLastTimeF() {
		return lastTime == null?"":Node.formateTimestamp(lastTime);
	}

	public void setLastTimeF(String lastTimeF) {
		this.lastTimeF = lastTimeF;
	}

	public Long getLastTime() {
		return lastTime;
	}

	public void setLastTime(Long lastTime) {
		this.lastTime = lastTime;
	}

	@Override
	public String toString() {
		return "UserInfo [id=" + id + ", full_name=" + full_name + ", edge_followed_by=" + edge_followed_by
				+ ", edge_follow=" + edge_follow + ", profile_pic_url_hd=" + profile_pic_url_hd + ", username="
				+ username + ", tiez=" + tiez + ", is_verified=" + is_verified + ", biography=" + biography
				+ ", external_url=" + external_url + ", lastTimeF=" + lastTimeF + ", query_hash=" + query_hash
				+ ", query_hash2=" + query_hash2 + ", query_hash3=" + query_hash3 + ", reels=" + reels + "]";
	}

	public String formatNumber(Long count) {
		String result = "";
		if(count < 1000) {
			result = ""+count;
		}else if(count>=1000 && count<1000000) {
			Double d = NumberUtil.div(count.doubleValue(), 1000);
			result = NumberUtil.roundStr(d, 1, RoundingMode.DOWN)+"千";
		}else if(count>=1000000 && count<100000000) {
			Double d = NumberUtil.div(count.doubleValue(), 1000000);
			result = NumberUtil.roundStr(d, 1, RoundingMode.DOWN)+"百万";
		}else if(count>=100000000) {
			Double d = NumberUtil.div(count.doubleValue(), 100000000);
			result = NumberUtil.roundStr(d, 1, RoundingMode.DOWN)+"亿";
		}
		return result;
	}
	
}
