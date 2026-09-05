package com.health.web.assistant.model;

/**
 * SSE 事件类型，对应前端 EventSource 的 event 名称（小写）。
 */
public enum SseEventType {
    TOKEN,      // 模型逐字输出
    TOOL_CALL,  // Phase2：工具调用
    TOOL_RESULT,// Phase2：工具结果
    CLARIFY,    // Phase3：状态机多轮澄清追问（携带缺失字段与问题）
    DONE,       // 结束，携带完整回答与结构化分诊
    PLAN,       // Phase5：Supervisor 编排决策（如组合请求→应急分诊优先）
    ERROR       // 失败兜底
}
