package com.health.quartz;

import com.health.common.core.AjaxResult;
import com.health.common.core.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务控制器
 */
@RestController
@RequestMapping("/api/v1/monitor/job")
public class SysJobController extends BaseController {

    @GetMapping("/list")
    public AjaxResult list() {
        return success("定时任务模块已就绪");
    }
}
