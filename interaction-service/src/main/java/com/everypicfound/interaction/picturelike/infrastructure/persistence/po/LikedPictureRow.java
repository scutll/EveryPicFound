package com.everypicfound.interaction.picturelike.infrastructure.persistence.po;

import java.time.LocalDateTime;

public final class LikedPictureRow {

    private long pictureId;
    private LocalDateTime likedAt;

    public long getPictureId() {
        return pictureId;
    }

    public void setPictureId(long pictureId) {
        this.pictureId = pictureId;
    }

    public LocalDateTime getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(LocalDateTime likedAt) {
        this.likedAt = likedAt;
    }
}
