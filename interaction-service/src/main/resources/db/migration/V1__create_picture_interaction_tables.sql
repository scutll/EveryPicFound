CREATE TABLE picture_comment(
	picture_id BIGINT UNSIGNED NOT NULL COMMENT '图片id对应image-asset表中image_id',
    comment_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '全局唯一评论ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    
    root_comment_id BIGINT NULL COMMENT '(回复评论) 所属一级评论id, 若NULL则为一级评论',
    reply_user_id BIGINT NULL COMMENT '被回复用户ID, 一级评论或直接回复一级评论时为NULL',
    
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    
    likes_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
    replies_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '回复数(包括直接和间接回复), 仅一级评论有效',
    
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    PRIMARY KEY (comment_id), 
    
    KEY idx_picture_root_time(
    	picture_id,
        root_comment_id,
        created_at,
        comment_id
    ),
    
    KEY idx_user_time(
    	user_id,
        created_at,
        comment_id
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '图片评论区表';

CREATE TABLE picture_comment_like_user(
	comment_id BIGINT NOT NULL COMMENT '被点赞的评论ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    PRIMARY KEY (comment_id, user_id),
    
    KEY idx_user_like_time(
    	user_id,
        created_at,
        comment_id
    ),
    
   	KEY idx_comment_like_time(
    	comment_id,
        created_at,
		user_id
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '评论点赞用户表';

CREATE TABLE picture_like_user(
	picture_id BIGINT UNSIGNED NOT NULL COMMENT '被点赞图片ID',
	user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    
    PRIMARY KEY (picture_id, user_id),
    
    KEY idx_user_like_time (
        user_id,
        created_at,
        picture_id
    ),

    KEY idx_picture_like_time (
        picture_id,
        created_at,
        user_id
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '图片点赞用户表';
