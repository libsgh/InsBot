package com.ins.bot.media;

import java.io.File;

public interface Uploader {
	
	String upload(File file, String uploadPath, String username);
	
	String type();
	
}
