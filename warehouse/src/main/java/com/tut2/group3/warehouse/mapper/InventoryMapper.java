package com.tut2.group3.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tut2.group3.warehouse.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * Reserve stock with optimistic locking
     * Returns the number of rows affected (1 if successful, 0 if failed due to concurrent modification)
     */
    @Update("UPDATE inventory SET available_quantity = available_quantity - #{quantity}, " +
            "reserved_quantity = reserved_quantity + #{quantity}, " +
            "version = version + 1 " +
            "WHERE warehouse_id = #{warehouseId} AND product_id = #{productId} " +
            "AND available_quantity >= #{quantity} AND version = #{version}")
    int reserveStock(@Param("warehouseId") Long warehouseId,
                     @Param("productId") Long productId,
                     @Param("quantity") Integer quantity,
                     @Param("version") Integer version);

    /**
     * Confirm stock reservation
     */
    @Update("UPDATE inventory SET reserved_quantity = reserved_quantity - #{quantity}, " +
            "version = version + 1 " +
            "WHERE warehouse_id = #{warehouseId} AND product_id = #{productId} " +
            "AND reserved_quantity >= #{quantity} AND version = #{version}")
    int confirmReservation(@Param("warehouseId") Long warehouseId,
                           @Param("productId") Long productId,
                           @Param("quantity") Integer quantity,
                           @Param("version") Integer version);

    /**
     * Release reserved stock back to available
     */
    @Update("UPDATE inventory SET available_quantity = available_quantity + #{quantity}, " +
            "reserved_quantity = reserved_quantity - #{quantity}, " +
            "version = version + 1 " +
            "WHERE warehouse_id = #{warehouseId} AND product_id = #{productId} " +
            "AND reserved_quantity >= #{quantity} AND version = #{version}")
    int releaseStock(@Param("warehouseId") Long warehouseId,
                     @Param("productId") Long productId,
                     @Param("quantity") Integer quantity,
                     @Param("version") Integer version);
}
