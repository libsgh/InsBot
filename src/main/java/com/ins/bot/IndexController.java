package com.ins.bot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.ins.bot.common.PicBed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.LiteDeviceResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import com.ins.bot.bean.Node;
import com.ins.bot.bean.UserInfo;
import com.ins.bot.task.Ins;
import com.jfinal.kit.StrKit;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

@Controller
public class IndexController {
	
	protected static Logger logger = LoggerFactory.getLogger(IndexController.class);
	
	@Autowired
	private Ins ins;
	
	@Autowired
	private MongoTemplate template;
	
	@Value("${download.path}")
	private String path;
	
	public static Integer pageSize = 12;

	@Value("${ui_collection_name:InsUserList}")
	private String uiCollectionName;

	@Value("${ud_collection_name:InsUserData}")
	private String udCollectionName;

	@GetMapping("/{username}")
	public String page(HttpServletRequest request,@PathVariable String username, Model model) {
		LiteDeviceResolver deviceResolver = new LiteDeviceResolver();
		Device device = deviceResolver.resolveDevice(request);
		model.addAttribute("isMobile", device.isMobile());
		model.addAttribute("isNormal", device.isNormal());
		model.addAttribute("isTablet", device.isTablet());
		List<UserInfo> list = template.findAll(UserInfo.class, uiCollectionName);
		model.addAttribute("unlist", list);
		Integer pageNum = 1;
		if(StrKit.isBlank(username)) {
			return "index";
		} else {
			UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, uiCollectionName);
			if(ui == null) {
				return "404";
			}else {
				Query pageQuery = new Query(Criteria.where("userId").is(ui.getId()))
						.skip((pageNum - 1) * pageSize).limit(pageSize)
						.with(Sort.by(Sort.Order.desc("timestamp")));
				List<Node> ud = template.find(pageQuery, Node.class, udCollectionName);
				model.addAttribute("ud", ud);
				model.addAttribute("ui", ui);
				return "ins";
			}
		}
	}
	@GetMapping("/")
	public String index(Model model) {
		List<UserInfo> list = template.findAll(UserInfo.class, uiCollectionName);
		if(list != null && !list.isEmpty()) {
			model.addAttribute("unlist", list);
		}
		return "index";
	}
	
	@PostMapping("/search")
	@ResponseBody
	public Object search(String link) {
		return ins.getPicInfo(link);
	}
	
	@GetMapping("/api/ui/{username}")
	@ResponseBody
	public Object getUserInfo(@PathVariable String username) {
		UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, uiCollectionName);
		if(StrKit.isBlank(username)) {
			return "404";
		} else if(ui == null){
			return "not found";
		}else {
			return ui;
		}
	}
	
	@GetMapping("/api/ud/{username}")
	@ResponseBody
	public Object getUserData(@PathVariable String username) {
		UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, uiCollectionName);
		if(StrKit.isBlank(username)) {
			return "404";
		} else if(ui == null){
			return "not found";
		}else {
			Query pageQuery = new Query(Criteria.where("userId").is(ui.getId()))
							.with(Sort.by(Sort.Order.desc("timestamp")));
			List<Node> ud = template.find(pageQuery, Node.class, udCollectionName);
			return ud;
		}
	}
	
	@GetMapping("/error/{code}")
	public String error(@PathVariable int code, Model model) {
		String pager = "404";
		switch (code) {
        case 404:
            model.addAttribute("code", 404);
            pager = "404";
            break;
		}
		return pager;
	}
	
	@GetMapping("/update/{userid}")
	@ResponseBody
	public String update(@PathVariable String userid, Boolean flag) {
		if(StrKit.isBlank(userid)) {
			return "请输入ins账号";
		} else if(userid.equals("all")){
			ins.run();
		}else{
			ins.excute(userid, false);
		}
		return "机器人已经开始工作，请稍等一会儿刷新页面查看";
	}
	
	@GetMapping("/run")
	@ResponseBody
	public String run() {
		ins.run();
		return "机器人已经开始工作，请稍等一会儿刷新页面查看";
	}
	
	@GetMapping("/remove/{username}")
	@ResponseBody
	public String remove(@PathVariable String username) {
		if(StrKit.isBlank(username)) {
			return "请输入ins账号";
		}else{
			UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, uiCollectionName);
			template.remove(new Query(Criteria.where("userId").is(ui.getId())), udCollectionName);
			template.remove(new Query(Criteria.where("_id").is(username)), uiCollectionName);
		}
		return "机器人已经开始工作，请稍等一会儿刷新页面查看";
	}
	
	@GetMapping("/down/{filename}")
	public void down(HttpServletRequest req, HttpServletResponse res, @PathVariable String filename) {
		RandomAccessFile in = null;
		OutputStream os = null;
		try {
			File file = new File(path + File.separator + filename);
			res.setContentType("application/octet-stream");
			res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
			res.setHeader("Accept-Ranges", "bytes");
			res.setContentLengthLong(file.length());
	        long downloadSize = file.length();
	        long fromPos = 0, toPos = 0;
	        if (req.getHeader("Range") == null) {
	        	res.setHeader("Content-Length", downloadSize + "");
	        } else {
	        	// 若客户端传来Range，说明之前下载了一部分，设置206状态(SC_PARTIAL_CONTENT)
	        	res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
	            String range = req.getHeader("Range");
	            String bytes = range.replaceAll("bytes=", "");
	            String[] ary = bytes.split("-");
	            fromPos = Long.parseLong(ary[0]);
	            if (ary.length == 2) {
	                toPos = Long.parseLong(ary[1]);
	            }
	            int size;
	            if (toPos > fromPos) {
	                size = (int) (toPos - fromPos);
	            } else {
	                size = (int) (downloadSize - fromPos);
	            }
	            res.setHeader("Content-Length", size + "");
	            downloadSize = size;
	        }
			os = res.getOutputStream();
			in = new RandomAccessFile(file, "rw");
            // 设置下载起始位置
            if (fromPos > 0) {
                in.seek(fromPos);
            }
            // 缓冲区大小
            int bufLen = (int) (downloadSize < 2048 ? downloadSize : 2048);
            byte[] buffer = new byte[bufLen];
            int num;
            int count = 0; // 当前写到客户端的大小
            os = res.getOutputStream();
            while ((num = in.read(buffer)) != -1) {
                os.write(buffer, 0, num);
                count += num;
                //处理最后一段，计算不满缓冲区的大小
                if (downloadSize - count < bufLen) {
                    bufLen = (int) (downloadSize-count);
                    if(bufLen==0){
                        break;
                    }
                    buffer = new byte[bufLen];
                }
            }
            res.flushBuffer();
		} catch (IOException e) {
			//logger.error(e.getMessage(), e);
			logger.info("数据被暂停或中断。");
		} finally {
			if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.info("数据被暂停或中断。");
                }
            }
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.info("数据被暂停或中断。");
                }
            }
		}
		
	}
	
	@RequestMapping("/loadMore")
	@ResponseBody
	public Object loadMore(String userId, Integer pageNum) {
		Query pageQuery = new Query(Criteria.where("userId").is(userId))
				.skip((pageNum - 1) * pageSize).limit(pageSize)
				.with(Sort.by(Sort.Order.desc("timestamp")));
		List<Node> ud = template.find(pageQuery, Node.class, udCollectionName);
		return ud;
	}

	/**
	 * 查询各个账户缓存任务状态
	 * @param model
	 * @return
	 */
	@RequestMapping("/tasks")
	public String tasks(Model model) {
		List<Map<String, Object>> rs = new ArrayList<Map<String,Object>>();
		List<UserInfo> list = template.findAll(UserInfo.class, uiCollectionName);
		for (UserInfo userInfo : list) {
			Map<String, Object> map = new HashMap<String, Object>();
			Long sourceCount = template.count(new Query(Criteria.where("soureType").is("ins").and("uname").is(userInfo.getUsername())), Long.class, udCollectionName);
			Long ghCdnCount = template.count(new Query(Criteria.where("soureType").is("gh_cdn").and("uname").is(userInfo.getUsername())), Long.class, udCollectionName);
			Long validCdnCount = template.count(new Query(Criteria.where("soureType").is("invalid").and("uname").is(userInfo.getUsername())), Long.class, udCollectionName);
			Long crCount = template.count(new Query(Criteria.where("soureType").is("cronRefresh").and("uname").is(userInfo.getUsername())), Long.class, udCollectionName);
			String percent = NumberUtil.formatPercent((double)ghCdnCount/(double)(ghCdnCount+sourceCount+validCdnCount), 2);
			map.put("total", (ghCdnCount+sourceCount+validCdnCount+crCount));
			map.put("ghCdnCount", ghCdnCount);
			map.put("sourceCount", sourceCount);
			map.put("percent", percent);
			map.put("validCdnCount", validCdnCount);
			map.put("crCount", crCount);
			map.put("lastTimeF", userInfo.getLastTimeF());
			map.put("username", userInfo.getUsername());
			map.put("headImg", userInfo.getProfile_pic_url_hd());
			rs.add(map);
		}
		model.addAttribute("unlist", rs);
		return "tasks";
	}

	@RequestMapping(value = "/api/video/{videoCode}")
	public String video(HttpServletRequest request, HttpServletResponse response, @PathVariable String videoCode) throws IOException {
		return "redirect:" + PicBed.getVideoUrl(videoCode);
	}

}
