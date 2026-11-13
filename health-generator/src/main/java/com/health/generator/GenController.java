package com.health.generator;

import com.health.common.core.AjaxResult;
import com.health.common.core.BaseController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代码生成控制器
 */
@RestController
@RequestMapping("/api/v1/tool/gen")
public class GenController extends BaseController {

    @GetMapping("/list")
    public AjaxResult list() {
        return success("代码生成模块已就绪");
    }
}
