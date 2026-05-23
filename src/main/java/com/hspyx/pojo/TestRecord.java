package com.hspyx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRecord {

    // 主键ID
    private Long id;

    // 产品编号 (核心索引)
    private String productNo;

    // 产品名称
    private String productName;

    // 生产厂商
    private String manufacturer;

    // 产地
    private String origin;

    // 送检/检测日期
    private LocalDate testDate;

     // 二次电压 (如 14.8 U)
    private String secondaryVoltage;


    //private BigDecimal temperature;

    // 环境温度 (℃)
    private String temperature;

    // 环境湿度 (%)
    private String humidity;

    // 最终结论 (合格/不合格)
    private String resultStatus;

    // 创建时间
    private LocalDateTime createTime;
}
