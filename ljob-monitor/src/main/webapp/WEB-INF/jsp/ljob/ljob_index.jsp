<%@ page language="java" pageEncoding="utf-8"%>
<% String contextPath = request.getContextPath();%>
<script type="text/javascript" src="<%=contextPath%>/js/jquery-1.11.0.min.js"></script>
<script type="text/javascript" src="<%=contextPath%>/js/My97DatePicker/WdatePicker.js"></script>
<script type="text/javascript" src="<%=contextPath%>/js/layer/layer.min.js"></script>
<link href="<%=contextPath%>/css/businessLog.css" rel="stylesheet" type="text/css" />
<link href="<%=contextPath%>/css/comPopupLayer.css" rel="stylesheet" type="text/css" />
	<div style="padding-top: 20px; width: 100%; margin: 0 auto; background: #fff; height: 100%;">
		<div>
			<input type="button" onclick="queryLjobs();" value="Refresh" class="btn btn-default"> 
		</div>
		<div class="conrightline" style="margin-top: 10px;"></div>
		<div id="list_data"></div>
		<div class="line20"></div>
		<div class="conrightline" style="margin-top: 10px;"></div>
		<div>
			<input type="button" onclick="queryLjobs();" value="Refresh" class="btn btn-default">
		</div>
	</div>
<script>

var loadi;
function waitShow(){
	loadi = layer.load('Loading…'); 
}

function waitHide(){
	layer.close(loadi);
}

function queryLjobs(){
	waitShow();
	var params={};
	params['currentUser']=$("#currentUser").val();
	$.post("<%=contextPath%>/ljob/get_online_ljobs", params, function(data) {
		$("#list_data").html(data);
		waitHide();
	});
}

function toChangeCron(groupName, jobName, ip){
	waitShow();
	var params={};
	params['ip']=ip;
	params['groupName']=groupName;
	params['jobName']=jobName;
	params['currentUser']=$("#currentUser").val();
	$.post("<%=contextPath%>/ljob/to_change_cron", params, function(data) {
		$("#list_data").html(data);
		waitHide();
	});
}

function sendChangeCron(){
	var groupName = $("#groupName").val();
	var jobName = $("#jobName").val();
	var ip = $("#ip").val();
	var cron = $("#cron").val();
	sendLjobCommand(groupName, jobName, ip, "2", cron, null);
	queryLjobs();
}

function toCustomRun(groupName, jobName, ip){
	waitShow();
	var params={};
	params['ip']=ip;
	params['groupName']=groupName;
	params['jobName']=jobName;
	params['currentUser']=$("#currentUser").val();
	$.post("<%=contextPath%>/ljob/to_custom_run", params, function(data) {
		$("#list_data").html(data);
		waitHide();
	});
}

function sendCustomRun(){
	var groupName = $("#groupName").val();
	var jobName = $("#jobName").val();
	var ip = $("#ip").val();
	var customParams = $("#customParams").val();
	sendLjobCommand(groupName, jobName, ip, "4", null, customParams);
	queryLjobs();
}

function sendLjobCommand(groupName, jobName, ip, command, cron, customParams){
	waitShow();
	var params={};
	params['ip']=ip;
	params['groupName']=groupName;
	params['jobName']=jobName;
	params['command']=command;
	params['cron']=cron;
	params['customParams']=customParams;
	params['currentUser']=$("#currentUser").val();
	$.post("<%=contextPath%>/ljob/send_ljob_command", params, function(data) {
		alert(data);
		waitHide();
	});
}

$(document).ready(function() {
	queryLjobs();
});

</script>
</html>
