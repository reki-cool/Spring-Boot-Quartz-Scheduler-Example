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

/**
 * 实际发送电子邮件的作业
 */
@Component
public class EmailJob extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(EmailJob.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailProperties mailProperties;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("注意：Executing Job with key {}", jobExecutionContext.getJobDetail().getKey());

        JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
        String subject = jobDataMap.getString("subject");
        String body = jobDataMap.getString("body");
        // 接受邮箱
        String recipientEmail = jobDataMap.getString("email");
        // 发送邮件(用户名，接受邮箱，主题，正文)
        sendMail(mailProperties.getUsername(), recipientEmail, subject, body);
    }

    private void sendMail(String fromEmail, String toEmail, String subject, String body) {
        try {
            logger.info("注意：Sending Email to {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper messageHelper = new MimeMessageHelper(message, StandardCharsets.UTF_8.toString());
            messageHelper.setSubject(subject);
            messageHelper.setText(body, true);
            messageHelper.setFrom(fromEmail);
            messageHelper.setTo(toEmail);

            mailSender.send(message);
        } catch (MessagingException ex) {
            logger.error("注意：Failed to send email to {}", toEmail);
        }
    }
}