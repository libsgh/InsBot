package com.ins.bot.task;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ins.bot.media.CacheService;

import cn.hutool.http.HttpUtil;

@Component
public class Tasks {
	
	protected static Logger logger = LoggerFactory.getLogger(Tasks.class);
	
	@Autowired
	private Ins ins;
	
	@Autowired
	private CacheService cacheService;
	
	@Scheduled(cron = "0 0/10 * * * ?")
	public void manualTask() throws IOException {
		logger.info("系统任务开始...");
		HttpUtil.createGet("http://insbox.herokuapp.com").execute().getStatus();
		//查询github repo状态
		cacheService.checkRepo();
		logger.info("系统任务结束...");
	}
	
	@Scheduled(cron = "0 0/1 * * * ?")
	public void chgCdnUrls() throws IOException {
		logger.info("缓存任务开始...");
		cacheService.chgCdnUrls();
		logger.info("缓存任务结束...");
	}
	
	@Scheduled(cron = "0 0/30 * * * ?")
	@Async
	public void refreshUrls() throws GeneralSecurityException {
		logger.info("刷新任务开始...");
		if(!ins.cookieValid()) {
			ins.refreshCookie();
		}else{
			logger.info("cookie有效，不需要刷新cookie...");
		}
		ins.refreshUrls("invalid");
		logger.info("刷新任务结束...");
	}
	
	@Scheduled(cron = "0 0/30 * * * ?")
	public void updateIns() {
		logger.info("定时刷新开始...");
		ins.run();
		logger.info("定时刷新结束...");
	}
	
	@Scheduled(cron = "0 0 0 1/2 * ?")
	public void crefreshUrls() {
		logger.info("刷新任务开始...");
		ins.refreshUrls("cronRefresh");
		logger.info("刷新任务结束...");
	}
	
}
