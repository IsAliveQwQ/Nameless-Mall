package com.nameless.mall.core.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 通用分頁結果封裝對象
 * <p>
 * 避免 API 模組 (feign clients) 依賴 Mybatis-Plus 的 Page 類別，
 * 確保架構的簡潔與獨立性。
 *
 * @param <T> 數據類型
 */
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 當前頁的數據列表 (對應 MyBatis-Plus Page 的 records)
     */
    private List<T> records = Collections.emptyList();

    /**
     * 總記錄數 (對應 MyBatis-Plus Page 的 total)
     */
    private long total = 0;

    /**
     * 每頁顯示筆數 (對應 MyBatis-Plus Page 的 size)
     */
    private long size = 10;

    /**
     * 當前頁碼 (對應 MyBatis-Plus Page 的 current)
     * 
     */
    private long current = 1;

    public PageResult(List<T> records, long total, long size, long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
    }
}
