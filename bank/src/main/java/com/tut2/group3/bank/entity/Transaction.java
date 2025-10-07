package com.tut2.group3.bank.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("transactions")
public class Transaction {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private String orderId;

    @TableField("user_id")
    private String userId;

    @TableField("tx_type")
    private TransactionType txType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency")
    private String currency;

    @TableField("status")
    private TransactionStatus status;

    @TableField("bank_tx_id")
    private String bankTxId;

    @TableField("message")
    private String message;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
