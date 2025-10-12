package com.tut2.group3.deliveryco.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Delivery repository interface
 * Uses MyBatis-Plus BaseMapper for common CRUD operations
 */
@Mapper
public interface DeliveryRepository extends BaseMapper<Delivery> {

    /**
     * Find deliveries by order ID
     * @param orderId Order ID from store service
     * @return Delivery entity
     */
    default Delivery findByOrderId(Long orderId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                .eq("order_id", orderId));
    }

    /**
     * Find deliveries by customer ID
     * @param customerId Customer ID
     * @return List of delivery entities
     */
    default List<Delivery> findByCustomerId(Long customerId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                .eq("customer_id", customerId));
    }

    /**
     * Find deliveries by status
     * @param status Delivery status
     * @return List of delivery entities
     */
    default List<Delivery> findByStatus(DeliveryStatus status) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Delivery>()
                .eq("status", status));
    }
}