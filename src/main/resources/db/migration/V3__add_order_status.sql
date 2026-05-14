-- 新增 status 欄位到 orders 表
ALTER TABLE orders
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';

-- 將現有 ECPay 付款成功的訂單設為 COMPLETED（預設已付款）
-- 若需要區分，可依照 trade_no 或其他欄位調整
UPDATE orders SET status = 'COMPLETED' WHERE status = 'COMPLETED';