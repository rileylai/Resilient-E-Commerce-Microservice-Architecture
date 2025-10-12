package com.tut2.group3.store.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @TableId(type = IdType.AUTO)
    private long id;

    private long userId;
    private String status;
    private float totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
