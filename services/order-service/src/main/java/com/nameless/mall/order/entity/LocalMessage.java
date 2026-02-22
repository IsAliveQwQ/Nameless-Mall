package com.nameless.mall.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地訊息表
 * 用於保證消息投遞的可靠性。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("local_message")
public class LocalMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息唯一識別碼
     */
    private String messageId;

    /**
     * 消息內容
     */
    private String content;

    /**
     * 交換機
     */
    private String exchange;

    /**
     * 路由鍵
     */
    private String routingKey;

    /**
     * 狀態: 0-新建, 1-已發送, 2-發送失敗, 3-已死亡
     */
    private Integer status;

    /**
     * 重試次數
     */
    private Integer retryCount;

    /**
     * 最大重試次數
     */
    private Integer maxRetry;

    /**
     * 下次重試時間
     */
    private LocalDateTime nextRetryTime;

    /**
     * 創建時間
     */
    private LocalDateTime createTime;

    /**
     * 更新時間
     */
    private LocalDateTime updateTime;
}
