package com.ins.bot.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.cache.impl.LRUCache;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.util.StrUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class PicBed {
	
	protected static Logger logger = LoggerFactory.getLogger(PicBed.class);

	private static LRUCache<String, String> cache = new LRUCache<String, String>(1000);

	private static TimedCache<String, String> videoUrlCache = new TimedCache<String, String>(1 * DateUnit.DAY.getMillis());


	public static String upload(String displayUrl) {
		try {
			if(StrUtil.isBlank(displayUrl)){
				return displayUrl;
			}
			String fileName = getFileNameFromUrl(displayUrl);
			if(cache.containsKey(fileName)){
				return cache.get(fileName);
			}
			byte[] dataBytes = HttpUtil.downloadBytes(displayUrl);
			String body = HttpRequest.
					post("https://baijiahao.baidu.com/builderinner/api/content/file/upload").
					form("no_compress", 1).
					form("id", "WU_FILE_0").
					form("is_avatar", "0").
					form("media", dataBytes, fileName).
					execute().body();
			if(JSONUtil.isTypeJSON(body)) {
				JSONObject jo = JSONUtil.parseObj(body);
				if(jo.getInt("errno") == 0) {
					String url = jo.getByPath("$.ret.https_url", String.class);
					cache.put(fileName, url);
					return url;
				}
			}
		} catch (HttpException e) {
			logger.error(e.getMessage(), e);
		}
		return displayUrl;
	}

	public static String uploadVideo(String displayUrl) {
		try {
			if(StrUtil.isBlank(displayUrl)){
				return displayUrl;
			}
			String fileName = getFileNameFromUrl(displayUrl);
			if(videoUrlCache.containsKey(fileName)){
				return videoUrlCache.get(fileName);
			}
			byte[] dataBytes = HttpUtil.downloadBytes(displayUrl);
			String body = HttpRequest.
					post("https://streamja.com/shortId.php").
					form("new", 1).
					execute().body();
			if(JSONUtil.isTypeJSON(body)) {
				JSONObject jo = JSONUtil.parseObj(body);
				if(jo.getInt("status") == 1) {
					String uploadUrl = jo.getByPath("$.uploadUrl", String.class);
					body = HttpRequest.
							post("https://streamja.com" + uploadUrl).
							form("file", dataBytes, fileName).
							execute().body();
					jo = JSONUtil.parseObj(body);
					if(jo.getInt("status") == 1){
						String videoUrl = jo.getByPath("$.url", String.class);
						videoUrl = "/api/video"+ videoUrl;
						videoUrlCache.put(fileName, videoUrl);
						return videoUrl;
					}
				}
			}
		} catch (HttpException e) {
			logger.error(e.getMessage(), e);
		}
		return displayUrl;
	}

	public static String getVideoUrl(String videoCode){
		try {
			Document doc = Jsoup.connect("https://streamja.com/" + videoCode).get();
			String videoUrl = doc.select("#video_container video").select("source").attr("src");
			return videoUrl;
		}catch (Exception e){
			logger.error(e.getMessage(), e);
		}

		return "https://streamja.com/" + videoCode;
	}


	public static String getFileNameFromUrl(String url) {
		try {
			String suffixes="avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|pdf|rar|zip|docx|doc";
			Pattern pat=Pattern.compile("[\\w]+[\\.]("+suffixes+")");//正则判断
			Matcher mc=pat.matcher(url);//条件匹配
			while(mc.find()){
				return mc.group();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return "";
	}
}
