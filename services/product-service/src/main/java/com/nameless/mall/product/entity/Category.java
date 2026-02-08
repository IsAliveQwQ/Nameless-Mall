package com.nameless.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分類實體類
 * <p>
 * 對應資料庫中的 `categories` 表。
 */
@Data
@TableName("categories")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分類唯一ID (主鍵, 自動增長)
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分類名稱
     */
    private String name;

    /**
     * 父分類ID (0代表頂層分類)
     */
    private Long parentId;

    /**
     * 分類層級 (1, 2, 3)
     */
    private Integer level;

    /**
     * 分類圖標 URL
     */
    private String icon;

    /**
     * 排序權重 (數字越大越靠前)
     */
    private Integer sortOrder;

    /**
     * 狀態 (1:啟用, 0:停用)
     */
    private Integer status;

    /**
     * 邏輯刪除標記 (0:正常, 1:已刪除)
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;
}

