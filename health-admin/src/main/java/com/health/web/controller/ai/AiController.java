package com.health.web.controller.ai;

import com.health.common.annotation.Log;
import com.health.common.core.BaseController;
import com.health.common.core.AjaxResult;
import com.health.framework.ai.DeepSeekClient;
import com.health.system.domain.AiConversation;
import com.health.system.domain.AiMessage;
import com.health.system.mapper.AiConversationMapper;
import com.health.system.mapper.AiMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController extends BaseController {

    private final DeepSeekClient deepSeekClient;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;

    public AiController(DeepSeekClient deepSeekClient,
                        AiConversationMapper conversationMapper,
                        AiMessageMapper messageMapper) {
        this.deepSeekClient = deepSeekClient;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam Long conversationId,
                             @RequestParam String message,
                             @RequestParam(defaultValue = "false") boolean roast) {
        AiConversation conv = conversationMapper.selectById(conversationId);

        String systemPrompt = roast
                ? "你是一位毒舌健康顾问，用犀利幽默的风格指出用户健康问题，带点嘲讽但最终给出有用建议。"
                : "你是一位专业的健康管理顾问，为用户提供科学、准确、详细的健康建议。";

        List<Map<String, String>> history = new ArrayList<>();
        history.add(Map.of("role", "system", "content", systemPrompt));

        List<AiMessage> prevMessages = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreateTime));
        for (AiMessage m : prevMessages) {
            history.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        messageMapper.insert(userMsg);

        if (conv != null) {
            conv.setLastMessage(message);
            conv.setMessageCount(prevMessages.size() / 2 + 1);
            conversationMapper.updateById(conv);
        }

        Flux<String> stream = deepSeekClient.chat(message, history);

        return stream.map(content -> {
            AiMessage aiMsg = new AiMessage();
            aiMsg.setConversationId(conversationId);
            aiMsg.setRole("assistant");
            aiMsg.setContent(content);
            aiMsg.setModel("deepseek-chat");
            messageMapper.insert(aiMsg);
            return content;
        });
    }

    @GetMapping("/conversation")
    @Log(title = "对话列表查询")
    public AjaxResult listConversation(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        Page<AiConversation> p = conversationMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<AiConversation>().orderByDesc(AiConversation::getCreateTime));
        return toPage(p.getTotal(), p.getRecords());
    }

    @GetMapping("/conversation/{id}")
    public AjaxResult getConversation(@PathVariable Long id) {
        return success(conversationMapper.selectById(id));
    }

    @PostMapping("/conversation")
    @Log(title = "新增对话")
    public AjaxResult createConversation(@RequestBody AiConversation conversation) {
        conversationMapper.insert(conversation);
        return success(conversation);
    }

    @PutMapping("/conversation")
    @Log(title = "修改对话")
    public AjaxResult updateConversation(@RequestBody AiConversation conversation) {
        conversationMapper.updateById(conversation);
        return success();
    }

    @DeleteMapping("/conversation/{ids}")
    @Log(title = "删除对话")
    public AjaxResult deleteConversation(@PathVariable List<Long> ids) {
        conversationMapper.deleteBatchIds(ids);
        return success();
    }
}
