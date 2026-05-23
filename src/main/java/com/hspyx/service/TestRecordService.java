package com.hspyx.service;


import com.hspyx.pojo.RecordDTO;
import com.hspyx.pojo.StatsDTO;
import com.hspyx.pojo.TestRecord;
import jakarta.servlet.ServletOutputStream;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Service
public interface TestRecordService {
    List<TestRecord> findAll(String keywords);

    RecordDTO getDetail(Integer id);

    void save(RecordDTO recordDTO);

    void deleteById(Integer id);

    StatsDTO getStats();

    void exportPdf(Integer id, OutputStream os);
}
