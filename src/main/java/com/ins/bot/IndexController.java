package com.ins.bot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.LiteDeviceResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
	
	@GetMapping("/{username}")
	public String page(HttpServletRequest request,@PathVariable String username, Model model) {
		LiteDeviceResolver deviceResolver = new LiteDeviceResolver();
		Device device = deviceResolver.resolveDevice(request);
		model.addAttribute("isMobile", device.isMobile());
		model.addAttribute("isNormal", device.isNormal());
		model.addAttribute("isTablet", device.isTablet());
		List<UserInfo> list = template.findAll(UserInfo.class, "InsUserList");
		model.addAttribute("unlist", list);
		Integer pageNum = 1;
		if(StrKit.isBlank(username)) {
			return "index";
		} else {
			UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, "InsUserList");
			if(ui == null) {
				return "404";
			}else {
				Query pageQuery = new Query(Criteria.where("userId").is(ui.getId()))
						.skip((pageNum - 1) * pageSize).limit(pageSize)
						.with(Sort.by(Sort.Order.desc("timestamp")));
				List<Node> ud = template.find(pageQuery, Node.class, "InsUserData");
				model.addAttribute("ud", ud);
				model.addAttribute("ui", ui);
				return "ins";
			}
		}
	}
	@GetMapping("/")
	public String index(Model model) {
		List<UserInfo> list = template.findAll(UserInfo.class, "InsUserList");
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
		UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, "InsUserList");
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
		UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, "InsUserList");
		if(StrKit.isBlank(username)) {
			return "404";
		} else if(ui == null){
			return "not found";
		}else {
			Query pageQuery = new Query(Criteria.where("userId").is(ui.getId()))
							.with(Sort.by(Sort.Order.desc("timestamp")));
			List<Node> ud = template.find(pageQuery, Node.class, "InsUserData");
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
	public String update(@PathVariable String userid) {
		if(StrKit.isBlank(userid)) {
			return "请输入ins账号";
		} else if(userid.equals("all")){
			ins.run();
		}else{
			ins.excute(userid, null, true);
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
			UserInfo ui = template.findOne(new Query(Criteria.where("username").is(username)), UserInfo.class, "InsUserList");
			template.remove(new Query(Criteria.where("userId").is(ui.getId())), "InsUserData");
			template.remove(new Query(Criteria.where("_id").is(username)), "InsUserList");
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
	
	@GetMapping("/ins/download/{filename:.+}")
	public void download(HttpServletResponse res, @PathVariable String filename) throws UnsupportedEncodingException {
		File file = new File(path+File.separator+"ins"+File.separator+filename);
		res.setHeader("content-type", "application/octet-stream");
		res.setContentType("application/octet-stream");
		res.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
		res.setContentLengthLong(file.length());
		byte[] buff = new byte[1024];
		BufferedInputStream bis = null;
		OutputStream os = null;
		try {
			os = res.getOutputStream();
			bis = new BufferedInputStream(new FileInputStream(file));
			int i = bis.read(buff);
			while (i != -1) {
				os.write(buff, 0, buff.length);
				os.flush();
				i = bis.read(buff);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
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
		List<Node> ud = template.find(pageQuery, Node.class, "InsUserData");
		return ud;
	}
	
	@RequestMapping("/compress/download")
	@ResponseBody
	public Map<String, Object> compressDownload(String ids, String uname) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			String random = RandomUtil.randomString(4);
			File file = new File(path + File.separator + uname + random + ".zip");
			ins.compressZip(file, ids, uname, random);
			if(file.exists()) {
				map.put("fileName",  uname + random + ".zip");
				map.put("errorCode",  0);
			}else{
				map.put("errorCode",  -1);
				map.put("fileName",  "");
			}
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
			map.put("errorCode",  -1);
			map.put("fileName",  "");
		}
		return map;
	}

	/*
	 * @RequestMapping("/reUpload")
	 * 
	 * @ResponseBody public Object reUpload() { Criteria c1 =
	 * Criteria.where("display_url").regex(".*?instagram.*"); Criteria c2 =
	 * Criteria.where("srcs.src").regex(".*?instagram.*"); Criteria c3 =
	 * Criteria.where("video_url").regex(".*?instagram.*"); Criteria cr = new
	 * Criteria(); Query query = new Query(cr.orOperator(c1, c2, c3)); List<Node>
	 * nlist = template.find(query, Node.class, "InsUserData"); for (Node node :
	 * nlist) { node.setSoureType("invalid"); template.save(node, "InsUserData"); }
	 * return nlist; }
	 */	
	/**
	 * 查询各个账户缓存任务状态
	 * @param model
	 * @return
	 */
	@RequestMapping("/tasks")
	public String tasks(Model model) {
		List<Map<String, Object>> rs = new ArrayList<Map<String,Object>>();
		List<UserInfo> list = template.findAll(UserInfo.class, "InsUserList");
		for (UserInfo userInfo : list) {
			Map<String, Object> map = new HashMap<String, Object>();
			Long sourceCount = template.count(new Query(Criteria.where("soureType").is("ins").and("uname").is(userInfo.getUsername())), Long.class, "InsUserData");
			Long ghCdnCount = template.count(new Query(Criteria.where("soureType").is("gh_cdn").and("uname").is(userInfo.getUsername())), Long.class, "InsUserData");
			Long validCdnCount = template.count(new Query(Criteria.where("soureType").is("invalid").and("uname").is(userInfo.getUsername())), Long.class, "InsUserData");
			Long crCount = template.count(new Query(Criteria.where("soureType").is("cronRefresh").and("uname").is(userInfo.getUsername())), Long.class, "InsUserData");
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
	
	/**
	 * 手动登录刷新缓存
	 * @param model
	 * @return
	 * @throws GeneralSecurityException 
	 */
	@RequestMapping("/refreshCookie")
	@ResponseBody
	public UserInfo refreshCookie() throws GeneralSecurityException {
		return ins.refreshCookie();
	}
	
	/**
	 * 
	 * @return
	 * @throws GeneralSecurityException
	 */
	@RequestMapping("/cookieValid")
	@ResponseBody
	public Object cookieValid() throws GeneralSecurityException {
		return ins.cookieValid();
	}
	
}
