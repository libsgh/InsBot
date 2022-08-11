var $$ = mdui.$;
function toBigImg() {
    $$(".opacityBottom").addClass("opacityBottom");//添加遮罩层
    $$(".opacityBottom").show();
    $$("html,body").addClass("none-scroll");//下层不可滑动
    $$(".bigImg").addClass("bigImg");//添加图片样式
    $$(".opacityBottom").on('click',function () {//点击关闭
	    $$("html,body").removeClass("none-scroll");
	    $$(".opacityBottom").remove();
	});
}
$$('#delete').on('click', function (e) {
	$$('#url').val("");
	var input = document.getElementById("url");
	input.blur();
});
$$('.search').on('click', function (e) {
	mdui.snackbar({
		  message: '获取中请稍后...'
	});
	$$.ajax({
		  method: 'POST',
		  url: '/search',
		  data: {link:$$('#url').val(),type:1},
		  success: function (data) {
			 if(data=="" || data==null){
				 mdui.snackbar({
					  message: '获取失败，请重新输入地址'
				});
				 return;
			 }
			 $$("#cards").html("");
			 $$.each(JSON.parse(data), function (i, item) {
					var html = '<div class="mdui-col">'+
					'	<div class="mdui-card">'+
					'		<div class="mdui-card-header">'+
					'    		<img class="mdui-card-header-avatar" src="'+item.pic_url+'"/>'+
					'  				<button class="mdui-btn mdui-btn-icon mdui-btn-dense mdui-color-theme-accent mdui-ripple mdui-float-right" onclick="downloadMedia(this)"><i class="mdui-icon material-icons">file_download</i></button>'+
					'    		<div class="mdui-card-header-title">'+item.username+'</div>'+
					'    		<div class="mdui-card-header-subtitle">'+item.fullname+'</div>'+
					'  		</div>'+
					'		<div class="mdui-card-media">'+
					'			<div class="swiper-container">'+
					'				<div class="swiper-wrapper">';
					$$.each(item.children, function (i, c) {
						html+='<div class="swiper-slide">';
						if(c.is_video){
						html+='			<video class="mdui-video-fluid" controls>'+
						'			  <source src="'+c.video_url+'" type="video/mp4">'+
						'			</video>';
						}else{
						html+='			<img src="'+c.display_url+'"/>';
						}
						html+='</div>';
					});
					html+='				</div>'+
					'			<div class="custom-button-next"></div>'+
					'			<div class="custom-button-prev"></div>'+
					'			<div class="swiper-pagination"></div>'+
					'			</div>'+
				    /*'<div class="mdui-card-menu">'+
				    '  <button class="mdui-btn mdui-btn-icon mdui-btn-dense mdui-color-theme-accent mdui-ripple" onclick="downloadMedia(this)"><i class="mdui-icon material-icons">file_download</i></button>'+
				    '</div>'+*/
					/*'			<div class="mdui-card-actions"><div class="mdui-center">';
					html+='				<select class="mdui-select">';
					$$.each(item.nodes,function(i, u){
						html+='<option value="/ins/download/'+u.src+'">'+u.width+' X '+u.height+'</option>';
					});
					html+='				</select>';
					html+='				<button class="mdui-btn mdui-btn-icon mdui-btn-dense mdui-color-theme-accent mdui-ripple" onclick="downloadMedia(this)"><i class="mdui-icon material-icons">file_download</i></button>'+
					'			</div></div>'+*/
					'		</div>'+
					'	</div>'+
					'</div>';
					$$("#cards").append(html);
			 });
			 new Swiper('.swiper-container', {
				  navigation: {
				        prevEl: '.custom-button-prev',
				        nextEl: '.custom-button-next',
				  },
			      pagination: {
			        el: '.swiper-pagination',
			      },
			 });
			 $$('img').on('click', function () {
			    var imgsrc = $$(this).attr("src");
			    var opacityBottom = '<div class="opacityBottom" style = "display:none"><img class="bigImg" src="' + imgsrc + '"></div>';
			    $$(document.body).append(opacityBottom);
			    toBigImg();//变大函数
			});
		  }
	});
});
function downloadMedia(obj){
	var ddo = $$(obj).parent().parent().children('.mdui-card-media').children('.swiper-container').children('.swiper-wrapper').children('.swiper-slide-active');
	if(ddo.html().indexOf('img') != -1){
		window.location.href = ddo.children('img').attr('src');
	}else{
		window.location.href = ddo.children('video').children('source').attr('src');
	}
}