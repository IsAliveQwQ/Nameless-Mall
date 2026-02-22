package com.nameless.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import com.nameless.mall.order.api.dto.OrderSubmitDTO;
import com.nameless.mall.order.entity.OrderShipment;

/**
 * 訂單運送資訊服務的接口
 * <p>
 * 繼承 IService<OrderShipment> 以獲得 MyBatis-Plus 提供的基礎 CRUD 功能。
 * 提供訂單運送相關的業務邏輯。
 */
public interface OrderShipmentService extends IService<OrderShipment> {

    /**
     * 根據提交資料構建運送資訊（純記憶體操作，不存檔）。
     * 用於事務外預先組裝，縮小事務範圍。orderId 待事務內回填。
     *
     * @param orderSn   訂單業務編號
     * @param submitDTO 提交的訂單資料
     * @return 未存檔的 OrderShipment
     */
    OrderShipment buildShipment(String orderSn, OrderSubmitDTO submitDTO);

    /**
     * 根據訂單 ID 查詢運送資訊
     * 
     * @param orderId 訂單 ID
     * @return 運送資訊
     */
    OrderShipment getByOrderId(Long orderId);

    /**
     * 確認簽收，更新簽收時間
     * 
     * @param orderId 訂單 ID
     */
    void confirmReceived(Long orderId);

    /**
     * 將運送資訊轉換為 DTO
     * 
     * @param shipment 運送資訊
     * @return DTO
     */
    OrderDetailDTO.ShipmentDTO toDTO(OrderShipment shipment);
}
