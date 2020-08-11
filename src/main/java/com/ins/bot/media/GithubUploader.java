package com.ins.bot.media;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.kohsuke.github.GHContentBuilder;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTreeBuilder;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.ins.bot.bean.GhCache;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;

@Service
public class GithubUploader implements Uploader{
	
	@Autowired
	private MongoTemplate template;
	
	@Value("${GITHUB_ACCESS_TOKEN}")
	private String accessToken;
	
	protected final Logger logger = LoggerFactory.getLogger(GithubUploader.class);
	
	public String repo = "";
	public final String user = "onedrive-x";
	public final int index = 0;
	@Override
	public String upload(File file, String uploadPath, String username) {
		if(StrUtil.isBlank(accessToken)) {
			logger.error("请在环境变量中设置GITHUB_ACCESS_TOKEN");
		}
		try {
			GitHub github = GitHubApi.getInstance(accessToken);
			if(StrUtil.isBlank(this.repo)) {
				checkRepo();
			}
			Thread.sleep(1500);
			GHRepository repo = github.getRepository(user+"/"+this.repo);
			GHRef masterRef = repo.getRef("heads/master");
	        String masterTreeSha = repo.getTreeRecursive("master", 1).getSha();
	        GHTreeBuilder treeBuilder = repo.createTree().baseTree(masterTreeSha);
			treeBuilder.add(uploadPath, FileUtil.readBytes(file), true);
			String treeSha = treeBuilder.create().getSha();
	        String commitSha = repo.createCommit()
	        	   .message(username + " updates")
	               .tree(treeSha)
	               .parent(masterRef.getObject().getSha())
	               .create()
	               .getSHA1();
	        masterRef.updateTo(commitSha);
			return "https://cdn.jsdelivr.net/gh/"+this.user+"/"+this.repo+"@master/" + uploadPath;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			logger.error(file.getName());
		}
		return "";
	}

	public Boolean checkUrlValid(String url) {
		int status = HttpRequest.get(url).execute().getStatus();
		if(status == HttpStatus.HTTP_NOT_FOUND) {
			return false;
		}else {
			return true;
		}
	}
	
	@Override
	public String type() {
		return "Github";
	}

	public void checkRepo() {
		try {
			GitHub github = GitHubApi.getInstance(accessToken);
			GHMyself myself = github.getMyself();
			Map<String, GHRepository> map = myself.getAllRepositories();
			Set<String> keys = map.keySet();
			int index = 0;
			for (String key : keys) {
				if(key.startsWith("media")) {
					index++;
					GhCache ghCache = new GhCache();
					ghCache.setName(key);
					ghCache.setUser(myself.getName());
					int size = map.get(key).getSize();
					ghCache.setSize(size);
					if(size > 870400) {
						//如果大于850M 设置仓库为false
						ghCache.setIsValid(false);
					}else {
						ghCache.setIsValid(true);
					}
					template.save(ghCache, "GhCache");
				}
			}
			GhCache gc = template.findOne(new Query(Criteria.where("isValid").is(true)), GhCache.class, "GhCache");
			if(gc == null) {
				String rep = "media-"+(index+1);
				GHRepository repoo = github.createRepository(rep).create();
				GHContentBuilder ghcb = repoo.createContent().content("# insbot图片视频缓存仓库")
						.message("init");
				ghcb.path("README.md");
				ghcb.commit();
				GhCache ghCache = new GhCache();
				ghCache.setName(rep);
				ghCache.setUser(myself.getName());
				ghCache.setIsValid(false);
				ghCache.setSize(0);
				template.save(ghCache, "GhCache");
				this.repo = rep;
			}else {
				this.repo = gc.getName();
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
