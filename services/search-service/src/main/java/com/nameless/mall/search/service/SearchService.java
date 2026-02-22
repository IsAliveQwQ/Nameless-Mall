package com.nameless.mall.search.service;

import com.nameless.mall.search.api.dto.SearchRequestDTO;
import com.nameless.mall.search.api.vo.SearchResponseVO;

/**
 * 搜尋服務接口
 */
public interface SearchService {

    /**
     * 全量同步 (從 MySQL 到 ES)
     */
    int syncAll();

    /**
     * 高級搜尋 (支援聚合篩選)
     */
    SearchResponseVO searchAdvanced(SearchRequestDTO request);

    /**
     * 單筆同步 (用於 MQ 實時更新)
     */
    void syncOne(Long productId);

    /**
     * 單筆刪除 (用於商品下架同步)
     */
    void deleteOne(Long productId);
}
