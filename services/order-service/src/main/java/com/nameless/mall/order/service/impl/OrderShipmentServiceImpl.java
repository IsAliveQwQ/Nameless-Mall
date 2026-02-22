package com.nameless.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nameless.mall.order.api.dto.OrderDetailDTO;
import com.nameless.mall.order.api.dto.OrderSubmitDTO;
import com.nameless.mall.order.entity.OrderShipment;
import com.nameless.mall.order.mapper.OrderShipmentMapper;
import com.nameless.mall.order.service.OrderShipmentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 訂單運送資訊服務的實現類
 * <p>
 * 負責處理與 OrderShipment 相關的業務邏輯。
 */
@Service
public class OrderShipmentServiceImpl extends ServiceImpl<OrderShipmentMapper, OrderShipment>
        implements OrderShipmentService {

    @Override
    public OrderShipment buildShipment(String orderSn, OrderSubmitDTO submitDTO) {
        OrderShipment shipment = new OrderShipment();
        // orderId 此時未知，留空，待事務內回填
        shipment.setOrderSn(orderSn);
        shipment.setShippingMethod(submitDTO.getShippingMethod());
        shipment.setReceiverName(submitDTO.getReceiverName());
        shipment.setReceiverPhone(submitDTO.getReceiverPhone());
        shipment.setReceiverAddress(submitDTO.getReceiverAddress());
        return shipment;
    }

    @Override
    public OrderShipment getByOrderId(Long orderId) {
        return this.getOne(
                new LambdaQueryWrapper<OrderShipment>().eq(OrderShipment::getOrderId, orderId));
    }

    @Override
    public void confirmReceived(Long orderId) {
        OrderShipment shipment = this.getByOrderId(orderId);
        if (shipment != null) {
            shipment.setReceivedAt(LocalDateTime.now());
            this.updateById(shipment);
        }
    }

    @Override
    public OrderDetailDTO.ShipmentDTO toDTO(OrderShipment shipment) {
        if (shipment == null) {
            return null;
        }
        OrderDetailDTO.ShipmentDTO dto = new OrderDetailDTO.ShipmentDTO();
        BeanUtils.copyProperties(shipment, dto);
        return dto;
    }
}
