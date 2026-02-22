package com.nameless.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nameless.mall.order.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 本地訊息表 Mapper
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {

    /**
     * 回收卡在 PROCESSING(9) 超過 60 秒的消息（實例崩潰保護）。
     * 將 status 重置為 NEW(0) 以便重新投遞。
     */
    @Update("UPDATE local_message SET status = 0, update_time = NOW() " +
            "WHERE status = 9 AND update_time < DATE_SUB(NOW(), INTERVAL 60 SECOND)")
    int recoverStaleProcessingMessages();
}
