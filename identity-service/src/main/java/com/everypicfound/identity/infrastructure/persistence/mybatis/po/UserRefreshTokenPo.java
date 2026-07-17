package com.everypicfound.identity.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * {@code user_refresh_token} 表的 MyBatis-Plus 持久化对象。
 */
@TableName("user_refresh_token")
public class UserRefreshTokenPo {

    @TableId(value = "token_hash", type = IdType.INPUT)
    private String tokenHash;
    private String sessionId;
    private String status;
    private LocalDateTime issuedTime;
    private LocalDateTime expiresTime;
    private LocalDateTime usedTime;
    private LocalDateTime revokedTime;

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getIssuedTime() {
        return issuedTime;
    }

    public void setIssuedTime(LocalDateTime issuedTime) {
        this.issuedTime = issuedTime;
    }

    public LocalDateTime getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(LocalDateTime expiresTime) {
        this.expiresTime = expiresTime;
    }

    public LocalDateTime getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(LocalDateTime usedTime) {
        this.usedTime = usedTime;
    }

    public LocalDateTime getRevokedTime() {
        return revokedTime;
    }

    public void setRevokedTime(LocalDateTime revokedTime) {
        this.revokedTime = revokedTime;
    }
}
