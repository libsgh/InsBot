package com.ins.bot.common;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.ins.bot.CompressDirective;
import com.jfinal.template.ext.spring.JFinalViewResolver;

public class MyViewResolver extends JFinalViewResolver implements ApplicationListener<ContextRefreshedEvent> {
	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		getEngine().addSharedObject("ctx", getServletContext().getContextPath()).addDirective("compress", CompressDirective.class);
	}
}
