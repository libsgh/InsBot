package com.ins.bot.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ins.bot.bean.Child;
import com.ins.bot.bean.InsSearch;
import com.ins.bot.bean.MediaVariables;
import com.ins.bot.bean.Node;
import com.ins.bot.bean.Reel;
import com.ins.bot.bean.ThumbnailResources;
import com.ins.bot.bean.UserInfo;
import com.ins.bot.common.CompressKit;
import com.ins.bot.common.InstagramAes;
import com.ins.bot.media.CacheService;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

@Service
public class Ins{
	
	@Autowired
	private MongoTemplate template;
	
	@Value("${download.path}")
	private String path;
	
	static String indexUrl = "https://www.instagram.com/%s/";
	
	static String detailUrl = "https://www.instagram.com/p/%s/?taken-by=%s";
	
	protected static Logger logger = LoggerFactory.getLogger(Ins.class);
	
	@Value("${download.path}")
	private String downPath;
	
	@Autowired
	private CacheService cacheService;
	
	private String cookie;
	
	@Value("${IG_USER_NAME}")
	private String igUserName;
	
	@Value("${IG_USER_PASSWORD}")
	private String igUserPassword;
	
	@Value("${GITHUB_ACCESS_TOKEN}")
	private String accessToken;
	
	public void run() {
		try {
			List<UserInfo> list = template.findAll(UserInfo.class, "InsUserList");
			for (UserInfo ui : list) {
				this.excute(ui.getUsername(), null, false);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Async
	public void excute(String username, MediaVariables m, boolean flag) {
		Long time = System.currentTimeMillis();
		Document document = getMainDoc(username);
		if(document == null) {
			//用户可能不存在
			return;
		}
		UserInfo ui = getUserInfo(document, username);
		template.save(ui, "InsUserList");
		String hash = ui.getQuery_hash();
		MediaVariables mv = m != null?m:new MediaVariables(ui.getId());
		if(flag) {
			template.remove(new Query(Criteria.where("userId").is(ui.getId())), "InsUserData");
		}
		int count = 0;
		List<String> ids = new ArrayList<String>();
		load(ui, hash, mv, ids, flag, count);
		template.save(ui, "InsUserList");
		logger.info("ID："+ui.getUsername()+"，更新耗时："+(System.currentTimeMillis()-time)+"ms");
	}
	
	@SuppressWarnings({ "resource" })
	private void load(UserInfo ui, String hash, MediaVariables mv, List<String> ids, boolean flag, Integer count) {
		String variables = JSONUtil.toJsonStr(mv);
		String url = new Formatter().format("https://www.instagram.com/graphql/query/?query_hash=%s&variables=%s", hash, URLUtil.encodeAll(variables)).toString();
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("cookie", cookie);
		String result = HttpRequest.get(url).addHeaders(headers).execute().body();
		JSONObject jo = null;
		try {
			jo = JSONUtil.parseObj(result)
						.getJSONObject("data")
						.getJSONObject("user")
						.getJSONObject("edge_owner_to_timeline_media");
			JSONArray jr = jo.getJSONArray("edges");
			ui.setLastTime(jr.getJSONObject(0).getJSONObject("node").getLong("taken_at_timestamp")*1000);
			for (int i = 0; i < jr.size(); i++) {
				count++;
				Node node = new Node();
				String display_url = jr.getJSONObject(i).getJSONObject("node").getStr("display_url");
				String text = jr.getJSONObject(i).getJSONObject("node").getByPath("$.edge_media_to_caption.edges[0].node.text", String.class);
				Boolean is_video = jr.getJSONObject(i).getJSONObject("node").getBool("is_video");
				//String fileName = display_url.split("/")[display_url.split("/").length-1];
				//String u = cacheImgToSinaPicBed(display_url);
				String shortcode = jr.getJSONObject(i).getJSONObject("node").getStr("shortcode");
				Long timestamp = jr.getJSONObject(i).getJSONObject("node").getLong("taken_at_timestamp");
				String id = jr.getJSONObject(i).getJSONObject("node").getStr("id");
				JSONObject edge_sidecar_to_children = jr.getJSONObject(i).getJSONObject("node").getJSONObject("edge_sidecar_to_children");
				Long commentCount = jr.getJSONObject(i).getJSONObject("node").getJSONObject("edge_media_to_comment").getLong("count");
				Long likeCount = jr.getJSONObject(i).getJSONObject("node").getJSONObject("edge_media_preview_like").getLong("count");
				JSONArray list = jr.getJSONObject(i).getJSONObject("node").getJSONArray("thumbnail_resources");
				List<ThumbnailResources> srcs = new ArrayList<ThumbnailResources>();
				for (Object object : list) {
					ThumbnailResources tr = new ThumbnailResources();
					JSONObject trJson = (JSONObject)object;
					tr.setConfig_width(trJson.getInt("config_width"));
					tr.setConfig_height(trJson.getInt("config_height"));
					tr.setSrc(trJson.getStr("src"));
					srcs.add(tr);
					//thumbnailResources.setSrc(getChangedUrl(ui.getUsername(), thumbnailResources.getSrc()));
				}
				//没必要加载src，而且图片地址会过期
				//List<DisplaySrc> list = (List<DisplaySrc>) jr.getJSONObject(i).getJSONObject("node").get("display_resources");
				node.setUname(ui.getUsername());
				node.setIs_video(is_video);
				node.setSrcs(srcs);
				//node.setDisplay_url(getChangedUrl(ui.getUsername(), display_url));//getChangedUrl(ui.getUsername(), display_url)
				//String dUrl = n!=null && PicBed.isSuccess(n.getDisplay_url(), 0)?n.getDisplay_url():PicBed.uploadMT(display_url, path, ui.getUsername());
				node.setDisplay_url(display_url);//getChangedUrl(ui.getUsername(), display_url)
				node.setShortcode(shortcode);
				node.setText(StrUtil.utf8Str(text));
				node.setId(id);
				node.setCommentCount(commentCount);
				node.setLikeCount(likeCount);
				node.setTimestamp(timestamp);
				node.setUserId(ui.getId());
				node.setSoureType("ins");
				ids.add(id);
				if(is_video) {
					//String vUrl = n!=null && PicBed.isSuccess(n.getDisplay_url(), 1)?n.getVideo_url():getChangedUrl(ui.getUsername(), videoUrl(shortcode, ui.getQuery_hash2()));
					node.setVideo_url(videoUrl(shortcode, ui.getQuery_hash2()));
				}
				if(edge_sidecar_to_children != null) {
					JSONArray children = jr.getJSONObject(i).getJSONObject("node").getJSONObject("edge_sidecar_to_children").getJSONArray("edges");
					List<Child> chd = new ArrayList<Child>();
					for (Object object : children) {
						Child child = new Child();
						JSONObject j = JSONUtil.parseObj(object);
						String du = j.getByPath("$.node.display_url", String.class);
						Boolean isV = j.getByPath("$.node.is_video", Boolean.class);
						child.setIs_video(isV);
						if(isV) {
							child.setVideo_url((String)JSONUtil.getByPath(j, "$.node.video_url"));
						}
						child.setDisplay_url(du);
						chd.add(child);
					}
					node.setChildren(chd);
				}
				if(!flag) {
					Node n = template.findById(id, Node.class, "InsUserData");
					if(n!=null) {
						return;
					}
				}
				template.save(node, "InsUserData");
				logger.info(ui.getUsername()+"------"+count+"/"+ui.getTiez());
			}
			boolean has_next_page = jo.getJSONObject("page_info").getBool("has_next_page");
			String end_cursor = jo.getJSONObject("page_info").getStr("end_cursor");
			if(has_next_page) {
				mv.setAfter(end_cursor);
				load(ui, hash, mv, ids, flag, count);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			try {
				Thread.sleep(60000);
				load(ui, hash, mv, ids, flag, count);
			} catch (InterruptedException e1) {
			}
		}
		
	}
	
	public String getUrlByCid(Node n, String cId, Integer flag) {
		cn.hutool.json.JSONArray children = JSONUtil.parseArray(n.getChildren());
		for (Object object : children) {
			cn.hutool.json.JSONObject j = JSONUtil.parseObj(object);
			String id = (String)JSONUtil.getByPath(j, "$.node.id");
			if(cId.equals(id)) {
				if(flag == 1) {
					return (String)JSONUtil.getByPath(j, "$.node.video_url");
				}
				return (String)JSONUtil.getByPath(j, "$.node.display_url");
			}
		}
		return "";
	}
	
	private String getFileNameFromUrl(String url) {
    	try {
			String suffixes="avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|pdf|rar|zip|docx|doc";
			Pattern pat=Pattern.compile("[\\w]+[\\.]("+suffixes+")");//正则判断
			Matcher mc=pat.matcher(url);//条件匹配
			while(mc.find()){
				return mc.group();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.info(url);
		}
    	return "";
    }
	
	public Document getMainDoc(String username) {
		try {
			Document doc = Jsoup.connect(String.format(indexUrl, username).toString()).header("cookie", cookie).get();
			return doc;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}
	
	public UserInfo getUserInfo(Document doc, String username) {
		UserInfo oui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, "InsUserList");
		Elements scripts = doc.select("script");
		for (Element script : scripts) {
			if(script.html().contains("window._sharedData =")) {
				 String str = script.html().replace("\n", ""); //这里是为了解决 无法多行匹配的问题
			      String pattern = "window._sharedData = \\{(.*?)\\};"; //()必须加，
			      Pattern r = Pattern.compile(pattern,Pattern.MULTILINE);// Pattern.MULTILINE 好像没有什么用，所以才使用上面的replace
			      Matcher m = r.matcher(str);
			      if(m.find()){
			        JSONObject jj =  new JSONObject();
					try {
						String json = m.group().replaceAll("window._sharedData = ", "").replaceAll(";", "");
						jj = JSONUtil.parseObj(json).getJSONObject("entry_data")
						.getJSONArray("ProfilePage")
						.getJSONObject(0).getJSONObject("graphql").getJSONObject("user");
					} catch (Exception e) {
						e.printStackTrace();
					}
					UserInfo ui = new UserInfo();
					ui.setId(jj.getStr("id"));
					ui.setFull_name(jj.getStr("full_name"));
					ui.setUsername(jj.getStr("username"));
					ui.setEdge_followed_by(jj.getJSONObject("edge_followed_by").getLong("count"));
					ui.setEdge_follow(jj.getJSONObject("edge_follow").getLong("count"));
					//String url = cacheImgToSinaPicBed(jj.getString("profile_pic_url_hd"));
					if(oui != null) {
						if(this.needRefresh(oui.getProfile_pic_url_hd(), jj.getStr("profile_pic_url_hd"))) {
							ui.setProfile_pic_url_hd(cacheService.cache(jj.getStr("profile_pic_url_hd"), jj.getStr("username")));
						}else{
							ui.setProfile_pic_url_hd(oui.getProfile_pic_url_hd());
						}
					}else {
						ui.setProfile_pic_url_hd(cacheService.cache(jj.getStr("profile_pic_url_hd"), jj.getStr("username")));
					}
					ui.setTiez(jj.getJSONObject("edge_owner_to_timeline_media").getLong("count"));
					ui.setBiography(jj.getStr("biography"));
					ui.setExternal_url(jj.getStr("external_url"));
					ui.setIs_verified(jj.getBool("is_verified"));
					String js = doc.select("script").parallelStream().filter(c -> c.attr("src").contains("Consumer.js")).collect(Collectors.toList()).get(0).attr("src");
					String url = "https://www.instagram.com" + js;
					String content =  HttpRequest.get(url).execute().body();
					String hash = StrUtil.subBetween(content, "void 0:s.pagination},queryId:\"", "\"");
					if(StrUtil.isBlank(hash)) {
						hash = StrUtil.subBetween(content, "void 0:l.pagination},queryId:\"", "\"");
					}
					//FileUtil.writeString(content, "/home/single/Desktop/1.txt", "UTF-8");
					String hash2 = StrUtil.subBetween(content, "()=>s(o(t))}})}}Object.defineProperty(e,'__esModule',{value:!0});const s=\"", "\"");
					if(StrUtil.isBlank(hash2)) {
						hash2 = StrUtil.subBetween(content, "function(){return c(o(t))}}})}}Object.defineProperty(e,'__esModule',{value:!0});var c=\"", "\"");
					}
					String hash3 = StrUtil.subBetween(content, "{return s(o.next(t,()=>s(n(t)),!1))}}Object.defineProperty(e,'__esModule',{value:!0});const s=\"", "\"");
					if(StrUtil.isBlank(hash3)) {
						hash3 = StrUtil.subBetween(content, "{value:!0});var s=\"", "\"");
					}
					ui.setQuery_hash(hash);//用于帖子查询
					ui.setQuery_hash2(hash2);//用于video查询
					ui.setQuery_hash3(hash3);//用于highlightReel查询
					ui.setReels(highlightReels(oui, ui.getId(), hash3, ui.getUsername()));
					return ui;
			    }
			}
		}
		return null;
	}
	
	public Boolean needRefresh(String oldUrl, String newUrl) {
		if(StrUtil.isBlank(oldUrl)) {
			return true;
		}
		String parent = path+File.separator+"temp";
		FileUtil.mkdir(parent);
		String oldMD5 = StrUtil.subBefore(StrUtil.subAfter(oldUrl, "/", true), ".", true);
		//File oldFile = new File(parent+File.separator+IdUtil.fastSimpleUUID());
		//HttpUtil.downloadFile(oldUrl, oldFile);
		File newFile = new File(parent+File.separator+IdUtil.fastSimpleUUID());
		HttpUtil.downloadFile(newUrl, newFile);
		if(oldMD5.equals(SecureUtil.md5(newFile))) {
			return false;
		}
		return true;
		
	}
	
	public List<Reel> highlightReels(UserInfo oui, String userId, String hash, String username){
		JSONObject jsonObject = new JSONObject();
		jsonObject.set("user_id", userId);
		jsonObject.set("include_chaining", false);
		jsonObject.set("include_reel", false);
		jsonObject.set("include_suggested_users", false);
		jsonObject.set("include_logged_out_extras", true);
		jsonObject.set("include_highlight_reels", true);
		String url = String.format("https://www.instagram.com/graphql/query/?query_hash=%s&variables=%s", hash, URLUtil.encodeAll(jsonObject.toString())).toString();
		String content =  HttpRequest.get(url).execute().body();
		JSONObject jsonResult = JSONUtil.parseObj(content);
		if(jsonResult.getStr("status").equals("ok")) {
			JSONArray js = jsonResult.getJSONObject("data")
					.getJSONObject("user").getJSONObject("edge_highlight_reels").getJSONArray("edges");
			return js.stream().map(r->{
				JSONObject j = ((JSONObject)r).getJSONObject("node");
				Reel reel = new Reel();
				reel.setId(j.getStr("id"));
				reel.setTitle(j.getStr("title"));
				if(oui != null) {
					String oldCoverUrl = this.getCoverMediaThumbnail(oui, j.getStr("id"));
					if(this.needRefresh(oldCoverUrl, j.getJSONObject("cover_media_cropped_thumbnail").getStr("url"))) {
						reel.setCover_media_thumbnail(cacheService.cache(j.getJSONObject("cover_media_cropped_thumbnail").getStr("url"), username));
					}else{
						reel.setCover_media_thumbnail(oldCoverUrl);
					}
				}else{
					reel.setCover_media_thumbnail(cacheService.cache(j.getJSONObject("cover_media_cropped_thumbnail").getStr("url"), username));
				}
				return reel;
			}).collect(Collectors.toList());
		}else{
			return null;
		}
	}
	
	private String getCoverMediaThumbnail(UserInfo oui, String id) {
		try {
			for (Reel reel : oui.getReels()) {
				if(reel.getId().equals(id)) {
					return reel.getCover_media_thumbnail();
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	private static String getRedirectUrl(String path) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(path)
				.openConnection();
		conn.setInstanceFollowRedirects(false);
		conn.setConnectTimeout(5000);
        return conn.getHeaderField("Location");
	}
	/*public String cacheImgToSinaPicBed(String url) {
		String fileName = url.split("/")[url.split("/").length-1];
		File piclFile = HttpKit.downloadFile(url, fileName, downPath);
		try {
			if(piclFile != null) {
				String  sinaPicBedUrl = SinaPicBedKit.uploadFile(piclFile, 0);//large
				return sinaPicBedUrl;
			}
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		return url;
	}*/
	public String videoUrl(String shortcode, String hash2) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.set("shortcode", shortcode);
		String url = String.format("https://www.instagram.com/graphql/query/?query_hash=%s&variables=%s", hash2, jsonObject.toString()).toString();
		String content = HttpRequest.get(url).execute().body();
		return JSONUtil.parseObj(content).getByPath("$.data.shortcode_media.video_url", String.class);
	}
	/**
     * 截取字符串
     * @param str 原字符串
     * @param s1 起始字符串
     * @param s2 结束字符串
     * @return
     */
    public static String substring(String str, String s1, String s2) {
		// 1、先获得0-s1的字符串，得到新的字符串sb1
		// 2、从sb1中开始0-s2获得最终的结果。
		try {
			StringBuffer sb = new StringBuffer(str);
			String sb1 = sb.substring(sb.indexOf(s1) + s1.length());
			return String.valueOf(sb1.substring(0, sb1.indexOf(s2)));
		} catch (StringIndexOutOfBoundsException e) {
			return str;
		}
	}
	public void compressZip(File file, String ids, String uanme, String random) {
		String[] idList = ids.split(",");
		String folder = uanme + random;
		String path = downPath+File.separator+folder;
		FileUtil.mkdir(path);
		for (String id : idList) {
			Query query = new Query(Criteria.where("id").is(id));
			Node node = template.findOne(query, Node.class,"InsUserData");
			String dPath = path+File.separator+id;
			FileUtil.mkdir(dPath);
			if(node.getChildren() != null) {
				JSONArray jarr = JSONUtil.parseArray(node.getChildren());
				for (int i = 0; i < jarr.size(); i++) {
					String dUrl = jarr.getJSONObject(i).getStr("display_url");
					Boolean isVideo = jarr.getJSONObject(i).getBool("is_video");
					if(!isVideo) {
						HttpUtil.downloadFile(dUrl, dPath);
					}else {
						dUrl = jarr.getJSONObject(i).getStr("video_url");
						//System.out.println(HttpRequest.get(dUrl).setMaxRedirectCount(3).);
						try {
							dUrl = getRedirectUrl(dUrl);
						} catch (Exception e) {
							e.printStackTrace();
						}
						HttpUtil.downloadFile(dUrl, dPath);
					}
				}
			}else {
				if(node.getIs_video()) {
					try {
						String du = getRedirectUrl(node.getVideo_url());
						HttpUtil.downloadFile(du, dPath);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					HttpUtil.downloadFile(node.getDisplay_url(), dPath);
				}
			}
		}
		File sourceDir = new File(path);
		ZipOutputStream zos = null;
		try {
			 zos = new ZipOutputStream(new FileOutputStream(file));
			 String baseDir = folder+"/";
			 CompressKit.compress(sourceDir, baseDir, zos);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage(), e);
		} finally{
			if(zos!=null)
				try {
					zos.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
		}
		
	}
	public Object getPicInfo(String urls) {
		try {
			String[] uls = urls.split("\n");
			List<InsSearch> inss = new ArrayList<InsSearch>();
			for (String url : uls) {
				url = url.trim();
				String shortCode = url.split("/")[4];
				Document doc = Jsoup.connect(url)
							.header("cookie", cookie)
							.get();
				String result = doc.html();
				String json = substring(result, "window.__additionalDataLoaded('/p/"+shortCode+"/',", ");");
				JSONObject jo = JSONUtil.parseObj(json);
				String picUrl = jo.getByPath("$.graphql.shortcode_media.owner.profile_pic_url", String.class);
				String username = jo.getByPath("$.graphql.shortcode_media.owner.username", String.class);
				String fullname = jo.getByPath("$.graphql.shortcode_media.owner.full_name", String.class);
				InsSearch ins = new InsSearch();
				ins.setFullname(fullname);
				ins.setUsername(username);
				ins.setPic_url(this.toLocalName(picUrl));
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> list = jo.getByPath("$.graphql.shortcode_media.edge_sidecar_to_children.edges", List.class);
				if(list == null) {
					//单图片
					list = new 	ArrayList<Map<String,Object>>();
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("node", jo.getByPath("$.graphql.shortcode_media"));
					list.add(map);
				}
				List<Child> children = new ArrayList<Child>();
				for (Map<String, Object> map : list) {
					Child child = new Child();
					//ins.setPic_url(PicBed.uploadMT(picUrl, path, username));
					String display_url = (String)JSONUtil.getByPath(JSONUtil.parse(map), "$.node.display_url");
					Boolean isVideo = (Boolean)JSONUtil.getByPath(JSONUtil.parse(map), "$.node.is_video");
					child.setDisplay_url(this.toLocalName(display_url));
					//ins.setDisplay_url(display_url);
					if(!isVideo) {
						child.setIs_video(false);
					}else {
						String video_url = (String)JSONUtil.getByPath(JSONUtil.parse(map), "$.node.video_url");
						child.setIs_video(true);
						//ins.setVideo_url(this.toLocalName(JSONPath.eval(map, "$.node.video_url").toString()));
						//ins.setVideo_url(video_url);
						child.setVideo_url(this.toLocalName(video_url));
					}
					children.add(child);
				}
				ins.setChildren(children);
				inss.add(ins);
			}
			return inss;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	private String toLocalName(String url) {
		 String filename = DigestUtil.md5Hex(url.substring(url.lastIndexOf('/')+1))
				 + getFileNameFromUrl(url);
		 File file = new File(path+File.separator+"ins"+File.separator+filename);
		 if(!file.exists()) {
			 HttpUtil.downloadFile(url, path+File.separator+"ins"+File.separator+filename);
		 }
		return filename;
	}
	
	public void refreshUrls(String type) {
		List<Node> list = template.find(new Query(Criteria.where("soureType").is(type)).limit(50), Node.class, "InsUserData");
		for (Node node : list) {
			UserInfo ui = template.findById(node.getUname(), UserInfo.class, "InsUserList");
			String url = "https://www.instagram.com/p/" + node.getShortcode();
			if(HttpRequest.get("https://www.instagram.com/p/" + node.getShortcode()+"/media/?size=m").setMaxRedirectCount(3).execute().getStatus() == 410) {
				//instagram源图片问题，跳过缓存
				node.setSoureType("cronRefresh");
				template.save(node, "InsUserData");
				continue;
			}
			Document doc;
			try {
				doc = Jsoup.connect(url).header("cookie", cookie).get();
				String result = doc.html();
				String json = substring(result, "window.__additionalDataLoaded('/p/" + node.getShortcode() + "/',",
						");");
				JSONObject jo = JSONUtil.parseObj(json, false);
				String displayUrl = jo.getByPath("$.graphql.shortcode_media.display_url", String.class);
				JSONArray arr = jo.getByPath("$.graphql.shortcode_media.display_resources",
						JSONArray.class);
				JSONObject children = jo.getByPath("$.graphql.shortcode_media.edge_sidecar_to_children",
						JSONObject.class);
				node.setDisplay_url(displayUrl);
				if (node.getIs_video()) { // String videoUrl =
					jo.getByPath("$.graphql.shortcode_media.video_url", String.class);
					node.setVideo_url(videoUrl(node.getShortcode(), ui.getQuery_hash2()));
				}
				List<ThumbnailResources> srcs = new ArrayList<ThumbnailResources>();
				for (Object src : arr) {
					ThumbnailResources thumbnailResources = new ThumbnailResources();
					thumbnailResources.setSrc(((JSONObject) src).getStr("src"));
					thumbnailResources.setConfig_width(((JSONObject) src).getInt("config_width"));
					thumbnailResources.setConfig_height(((JSONObject) src).getInt("config_height"));
					srcs.add(thumbnailResources);
				}
				node.setSrcs(srcs);
				if (children != null) {
					JSONArray childrenArr = children.getJSONArray("edges");
					List<Child> cr = new ArrayList<Child>();
					for (Object child : childrenArr) {
						Child c = new Child();
						JSONObject n = ((JSONObject) child).getJSONObject("node");
						String du = n.getStr("display_url");
						Boolean isV = n.getBool("is_video");
						c.setIs_video(isV);
						if (isV) {
							c.setVideo_url(n.getStr("video_url"));
						}
						c.setDisplay_url(du);
						cr.add(c);
					}
					node.setChildren(cr);
				}
				node.setSoureType(type.equals("cronRefresh")?"cronRefresh":"ins");
				template.save(node, "InsUserData");
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	public void refreshCookie() throws GeneralSecurityException {
		HttpResponse firstResponse = HttpRequest.get("https://www.instagram.com/").execute();
		  System.out.println(firstResponse.getCookieStr());
		  String configBody = firstResponse.body();
		  String configJson = StrUtil.subBetween(configBody, "window._sharedData = ", ";");
		  JSONObject config = JSONUtil.parseObj(configJson);
		  String rolloutHash = config.getByPath("$.rollout_hashn", String.class);
		  String csrftoken = config.getByPath("$.config.csrf_token", String.class);
		  String encPublicKey = config.getByPath("$.encryption.public_key", String.class);
		  Integer enckeyId = config.getByPath("$.encryption.key_id", Integer.class);
		  String passowrd = InstagramAes.enc(enckeyId, encPublicKey, igUserPassword);
		  HttpResponse rep = HttpRequest.post("https://www.instagram.com/accounts/login/ajax/")
									  .header("user-agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36")
									  .header("content-type", "application/x-www-form-urlencoded")
									  .header("referer","https://www.instagram.com/")
									  .header("origin", "https://www.instagram.com")
									  .header("sec-fetch-dest", "empty")
									  .header("sec-fetch-mode", "cors")
									  .header("sec-fetch-site", "same-origin")
									  .header("x-csrftoken", csrftoken)
									  .header("x-ig-app-id", "936619743392459") 
									  .header("x-ig-www-claim", "0")
									  .header("x-instagram-ajax", rolloutHash)
									  .form("username", "ponbous")
									  .form("enc_password", passowrd)
									  .form("optIntoOneTap", false)
									  .form("queryParams", "{}")
									  .execute();
		  System.out.println(rep.getStatus());
		  StringBuffer sb = new StringBuffer();
		  for (String sc : rep.headerList("Set-Cookie")) {
			  String name = StrUtil.trim(sc.split(";")[0].split("=")[0]);
			  String value =  StrUtil.trim(sc.split(";")[0].split("=")[1]);
			  sb.append(name+"="+value+";");
		}
		cookie = sb.toString();
	}
}
