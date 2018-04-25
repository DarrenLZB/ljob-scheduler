<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<%@ page language="java" import="java.util.*,java.net.URL"
	pageEncoding="utf-8"%>
<% String contextPath = request.getContextPath();%>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>LJOB Server</title>
<link href="<%=contextPath%>/css/zuoRcss.css" rel="stylesheet" type="text/css" />
<link href="<%=contextPath%>/css/bootstrap.css" rel="stylesheet" type="text/css" />
<link href="<%=contextPath%>/css/popModal.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="<%=contextPath%>/js/jquery-1.11.0.min.js"></script>
<script type="text/javascript" src="<%=contextPath%>/js/layer/layer.min.js"></script>
<script type="text/javascript" src="<%=contextPath%>/js/My97DatePicker/WdatePicker.js"></script>
<script type="text/javascript" src="<%=contextPath%>/js/index.js"></script>
<style type="text/css">
a{blr:e­xpression(this.onFocus=this.close());} /* 针对IE */
a:focus { -moz-outline-style: none; } /*针对firefox*/
</style>
</head>
<body>
<input type="hidden"  id="contextPath" name="contextPath" value="<%=contextPath%>"/>
<input type="hidden"  id="currentUser" name="currentUser" value="${currentUser}"/>
<div class="gzheader">
    <div class="gzhead">
        <ul class ="mokuai" style="padding-left: 0px;">
          <li>
              <a href="javascript:void(0);" class="topaaa" alt="monitor"
                 data="/ljob/to_ljob_index" onclick="load(this);">
              <div class="ljobMonitor" style="background:url(../images/ic_time.png); width: 28px; height: 28px; margin-top: 20px; margin-bottom: 12px; margin-left: auto; margin-right: auto;"></div>
              <p>Monitor</p>
              </a>
          </li>
        </ul>
        <dl style="padding-right: 50px;">
            <dd class="nonal" ><a href="/login/login_out">Logout</a></dd>
        </dl>
    </div>
</div>
<div class="gzcon">
    <div id="member_content"></div>
</div>
</body>
</html>



