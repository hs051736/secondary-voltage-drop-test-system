package com.hspyx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsDTO {
    // --- 核心指标 ---
    private Integer monthCount;      // 本月检测数
    private String monthPassRate;    // 本月合格率
    private Integer totalCount;      // 总检测数
    private String totalPassRate;    // 总合格率

    // --- 图表数据 (近6个月趋势) ---
    private List<String> trendDates; // X轴：月份 (e.g. "2023-10", "2023-11")
    private List<Integer> trendCounts; // Y轴：数量

    // --- 图表数据 (总体分布) ---
    private Integer totalQualified;  // 总合格数
    private Integer totalUnqualified; // 总不合格数
}
