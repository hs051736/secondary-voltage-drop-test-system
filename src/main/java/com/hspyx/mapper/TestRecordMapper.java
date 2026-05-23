package com.hspyx.mapper;

import com.hspyx.pojo.TestInputDetail;
import com.hspyx.pojo.TestPhaseResult;
import com.hspyx.pojo.TestRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TestRecordMapper {

//    @Select("SELECT id, product_no, product_name, manufacturer, origin, test_date, result_status FROM test_records ORDER BY id DESC")
//    List<TestRecord> findAll();

    /**
     * 查询所有记录，支持模糊搜索
     * 使用 <script> 标签实现动态 SQL
     */
    @Select("<script>SELECT * FROM test_records WHERE 1=1 <if test='keyword != null and keyword != \"\"'> AND (product_no LIKE CONCAT('%', #{keyword}, '%') OR manufacturer LIKE CONCAT('%', #{keyword}, '%')) </if> ORDER BY create_time DESC</script>")
    List<TestRecord> findAll(@Param("keyword") String keyword);


    // 根据id查询测试记录
    @Select("SELECT id, product_no, product_name, manufacturer, origin, test_date, result_status, secondary_voltage, temperature, humidity FROM test_records WHERE id = #{id}")
    TestRecord getById(Integer id);


    // 根据id查询测试输入详情
    @Select("SELECT id, record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, " +
            "secondary_voltage, temperature, humidity, result_status FROM test_input_details WHERE record_id = #{id} ORDER BY id")
    List<TestInputDetail> getInputDetailsByRecordId(Integer id);

    // 根据id查询测试相位结果 - 按 item_name 和 phase 排序
    @Select("SELECT id, record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent " +
            "FROM test_phase_results WHERE record_id = #{id} ORDER BY item_name, phase")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "recordId", column = "record_id"),
        @Result(property = "itemName", column = "item_name"),
        @Result(property = "phase", column = "phase"),
        @Result(property = "fPercent", column = "f_percent"),
        @Result(property = "dPercent", column = "d_percent"),
        @Result(property = "duPercent", column = "du_percent"),
        @Result(property = "uptUPercent", column = "uptu_percent"),
        @Result(property = "uybUPercent", column = "uybu_percent")
    })
    List<TestPhaseResult> getPhaseResultsByRecordId(Integer id);

    // 新增测试记录
    @Insert("INSERT INTO test_records(product_no, product_name, manufacturer, origin, test_date, secondary_voltage, temperature, humidity, result_status, create_time) " +
            "VALUES(#{productNo}, #{productName}, #{manufacturer}, #{origin}, #{testDate}, #{secondaryVoltage}, #{temperature}, #{humidity}, #{resultStatus}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void addRecord(TestRecord record);

    // 新增测试输入详情
    @Insert("INSERT INTO test_input_details(record_id, item_name, rating, percentage, min_limit, max_limit, measured_val, secondary_voltage, temperature, humidity, result_status) " +
            "VALUES(#{recordId}, #{itemName}, #{rating}, #{percentage}, #{minLimit}, #{maxLimit}, #{measuredVal}, #{secondaryVoltage}, #{temperature}, #{humidity}, #{resultStatus})")
    void addInputDetail(TestInputDetail detail);

    // 新增测试相位结果
    @Insert("INSERT INTO test_phase_results(record_id, item_name, phase, f_percent, d_percent, du_percent, uptu_percent, uybu_percent) " +
            "VALUES(#{recordId}, #{itemName}, #{phase}, #{fPercent}, #{dPercent}, #{duPercent}, #{uptUPercent}, #{uybUPercent})")
    void addPhaseResult(TestPhaseResult result);

    // 根据id删除测试记录
    @Delete("DELETE FROM test_records WHERE id = #{id}")
    void deleteById(Integer id);

    // 根据id删除测试输入详情
    @Delete("DELETE FROM test_input_details WHERE record_id = #{id}")
    void deleteInputDetailsByRecordId(Integer id);

    // 根据id删除测试相位结果
    @Delete("DELETE FROM test_phase_results WHERE record_id = #{id}")
    void deletePhaseResultsByRecordId(Integer id);

    // 范围统计
    @Select("SELECT COUNT(*) FROM test_records WHERE test_date BETWEEN #{start} AND #{end}")
    Integer countByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COUNT(*) FROM test_records WHERE result_status = #{status} AND test_date BETWEEN #{start} AND #{end}")
    Integer countQualifiedByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("status") String status);

    //  总量统计
    @Select("SELECT COUNT(*) FROM test_records")
    Integer countTotal();

    @Select("SELECT COUNT(*) FROM test_records WHERE result_status = '合格'")
    Integer countTotalQualified();
}
