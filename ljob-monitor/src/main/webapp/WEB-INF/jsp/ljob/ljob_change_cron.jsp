<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
	String contextPath = request.getContextPath();
%>
<c:if test="${!empty job}">
    <table class="imagetable" style="width: 1000px;">
        <tr style="text-align: left;">
            <td style="text-align: left; width: 250px; font-weight: bold;">IP</td>
            <td style="text-align: left;">${job.ip}</td>
        </tr>
        <tr style="text-align: left;">
            <td style="text-align: left; width: 250px; font-weight: bold;">Group Name</td>
            <td style="text-align: left;">${job.groupName}</td>
        </tr>
        <tr style="text-align: left;">
            <td style="text-align: left; width: 250px; font-weight: bold;">Job Name</td>
            <td style="text-align: left;">${job.jobName}</td>
        </tr>
        <tr style="text-align: left;">
            <td style="text-align: left; width: 250px; font-weight: bold;">Current CronExpression</td>
            <td style="text-align: left;">${job.jobCronExpression}</td>
        </tr>
        <tr style="text-align: left;">
            <td style="text-align: left; width: 250px; font-weight: bold;">New CronExpression</td>
            <td style="text-align: left;"><input class="form-control Wdate" type="text" name="cron" id="cron" value="" style="width: 700px;"></td>
        </tr>
    </table>
	<div class="line10"></div>
		<div>
			<input type="hidden" name="ip" id="ip" value="${job.ip}">
			<input type="hidden" name="groupName" id="groupName" value="${job.groupName}">
			<input type="hidden" name="jobName" id="jobName" value="${job.jobName}">
		</div>
		<div>
		    <input type="button" onclick="sendChangeCron();" value="Change CronExpression" class="btn btn-default" style="width: 220px;">
		</div>
</c:if>
<c:if test="${empty job}">
	<div class="errorRed" style="text-align: center;">no job found</div>
</c:if>
