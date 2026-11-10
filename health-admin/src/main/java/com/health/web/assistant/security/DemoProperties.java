package com.health.web.assistant.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示环境安全化配置（T8）。
 * demo.enabled=true 时启用只读拦截与聊天额度；默认关闭，不影响本地开发。
 */
@Component
@ConfigurationProperties(prefix = "triagent.demo")
public class DemoProperties {

    /** 是否启用演示安全化（拦截写操作 + 聊天额度） */
    private boolean enabled = false;

    /** 只读演示账号的用户名列表（sql/demo_account.sql 创建的 visitor） */
    private List<String> readonlyUsers = new ArrayList<>(List.of("visitor"));

    /** 演示账号每日聊天次数上限 */
    private int dailyChatLimit = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getReadonlyUsers() {
        return readonlyUsers;
    }

    public void setReadonlyUsers(List<String> readonlyUsers) {
        this.readonlyUsers = readonlyUsers;
    }

    public int getDailyChatLimit() {
        return dailyChatLimit;
    }

    public void setDailyChatLimit(int dailyChatLimit) {
        this.dailyChatLimit = dailyChatLimit;
    }
}
