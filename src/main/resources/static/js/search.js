var $$ = mdui.JQ;
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
			 $$.each(eval("("+data+")"), function (i, item) {
					var html = '<div class="mdui-col">'+
					'	<div class="mdui-card">'+
					'		<div class="mdui-card-header">'+
					'    		<img class="mdui-card-header-avatar" src="/ins/download/'+item.pic_url+'"/>'+
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
						'			  <source src="/ins/download/'+c.video_url+'" type="video/mp4">'+
						'			</video>';
						}else{
						html+='			<img src="/ins/download/'+c.display_url+'"/>';
						}
						html+='</div>';
					});
					html+='				</div>'+
					'			<div class="swiper-button-next"></div>'+
					'			<div class="swiper-button-prev"></div>'+
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
			        nextEl: '.swiper-button-next',
			        prevEl: '.swiper-button-prev',
			        hideOnClick: true,
			      },
			      pagination: {
			        el: '.swiper-pagination',
			        hideOnClick: true,
			      },
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