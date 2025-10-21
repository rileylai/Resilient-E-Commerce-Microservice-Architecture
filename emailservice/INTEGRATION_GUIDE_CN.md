# EmailService 集成指南 - Store 服务开发者

**版本:** 1.0.0
**最后更新:** 2025-10-21

---

## 概述

本文档为 Store 开发者提供与 EmailService 集成的所有必要信息，用于向客户发送邮件通知。

---

## RabbitMQ 配置

### 交换机

| 交换机名称 | 类型 | 持久化 | 创建者 |
|-----------|------|--------|--------|
| `delivery.exchange` | Topic | 是 | DeliveryCo |
| `store.exchange` | Topic | 是 | EmailService/Store |

### EmailService 消费的队列

| 队列名称 | 用途 | 消息来源 |
|---------|------|---------|
| `delivery.email.queue` | 配送状态通知 | DeliveryCo |
| `email.orderfail.queue` | 订单失败通知 | Store |
| `email.refund.queue` | 退款通知 | Store |

### 路由键

| 路由键 | 交换机 | 队列 | 用途 |
|-------|--------|------|------|
| `notification.email` | `delivery.exchange` | `delivery.email.queue` | 配送更新 |
| `email.orderfail` | `store.exchange` | `email.orderfail.queue` | 订单失败 |
| `email.refund` | `store.exchange` | `email.refund.queue` | 退款 |

---

## Store 服务集成

### 场景 1: 订单处理失败

**何时使用:** 订单处理因任何原因失败（库存检查、银行转账等）

**EmailService 输出:**

```
════════════════════════════════════════════════════════════
EMAIL NOTIFICATION SERVICE - Order Failure
────────────────────────────────────────────────────────────
Time:       2025-10-21 16:30:45
Recipient:  customer@example.com
Order ID:   1001
────────────────────────────────────────────────────────────
Subject:    Order Processing Failed
Body:       We apologize, but your order #1001 has failed.
            Reason: Insufficient inventory
            Your order has been cancelled. Any charges will be refunded.
────────────────────────────────────────────────────────────
Email sent successfully
════════════════════════════════════════════════════════════
```

---

### 场景 2: 客户取消订单（含退款）

**EmailService 输出:**
```
════════════════════════════════════════════════════════════
EMAIL NOTIFICATION SERVICE - Refund
────────────────────────────────────────────────────────────
Time:       2025-10-21 16:35:20
Recipient:  customer@example.com
Order ID:   1001
────────────────────────────────────────────────────────────
Subject:    Refund Processed Successfully
Body:       Your order #1001 has been refunded.
            Refund amount: $99.99
            The refund will appear in your account within 3-5 business days.
────────────────────────────────────────────────────────────
Email sent successfully
════════════════════════════════════════════════════════════
```

---

## 完整集成工作流

### 订单处理流程与邮件通知

```
┌─────────────────────────────────────────────────────────────────┐
│                    STORE 订单处理流程                            │
└─────────────────────────────────────────────────────────────────┘

1. 客户下单
   ↓
2. 检查库存
   ├─ 成功 → 继续
   └─ 失败 → 发送 OrderFailureNotificationDTO → EmailService

3. 处理支付（Bank）
   ├─ 成功 → 继续
   └─ 失败 → 发送 OrderFailureNotificationDTO → EmailService

4. 发送配送请求到 DeliveryCo
   ↓
5. DeliveryCo 发送状态更新 → EmailService
   (由 DeliveryCo 集成自动处理)

客户取消订单路径:
──────────────────────────
如果客户在步骤 4 之前取消:
   ↓
1. 恢复库存
2. 通过 Bank 处理退款
3. 发送 RefundNotificationDTO → EmailService
```

---

## 消息格式参考

### OrderFailureNotificationDTO JSON
```json
{
  "orderId": 1001,
  "customerEmail": "customer@example.com",
  "reason": "库存不足",
  "timestamp": "2025-10-21T16:30:45",
  "errorDetails": "商品 XYZ 在所有仓库中缺货"
}
```

### RefundNotificationDTO JSON
```json
{
  "orderId": 1001,
  "customerEmail": "customer@example.com",
  "amount": 99.99,
  "timestamp": "2025-10-21T16:35:20",
  "reason": "客户取消订单"
}
```
