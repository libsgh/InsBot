package com.ins.bot.media;

import java.io.File;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class MtUploader implements Uploader{

	@Override
	public String upload(File file, String uploadPath, String username) {
		String body = HttpRequest.post("https://chat.dianping.com/upload").form("file", file).execute().body();
		if(JSONUtil.isJson(body)) {
			JSONObject jo = JSONUtil.parseObj(body);
			if(jo.getInt("success") == 1) {
				return jo.getStr("path");
			}
		}
		return "";
	}

	@Override
	public String type() {
		return "美团";
	}

}
