package com.ins.bot.task;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import cn.hutool.http.HttpUtil;

@Component
public class Tasks {
	
	protected static Logger logger = LoggerFactory.getLogger(Tasks.class);
	
	@Autowired
	private Ins ins;
	
	//@Scheduled(cron = "0 0/10 * * * ?")
	public void manualTask() throws IOException {
		logger.info("系统任务开始...");
		HttpUtil.createGet("http://insbox.herokuapp.com").execute().getStatus();
		//查询github repo状态
		//cacheService.checkRepo();
		logger.info("系统任务结束...");
	}
	
	//@Scheduled(cron = "0 0/1 * * * ?")
	public void chgCdnUrls() throws IOException {
		logger.info("缓存任务开始...");
		//cacheService.chgCdnUrls();
		logger.info("缓存任务结束...");
	}
	
	//@Scheduled(cron = "0 0 10,20 * * ?")
	public void updateIns() {
		logger.info("定时刷新开始...");
		ins.run();
		logger.info("定时刷新结束...");
	}

	@Scheduled(cron = "* 12 17 * * ?")
	public void cacheIns() {
		ins.syncCacheUrl();
	}
	
}
