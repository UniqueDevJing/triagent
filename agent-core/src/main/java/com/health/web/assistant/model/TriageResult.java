package com.health.web.assistant.model;

import java.util.List;

/**
 * 结构化安全分诊结果。由模型在回答末尾的 ```json``` 代码块输出，编排层解析填充。
 */
public class TriageResult {

    /** EMERGENCY / URGENT / ROUTINE */
    private String urgency;

    /** 推荐科室列表 */
    private List<String> departments;

    /** 建议医院等级，如 三甲 / 社区 */
    private String hospitalLevel;

    /** 置信度 0~1 */
    private Double confidence;

    /** 需进一步澄清的问题 */
    private List<String> followUp;

    /** 强制免责声明 */
    private String disclaimer;

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public void setDepartments(List<String> departments) {
        this.departments = departments;
    }

    public String getHospitalLevel() {
        return hospitalLevel;
    }

    public void setHospitalLevel(String hospitalLevel) {
        this.hospitalLevel = hospitalLevel;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public List<String> getFollowUp() {
        return followUp;
    }

    public void setFollowUp(List<String> followUp) {
        this.followUp = followUp;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
