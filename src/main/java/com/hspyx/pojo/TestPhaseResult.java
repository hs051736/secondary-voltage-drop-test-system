package com.hspyx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestPhaseResult {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联主表ID (test_records.id)
     */
    private Long recordId;

    /**
     * 相位标识 (ao, bo, co)
     */
    private String phase;

    /**
     * 比差 f(%)
     */
    private BigDecimal fPercent;

    /**
     * d(分)
     */
    private BigDecimal dPercent;

    /**
     * 压降误差 dU(%)
     */
    private BigDecimal duPercent;

    /**
     * Upt:U
     */
    private BigDecimal uptUPercent;

    /**
     * Uyb:U
     */
    private BigDecimal uybUPercent;

    /**
     * 关联的项目名称 (PT1, PT2, CT1...)
     */
    private String itemName;
}
