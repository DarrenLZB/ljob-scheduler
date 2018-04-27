<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<% String contextPath = request.getContextPath();%>
<div class="errorRed">
Current Server：${serverStatus }
</div>
<div class="line20"></div>
<c:if test="${!empty jobList}">
	<table class="imagetable" style="width: 1500px;">
		<tr style="width: 1500px;">
			<th style="width: 150px;">IP</th>
			<th style="width: 150px;">Group Name</th>
			<th style="width: 150px;">Job Name</th>
			<th style="width: 100px;">Distributed</th>
			<th style="width: 200px;">Job CronExpression</th>
			<th style="width: 170px;">Job Heartbeat</th>
			<th style="width: 140px;">Scheduling</th>
			<th style="width: 140px;">Working</th>
			<th style="width: 140px;">Custom Working</th>
		</tr>

		<c:forEach var="job" items="${jobList }" varStatus="status">
			<tr style="text-align: center;">
				<td style="text-align: center;">${job.ip}</td>
				<td style="text-align: center;">${job.groupName}</td>
				<td style="text-align: center;"><span title="${job.description}">${job.jobName}</span></td>
				<c:choose>
				    <c:when test="${job.distributed == 'true'}"><td style="text-align: center;">true</td></c:when>
				    <c:otherwise><td style="text-align: center;">false</td></c:otherwise>
				</c:choose>
				<td style="text-align: center;">${job.jobCronExpression}
				    <br /><input type="button" onclick="toChangeCron('${job.groupName}', '${job.jobName}', '${job.ip}');" value="Change CronExpression" class="btn btn-default" style="width: 190px;">
				</td>
				<td style="text-align: center;">${job.sendTimeShow}</td>
				<c:choose>
				    <c:when test="${job.scheduling == 'true'}">
				        <td style="text-align: center;color: #00DB00;">true
				            <c:if test="${job.supportCloseJob == 'true'}">
				            <br /><input type="button" onclick="sendLjobCommand('${job.groupName}', '${job.jobName}', '${job.ip}', '0', null, null);" value="Stop Schedule" class="btn btn-default" style="color: #EAC100; width: 130px;">
				            </c:if>
				        </td>
				    </c:when>
				    <c:otherwise>
				        <td style="text-align: center;color: #EAC100;">false
				            <br /><input type="button" onclick="sendLjobCommand('${job.groupName}', '${job.jobName}', '${job.ip}', '1', null, null);" value="Start Schedule" class="btn btn-default" style="color: #00DB00; width: 130px;">
				        </td>
				    </c:otherwise>
				</c:choose>
				<c:choose>
				    <c:when test="${job.working == 'true'}">
				        <td style="text-align: center;">true</td>
				    </c:when>
				    <c:otherwise>
				        <td style="text-align: center;">false
				            <c:if test="${job.supportInstantRunReq == 'true'}">
				            <br /><input type="button" onclick="sendLjobCommand('${job.groupName}', '${job.jobName}', '${job.ip}', '3', null, null);" value="Run Job" class="btn btn-default" style="color: #00DB00; width: 130px;">
				            </c:if>
				        </td>
				    </c:otherwise>
				</c:choose>
				<c:choose>
				    <c:when test="${job.customWorking == 'true'}">
				        <td style="text-align: center;">true</td>
				    </c:when>
				    <c:otherwise>
				        <td style="text-align: center;">false
				            <c:if test="${job.supportCustomRunReq == 'true'}">
				            <br /><input type="button" onclick="toCustomRun('${job.groupName}', '${job.jobName}', '${job.ip}');" value="Run Custom Job" class="btn btn-default" style="color: #00DB00; width: 130px;">
				            </c:if>
				        </td>
				    </c:otherwise>
				</c:choose>
			</tr>
		</c:forEach>
	</table>
</c:if>
<c:if test="${empty jobList}">
<div class="errorRed" style="text-align: center;">No job found!</div>
</c:if>
<script>
$(document).ready(function() {
	$(".popLayerPos2").hover(function() {
	    $("#runHistory", this).show();
	}, function() {
	    $("#runHistory", this).hide();
	});
});
</script>