package com.ins.bot.media;

import java.io.IOException;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpConnector;
import org.kohsuke.github.extras.ImpatientHttpConnector;

public class GitHubApi {
	
	private static volatile GitHub INSTANCE;
	
	private GitHubApi() {
		
	}
	
	public static GitHub getInstance(String accessToken) {
		if(INSTANCE == null) {
			synchronized (GitHubApi.class) {
				if(INSTANCE == null) {
					try {
						INSTANCE =  new GitHubBuilder().withConnector(new ImpatientHttpConnector(HttpConnector.DEFAULT,1000000, 1000000)).withJwtToken(accessToken).build();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return INSTANCE;
	}
}
