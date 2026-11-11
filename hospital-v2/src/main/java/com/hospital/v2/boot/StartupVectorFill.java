package com.hospital.v2.boot;

import com.hospital.v2.service.KnowledgeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 启动后补齐种子知识片段的向量（embedding 列为 PENDING）。 */
@Component
public class StartupVectorFill implements CommandLineRunner {
    private final KnowledgeService knowledge;

    public StartupVectorFill(KnowledgeService knowledge) {
        this.knowledge = knowledge;
    }

    @Override
    public void run(String... args) {
        knowledge.embedPending();
    }
}
