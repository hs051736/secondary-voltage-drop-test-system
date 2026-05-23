package com.hspyx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestInputDetail {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联主表ID (test_records.id)
     */
    private Long recordId;

    /**
     * 项目名称 (PT1, PT2, CT1, CT2)
     */
    private String itemName;

    /**
     * 档位
     */
    private String rating;

    /**
     * 百分比
     */
    private String percentage;

    /**
     * 下限值
     */
    private String minLimit;

    /**
     * 上限值
     */
    private String maxLimit;

    /**
     * 实测值
     */
    private String measuredVal;

    /**
     * 二次电压（每个项目可单独设置）
     */
    private String secondaryVoltage;

    /**
     * 温度（每个项目可单独设置）
     */
    private String temperature;

    /**
     * 湿度（每个项目可单独设置）
     */
    private String humidity;

    /**
     * 该项目检测结论
     */
    private String resultStatus;
}
