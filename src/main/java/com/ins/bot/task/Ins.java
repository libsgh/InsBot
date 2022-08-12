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

import cn.hutool.core.collection.ListUtil;
import com.github.instagram4j.instagram4j.models.media.ImageVersionsMeta;
import com.github.instagram4j.instagram4j.models.media.timeline.*;
import com.ins.bot.common.InsUtil;
import com.ins.bot.common.PicBed;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

@Service
@Slf4j
public class Ins {

	@Autowired
	private MongoTemplate template;

	@Autowired
	private InsUtil insUtil;

	@Value("${ui_collection_name:InsUserList}")
	private String uiCollectionName;

	@Value("${ud_collection_name:InsUserData}")
	private String udCollectionName;

	public void run() {
		try {
			List<UserInfo> list = template.findAll(UserInfo.class, uiCollectionName);
			for (UserInfo ui : list) {
				this.excute(ui.getUsername(), false);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Async
	public void excute(String username, boolean flag) {
		Long time = System.currentTimeMillis();
		UserInfo ui = insUtil.getUserInfo(username);
		template.save(ui, uiCollectionName);
		if (flag) {
			template.remove(new Query(Criteria.where("userId").is(ui.getId())), udCollectionName);
		}
		load(ui, flag);
		template.save(ui, uiCollectionName);
		log.info("ID：" + ui.getUsername() + "，更新耗时：" + (System.currentTimeMillis() - time) + "ms");
	}

	@SuppressWarnings({"resource"})
	private void load(UserInfo ui, boolean flag) {
		String lastId = "";
		if (!flag) {
			Node lastNode = template.findOne(new Query(Criteria.where("userId").is(ui.getId())).limit(1)
					.with(Sort.by(Sort.Order.desc("timestamp"))), Node.class, udCollectionName);
			if(lastNode != null){
				lastId = lastNode.getId();
			}
		}
		List<Node> nodes = insUtil.getUserFeed(ui.getUsername(), lastId);
		int c = 0;
		for (Node node : nodes) {
			if (!flag) {
				Node n = template.findById(node.getId(), Node.class, udCollectionName);
				if (n != null) {
					continue;
				}
			}
			c++;
			template.save(node, udCollectionName);
			log.info("ID：" + ui.getUsername() + "，媒体进度：" + c + "/" + ui.getTiez());
		}
		ui.setLastTime(nodes.get(0).getTimestamp()*1000);
	}

	public List<InsSearch> getPicInfo(String urls) {
		try {
			String[] uls = urls.split("\n");
			List<InsSearch> inss = new ArrayList<InsSearch>();
			for (String url : uls) {
				String shortCode = url.split("/")[4];
				List<TimelineMedia> list = insUtil.getMediaFromCode(shortCode);
				if(list != null && list.size() > 0){
					TimelineMedia  timelineMedia = list.get(0);
					InsSearch ins = new InsSearch();
					ins.setFullname(timelineMedia.getUser().getFull_name());
					ins.setUsername(timelineMedia.getUser().getUsername());
					ins.setPic_url(PicBed.upload(timelineMedia.getUser().getProfile_pic_url()));
					if(timelineMedia.getMedia_type().equals("1")){
						//图片
						TimelineImageMedia image = (TimelineImageMedia) timelineMedia;
						List<ImageVersionsMeta> images = image.getImage_versions2().getCandidates();
						ins.setIs_video(false);
						ins.setDisplay_url(PicBed.upload(images.get(0).getUrl()));
						ins.setVideo_url("");
						ins.setChildren(ListUtil.list(false, new Child(ins.getDisplay_url(), false, "")));
					}else if(timelineMedia.getMedia_type().equals("2")){
						//视频
						TimelineVideoMedia video = (TimelineVideoMedia) timelineMedia;
						List<ImageVersionsMeta> images = video.getImage_versions2().getCandidates();
						ins.setIs_video(true);
						ins.setDisplay_url(PicBed.upload(images.get(0).getUrl()));
						ins.setVideo_url(PicBed.uploadVideo(video.getVideo_versions().get(0).getUrl()));
						ins.setChildren(ListUtil.list(false, new Child(ins.getDisplay_url(), true, ins.getVideo_url())));
					}else if(timelineMedia.getMedia_type().equals("8")){
						//轮播
						TimelineCarouselMedia carouse = (TimelineCarouselMedia) timelineMedia;
						List<CarouselItem> items = carouse.getCarousel_media();
						ins.setIs_video(false);
						ins.setVideo_url("");
						ins.setChildren(items.stream().map(i->{
							if(i.getMedia_type().equals("1")){
								//图片
								ImageCarouselItem ici = (ImageCarouselItem)i;
								Child child = new Child();
								child.setDisplay_url(PicBed.upload(ici.getImage_versions2().getCandidates().get(0).getUrl()));
								child.setIs_video(false);
								child.setVideo_url("");
								return child;
							}else if(i.getMedia_type().equals("2")){
								VideoCarouselItem vci = (VideoCarouselItem)i;
								Child child = new Child();
								child.setDisplay_url(PicBed.upload(vci.getImage_versions2().getCandidates().get(0).getUrl()));
								child.setIs_video(true);
								child.setVideo_url(PicBed.uploadVideo(vci.getVideo_versions().get(0).getUrl()));
								return child;
							}
							return new Child();
						}).collect(Collectors.toList()));
						ins.setDisplay_url(ins.getChildren().get(0).getDisplay_url());
					}
					inss.add(ins);
				}
			}
			return inss;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public void syncCacheUrl() {
		List<UserInfo> list = template.findAll(UserInfo.class, uiCollectionName);
		for (UserInfo ui : list) {
			List<Node> nodes = template.find(new Query(Criteria.where("userId").is(ui.getId()).and("soureType").is("ins"))
					.with(Sort.by(Sort.Order.desc("timestamp"))), Node.class, udCollectionName);
			int c = 0;
			for (Node node : nodes) {
				if(!node.getIs_video()){
					String cdnUrl = PicBed.upload(node.getDisplay_url());
					node.setDisplay_url(cdnUrl);
					List<Child> children = new ArrayList<>();
					for (Child child : node.getChildren()) {
						child.setVideo_url(PicBed.uploadVideo(child.getVideo_url()));
						child.setDisplay_url(PicBed.upload(child.getDisplay_url()));
						children.add(child);
					}
					node.setChildren(children);
				}else{
					String videoUrl = PicBed.upload(node.getVideo_url());
					String cdnUrl = PicBed.upload(node.getDisplay_url());
					node.setDisplay_url(cdnUrl);
					node.setVideo_url(videoUrl);
				}
				List<ThumbnailResources> newSrcs = new ArrayList<>();
				for (ThumbnailResources thumbnailResources : node.getSrcs()) {
					thumbnailResources.setSrc(PicBed.upload(thumbnailResources.getSrc()));
					newSrcs.add(thumbnailResources);
				}
				node.setSrcs(newSrcs);
				node.setSoureType("cdn");
				template.save(node, udCollectionName);
				c++;
				log.info("ID: " + ui.getUsername() + ", 进度："+ c + "/" + nodes.size());
			}
		}

	}
}
