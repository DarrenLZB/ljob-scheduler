<%@ page language="java" import="java.util.*,java.net.URL"	pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<% String contextPath = request.getContextPath();%>
<!DOCTYPE html>
<html>
<head>
<title>LJOB Server</title>
<meta charset="utf-8"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, uer-scalable=no"/>
<meta name="description" content=""/>
<link type="text/css" rel="stylesheet" href="<%=contextPath%>/css/bootstrap.css" />
<link type="text/css" rel="stylesheet" href="<%=contextPath%>/css/zuoRcss.css" />
<script type="text/javascript" src="<%=contextPath%>/js/jquery-1.11.0.min.js"></script>
</head>
<body style="text-align: center;">
<div class="loginTop">
    <div style="padding-top: 70px; width: 1118px; height: 220px; margin-left: auto; margin-right: auto;">
        <img src="<%=contextPath%>/images/login_line.png" style="float: left; padding-top: 60px; padding-right: 20px;" />
        <img src="<%=contextPath%>/images/login_icon_camera.png" style="float: left; padding-top: 15px; padding-right: 26px;"/>
        <div style="margin-left: auto; margin-right: auto; font-size: 40px; color: #fff; float: left; width: 240px; padding-top: 30px;">LJOB Server</div>
        <img src="<%=contextPath%>/images/login_line.png" style="-moz-transform: scale(-1,1); float: left; padding-top: 60px; margin-left: 20px;" />
    </div>
    <form action="<%=contextPath%>/login/do_login" method="post" >
    <div style="background:url(../images/login_input_bg.png); width: 489px; height: 333px; margin-left: auto; margin-right: auto; overflow: hidden;">
        <div style="background:url(../images/login_user_input.png); width: 342px; height: 56px; margin-left: auto; margin-right: auto; margin-top: 50px;">
            <img src="<%=contextPath%>/images/user.png" style="float: left; margin-top: 17px; margin-left: 20px;" />
            <input type="text" value="${loginName}" id="loginName" name="loginName" placeholder="username"
                   style="height: 56px; width: 260px; font-size: 16px; border-style: none; background-color: transparent;">
        </div>
        <div style="background:url(../images/login_user_input.png); width: 342px; height: 56px; margin-left: auto; margin-right: auto; margin-top: 14px;">
            <img src="<%=contextPath%>/images/password.png" style="float: left; margin-top: 17px; margin-left: 22px;" />
            <input type="password" value="" id="password" name="password" placeholder="password"
                   style="height: 56px; width: 260px; font-size: 16px; border-style: none; background-color: transparent;">
        </div>
        <div style="background:url(../images/login_input.png); width: 342px; height: 56px; margin-left: auto; margin-right: auto; margin-top: 22px;">
            <button id="submit" type="submit" style="width: 340px; height: 55px; border-style: none; background-color: transparent; font-size: 20px; color: #fff;">Login</button>
        </div>
        <c:if test="${not empty msg}">
        <div class="login-form2">
		    <span style="color: #E50140; font-size: 14px; display: block; line-height: 25px;">${msg}</span>
	    </div>
	    </c:if>
    </div>
    </form>
    
</div>
</body>
<script type="text/javascript">
	$(document).ready(function(){
		if($(".gzheader").length>0){
			//console.log($(".gzheader"));
			window.location.href="<%=contextPath%>/login/do_login";
		}
		if(top!=self){
			var url = window.location.href;
			window.top.location.href=url;
		}
		
	});
	
</script>
</html>
