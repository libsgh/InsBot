package com.ins.bot.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ins.bot.bean.Child;
import com.ins.bot.bean.Node;
import com.ins.bot.bean.ThumbnailResources;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;

@Service
public class CacheService {
	
	protected static Logger logger = LoggerFactory.getLogger(CacheService.class);
	
	@Value("${download.path}")
	private String localPath;
	
	@Autowired
	private MongoTemplate template;
	
	@Value("${GITHUB_ACCESS_TOKEN}")
	private String accessToken;
	
	@Autowired
	private GithubUploader githubUploader;
	
	final static Pattern pattern = Pattern.compile("\\S*[?]\\S*");
	
	/**
	 * 三种情况
	 * 1. 文件大于20M由于jsdelivr不支持超过20M的缓存，直接返回gh githubusercontent
	 * 2. 文件大于40M上传gh会返回502， 这样的视频保存原来地址，靠定时任务刷新 null
	 * 3. 小于20M使用jsdelivr缓存加速地址 jsdelivr
	 * @param mediaUrl
	 * @param username
	 * @return
	 */
	public String cache(String mediaUrl, String username) {
		try {
			if(mediaUrl.contains("jsdelivr") || mediaUrl.contains("githubusercontent")) {
				return mediaUrl;
			}
			Long start = System.currentTimeMillis();
			FileUtil.mkdir(localPath + File.separator + username);
			String extName = FileUtil.extName(getFileNameFromUrl(mediaUrl));
			String filename = IdUtil.fastSimpleUUID()
					+ "." + extName;
			File file = new File(localPath+File.separator+username+File.separator+filename);
			HttpUtil.downloadFile(mediaUrl, file);
			String url = githubUploader.upload(file, username+"/"+SecureUtil.md5(file)+ "." + extName, username);
			if(FileUtil.size(file) > 20971520 && FileUtil.size(file) < 41943040) {
				//由于jsdelivr限制 如果文件大于20M这里返回github地址
				url = url.replaceAll("@", "/").replaceAll("https://cdn.jsdelivr.net/gh", "https://raw.githubusercontent.com");
			}else if( FileUtil.size(file) > 41943040) {
				return null;
			}
			logger.info(githubUploader.type() + ":" + url + "\t耗时：" +(System.currentTimeMillis()-start));
			if(StrUtil.isNotBlank(url)) {
				return url;
			}else{
				logger.info("缓存失败，源地址："+mediaUrl);
			}
		} catch (HttpException e) {
			logger.error("下载文件失败：" + username + "----" + mediaUrl);
			logger.error(e.getMessage(), e);
			return "";
		}
		return mediaUrl;
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
			logger.error("文件后缀名获取失败：\t"+url);
		}
    	return "";
    }
	public void checkRepo() {
		githubUploader.checkRepo();
	}
	
	public static Boolean flag = false;
	
	public void chgCdnUrls() {
		if(!flag) {
			flag = true;
			try {
				// 1. 先查询所有源图、源视频地址的节点数据
				// 2. 遍历所有node循环进行cdn缓存地址替换
				Query query = new Query(Criteria.where("soureType").is("ins")).limit(5);
				List<Node> ud = template.find(query, Node.class, "InsUserData");
				for (Node node : ud) {
					node.setSoureType("gh_cdn");
					String display_url = cache(node.getDisplay_url(), node.getUname());
					if(display_url == null) {
						node.setSoureType("cronRefresh");
						node.setDisplay_url(node.getDisplay_url());
					}else if(display_url != null && display_url.contains("instagram")) {
						node.setSoureType("ins");
						node.setDisplay_url(display_url);
					}else if (display_url != null && display_url.equals("")) {
						node.setSoureType("invalid");
						node.setDisplay_url(node.getDisplay_url());
						template.save(node, "InsUserData");
						return;
					}
					node.setDisplay_url(display_url);
					List<ThumbnailResources> trs = new ArrayList<ThumbnailResources>();
					for (ThumbnailResources tr : node.getSrcs()) {
						String src = this.cache(tr.getSrc(), node.getUname());
						if(src == null) {
							node.setSoureType("cronRefresh");
							tr.setSrc(tr.getSrc());
						}else if(src != null && src.contains("instagram")) {
							node.setSoureType("ins");
							tr.setSrc(tr.getSrc());
						}else if (src != null && src.equals("")) {
							tr.setSrc(src);
							node.setSoureType("invalid");
							template.save(node, "InsUserData");
							return;
						}
						tr.setSrc(src);
						trs.add(tr);
					}
					node.setSrcs(trs);
					if(node.getIs_video()) {
						if(StrUtil.isNotBlank(node.getVideo_url())) {
							String video_url = this.cache(node.getVideo_url(), node.getUname());
							if (video_url == null) {
								node.setSoureType("cronRefresh");
								node.setVideo_url(node.getVideo_url());
							}else if (video_url != null && video_url.contains("instagram")) {
								node.setSoureType("ins");
								template.save(node, "InsUserData");
								return;
							}else if (video_url != null && video_url.equals("")) {
								node.setVideo_url(node.getVideo_url());
								node.setSoureType("invalid");
								template.save(node, "InsUserData");
								return;
							}
							node.setVideo_url(video_url);
						}else{
							node.setVideo_url("");
							node.setSoureType("invalid");
							template.save(node, "InsUserData");
							return;
						}
					}
					if(node.getChildren() != null) {
						//包含子节点
						List<Child> children = new ArrayList<Child>();
						for (Child c : node.getChildren()) {
							String c_display_url = this.cache(c.getDisplay_url(), node.getUname());
							if(c_display_url == null) {
								node.setSoureType("cronRefresh");
								c.setDisplay_url(c.getDisplay_url());
							}else if(c_display_url != null && c_display_url.contains("instagram")) {
								node.setSoureType("ins");
								c.setDisplay_url(c_display_url);
							}else if (c_display_url != null && c_display_url.equals("")) {
								node.setSoureType("invalid");
								c.setDisplay_url(c_display_url);
								template.save(node, "InsUserData");
								return;
							}
							c.setDisplay_url(c_display_url);
							if(c.getIs_video()) {
								String c_video_url = cache(c.getVideo_url(), node.getUname());
								if (c_video_url == null) {
									node.setSoureType("cronRefresh");
									c.setVideo_url(c.getVideo_url());
								}else if (c_video_url != null && c_video_url.contains("instagram")) {
									node.setSoureType("ins");
									template.save(node, "InsUserData");
									return;
								}else if (c_video_url != null && c_video_url.equals("")) {
									c.setVideo_url(c.getVideo_url());
									node.setSoureType("invalid");
									template.save(node, "InsUserData");
									return;
								}
								c.setVideo_url(c_video_url);
							}
							children.add(c);
						}
						node.setChildren(children);
					}
					template.save(node, "InsUserData");
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}finally {
				flag = false;
			}
		}
	}
}
