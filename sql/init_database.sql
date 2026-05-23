-- =============================================
-- 二次压降检测系统数据库设计 - 完整版（含用户管理）
-- =============================================

-- 删除已存在的表（如果需要重建）
DROP TABLE IF EXISTS test_phase_results;
DROP TABLE IF EXISTS test_input_details;
DROP TABLE IF EXISTS test_records;
DROP TABLE IF EXISTS users;

-- =============================================
-- 0. 用户表
-- =============================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    nickname VARCHAR(100) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入默认管理员账号（密码: admin123）
-- BCrypt加密后的密码 (使用 Spring Security BCrypt 生成)
INSERT INTO users (username, password, nickname, role, status) VALUES
('admin', '$2a$10$rqJhcMyOX.sGgKf0rXW7mOK8pFnPqQTqh8qW3Gz5y2Z9z9X5wXy5O', '系统管理员', 'ADMIN', 1),
('test', '$2a$10$rqJhcMyOX.sGgKf0rXW7mOK8pFnPqQTqh8qW3Gz5y2Z9z9X5wXy5O', '测试用户', 'USER', 1);

-- =============================================
-- 1. 主表：检测记录表
-- =============================================
CREATE TABLE test_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_no VARCHAR(100) NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称',
    manufacturer VARCHAR(200) COMMENT '制造商',
    origin VARCHAR(200) COMMENT '产地',
    test_date DATE COMMENT '检测日期',
    secondary_voltage VARCHAR(50) COMMENT '二次电压',
    temperature VARCHAR(20) COMMENT '温度',
    humidity VARCHAR(20) COMMENT '湿度',
    result_status VARCHAR(20) DEFAULT '待检' COMMENT '检测结论',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product_no (product_no),
    INDEX idx_test_date (test_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测记录主表';

-- =============================================
-- 2. 子表1：检测项目详情表
-- =============================================
CREATE TABLE test_input_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '关联的检测记录ID',
    item_name VARCHAR(50) NOT NULL COMMENT '项目名称',
    rating VARCHAR(50) COMMENT '档位',
    percentage VARCHAR(20) COMMENT '百分比',
    min_limit VARCHAR(50) COMMENT '下限',
    max_limit VARCHAR(50) COMMENT '上限',
    measured_val VARCHAR(50) COMMENT '实测值',
    secondary_voltage VARCHAR(50) COMMENT '二次电压',
    temperature VARCHAR(20) COMMENT '温度',
    humidity VARCHAR(20) COMMENT '湿度',
    result_status VARCHAR(20) DEFAULT '待检' COMMENT '该项目检测结论',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_record_id (record_id),
    INDEX idx_item_name (item_name),
    FOREIGN KEY (record_id) REFERENCES test_records(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测项目详情表';

-- =============================================
-- 3. 子表2：三相误差结果表
-- =============================================
CREATE TABLE test_phase_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    record_id BIGINT NOT NULL COMMENT '关联的检测记录ID',
    item_name VARCHAR(50) NOT NULL COMMENT '关联的项目名称',
    phase VARCHAR(10) NOT NULL COMMENT '相位：ao, bo, co',
    f_percent DECIMAL(15,8) COMMENT '比差 f (%)',
    d_percent DECIMAL(15,8) COMMENT '角差 d (分)',
    du_percent DECIMAL(15,8) COMMENT '压降误差 dU (%)',
    uptu_percent DECIMAL(15,8) COMMENT 'Upt:U 比值',
    uybu_percent DECIMAL(15,8) COMMENT 'Uyb:U 比值',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_record_id (record_id),
    INDEX idx_item_name (item_name),
    INDEX idx_record_item (record_id, item_name),
    FOREIGN KEY (record_id) REFERENCES test_records(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三相误差结果表';

-- =============================================
-- 插入测试数据
-- =============================================

-- 记录1：电能表0.5S级，2个项目
INSERT INTO test_records (product_no, product_name, manufacturer, origin, test_date, secondary_voltage, temperature, humidity, result_status)
VALUES ('ELEC-2024-0001', '电能表0.5S级', '华东电力设备有限公司', '上海', '2024-01-05', '100V', '22°C', '55%', '合格');

SET @r1 = LAST_INSERT_ID();
INSERT INTO test_input_details (record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, secondary_voltage, temperature, humidity, result_status)
VALUES (@r1, 'PT1', '1', '100%', '-0.05', '+0.05', '0.023', '100V', '22°C', '55%', '合格'),
       (@r1, 'PT2', '1', '100%', '-0.05', '+0.05', '0.031', '100V', '22°C', '55%', '合格');
INSERT INTO test_phase_results (record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent)
VALUES (@r1, 'PT1', 'ao', 0.012, 1.5, 0.018, 1.00012, 1.00008),
       (@r1, 'PT1', 'bo', 0.015, 2.0, 0.022, 1.00015, 1.00010),
       (@r1, 'PT1', 'co', 0.018, 2.3, 0.025, 1.00018, 1.00012),
       (@r1, 'PT2', 'ao', 0.025, 3.1, 0.035, 1.00025, 1.00018),
       (@r1, 'PT2', 'bo', 0.028, 3.5, 0.040, 1.00028, 1.00020),
       (@r1, 'PT2', 'co', 0.031, 3.8, 0.044, 1.00031, 1.00022);

-- 记录2：电能表0.2S级，3个项目
INSERT INTO test_records (product_no, product_name, manufacturer, origin, test_date, secondary_voltage, temperature, humidity, result_status)
VALUES ('ELEC-2024-0002', '电能表0.2S级', '华北电力科技公司', '北京', '2024-01-08', '100V', '20°C', '48%', '合格');

SET @r2 = LAST_INSERT_ID();
INSERT INTO test_input_details (record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, secondary_voltage, temperature, humidity, result_status)
VALUES (@r2, 'PT1', '1', '100%', '-0.02', '+0.02', '0.008', '100V', '20°C', '48%', '合格'),
       (@r2, 'PT2', '1', '100%', '-0.02', '+0.02', '0.012', '100V', '20°C', '48%', '合格'),
       (@r2, 'CT1', '1', '100%', '-0.05', '+0.05', '0.028', '100V', '20°C', '48%', '合格');
INSERT INTO test_phase_results (record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent)
VALUES (@r2, 'PT1', 'ao', 0.004, 0.6, 0.006, 1.00004, 1.00002),
       (@r2, 'PT1', 'bo', 0.006, 0.9, 0.009, 1.00006, 1.00004),
       (@r2, 'PT1', 'co', 0.008, 1.2, 0.012, 1.00008, 1.00006),
       (@r2, 'PT2', 'ao', 0.007, 1.0, 0.010, 1.00007, 1.00005),
       (@r2, 'PT2', 'bo', 0.009, 1.3, 0.013, 1.00009, 1.00007),
       (@r2, 'PT2', 'co', 0.012, 1.5, 0.018, 1.00012, 1.00008),
       (@r2, 'CT1', 'ao', 0.015, 2.0, 0.022, 1.00015, 1.00010),
       (@r2, 'CT1', 'bo', 0.020, 2.8, 0.030, 1.00020, 1.00015),
       (@r2, 'CT1', 'co', 0.028, 3.5, 0.040, 1.00028, 1.00020);

-- 记录3：电能表1级，不合格
INSERT INTO test_records (product_no, product_name, manufacturer, origin, test_date, secondary_voltage, temperature, humidity, result_status)
VALUES ('ELEC-2024-0003', '电能表1级', '华南电力设备厂', '广州', '2024-01-12', '100V', '25°C', '62%', '不合格');

SET @r3 = LAST_INSERT_ID();
INSERT INTO test_input_details (record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, secondary_voltage, temperature, humidity, result_status)
VALUES (@r3, 'PT1', '1', '100%', '-0.10', '+0.10', '0.085', '100V', '25°C', '62%', '合格'),
       (@r3, 'PT2', '1', '100%', '-0.10', '+0.10', '0.125', '100V', '25°C', '62%', '不合格');
INSERT INTO test_phase_results (record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent)
VALUES (@r3, 'PT1', 'ao', 0.035, 4.2, 0.050, 1.00035, 1.00025),
       (@r3, 'PT1', 'bo', 0.042, 5.0, 0.060, 1.00042, 1.00030),
       (@r3, 'PT1', 'co', 0.055, 6.2, 0.078, 1.00055, 1.00040),
       (@r3, 'PT2', 'ao', 0.065, 7.5, 0.092, 1.00065, 1.00050),
       (@r3, 'PT2', 'bo', 0.085, 9.2, 0.120, 1.00085, 1.00065),
       (@r3, 'PT2', 'co', 0.110, 12.0, 0.155, 1.00110, 1.00085);

-- 记录4：电能表0.5S级，2个项目
INSERT INTO test_records (product_no, product_name, manufacturer, origin, test_date, secondary_voltage, temperature, humidity, result_status)
VALUES ('ELEC-2024-0004', '电能表0.5S级', '华东电力设备有限公司', '上海', '2024-01-15', '100V', '18°C', '45%', '合格');

SET @r4 = LAST_INSERT_ID();
INSERT INTO test_input_details (record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, secondary_voltage, temperature, humidity, result_status)
VALUES (@r4, 'PT1', '1', '100%', '-0.05', '+0.05', '0.015', '100V', '18°C', '45%', '合格'),
       (@r4, 'CT1', '1', '100%', '-0.05', '+0.05', '0.032', '100V', '18°C', '45%', '合格');
INSERT INTO test_phase_results (record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent)
VALUES (@r4, 'PT1', 'ao', 0.008, 1.0, 0.012, 1.00008, 1.00005),
       (@r4, 'PT1', 'bo', 0.010, 1.4, 0.015, 1.00010, 1.00007),
       (@r4, 'PT1', 'co', 0.012, 1.8, 0.018, 1.00012, 1.00008),
       (@r4, 'CT1', 'ao', 0.018, 2.2, 0.025, 1.00018, 1.00012),
       (@r4, 'CT1', 'bo', 0.022, 2.8, 0.032, 1.00022, 1.00015),
       (@r4, 'CT1', 'co', 0.028, 3.5, 0.040, 1.00028, 1.00020);

-- 记录5：电能表0.2S级，3个项目
INSERT INTO test_records (product_no, product_name, manufacturer, origin, test_date, secondary_voltage, temperature, humidity, result_status)
VALUES ('ELEC-2024-0005', '电能表0.2S级', '华北电力科技公司', '北京', '2024-01-18', '100V', '23°C', '50%', '合格');

SET @r5 = LAST_INSERT_ID();
INSERT INTO test_input_details (record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, secondary_voltage, temperature, humidity, result_status)
VALUES (@r5, 'PT1', '1', '100%', '-0.02', '+0.02', '0.010', '100V', '23°C', '50%', '合格'),
       (@r5, 'PT2', '1', '100%', '-0.02', '+0.02', '0.014', '100V', '23°C', '50%', '合格'),
       (@r5, 'CT1', '1', '100%', '-0.05', '+0.05', '0.035', '100V', '23°C', '50%', '合格');
INSERT INTO test_phase_results (record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent)
VALUES (@r5, 'PT1', 'ao', 0.005, 0.7, 0.008, 1.00005, 1.00003),
       (@r5, 'PT1', 'bo', 0.007, 1.0, 0.010, 1.00007, 1.00005),
       (@r5, 'PT1', 'co', 0.009, 1.3, 0.013, 1.00009, 1.00006),
       (@r5, 'PT2', 'ao', 0.008, 1.1, 0.012, 1.00008, 1.00006),
       (@r5, 'PT2', 'bo', 0.010, 1.5, 0.015, 1.00010, 1.00008),
       (@r5, 'PT2', 'co', 0.012, 1.8, 0.018, 1.00012, 1.00009),
       (@r5, 'CT1', 'ao', 0.020, 2.5, 0.030, 1.00020, 1.00015),
       (@r5, 'CT1', 'bo', 0.025, 3.2, 0.038, 1.00025, 1.00018),
       (@r5, 'CT1', 'co', 0.030, 4.0, 0.045, 1.00030, 1.00022);

-- =============================================
-- 验证数据
-- =============================================
SELECT '=== 用户列表 ===' as '';
SELECT id, username, nickname, role, status FROM users;

SELECT '=== 检测记录统计 ===' as '';
SELECT COUNT(*) as '总记录数' FROM test_records;
SELECT COUNT(*) as '合格数' FROM test_records WHERE result_status = '合格';
SELECT COUNT(*) as '不合格数' FROM test_records WHERE result_status = '不合格';
