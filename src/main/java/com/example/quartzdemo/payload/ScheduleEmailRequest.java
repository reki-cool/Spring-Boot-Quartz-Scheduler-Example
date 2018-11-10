package com.example.quartzdemo.payload;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 调度的邮件请求
 */
public class ScheduleEmailRequest {

    /**
     * email邮箱
     */
    @Email
    @NotEmpty
    private String email;

    /**
     * 主题
     */
    @NotEmpty
    private String subject;

    /**
     * 正文
     */
    @NotEmpty
    private String body;

    /**
     * 时间
     */
    @NotNull
    private LocalDateTime dateTime;

    /**
     * 时区
     */
    @NotNull
    private ZoneId timeZone;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(ZoneId timeZone) {
        this.timeZone = timeZone;
    }
}
