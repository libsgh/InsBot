package com.ins.bot.bean;

import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;

public class Node {
	
	private String userId;//uid
	
	private String uname;//uname
	
	private List<ThumbnailResources> srcs;
	
	private String shortcode;
	
	private String display_url;
	
	private String text;
	
	private Boolean is_video;
	
	private String video_url;
	
	@Id
	private String id;
	
	private List<Child> children;
	
	private String commentCount;//评论数
	
	private String likeCount;//赞
	
	private Long timestamp;//时间戳
	
	private String timeF;//时间戳（格式化）
	
	private String srcset;

	private String soureType;//ins(原始地址),cache(已缓存)
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUname() {
		return uname;
	}

	public void setUname(String uname) {
		this.uname = uname;
	}

	public List<ThumbnailResources> getSrcs() {
		return srcs;
	}

	public void setSrcs(List<ThumbnailResources> srcs) {
		this.srcs = srcs;
	}

	public String getShortcode() {
		return shortcode;
	}

	public void setShortcode(String shortcode) {
		this.shortcode = shortcode;
	}

	public String getDisplay_url() {
		return display_url;
	}

	public void setDisplay_url(String display_url) {
		this.display_url = display_url;
	}

	public Boolean getIs_video() {
		return is_video;
	}

	public void setIs_video(Boolean is_video) {
		this.is_video = is_video;
	}

	public String getText() {
		return text == null?"":text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getVideo_url() {
		return video_url;
	}

	public void setVideo_url(String video_url) {
		this.video_url = video_url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}

	public String getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(Long commentCount) {
		this.commentCount = this.formatNumber(commentCount);
	}
	public String getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Long likeCount) {
		this.likeCount = this.formatNumber(likeCount);
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public String getTimeF() {
		return formateTimestamp(this.timestamp*1000);
	}

	public void setTimeF(String timeF) {
		this.timeF = timeF;
	}

	public String getSoureType() {
		return soureType;
	}

	public void setSoureType(String soureType) {
		this.soureType = soureType;
	}

	public String getSrcset() {
		List<ThumbnailResources> list = this.srcs;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			ThumbnailResources thumbnailResources = list.get(i);
			if(i == list.size()-1) {
				sb.append(thumbnailResources.getSrc()+ " "+ thumbnailResources.getConfig_width()+"w");
			}else{
				sb.append(thumbnailResources.getSrc()+ " "+ thumbnailResources.getConfig_width()+"w,");
			}
		}
		return sb.toString();
	}

	public void setSrcset(String srcset) {
		this.srcset = srcset;
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
	public static String formateTimestamp(Long timestamp) {
		String result = "";
		int minute = 1000 * 60;
		int hour = minute * 60;
		int day = hour * 24;
		int week = day * 7;
		int month = day * 30;
	    Long now = new Date().getTime();
	    Long diffValue = now - timestamp;
	    if(diffValue < 0){
	        return result;
	    }
	    long minC = diffValue/minute;
	    long hourC = diffValue/hour;
	    long dayC = diffValue/day;
	    long weekC = diffValue/week;
	    long monthC = diffValue/month;
	    if(monthC >= 1 && monthC <= 3){
	        result = monthC + "月前";
	    }else if(weekC >= 1 && weekC <= 3){
	        result = weekC + "周前";
	    }else if(dayC >= 1 && dayC <= 6){
	        result = dayC + "天前";
	    }else if(hourC >= 1 && hourC <= 23){
	        result = hourC + "小时前";
	    }else if(minC >= 1 && minC <= 59){
	        result = minC + "分钟前";
	    }else if(diffValue >= 0 && diffValue <= minute){
	        result = "刚刚";
	    }else {
	    	if(DateUtil.year(new Date(timestamp)) == DateUtil.year(new Date())) {
	    		result = DateUtil.format(new Date(timestamp), "MM月dd日");
	    	}else{
	    		result = DateUtil.format(new Date(timestamp), "yyyy年MM月dd日");
	    	}
	    }
	    return result;
	}
}
