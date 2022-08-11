package com.ins.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;

import com.ins.bot.common.MyViewResolver;
import com.ins.bot.task.Ins;
import com.jfinal.template.source.ClassPathSourceFactory;

@SpringBootApplication
@Controller
@EnableAsync
//@EnableScheduling
public class App implements ApplicationRunner {
	
	@Autowired
	private Ins ins;
	@Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"));
        return factory;
    }
	
	@Bean(name = "myViewResolver")
	public MyViewResolver getJFinalViewResolver(){
		MyViewResolver jf = new MyViewResolver();
		jf.setDevMode(true);
		jf.setSourceFactory(new ClassPathSourceFactory());
		//这里根据自己的目录修改，一般页面放到/templates下面
		jf.setPrefix("/templates/");
		jf.setSuffix(".html");
		jf.setContentType("text/html;charset=UTF-8");
		jf.setOrder(0);
		return jf;
	}
	
	public static void main(String[] args) {
		System.setProperty("socksProxyHost", "127.0.0.1");
		System.setProperty("socksProxyPort", "10808");
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		//ins.refreshCookie();
	
	}
	
}
