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
         - EmailJobSchedulerController // Email作业的调度控制器
     - job // 作业类包
         - EmailJob // Email发送类
     - payload // 有效负载类（DTO类）包
         - ScheduleEmailRequest     // 调度Email的请求类
         - ScheduleEmailResponse    // 调度Email的响应类
         
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
    ```java
    /**
    *  ScheduleEmailRequest   装scheduleEmailAPI的请求数据类
    */
    package com.example.quartzdemo.payload;
    
    import javax.validation.constraints.Email;
    import javax.validation.constraints.NotEmpty;
    import javax.validation.constraints.NotNull;
    import java.time.LocalDateTime;
    import java.time.ZoneId;
    
    public class ScheduleEmailRequest {
        @Email
        @NotEmpty
        private String email;
    
        @NotEmpty
        private String subject;
    
        @NotEmpty
        private String body;
    
        @NotNull
        private LocalDateTime dateTime;
    
        @NotNull
        private ZoneId timeZone;
        
        // Getters and Setters (Omitted for brevity)
    }
    ```
    ```java
    /**
    *  ScheduleEmailResponse   装scheduleEmailAPI的响应数据类
    */
    package com.example.quartzdemo.payload;
    
    import com.fasterxml.jackson.annotation.JsonInclude;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class ScheduleEmailResponse {
        private boolean success;
        private String jobId;
        private String jobGroup;
        private String message;
    
        public ScheduleEmailResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    
        public ScheduleEmailResponse(boolean success, String jobId, String jobGroup, String message) {
            this.success = success;
            this.jobId = jobId;
            this.jobGroup = jobGroup;
            this.message = message;
        }
    
        // Getters and Setters (Omitted for brevity)
    }
    ```
    ```java
    /**
    * EmailJobSchedulerController  用于定义调度邮件任务的scheduleEmail的API接口，以及定义构建作业实例和触发器示例的方法
    */
    package com.example.quartzdemo.controller;
    
    import com.example.quartzdemo.job.EmailJob;
    import com.example.quartzdemo.payload.ScheduleEmailRequest;
    import com.example.quartzdemo.payload.ScheduleEmailResponse;
    import org.quartz.*;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RestController;
    
    import javax.validation.Valid;
    import java.time.ZonedDateTime;
    import java.util.Date;
    import java.util.UUID;
    
    @RestController
    public class EmailJobSchedulerController {
        private static final Logger logger = LoggerFactory.getLogger(EmailJobSchedulerController.class);
    
        @Autowired
        private Scheduler scheduler;
    
        @PostMapping("/scheduleEmail")
        public ResponseEntity<ScheduleEmailResponse> scheduleEmail(@Valid @RequestBody ScheduleEmailRequest scheduleEmailRequest) {
            try {
                ZonedDateTime dateTime = ZonedDateTime.of(scheduleEmailRequest.getDateTime(), scheduleEmailRequest.getTimeZone());
                if(dateTime.isBefore(ZonedDateTime.now())) {
                    ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(false,
                            "dateTime must be after current time");
                    return ResponseEntity.badRequest().body(scheduleEmailResponse);
                }
    
                JobDetail jobDetail = buildJobDetail(scheduleEmailRequest);
                Trigger trigger = buildJobTrigger(jobDetail, dateTime);
                scheduler.scheduleJob(jobDetail, trigger);
    
                ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(true,
                        jobDetail.getKey().getName(), jobDetail.getKey().getGroup(), "Email Scheduled Successfully!");
                return ResponseEntity.ok(scheduleEmailResponse);
            } catch (SchedulerException ex) {
                logger.error("Error scheduling email", ex);
    
                ScheduleEmailResponse scheduleEmailResponse = new ScheduleEmailResponse(false,
                        "Error scheduling email. Please try later!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(scheduleEmailResponse);
            }
        }
    
        private JobDetail buildJobDetail(ScheduleEmailRequest scheduleEmailRequest) {
            JobDataMap jobDataMap = new JobDataMap();
    
            jobDataMap.put("email", scheduleEmailRequest.getEmail());
            jobDataMap.put("subject", scheduleEmailRequest.getSubject());
            jobDataMap.put("body", scheduleEmailRequest.getBody());
    
            return JobBuilder.newJob(EmailJob.class)
                    .withIdentity(UUID.randomUUID().toString(), "email-jobs")
                    .withDescription("Send Email Job")
                    .usingJobData(jobDataMap)
                    .storeDurably()
                    .build();
        }
    
        private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
            return TriggerBuilder.newTrigger()
                    .forJob(jobDetail)
                    .withIdentity(jobDetail.getKey().getName(), "email-triggers")
                    .withDescription("Send Email Trigger")
                    .startAt(Date.from(startAt.toInstant()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                    .build();
        }
    }
    ```
   - Spring Boot内置了对Quartz的支持。它会自动创建一个Quartz的Schedulerbean，其中包含我们在application.properties文件中提供的配置。这就是我们可以直接注入Scheduler控制器的原因。
   
   - 在/scheduleEmail这个API中，
   
   - 我们首先验证请求正文（即发送一个ScheduleEmailRequest对象去请求调度邮件发送作业）
   
   - 然后，使用包含收件人电子邮件，主题和正文的JobDataMap(这些信息都来自我们的请求正文)构建JobDetail实例。在JobDetail我们创建的类型的EmailJob。我们将EmailJob在下一节中定义。
   
   - 接下来，我们构建一个Trigger(触发器)实例，该实例定义何时应该执行Job。
   
   - 最后，我们使用scheduler.scheduleJob()的API 安排这个Job(作业或任务) 。
   
   - 创建作业
     - Spring Boot提供了一个名为Quartz Scheduler的Job接口的包装器QuartzJobBean
     - 所以只需要定义一个作业类去实现这个包装器即可
     - 创建一个EmailJob类
        ```java
        package com.example.quartzdemo.job;
        
        import org.quartz.JobDataMap;
        import org.quartz.JobExecutionContext;
        import org.quartz.JobExecutionException;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.boot.autoconfigure.mail.MailProperties;
        import org.springframework.mail.javamail.JavaMailSender;
        import org.springframework.mail.javamail.MimeMessageHelper;
        import org.springframework.scheduling.quartz.QuartzJobBean;
        import org.springframework.stereotype.Component;
        
        import javax.mail.MessagingException;
        import javax.mail.internet.MimeMessage;
        import java.nio.charset.StandardCharsets;
        
        @Component
        public class EmailJob extends QuartzJobBean {
            private static final Logger logger = LoggerFactory.getLogger(EmailJob.class);
        
            @Autowired
            private JavaMailSender mailSender;
        
            @Autowired
            private MailProperties mailProperties;
            
            @Override
            protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
                logger.info("Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());
        
                JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
                String subject = jobDataMap.getString("subject");
                String body = jobDataMap.getString("body");
                String recipientEmail = jobDataMap.getString("email");
        
                sendMail(mailProperties.getUsername(), recipientEmail, subject, body);
            }
        
            private void sendMail(String fromEmail, String toEmail, String subject, String body) {
                try {
                    logger.info("Sending Email to {}", toEmail);
                    MimeMessage message = mailSender.createMimeMessage();
        
                    MimeMessageHelper messageHelper = new MimeMessageHelper(message, StandardCharsets.UTF_8.toString());
                    messageHelper.setSubject(subject);
                    messageHelper.setText(body, true);
                    messageHelper.setFrom(fromEmail);
                    messageHelper.setTo(toEmail);
        
                    mailSender.send(message);
                } catch (MessagingException ex) {
                    logger.error("Failed to send email to {}", toEmail);
                }
            }
        }
        ```
### 运行和测试
 - 在idea的Terminal控制台输入命令```java mvn spring-boot:run```
 - 如果不在配置文件中指定密码，在控制台中指定密码，则输入命令```java  -Dspring.mail.password=<YOUR_SMTP_PASSWORD>```
 - 使用postman(邮递员)工具进行测试
    - 注意这里默认使用8080端口
    - 时间格式是2018-11-10T14:11:00，即yyyy-MM-ddTHH:mm:ss，时间必须大于当前时间(这样才会安排发邮件的作业被执行)
    - 时区，我这里选择的是Asia/Shanghai，根据你当前的时区选择即可
    - https://raw.githubusercontent.com/duyanhan1995/Spring-Boot-Quartz-Scheduler-Example/master/src/main/resources/postmanTestScheduleEmail.png
    