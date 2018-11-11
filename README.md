# Spring Boot 整合 Quartz 示例
## 示例简介
使用SpringBoot, Quartz 再结合邮件组件，构建一个发送邮件的作业调度
> 参考： https://www.callicoder.com/spring-boot-quartz-scheduler-email-scheduling-example/
## 构建步骤
### 创建应用程序
 - idea -> File -> New -> Project...-> Spring Initializr
 - 选择 http://start.spring.io -> Next
 - Artifact 字段填"quartz-demo"
 - 依赖组件选择：Web, JPA, MySQL, Quartz, Mail

### 目录结构预览
 - com.example.quartzdemo
     - controller // 控制器
         - [EmailJobSchedulerController](https://github.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/blob/master/src/main/java/com/example/quartzdemo/controller/EmailJobSchedulerController.java) // Email作业的调度控制器
     - job // 作业类包
         - [EmailJob](https://github.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/blob/master/src/main/java/com/example/quartzdemo/job/EmailJob.java) // Email发送类
     - payload // 有效负载类（DTO类）包
         - [ScheduleEmailRequest](https://github.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/blob/master/src/main/java/com/example/quartzdemo/payload/ScheduleEmailRequest.java)     // 调度Email的请求类
         - [ScheduleEmailResponse](https://github.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/blob/master/src/main/java/com/example/quartzdemo/payload/ScheduleEmailResponse.java)    // 调度Email的响应类
         
### 写配置文件
```properties
## Spring DATASOURCE (数据源配置)
spring.datasource.url = jdbc:mysql://localhost:3306/quartz_demo?useSSL=false&serverTimezone=GMT
spring.datasource.username = root
spring.datasource.password = 123456

## QuartzProperties  (任务调度配置，job-store-type是存储类型，使用数据库存储任务调度相关的信息)
spring.quartz.job-store-type = jdbc
spring.quartz.properties.org.quartz.threadPool.threadCount = 5

## MailProperties (邮件配置，这里以qq邮箱发信作为例子)
spring.mail.host=smtp.qq.com
spring.mail.port=465
spring.mail.username=这里填发信邮箱
spring.mail.password=这里填发信邮箱的密码(QQ邮箱这里使用授权码)
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.starttls.enable=true
```
### 邮件配置
 - 打开QQ邮箱设置，找到发信设置，启用SMTP发信
 - 获取授权码，填于spring.mail.password属性处

### 数据库配置
 - 创建一个名为quartz_demo的数据库，数据库的用户名和密码为root/123456
 - 下载quartz_demo.sql脚本，链接：https://github.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/blob/master/src/main/resources/quartz_tables.sql
 - 切换到quartz_demo数据库，使用"source quartz_demo.sql脚本路径"方式导入quartz_demo.sql脚本，创建相关表

### Quartz Scheduler的API和术语概述
 - Scheduler：把它理解为“安排”，它用于安排增加删除“作业(任务)”和“触发器”及两者间的关系
 - Job：“作业(任务)”的接口，包含一个execute的方法。
 - JobDetail：“作业”的示例，他还包含JobDataMap行使的其他数据，在执行时传递给Job
 - Trigger：“触发器”，用于定义作业的执行计划，“作业”和“触发器”的关系是一对多
 - JobBuilder：用于构建JobDetail实例的构建器
 - TriggerBuilder：用于构建Trigger实例的构建器
 
### 创建类
  - 创建用于控制器中 “scheduleEmail”这个API的请求和响应(有效负载)的DTO类。 ->简单说就是装传输数据的类
     - 创建 ScheduleEmailRequest   装scheduleEmailAPI的请求数据类
     - 创建 ScheduleEmailResponse   装scheduleEmailAPI的响应数据类
     - 创建 EmailJobSchedulerController  用于定义调度邮件任务的scheduleEmail的API接口，以及定义构建作业实例和触发器示例的方法
        - Spring Boot内置了对Quartz的支持。它会自动创建一个Quartz的Schedulerbean，其中包含我们在application.properties文件中提供的配置。这就是我们可以直接注入Scheduler控制器的原因。
     - 创建 EmailJob 用于实际发送Email的作业类
        - Spring Boot提供了一个名为Quartz Scheduler的Job接口的包装器QuartzJobBean
        - 所以只需要定义一个作业类去实现这个包装器即可
   
### 工作流程
  - 我们首先验证请求正文（即向**/scheduleEmail**这个接口发送一个ScheduleEmailRequest对象去请求调度邮件发送作业）
   
  - 然后，使用包含收件人电子邮件，主题和正文的JobDataMap(这些信息都来自我们的请求正文)构建JobDetail实例。在JobDetail我们创建的类型的EmailJob。我们将EmailJob在下一节中定义。
   
  - 接下来，我们构建一个Trigger(触发器)实例，该实例定义何时应该执行Job。
   
  - 最后，我们使用scheduler.scheduleJob()的API 安排这个Job(作业或任务) 。
 
### 运行和测试
 - 在idea的Terminal控制台输入命令```java mvn spring-boot:run```
 - 如果不在配置文件中指定密码，在控制台中指定密码，则输入命令```java  -Dspring.mail.password=<YOUR_SMTP_PASSWORD>```
 - 使用postman(邮递员)工具进行测试
    - 注意这里默认使用8080端口
    - 时间格式是2018-11-10T14:11:00，即yyyy-MM-ddTHH:mm:ss，时间必须大于当前时间(这样才会安排发邮件的作业被执行)
    - 时区，我这里选择的是Asia/Shanghai，根据你当前的时区选择即可
    - 使用postman具体发送：
       ![avatar](https://raw.githubusercontent.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/master/src/main/resources/postmanTestScheduleEmail.png)
    - 邮箱成功接收：
       ![avatar](https://raw.githubusercontent.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/master/src/main/resources/postmanTestScheduleEmailSuccess.png)