package com.nameless.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.product.entity.Tag;
import com.nameless.mall.product.mapper.TagMapper;
import com.nameless.mall.product.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 商品標籤服務實作 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public List<String> getTagNamesByIds(List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Tag> query = new LambdaQueryWrapper<>();
        query.in(Tag::getId, tagIds);
        query.select(Tag::getName);
        return this.list(query).stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }
}
