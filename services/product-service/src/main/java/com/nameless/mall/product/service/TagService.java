package com.nameless.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.product.entity.Tag;

import java.util.List;

public interface TagService extends IService<Tag> {
    /**
     * 根據 Tag ID 列表取得 Tag 名稱列表
     */
    List<String> getTagNamesByIds(List<Long> tagIds);
}
