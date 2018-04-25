# ljob-scheduler
描述：基于Redis的Java分布式定时任务调度与监控框架
Wiki：https://github.com/DarrenLZB/ljob-scheduler/wiki

# 依赖：
1. 安装JDK 1.8
2. 安装Redis服务
3. 安装Maven

# 运行定时任务例子ljob-scheduler-spring5-example
1. 修改ljob-scheduler-spring5-example\src\main\resources\application.properties下的Redis配置信息
2. 在ljob-scheduler-spring5-example下执行mvn install
3. 解压ljob-scheduler-spring5-example\target\ljob-scheduler-spring5-example-assembly.zip
4. 运行解压后的启动脚本ljob-scheduler-spring5-example-assembly\ljob-scheduler-spring5-example\bin\start.sh

# 运行定时任务调度监控中心ljob-monitor
1. 修改ljob-monitor\src\main\resources\application.properties下的Redis配置信息
2. 在ljob-monitor下执行mvn install
3. 解压ljob-monitor\target\ljob-monitor-assembly.zip
4. 运行解压后的启动脚本ljob-monitor-assembly\ljob-monitor\bin\start.sh
5. 访问：http://127.0.0.1:6060 登录平台，默认账号密码：root/root
