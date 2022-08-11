package com.ins.bot.common;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.media.ImageVersionsMeta;
import com.github.instagram4j.instagram4j.models.media.timeline.*;
import com.github.instagram4j.instagram4j.models.user.User;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaInfoRequest;
import com.github.instagram4j.instagram4j.requests.users.UsersUsernameInfoRequest;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaInfoResponse;
import com.github.instagram4j.instagram4j.responses.users.UserResponse;
import com.github.instagram4j.instagram4j.utils.IGUtils;
import com.ins.bot.bean.Child;
import com.ins.bot.bean.Node;
import com.ins.bot.bean.ThumbnailResources;
import com.ins.bot.bean.UserInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InsUtil {

    @Value("${download.path}")
    private String path;

    @Value("${IG_USER_NAME}")
    private String igUserName;

    @Value("${IG_USER_PASSWORD}")
    private String igUserPassword;

    public IGClient createClient(){
        try {
            File clientFile = new File(path + File.separator + igUserName + "_client.txt");
            File cookieFile = new File(path + File.separator + igUserName + "_cookie.txt");
            if(FileUtil.exist(cookieFile) && FileUtil.exist(clientFile)){
                IGClient deserializedClient = IGClient.deserialize(clientFile, cookieFile);
                if(deserializedClient.isLoggedIn()){
                    return deserializedClient;
                }
            }
            OkHttpClient httpClient = IGUtils.defaultHttpClientBuilder().build();
            IGClient client = IGClient.builder()
                    .username(igUserName)
                    .password(igUserPassword)
                    .client(httpClient)
                    .login();
            client.serialize(clientFile, cookieFile);
            return client;
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取用户信息
     * @param username
     * @return
     */
    public UserInfo getUserInfo(String username){
        try {
            IGClient client = createClient();
            if(client != null){
                UserResponse usernameInfoRequest = new UsersUsernameInfoRequest(username)
                        .execute(client).join();
                User user = usernameInfoRequest.getUser();
                UserInfo userInfo = new UserInfo();
                userInfo.setId(user.getPk().toString());
                userInfo.setFull_name(user.getFull_name());
                userInfo.setBiography(user.getBiography());
                userInfo.setEdge_follow((long)user.getFollowing_count());
                userInfo.setEdge_followed_by((long)user.getFollower_count());
                userInfo.setExternal_url(user.getExternal_url());
                userInfo.setUsername(user.getUsername());
                userInfo.setProfile_pic_url_hd(PicBed.upload(user.getProfile_pic_url()));
                userInfo.setIs_verified(user.is_verified());
                userInfo.setTiez((long)user.getMedia_count());
                return userInfo;
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取用户帖子
     * @param username
     * @param lastId
     * @return
     */
    public List<Node> getUserFeed(String username, String lastId){
        List<Node> nodes = new ArrayList<>();
        List<TimelineMedia> list = new ArrayList<>();
        try {
            IGClient client = createClient();
            if(client != null){
                UserResponse usernameInfoRequest = new UsersUsernameInfoRequest(username)
                        .execute(client).join();
                FeedUserResponse userFeed =
                        new FeedUserRequest(usernameInfoRequest.getUser().getPk())
                                .execute(client).join();
                log.info("media count: "+usernameInfoRequest.getUser().getMedia_count());
                list.addAll(userFeed.getItems());
                if(!this.contains(list, lastId)){
                    if(StrUtil.isNotBlank(userFeed.getNext_max_id())){
                        this.getAllItems(usernameInfoRequest.getUser().getPk(), userFeed.getNext_max_id(), list, lastId);
                    }
                }
                log.info("items: "+list.size());
                nodes = list.parallelStream().map(r->{
                    Node node = new Node();
                    node.setUserId(usernameInfoRequest.getUser().getPk().toString());
                    node.setCommentCount((long)r.getComment_count());
                    node.setTimestamp(r.getTaken_at());
                    node.setId(r.getId());
                    node.setTimestamp(r.getTaken_at());
                    node.setShortcode(r.getCode());
                    node.setLikeCount((long)r.getLike_count());
                    node.setUname(usernameInfoRequest.getUser().getUsername());
                    node.setText(r.getCaption() == null || StrUtil.isBlank(r.getCaption().getText())?"":StrUtil.utf8Str(r.getCaption().getText()));
                    node.setSoureType("ins");
                    //三种媒体类型：图片1、视频2、轮播8
                    if(r.getMedia_type().equals("1")){
                        //图片
                        TimelineImageMedia image = (TimelineImageMedia) r;
                        List<ImageVersionsMeta> images = image.getImage_versions2().getCandidates();
                        node.setIs_video(false);
                        node.setSrcs(buildThumbnailSources(images));
                        node.setDisplay_url(PicBed.upload(images.get(0).getUrl()));
                        node.setVideo_url("");
                        node.setChildren(ListUtil.list(false, new Child(node.getDisplay_url(), false, "")));
                    }else if(r.getMedia_type().equals("2")){
                        //视频
                        TimelineVideoMedia video = (TimelineVideoMedia) r;
                        List<ImageVersionsMeta> images = video.getImage_versions2().getCandidates();
                        node.setIs_video(true);
                        node.setSrcs(buildThumbnailSources(images));
                        node.setDisplay_url(PicBed.upload(images.get(0).getUrl()));
                        node.setVideo_url(PicBed.uploadVideo(video.getVideo_versions().get(0).getUrl()));
                        node.setChildren(ListUtil.list(false, new Child(node.getDisplay_url(), true, node.getVideo_url())));
                    }else if(r.getMedia_type().equals("8")){
                        //轮播
                        TimelineCarouselMedia carouse = (TimelineCarouselMedia) r;
                        List<CarouselItem> items = carouse.getCarousel_media();
                        node.setIs_video(false);
                        node.setVideo_url("");
                        node.setSrcs(buildThumbnailSources(items.get(0)));
                        node.setChildren(items.stream().map(i->{
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
                        node.setDisplay_url(node.getChildren().get(0).getDisplay_url());
                    }
                    return node;
                }).collect(Collectors.toList());
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
        return nodes;
    }

    private List<ThumbnailResources> buildThumbnailSources(List<ImageVersionsMeta> images) {
        return images.stream().map(r -> {
            ThumbnailResources thumbnail = new ThumbnailResources();
            thumbnail.setSrc(PicBed.upload(r.getUrl()));
            thumbnail.setConfig_height(r.getHeight());
            thumbnail.setConfig_width(r.getWidth());
            return thumbnail;
        }).collect(Collectors.toList());
    }

    private List<ThumbnailResources> buildThumbnailSources(CarouselItem carouselItem) {
        if(carouselItem instanceof ImageCarouselItem){
            return ((ImageCarouselItem)carouselItem).getImage_versions2().getCandidates().stream().map(r -> {
                ThumbnailResources thumbnail = new ThumbnailResources();
                thumbnail.setSrc(PicBed.upload(r.getUrl()));
                thumbnail.setConfig_height(r.getHeight());
                thumbnail.setConfig_width(r.getWidth());
                return thumbnail;
            }).collect(Collectors.toList());
        } else {
            return ((VideoCarouselItem)carouselItem).getImage_versions2().getCandidates().stream().map(r -> {
                ThumbnailResources thumbnail = new ThumbnailResources();
                thumbnail.setSrc(PicBed.upload(r.getUrl()));
                thumbnail.setConfig_height(r.getHeight());
                thumbnail.setConfig_width(r.getWidth());
                return thumbnail;
            }).collect(Collectors.toList());
        }

    }

    private List<ThumbnailResources> buildThumbnailSources(VideoCarouselItem videoCarouselItem) {
        return videoCarouselItem.getImage_versions2().getCandidates().stream().map(r -> {
            ThumbnailResources thumbnail = new ThumbnailResources();
            thumbnail.setSrc(r.getUrl());
            thumbnail.setConfig_height(r.getHeight());
            thumbnail.setConfig_width(r.getWidth());
            return thumbnail;
        }).collect(Collectors.toList());
    }

    /**
     * 递归获取所有用户帖子
     * @param pk
     * @param next_max_id
     * @param list
     */
    private void getAllItems(Long pk, String next_max_id, List<TimelineMedia> list, String lastId) {
        try {
            IGClient client = createClient();
            if(client != null){
                FeedUserResponse userFeed =
                        new FeedUserRequest(pk, next_max_id)
                                .execute(client).join();
                list.addAll(userFeed.getItems());
                this.contains(list, lastId);
                if(!this.contains(list, lastId) && StrUtil.isNotBlank(userFeed.getNext_max_id())){
                    this.getAllItems(pk, userFeed.getNext_max_id(), list, lastId);
                }
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
    }

    private Boolean contains(List<TimelineMedia> list, String lastId) {
        if(StrUtil.isBlank(lastId)){
            return false;
        }
        return list.parallelStream().filter(r -> r.getId().equals(lastId)).findAny().isPresent();
    }

    public List<TimelineMedia> getMediaFromCode(String mediaCode){
        try {
            IGClient client = createClient();
            if(client != null){
                MediaInfoResponse resp = new MediaInfoRequest(getMediaIdFromURL(mediaCode)).execute(client).join();
                return resp.getItems();
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    public static String getMediaIdFromURL(String mediaCode) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        long id = 0;
        for (int i = 0; i < mediaCode.length(); i++) {
            char c = mediaCode.charAt(i);
            id = id * 64 + alphabet.indexOf(c);
        }
        return id + "";
    }

}
