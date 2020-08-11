var $$ = mdui.JQ;
var pageNum = 1;
var clipboard = new ClipboardJS('.mdui-card-content');
function loadMore(id, isMobile){
	$.get("/loadMore?userId="+id+"&pageNum="+(pageNum+1), function(data){
	pageNum = pageNum + 1;
	 $.each(data, function (index, x) {
	   var  html = "<div class=\"mdui-col\"> "+
		 "   <div class=\"mdui-grid-tile mdui-card mdui-hoverable\"> ";
	   console.log(isMobile);
		 if(isMobile == false){
			 html+= "    <div class=\"mdui-card-media\" style=\"height: 312px\">";
		 }else{
			 html+= "    <div class=\"mdui-card-media\">";
		 }
		 if(x.is_video){
			 html += "    <input type=\"hidden\" class=\"html5lightbox\" data-group=\"gallery_"+x.id+"\" href=\""+x.video_url+"\" />";
			 html += "     <a class=\"html5lightbox\" data-group=\"gallery_"+x.id+"\" href=\""+x.video_url+"\"> "+
		 "      <video class=\"mdui-video-fluid\" webkit-playsinline=\"\" style=\"height: 100%;width: 100%\" playsinline=\"\" controls=\"\" poster=\""+x.srcs[0].src+"\"> "+
		 "       <source src=\""+x.video_url+"\" type=\"video/mp4\"></source> "+
		 "      </video> </a> "+
		 "     <div class=\"mdui-card-menu\"> ";
		 if(isMobile || isMobile != "false"){
			 html += "      <button class=\"mdui-btn mdui-btn-icon mdui-text-color-white\"><i class=\"mdui-icon material-icons\">videocam</i></button> ";
		 }else{
			 html += "      <button class=\"mdui-btn mdui-btn-icon mdui-text-color-white\"  onclick=\"downloadFiles(this)\"><i class=\"mdui-icon material-icons\">videocam</i></button> ";
		 }
		 html += "     </div>";
		 }else{
			 html+="    <a class=\"html5lightbox\" data-group=\"gallery_"+x.id+"\" href=\""+x.display_url+"\"> <img decoding=\"auto\" srcset=\""+x.srcset+"\" /></a>";
			 html+="    <input type=\"hidden\" class=\"html5lightbox\" data-group=\"gallery_"+x.id+"\" href=\""+x.display_url+"\" />";
			 if(x.children){
				 $.each(x.children.edges, function (index, c) {
					 if(c.is_video){
						 html+="     <input type=\"hidden\" class=\"html5lightbox\" data-group=\"gallery_"+x.id+"\" href=\""+c.video_url+"\" /> ";
					 }else{
						 html+="     <input type=\"hidden\" class=\"html5lightbox\" data-group=\"gallery_"+x.id+"\" href=\""+c.display_url+"\" /> ";
					 }
				 });
				 if(isMobile == true){
					 html+= "<div class=\"mdui-card-menu\"> "+
					 "      <button class=\"mdui-btn mdui-btn-icon mdui-text-color-white\"><i class=\"mdui-icon material-icons\">photo_library</i></button> "+
					 "     </div>";
				 }else{
					 html+= "<div class=\"mdui-card-menu\"> "+
					 "      <button class=\"mdui-btn mdui-btn-icon mdui-text-color-white\" onclick=\"downloadFiles(this)\"><i class=\"mdui-icon material-icons\">photo_library</i></button> "+
					 "     </div>";
				 }
			 }else{
				 if(isMobile==true){
					 html+= "<div class=\"mdui-card-menu\"> "+
					 "      <button class=\"mdui-btn mdui-btn-icon mdui-text-color-white\"><i class=\"mdui-icon material-icons\">insert_photo</i></button> "+
					 "     </div>";
				 }else{
					 html+= "<div class=\"mdui-card-menu\"> "+
					 "      <button class=\"mdui-btn mdui-btn-icon mdui-text-color-white\" onclick=\"downloadFiles(this)\"><i class=\"mdui-icon material-icons\">insert_photo</i></button> "+
					 "     </div>";
				 }
			 }
		 }
		 /*"     <div class=\"mdui-card-media-covered  mdui-card-media-covered-gradient mdui-card-media-covered-top\"  style=\"display: none\"> "+
		 "      <div class=\"mdui-card-primary\"> "+
		 "       <i class=\"mdui-icon material-icons mdui-text-color-white\">favorite</i> "+x.likeCount+" "+
		 "       <i class=\"mdui-icon material-icons mdui-text-color-white\">chat_bubble</i> "+x.commentCount+" "+
		 "      </div> "+
		 "     </div> "+*/
		 html+="    </div> "+
		 "    <div class=\"mdui-card-content\" title=\""+x.text+"\" data-clipboard-text=\""+x.text+"\">";
			 html+="<p class=\"mdui-text-truncate mdui-float-left\" style=\"max-width: 160px;\">"+x.text+"</p><p class=\"mdui-float-right mdui-text-color-black-disabled\">"+x.timeF+"</p>";
		 html+= "    </div> "+
		 "   </div> "+
		 "  </div>";
	   $("#cards").append(html);
	   clipboard = new ClipboardJS('.mdui-card-content');
	 });
	$(".html5lightbox").html5lightbox();
	/*$('html, body').animate({
        scrollTop: $('html, body').height()
    }, 'slow');*/
	});
}

function downloadFiles (obj) {
	$(obj).parent().parent().find("input:hidden").each(function(){
		var url = $(this).attr("href");
		var filename = url.match(/^.+\/(\w+\.\w+)/i)[1];
		var type;
		if(filename.indexOf("jpg") != -1){
			//下载的是图片
			type = "image/jpg";
		}else if(filename.indexOf("mp4") != -1){
			type = "video/mpeg4";
		}
		setTimeout(function() {
			var x = new XMLHttpRequest();
			x.open("GET", url, true);
			x.responseType = 'blob';
			x.onload=function(e){download(x.response, filename ); }
			x.send();
		}, 1000);
	});
}

function downloadAll(uname){
	mdui.snackbar({
	  message: '正在打包下载，请耐心等候'
	});
	var arr = [];
	$("input[type=hidden]").each(function(){
		var id = $(this).attr("data-group").replace(/gallery_/g, "");
		arr.push(id);
	});
	$.ajax({
	  method: 'POST',
	  url: './compress/download?ids='+distinct(arr, arr).join(",")+'&uname='+uname,
	  success: function(data){
		if(data.errorCode == -1){
			mdui.snackbar({
				  message: '操作失败，请稍后重试'
			});
		  }else{
			location.href="/down/"+data.fileName;
		  }
	  }
	});
	
}
$("body").on("mouseenter", ".mdui-col", function() {
    $(this).find(".mdui-card-media-covered-top").show();
});
$("body").on("mouseleave", ".mdui-col", function() {
    $(this).find(".mdui-card-media-covered-top").hide();
});
function distinct(a, b) {
    let arr = a.concat(b)
    let result = []
    for (let i of arr) {
        !result.includes(i) && result.push(i)
    }
    return result
}
$$(".timeF").each(function (i, item) {
	var c = $$(this).html();
	$$(this).html(timeago(c));
});
function timeago(dateTimeStamp){
    var minute = 1000 * 60;
    var hour = minute * 60;
    var day = hour * 24;
    var week = day * 7;
    var halfamonth = day * 15;
    var month = day * 30;
    var now = new Date().getTime();
    var diffValue = now - dateTimeStamp;
    if(diffValue < 0){
        return "";
    }
    var minC = diffValue/minute;
    var hourC = diffValue/hour;
    var dayC = diffValue/day;
    var weekC = diffValue/week;
    var monthC = diffValue/month;
    if(monthC >= 1 && monthC <= 3){
        result = " " + parseInt(monthC) + "月前";
    }else if(weekC >= 1 && weekC <= 3){
        result = " " + parseInt(weekC) + "周前";
    }else if(dayC >= 1 && dayC <= 6){
        result = " " + parseInt(dayC) + "天前";
    }else if(hourC >= 1 && hourC <= 23){
        result = " " + parseInt(hourC) + "小时前";
    }else if(minC >= 1 && minC <= 59){
        result =" " + parseInt(minC) + "分钟前";
    }else if(diffValue >= 0 && diffValue <= minute){
        result = "刚刚";
    }else {
        var datetime = new Date();
        datetime.setTime(dateTimeStamp);
        var Nyear = datetime.getFullYear();
        var Nmonth = datetime.getMonth() + 1 < 10 ? "0" + (datetime.getMonth() + 1) : datetime.getMonth() + 1;
        var Ndate = datetime.getDate() < 10 ? "0" + datetime.getDate() : datetime.getDate();
        var Nhour = datetime.getHours() < 10 ? "0" + datetime.getHours() : datetime.getHours();
        var Nminute = datetime.getMinutes() < 10 ? "0" + datetime.getMinutes() : datetime.getMinutes();
        var Nsecond = datetime.getSeconds() < 10 ? "0" + datetime.getSeconds() : datetime.getSeconds();
        result = Nyear + "-" + Nmonth + "-" + Ndate;
    }
    return result;
}
/*$(window).bind("scroll", function () {
    if(getScrollHeight() == getDocumentTop() + getWindowHeight()){
        //当滚动条到底时,触发内容
    	loadMore($$(".loadMore").attr("data-id"));
    	alert("到底部了");
    }
});*/
function getDocumentTop() {
    var scrollTop =  0, bodyScrollTop = 0, documentScrollTop = 0;
    if (document.body) {
        bodyScrollTop = document.body.scrollTop;
    }
    if (document.documentElement) {
        documentScrollTop = document.documentElement.scrollTop;
    }
    scrollTop = (bodyScrollTop - documentScrollTop > 0) ? bodyScrollTop : documentScrollTop;
    return scrollTop;
}
function getWindowHeight() {
    var windowHeight = 0;
    if (document.compatMode == "CSS1Compat") {
        windowHeight = document.documentElement.clientHeight;
    } else {
        windowHeight = document.body.clientHeight;
    }
    return windowHeight;
}
function getScrollHeight() {
    var scrollHeight = 0, bodyScrollHeight = 0, documentScrollHeight = 0;
    if (document.body) {
        bodyScrollHeight = document.body.scrollHeight;
    }
    if (document.documentElement) {
        documentScrollHeight = document.documentElement.scrollHeight;
    }
    scrollHeight = (bodyScrollHeight - documentScrollHeight > 0) ? bodyScrollHeight : documentScrollHeight;
    return scrollHeight;
}
clipboard.on('success', function(e) {
	mdui.snackbar({message: '已复制到剪切板'});
});