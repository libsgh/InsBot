package com.ins.bot.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Child {
	
	private String display_url;
	
	private Boolean is_video;
	
	private String video_url;
	
}
