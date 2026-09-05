package com.health.web.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.AgentPreOrder;
import com.health.system.mapper.AgentPreOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * AI 预约预订单服务：两层幂等。
 * 1) Redis SETNX 预占（TTL 5min）：同一意图秒回已有预订单，不落重复单；
 * 2) DB idempotency_key 唯一索引兜底：并发/Redis 失效场景下由唯一键拦截。
 * 幂等键 = sha256(userId|memberId|department|yyyy-MM-dd)
 */
@Service
public class PreOrderService {

    private static final Logger log = LoggerFactory.getLogger(PreOrderService.class);
    private static final String REDIS_KEY_PREFIX = "agent:preorder:";
    private static final Duration CLAIM_TTL = Duration.ofMinutes(5);

    private final AgentPreOrderMapper preOrderMapper;
    private final StringRedisTemplate redis;

    public PreOrderService(AgentPreOrderMapper preOrderMapper, StringRedisTemplate redis) {
        this.preOrderMapper = preOrderMapper;
        this.redis = redis;
    }

    /**
     * 幂等创建预订单。同一 (userId, memberId, department, date) 重复调用返回同一单。
     */
    @Transactional
    public AgentPreOrder create(Long userId, Long memberId, String department, String appointmentDate, String symptomSummary) {
        String dept = department == null ? "" : department.trim();
        if (dept.isEmpty()) {
            throw new IllegalArgumentException("就诊科室不能为空");
        }
        LocalDate date = (appointmentDate == null || appointmentDate.isBlank())
                ? LocalDate.now().plusDays(1)
                : LocalDate.parse(appointmentDate.trim());
        String symptom = symptomSummary == null ? "" : symptomSummary.trim();
        String key = idempotencyKey(userId, memberId, dept, date);
        String redisKey = REDIS_KEY_PREFIX + key;

        // 第一层：Redis 预占。已被占 → 说明同一意图已创建过，直接回查返回（幂等命中）
        Boolean claimed = redis.opsForValue().setIfAbsent(redisKey, "1", CLAIM_TTL);
        if (Boolean.FALSE.equals(claimed)) {
            AgentPreOrder existing = findByKey(key);
            if (existing != null) {
                log.info("幂等命中(Redis)：key={}, 返回预订单 id={}", key, existing.getId());
                return existing;
            }
            // Redis 有占位但记录缺失（异常边角），落到 DB 唯一索引兜底
        }

        AgentPreOrder po = new AgentPreOrder();
        po.setUserId(userId);
        po.setMemberId(memberId);
        po.setDepartment(dept);
        po.setAppointmentDate(date);
        po.setSymptomSummary(symptom);
        po.setIdempotencyKey(key);
        po.setStatus("PENDING");
        try {
            preOrderMapper.insert(po);
        } catch (DuplicateKeyException ex) {
            // 第二层兜底：并发下唯一索引拦截 → 返回已有单
            AgentPreOrder existing = findByKey(key);
            if (existing != null) {
                log.info("幂等命中(DB唯一键)：key={}, 返回预订单 id={}", key, existing.getId());
                return existing;
            }
            throw ex;
        }
        redis.opsForValue().set(redisKey, String.valueOf(po.getId()), CLAIM_TTL);
        log.info("创建预订单: id={}, userId={}, memberId={}, dept={}, date={}", po.getId(), userId, memberId, dept, date);
        return po;
    }

    /** 用户确认预订单 → CONFIRMED */
    @Transactional
    public AgentPreOrder confirm(Long userId, Long id) {
        AgentPreOrder po = preOrderMapper.selectById(id);
        if (po == null) {
            throw new IllegalArgumentException("预订单不存在");
        }
        if (!po.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该预订单");
        }
        if (!"PENDING".equals(po.getStatus())) {
            throw new IllegalArgumentException("预订单状态为 " + po.getStatus() + "，无法确认");
        }
        po.setStatus("CONFIRMED");
        po.setConfirmTime(java.time.LocalDateTime.now());
        preOrderMapper.updateById(po);
        log.info("确认预订单: id={}, userId={}", id, userId);
        return po;
    }

    public AgentPreOrder findByKey(String idempotencyKey) {
        return preOrderMapper.selectOne(new LambdaQueryWrapper<AgentPreOrder>()
                .eq(AgentPreOrder::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    public static String idempotencyKey(Long userId, Long memberId, String department, LocalDate date) {
        String raw = userId + "|" + (memberId == null ? "" : memberId) + "|" + department + "|" + date;
        return sha256Hex(raw);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
