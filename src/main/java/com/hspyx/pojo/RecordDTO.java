package com.hspyx.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordDTO {

    private DeviceInfo deviceInfo;

    private List<ProjectItem> projects;

    private List<TestItem> testData;

    private TestResult testResult;

    public static class DeviceInfo {
        public String productNo;
        public String productName;
        public String manufacturer;
        public String testDate;
        public String origin;
    }

    public static class ProjectItem {
        public String projectName;
        public String rating;
        public String percentage;
        public String min;
        public String max;
        public String measured;
        public String secondaryVoltage;
        public String temperature;
        public String humidity;
        public String resultStatus;
        public Boolean verified;
        public Map<String, PhaseData> phaseResults;
    }

    public static class PhaseData {
        public String f;
        public String d;
        public String dU;
        public String uptU;
        public String uybU;
    }

    public static class TestItem {
        public String item;
        public String rating;
        public String percentage;
        public String min;
        public String max;
        public String measured;
    }

    public static class TestResult {
        public String secondaryVoltage;
        public String temperature;
        public String humidity;
        public String resultStatus;
        public Map<String, PhaseData> phaseResults;
    }
}
