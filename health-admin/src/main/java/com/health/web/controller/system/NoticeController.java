package com.health.web.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.common.annotation.Log;
import com.health.common.core.AjaxResult;
import com.health.common.core.BaseController;
import com.health.framework.realtime.EventPublisher;
import com.health.system.domain.SysNotice;
import com.health.system.mapper.SysNoticeMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/notice")
public class NoticeController extends BaseController {

    private final SysNoticeMapper noticeMapper;
    private final EventPublisher eventPublisher;

    public NoticeController(SysNoticeMapper noticeMapper, EventPublisher eventPublisher) {
        this.noticeMapper = noticeMapper;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    public AjaxResult list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<SysNotice> p = noticeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysNotice>()
                        .and(w -> w.eq(SysNotice::getUserId, userId)
                                .or().eq(SysNotice::getUserId, 0L))
                        .orderByDesc(SysNotice::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = noticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>()
                        .and(w -> w.eq(SysNotice::getUserId, userId)
                                .or().eq(SysNotice::getUserId, 0L))
                        .eq(SysNotice::getIsRead, 0));
        Map<String, Object> result = new HashMap<>();
        result.put("count", count != null ? count : 0);
        return success(result);
    }

    @PutMapping("/{id}/read")
    public AjaxResult markAsRead(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) return AjaxResult.error("通知不存在");
        if (!userId.equals(notice.getUserId()) && notice.getUserId() != 0L) {
            return AjaxResult.error(403, "无权操作此通知");
        }
        notice.setIsRead(1);
        noticeMapper.updateById(notice);
        return success();
    }

    @PutMapping("/read-all")
    public AjaxResult markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysNotice update = new SysNotice();
        update.setIsRead(1);
        noticeMapper.update(update, new LambdaUpdateWrapper<SysNotice>()
                .and(w -> w.eq(SysNotice::getUserId, userId)
                        .or().eq(SysNotice::getUserId, 0L))
                .eq(SysNotice::getIsRead, 0));
        return success();
    }

    @PostMapping
    @Log(title = "发送通知")
    public AjaxResult create(@RequestBody SysNotice notice) {
        if (notice.getUserId() == null) notice.setUserId(0L);
        notice.setIsRead(0);
        noticeMapper.insert(notice);
        eventPublisher.publish("notifications", "notice_created",
                "{\"id\":" + notice.getId() + ",\"title\":\"" + notice.getTitle() + "\"}");
        return success(notice);
    }

    @DeleteMapping("/{id}")
    @Log(title = "删除通知")
    public AjaxResult delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) return success();
        if (!userId.equals(notice.getUserId()) && notice.getUserId() != 0L) {
            return AjaxResult.error(403, "无权删除此通知");
        }
        noticeMapper.deleteById(id);
        return success();
    }
}
