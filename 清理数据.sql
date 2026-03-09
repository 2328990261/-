-- 清理刚才错误上传的Campione数据
-- 查看最新的数据
SELECT * FROM novel_book_main WHERE id >= 117 ORDER BY id DESC;

-- 删除错误的主卷数据（ID 117-119是刚才上传的）
DELETE FROM novel_book_main WHERE id >= 117;

-- 删除对应的分卷数据
DELETE FROM novel_book_volume WHERE main_book_id >= 117;

-- 重置自增ID（可选）
-- ALTER TABLE novel_book_main AUTO_INCREMENT = 117;
-- ALTER TABLE novel_book_volume AUTO_INCREMENT = 1089;
