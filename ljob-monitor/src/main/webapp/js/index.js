  $(document).ready(function(){
		
		$(".mokuai").find("li").click(function(){
			//console.log(this);
			//console.log($(this).find("a"));
			load($(this).find("a")[0]);
			//console.log($a);
		});
  });

	var contextPath = $("#contextPath").val();
	var loadImg = contextPath+'/images/loading.gif';
	
	var loadi;
	function waitShow(){
		loadi = layer.load('加载中…'); 
	}
	function waitHide(){
		//$("#wait").find('img').attr('src','');
		layer.close(loadi);
		//需关闭加载层时，执行layer.close(loadi)即可
	}
	function load(obj){
		$(obj).parent().parent().find("li").removeClass("active");
		$('.gzleftmenu').find("img").attr({ src: "/monitor/images/menu_t2.png"});
		//$(obj).css('display','block');
		var url = $(obj).attr("data").replace(/[ ]/g,"%20");
		var currentUser = $('#currentUser').val();
		if (url.indexOf("?") == -1){
			url = url + "?currentUser=" + currentUser;
		}else{
			url = url + "&currentUser=" + currentUser;
		}

		if($(obj).parent().attr("class")=="active"){
			$('#member_content').load(url);
			return;
		}
		if(url){
			$(obj).parent().addClass('active');
			$('#member_content').load(url);
		};
	};