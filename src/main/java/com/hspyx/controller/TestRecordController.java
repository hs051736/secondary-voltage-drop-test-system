package com.hspyx.controller;

import com.hspyx.pojo.*;
import com.hspyx.service.TestRecordService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TestRecordController {

    @Autowired
    private TestRecordService testRecordService;


    // 获取所有记录 (支持模糊查询)
    @GetMapping("/records")
    public Result getAllRecords(@RequestParam(required = false) String keyword) {
        // 将 keyword 传给 Service
        List<TestRecord> list = testRecordService.findAll(keyword);
        return Result.success(list);
    }

    // 下载PDF文档
    @GetMapping("/records/{id}/pdf")
    public void downloadPdf(@PathVariable Integer id, HttpServletResponse response) throws IOException {
        // 设置响应头，告诉浏览器下载文件
        response.setContentType("application/pdf");
        // inline 表示在浏览器直接打开，attachment 表示下载。这里用 attachment 并指定文件名
        response.setHeader("Content-Disposition", "attachment; filename=report_" + id + ".pdf");

        testRecordService.exportPdf(id, response.getOutputStream());
    }

    // 获取单条记录详情
    @GetMapping("/records/{id}")
    public Result getInfo(@PathVariable("id") Integer id) {
        System.out.println("根据id获取单条记录详情：" + id);
        RecordDTO recordDTO = testRecordService.getDetail(id);
        return Result.success(recordDTO);
    }


    // 新增/保存检测记录
    @PostMapping("/records")
    public Result save(@RequestBody RecordDTO recordDTO) {
        testRecordService.save(recordDTO);
        return Result.success();
    }


    // 删除检测记录
    @DeleteMapping("/records/{id}")
    public Result delete(@PathVariable("id") Integer id){
        System.out.println("删除id为" + id + "的检测记录");
        testRecordService.deleteById(id);

        return Result.success();
    }

    // 获取统计概览
    @GetMapping("/stats")
    public Result getStats() {
        StatsDTO stats = testRecordService.getStats();
        return Result.success(stats);
    }
}
