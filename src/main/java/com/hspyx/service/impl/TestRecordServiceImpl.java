package com.hspyx.service.impl;

import com.hspyx.mapper.TestRecordMapper;
import com.hspyx.pojo.*;
import com.hspyx.service.TestRecordService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.ServletOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestRecordServiceImpl implements TestRecordService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat NUMBER_FMT = new DecimalFormat("#.####");

    @Autowired
    private TestRecordMapper testRecordMapper;

    @Override
    public List<TestRecord> findAll(String keywords) {
        return testRecordMapper.findAll(keywords);
    }

    @Override
    public RecordDTO getDetail(Integer id) {
        // 1. 查询主表 (TestRecord)
        // 如果连主记录都找不到，直接返回 null
        TestRecord record = testRecordMapper.getById(id);
        if (record == null) {
            return null;
        }

        // 2. 创建 DTO 空壳
        RecordDTO dto = new RecordDTO();

        // 3. 组装 DeviceInfo (设备基本信息)
        // 数据来源：主表 (test_records)
        RecordDTO.DeviceInfo info = new RecordDTO.DeviceInfo();
        info.productNo = record.getProductNo();
        info.productName = record.getProductName();
        info.manufacturer = record.getManufacturer();
        info.origin = record.getOrigin();
        // 处理日期转字符串 (LocalDate -> String)
        if (record.getTestDate() != null) {
            info.testDate = record.getTestDate().toString();
        }
        dto.setDeviceInfo(info);

        // 4. 组装 TestResult (环境数据 + 结论 + 三相结果)
        RecordDTO.TestResult result = new RecordDTO.TestResult();
        // 4.1 填充主表里的环境数据
        result.secondaryVoltage = record.getSecondaryVoltage();
        result.resultStatus = record.getResultStatus();
        // 处理数值转字符串 (BigDecimal -> String)
        if (record.getTemperature() != null) result.temperature = record.getTemperature();
        if (record.getHumidity() != null) result.humidity = record.getHumidity();

        // 4.2 填充子表2 (test_phase_results) 里的三相数据
        List<TestPhaseResult> phaseList = testRecordMapper.getPhaseResultsByRecordId(id);
        Map<String, RecordDTO.PhaseData> phaseMap = new HashMap<>();

        // 技巧：先初始化好 ao, bo, co 三个空对象。
        // 这样即使数据库里缺了某相数据，前端也不会报错
        phaseMap.put("ao", new RecordDTO.PhaseData());
        phaseMap.put("bo", new RecordDTO.PhaseData());
        phaseMap.put("co", new RecordDTO.PhaseData());

        if (phaseList != null) {
            for (TestPhaseResult p : phaseList) {
                RecordDTO.PhaseData pd = new RecordDTO.PhaseData();
                if (p.getFPercent() != null) pd.f = p.getFPercent().toString();
                if (p.getDPercent() != null) pd.d = p.getDPercent().toString();
                if (p.getDuPercent() != null) pd.dU = p.getDuPercent().toString();
                if (p.getUptUPercent() != null) pd.uptU = p.getUptUPercent().toString();
                if (p.getUybUPercent() != null) pd.uybU = p.getUybUPercent().toString();

                phaseMap.put(p.getPhase(), pd);
            }
        }
        result.phaseResults = phaseMap;
        dto.setTestResult(result);

        // 5. 组装 TestData (实验数据列表)
        // 数据来源：子表1 (test_input_details)
        List<TestInputDetail> inputList = testRecordMapper.getInputDetailsByRecordId(id);
        List<RecordDTO.TestItem> testItemList = new ArrayList<>();

        if (inputList != null) {
            for (TestInputDetail input : inputList) {
                RecordDTO.TestItem item = new RecordDTO.TestItem();
                // 这里的字段名要和前端表格列对应
                item.item = input.getItemName(); // PT1, PT2...
                item.rating = input.getRating();
                item.percentage = input.getPercentage();
                item.min = input.getMinLimit();
                item.max = input.getMaxLimit();
                item.measured = input.getMeasuredVal();

                testItemList.add(item);
            }
        }
        dto.setTestData(testItemList);

        // 5.1 同时也填充到 projects 结构（向后兼容）
        List<RecordDTO.ProjectItem> projectList = new ArrayList<>();

        if (inputList != null && !inputList.isEmpty()) {
            // 使用 Map 按 itemName 分组相位数据
            Map<String, Map<String, RecordDTO.PhaseData>> phaseByProject = new LinkedHashMap<>();

            // 初始化相位数据容器
            for (TestInputDetail input : inputList) {
                phaseByProject.put(input.getItemName(), new HashMap<>());
            }

            // 使用 itemName 直接分组相位数据
            if (phaseList != null) {
                for (TestPhaseResult pr : phaseList) {
                    String projectName = pr.getItemName();
                    if (projectName == null || projectName.isEmpty()) {
                        continue;
                    }

                    Map<String, RecordDTO.PhaseData> projectPhases = phaseByProject.get(projectName);
                    if (projectPhases != null) {
                        RecordDTO.PhaseData pd = new RecordDTO.PhaseData();
                        if (pr.getFPercent() != null) pd.f = pr.getFPercent().toString();
                        if (pr.getDPercent() != null) pd.d = pr.getDPercent().toString();
                        if (pr.getDuPercent() != null) pd.dU = pr.getDuPercent().toString();
                        if (pr.getUptUPercent() != null) pd.uptU = pr.getUptUPercent().toString();
                        if (pr.getUybUPercent() != null) pd.uybU = pr.getUybUPercent().toString();
                        projectPhases.put(pr.getPhase(), pd);
                    }
                }
            }

            // 创建每个项目的数据
            for (TestInputDetail input : inputList) {
                RecordDTO.ProjectItem projectItem = new RecordDTO.ProjectItem();
                projectItem.projectName = input.getItemName();
                projectItem.rating = input.getRating();
                projectItem.percentage = input.getPercentage();
                projectItem.min = input.getMinLimit();
                projectItem.max = input.getMaxLimit();
                projectItem.measured = input.getMeasuredVal();
                // 从子表获取温度湿度和二次电压（每个项目独立）
                projectItem.secondaryVoltage = input.getSecondaryVoltage();
                projectItem.temperature = input.getTemperature();
                projectItem.humidity = input.getHumidity();
                projectItem.resultStatus = input.getResultStatus();
                // 获取该项目对应的相位数据
                projectItem.phaseResults = phaseByProject.get(input.getItemName());
                projectItem.verified = true;
                projectList.add(projectItem);
            }
        }
        dto.setProjects(projectList);

        // 6. 返回封装好的大对象
        return dto;
    }

    // 新增/保存检测记录
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，保证数据同时写入成功
    public void save(RecordDTO dto) {
        if (dto == null || dto.getDeviceInfo() == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }

        // 1. 保存主表 (TestRecord)
        TestRecord record = new TestRecord();
        RecordDTO.DeviceInfo info = dto.getDeviceInfo();
        record.setProductNo(info.productNo);
        record.setProductName(info.productName);
        record.setManufacturer(info.manufacturer);
        record.setOrigin(info.origin);
        if (info.testDate != null && !info.testDate.isEmpty()) {
            record.setTestDate(LocalDate.parse(info.testDate, DATE_FMT));
        }

        // 处理projects（多检测项目）
        if (dto.getProjects() != null && !dto.getProjects().isEmpty()) {
            // 取第一个项目的温度、湿度、二次电压作为主表记录
            RecordDTO.ProjectItem firstProject = dto.getProjects().get(0);
            if (firstProject.secondaryVoltage != null && !firstProject.secondaryVoltage.isEmpty()) {
                record.setSecondaryVoltage(firstProject.secondaryVoltage);
            }
            if (firstProject.temperature != null && !firstProject.temperature.isEmpty()) {
                record.setTemperature(firstProject.temperature);
            }
            if (firstProject.humidity != null && !firstProject.humidity.isEmpty()) {
                record.setHumidity(firstProject.humidity);
            }
            // 汇总所有项目的判定结果
            boolean allQualified = dto.getProjects().stream()
                .allMatch(p -> "合格".equals(p.resultStatus));
            record.setResultStatus(allQualified ? "合格" : "不合格");
        }

        record.setCreateTime(LocalDateTime.now());

        // 执行插入，MyBatis 会将生成的 ID 回填到 record 对象中
        testRecordMapper.addRecord(record);
        Long recordId = record.getId();

        // 2. 保存测试数据 (TestInputDetail) - 遍历所有projects
        if (dto.getProjects() != null) {
            for (RecordDTO.ProjectItem item : dto.getProjects()) {
                TestInputDetail detail = new TestInputDetail();
                detail.setRecordId(recordId);
                detail.setItemName(item.projectName);
                detail.setRating(item.rating);
                detail.setPercentage(item.percentage);
                detail.setMinLimit(item.min);
                detail.setMaxLimit(item.max);
                detail.setMeasuredVal(item.measured);
                detail.setSecondaryVoltage(item.secondaryVoltage);
                detail.setTemperature(item.temperature);
                detail.setHumidity(item.humidity);
                detail.setResultStatus(item.resultStatus);
                testRecordMapper.addInputDetail(detail);

                // 3. 保存相位结果 (TestPhaseResult)
                if (item.phaseResults != null) {
                    item.phaseResults.forEach((phaseKey, phaseData) -> {
                        TestPhaseResult pr = new TestPhaseResult();
                        pr.setRecordId(recordId);
                        pr.setItemName(item.projectName);
                        pr.setPhase(phaseKey);
                        if (phaseData.f != null && !phaseData.f.trim().isEmpty()) {
                            pr.setFPercent(parseBigDecimal(phaseData.f));
                        }
                        if (phaseData.d != null && !phaseData.d.trim().isEmpty()) {
                            pr.setDPercent(parseBigDecimal(phaseData.d));
                        }
                        if (phaseData.dU != null && !phaseData.dU.trim().isEmpty()) {
                            pr.setDuPercent(parseBigDecimal(phaseData.dU));
                        }
                        if (phaseData.uptU != null && !phaseData.uptU.trim().isEmpty()) {
                            pr.setUptUPercent(parseBigDecimal(phaseData.uptU));
                        }
                        if (phaseData.uybU != null && !phaseData.uybU.trim().isEmpty()) {
                            pr.setUybUPercent(parseBigDecimal(phaseData.uybU));
                        }

                        testRecordMapper.addPhaseResult(pr);
                    });
                }
            }
        }
    }

    // 安全解析BigDecimal，过滤非数字字符
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // 移除非数字字符（保留小数点、负号）
            String cleaned = value.replaceAll("[^\\d.\\-]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 删除检测记录
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        if (id == null) {
            throw new RuntimeException("删除失败：ID不能为空");
        }
        testRecordMapper.deleteById(id);
        testRecordMapper.deleteInputDetailsByRecordId(id);
        testRecordMapper.deletePhaseResultsByRecordId(id);

    }

    @Override
    public StatsDTO getStats() {
        StatsDTO stats = new StatsDTO();
        LocalDate now = LocalDate.now();

        // 1. 本月数据
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        Integer monthCount = testRecordMapper.countByDateRange(startOfMonth, endOfMonth);
        Integer monthQualified = testRecordMapper.countQualifiedByDateRange(startOfMonth, endOfMonth, "合格");

        stats.setMonthCount(monthCount == null ? 0 : monthCount);
        stats.setMonthPassRate(calculateRate(monthQualified, monthCount));

        // 2. 总数据
        Integer totalCount = testRecordMapper.countTotal();
        Integer totalQualified = testRecordMapper.countTotalQualified();

        stats.setTotalCount(totalCount == null ? 0 : totalCount);
        stats.setTotalPassRate(calculateRate(totalQualified, totalCount));

        // 设置用于饼图的数据
        stats.setTotalQualified(totalQualified == null ? 0 : totalQualified);
        stats.setTotalUnqualified((totalCount == null ? 0 : totalCount) - (totalQualified == null ? 0 : totalQualified));

        // 3. 近6个月趋势图数据
        List<String> trendDates = new ArrayList<>();
        List<Integer> trendCounts = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            LocalDate start = date.withDayOfMonth(1);
            LocalDate end = date.withDayOfMonth(date.lengthOfMonth());

            Integer count = testRecordMapper.countByDateRange(start, end);

            // X轴格式：2023-12
            trendDates.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            trendCounts.add(count == null ? 0 : count);
        }

        stats.setTrendDates(trendDates);
        stats.setTrendCounts(trendCounts);

        return stats;
    }

    // 辅助方法：计算百分比字符串
    private String calculateRate(Integer qualified, Integer total) {
        if (total == null || total == 0 || qualified == null) {
            return "0%";
        }
        double rate = (double) qualified / total * 100;
        return String.format("%.1f%%", rate);
    }

    // --- PDF 导出实现 ---
    @Override
    public void exportPdf(Integer id, OutputStream os) {
        RecordDTO detail = getDetail(id);
        if (detail == null) {
            throw new RuntimeException("记录不存在");
        }

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, os);
            document.open();

            // 1. 设置字体 (使用 STSong-Light 支持中文)
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font headFont = new Font(bfChinese, 12, Font.BOLD);
            Font textFont = new Font(bfChinese, 10, Font.NORMAL);

            // 2. 标题
            Paragraph title = new Paragraph("二次压降检测报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // 3. 基础信息表格
            PdfPTable infoTable = new PdfPTable(4); // 4列
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(10f);
            infoTable.setWidths(new float[]{3f, 7f, 3f, 7f}); // 列宽比例

            addCell(infoTable, "产品编号", headFont);
            addCell(infoTable, detail.getDeviceInfo().productNo, textFont);
            addCell(infoTable, "产品名称", headFont);
            addCell(infoTable, detail.getDeviceInfo().productName, textFont);

            addCell(infoTable, "制造商", headFont);
            addCell(infoTable, detail.getDeviceInfo().manufacturer, textFont);
            addCell(infoTable, "产地", headFont);
            addCell(infoTable, detail.getDeviceInfo().origin, textFont);

            addCell(infoTable, "检测日期", headFont);
            addCell(infoTable, detail.getDeviceInfo().testDate, textFont);
            addCell(infoTable, "结论", headFont);
            addCell(infoTable, detail.getTestResult().resultStatus, textFont);

            document.add(infoTable);

            // 4. 环境参数和三相数据 - 遍历所有项目
            if (detail.getProjects() != null && !detail.getProjects().isEmpty()) {
                // 遍历每个项目
                for (RecordDTO.ProjectItem project : detail.getProjects()) {
                    // 项目标题
                    Paragraph projectTitle = new Paragraph(project.projectName + " 检测结果", headFont);
                    projectTitle.setSpacingBefore(15f);
                    projectTitle.setSpacingAfter(5f);
                    document.add(projectTitle);

                    // 项目属性
                    PdfPTable attrTable = new PdfPTable(5);
                    attrTable.setWidthPercentage(100);
                    attrTable.setSpacingAfter(5f);

                    addCell(attrTable, "档位: " + (project.rating != null ? project.rating : "-"), textFont);
                    addCell(attrTable, "百分比: " + (project.percentage != null ? project.percentage : "-"), textFont);
                    addCell(attrTable, "下限: " + (project.min != null ? project.min : "-"), textFont);
                    addCell(attrTable, "上限: " + (project.max != null ? project.max : "-"), textFont);
                    addCell(attrTable, "实测值: " + (project.measured != null ? project.measured : "-"), textFont);

                    document.add(attrTable);

                    // 环境参数
                    PdfPTable envTable = new PdfPTable(4);
                    envTable.setWidthPercentage(100);
                    envTable.setSpacingAfter(10f);

                    addCell(envTable, "二次电压: " + (project.secondaryVoltage != null ? project.secondaryVoltage : "-"), textFont);
                    addCell(envTable, "温度: " + (project.temperature != null ? project.temperature : "-") + " °C", textFont);
                    addCell(envTable, "湿度: " + (project.humidity != null ? project.humidity : "-") + " %", textFont);
                    addCell(envTable, "结论: " + (project.resultStatus != null ? project.resultStatus : "-"), textFont);

                    document.add(envTable);

                    // 三相数据表
                    if (project.phaseResults != null) {
                        PdfPTable phaseTable = new PdfPTable(4);
                        phaseTable.setWidthPercentage(100);
                        phaseTable.setSpacingAfter(10f);

                        addCell(phaseTable, "参数", headFont);
                        addCell(phaseTable, "A相 (ao)", headFont);
                        addCell(phaseTable, "B相 (bo)", headFont);
                        addCell(phaseTable, "C相 (co)", headFont);

                        RecordDTO.PhaseData ao = project.phaseResults.get("ao");
                        RecordDTO.PhaseData bo = project.phaseResults.get("bo");
                        RecordDTO.PhaseData co = project.phaseResults.get("co");

                        addCell(phaseTable, "比差 f (%)", headFont);
                        addCell(phaseTable, ao != null ? formatNumber(ao.f) : "-", textFont);
                        addCell(phaseTable, bo != null ? formatNumber(bo.f) : "-", textFont);
                        addCell(phaseTable, co != null ? formatNumber(co.f) : "-", textFont);

                        addCell(phaseTable, "角差 d (分)", headFont);
                        addCell(phaseTable, ao != null ? formatNumber(ao.d) : "-", textFont);
                        addCell(phaseTable, bo != null ? formatNumber(bo.d) : "-", textFont);
                        addCell(phaseTable, co != null ? formatNumber(co.d) : "-", textFont);

                        addCell(phaseTable, "压降误差 dU (%)", headFont);
                        addCell(phaseTable, ao != null ? formatNumber(ao.dU) : "-", textFont);
                        addCell(phaseTable, bo != null ? formatNumber(bo.dU) : "-", textFont);
                        addCell(phaseTable, co != null ? formatNumber(co.dU) : "-", textFont);

                        addCell(phaseTable, "Upt:U", headFont);
                        addCell(phaseTable, ao != null ? formatNumber(ao.uptU) : "-", textFont);
                        addCell(phaseTable, bo != null ? formatNumber(bo.uptU) : "-", textFont);
                        addCell(phaseTable, co != null ? formatNumber(co.uptU) : "-", textFont);

                        addCell(phaseTable, "Uyb:U", headFont);
                        addCell(phaseTable, ao != null ? formatNumber(ao.uybU) : "-", textFont);
                        addCell(phaseTable, bo != null ? formatNumber(bo.uybU) : "-", textFont);
                        addCell(phaseTable, co != null ? formatNumber(co.uybU) : "-", textFont);

                        document.add(phaseTable);
                    }
                }
            } else {
                // 兼容旧数据：如果没有projects，使用旧的展示方式
                Paragraph p2 = new Paragraph("环境参数与结果", headFont);
                p2.setSpacingAfter(5f);
                document.add(p2);

                PdfPTable envTable = new PdfPTable(3);
                envTable.setWidthPercentage(100);
                envTable.setSpacingAfter(10f);

                addCell(envTable, "二次电压: " + detail.getTestResult().secondaryVoltage, textFont);
                addCell(envTable, "温度: " + detail.getTestResult().temperature + " °C", textFont);
                addCell(envTable, "湿度: " + detail.getTestResult().humidity + " %", textFont);

                document.add(envTable);

                // 三相误差结果
                if (detail.getTestResult().phaseResults != null) {
                    Paragraph p3 = new Paragraph("三相误差数据", headFont);
                    p3.setSpacingAfter(5f);
                    document.add(p3);

                    PdfPTable phaseTable = new PdfPTable(4);
                    phaseTable.setWidthPercentage(100);
                    phaseTable.setSpacingAfter(10f);

                    addCell(phaseTable, "参数", headFont);
                    addCell(phaseTable, "A相 (ao)", headFont);
                    addCell(phaseTable, "B相 (bo)", headFont);
                    addCell(phaseTable, "C相 (co)", headFont);

                    RecordDTO.PhaseData ao = detail.getTestResult().phaseResults.get("ao");
                    RecordDTO.PhaseData bo = detail.getTestResult().phaseResults.get("bo");
                    RecordDTO.PhaseData co = detail.getTestResult().phaseResults.get("co");

                    addCell(phaseTable, "比差 f (%)", headFont);
                    addCell(phaseTable, ao != null ? formatNumber(ao.f) : "-", textFont);
                    addCell(phaseTable, bo != null ? formatNumber(bo.f) : "-", textFont);
                    addCell(phaseTable, co != null ? formatNumber(co.f) : "-", textFont);

                    addCell(phaseTable, "角差 d (分)", headFont);
                    addCell(phaseTable, ao != null ? formatNumber(ao.d) : "-", textFont);
                    addCell(phaseTable, bo != null ? formatNumber(bo.d) : "-", textFont);
                    addCell(phaseTable, co != null ? formatNumber(co.d) : "-", textFont);

                    addCell(phaseTable, "压降误差 dU (%)", headFont);
                    addCell(phaseTable, ao != null ? formatNumber(ao.dU) : "-", textFont);
                    addCell(phaseTable, bo != null ? formatNumber(bo.dU) : "-", textFont);
                    addCell(phaseTable, co != null ? formatNumber(co.dU) : "-", textFont);

                    addCell(phaseTable, "Upt:U", headFont);
                    addCell(phaseTable, ao != null ? formatNumber(ao.uptU) : "-", textFont);
                    addCell(phaseTable, bo != null ? formatNumber(bo.uptU) : "-", textFont);
                    addCell(phaseTable, co != null ? formatNumber(co.uptU) : "-", textFont);

                    addCell(phaseTable, "Uyb:U", headFont);
                    addCell(phaseTable, ao != null ? formatNumber(ao.uybU) : "-", textFont);
                    addCell(phaseTable, bo != null ? formatNumber(bo.uybU) : "-", textFont);
                    addCell(phaseTable, co != null ? formatNumber(co.uybU) : "-", textFont);

                    document.add(phaseTable);
                }
            }

            // 6. 详细测试数据列表 (旧数据兼容)
            if (detail.getTestData() != null && !detail.getTestData().isEmpty()) {
                Paragraph p4 = new Paragraph("详细测试数据", headFont);
                p4.setSpacingAfter(5f);
                document.add(p4);

                PdfPTable detailTable = new PdfPTable(6);
                detailTable.setWidthPercentage(100);

                String[] headers = {"项目", "档位", "百分比", "下限", "上限", "实测值"};
                for (String h : headers) addCell(detailTable, h, headFont);

                for (RecordDTO.TestItem item : detail.getTestData()) {
                    addCell(detailTable, item.item, textFont);
                    addCell(detailTable, item.rating, textFont);
                    addCell(detailTable, item.percentage, textFont);
                    addCell(detailTable, item.min, textFont);
                    addCell(detailTable, item.max, textFont);
                    addCell(detailTable, item.measured, textFont);
                }
                document.add(detailTable);
            }

            // 7. 底部签字区
            Paragraph footer = new Paragraph("\n\n检测员签名：__________________          审核员签名：__________________", textFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("生成PDF失败");
        }
    }

    // 辅助方法：添加单元格
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    // 辅助方法：格式化数字，保留4位小数，不足4位不补0
    private String formatNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        try {
            double num = Double.parseDouble(value.trim());
            String result = NUMBER_FMT.format(num);
            if (result.endsWith(".")) {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        } catch (NumberFormatException e) {
            return value;
        }
    }

}
