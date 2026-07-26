package com.everypicfound.interaction.picturelike.infrastructure.persistence.po;

import java.time.LocalDateTime;

public final class PictureLikeRow {

    private long pictureId;
    private long userId;
    private LocalDateTime createdAt;

    public long getPictureId() {
        return pictureId;
    }

    public void setPictureId(long pictureId) {
        this.pictureId = pictureId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
