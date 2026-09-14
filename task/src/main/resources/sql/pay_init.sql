-- ============================================================
-- 支付功能初始化脚本（支付宝沙箱）
-- 数据库：db_task
-- ============================================================

-- 1. t_order 增加支付相关字段
ALTER TABLE t_order
    ADD COLUMN pay_state       VARCHAR(2)  DEFAULT '0' COMMENT '赏金支付状态：0未托管 1已托管 2已结算 3已退款',
    ADD COLUMN deposit_state   VARCHAR(2)  DEFAULT '0' COMMENT '押金支付状态：0未支付 1已支付 2已退还',
    ADD COLUMN pay_trade_no    VARCHAR(64) DEFAULT NULL COMMENT '赏金支付宝流水号',
    ADD COLUMN deposit_trade_no VARCHAR(64) DEFAULT NULL COMMENT '押金支付宝流水号';

-- 2. 支付流水表（每笔钱都要有记录）
CREATE TABLE IF NOT EXISTS t_payment (
    payment_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     INT          NOT NULL COMMENT '订单ID',
    user_id      INT          NOT NULL COMMENT '付款用户ID',
    pay_type     VARCHAR(16)  NOT NULL COMMENT '支付类型：BOUNTY赏金 DEPOSIT押金',
    amount       DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    out_trade_no VARCHAR(64)  NOT NULL COMMENT '商户订单号（自己生成）',
    trade_no     VARCHAR(64)  DEFAULT NULL COMMENT '支付宝流水号',
    state        VARCHAR(2)   DEFAULT '0' COMMENT '状态：0待支付 1支付成功 2已关闭',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time     DATETIME     DEFAULT NULL COMMENT '支付成功时间',
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_out_trade_no (out_trade_no),
    KEY idx_order_type (order_id, pay_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
