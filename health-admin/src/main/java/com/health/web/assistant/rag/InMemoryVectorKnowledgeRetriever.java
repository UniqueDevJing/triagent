package com.health.web.assistant.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.DiseaseLibrary;
import com.health.system.domain.KnowledgeArticle;
import com.health.system.mapper.DiseaseLibraryMapper;
import com.health.system.mapper.KnowledgeArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内存向量检索（T-A）：知识库规模小（疾病库+科普文章为百级），不需要独立向量库——
 * 全量文档的嵌入缓存在进程内存，查询时对全量做余弦排序即可。
 *
 * <p>降级边界（诚实声明）：
 * <ul>
 *   <li>启动只加载文档文本，<b>不调嵌入 API</b>（嵌入在首次查询时懒加载并缓存）；</li>
 *   <li>嵌入调用失败 → 标记不可用并永远返回空列表（只告警一次），调用方（Hybrid）
 *       自动退化为纯关键词检索，RAG 可用性不受影响；</li>
 *   <li>文档量增长到万级时应换成专用向量库（pgvector / Milvus），届时仅替换本类。</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "assistant.vector-rag.enabled", havingValue = "true")
public class InMemoryVectorKnowledgeRetriever implements KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorKnowledgeRetriever.class);

    /** 内嵌的一条知识：文本用于嵌入，doc 用于产出 */
    private record Entry(KnowledgeDoc doc, String text, float[] vector) {
        Entry withVector(float[] v) {
            return new Entry(doc, text, v);
        }
    }

    private final DiseaseLibraryMapper diseaseLibraryMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final EmbeddingClient embeddingClient;

    private final AtomicBoolean unavailable = new AtomicBoolean(false);
    private volatile List<Entry> entries = List.of();
    private volatile boolean loaded = false;
    private final Object lock = new Object();

    public InMemoryVectorKnowledgeRetriever(DiseaseLibraryMapper diseaseLibraryMapper,
                                            KnowledgeArticleMapper knowledgeArticleMapper,
                                            EmbeddingClient embeddingClient) {
        this.diseaseLibraryMapper = diseaseLibraryMapper;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.embeddingClient = embeddingClient;
    }

    @PostConstruct
    void loadAll() {
        // 只加载文本；嵌入延后到首次查询（避免启动期 API key 缺失直接把应用打挂）
        List<Entry> list = new ArrayList<>();
        for (DiseaseLibrary d : diseaseLibraryMapper.selectList(
                new LambdaQueryWrapper<DiseaseLibrary>().eq(DiseaseLibrary::getStatus, "ACTIVE"))) {
            String text = joinNonBlank(d.getDiseaseName(), d.getSymptoms(), d.getCauses(), d.getTreatment());
            String body = firstNonBlank(d.getSymptoms(), d.getTreatment());
            list.add(new Entry(new KnowledgeDoc("疾病库", d.getId(), d.getDiseaseName(), body, 0.0), text, null));
        }
        for (KnowledgeArticle a : knowledgeArticleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getStatus, "PUBLISHED"))) {
            String text = joinNonBlank(a.getTitle(), a.getContent());
            list.add(new Entry(new KnowledgeDoc("科普文章", a.getId(), a.getTitle(), text, 0.0), text, null));
        }
        entries = list;
        loaded = true;
        log.info("[vector-rag] 已加载 {} 条知识文档（嵌入将懒加载，模型={}）",
                list.size(), embeddingClient.model());
    }

    @Override
    public List<KnowledgeDoc> retrieve(String query, int topK) {
        if (!loaded || unavailable.get() || query == null || query.isBlank()) {
            return List.of();
        }
        float[] qv = embeddingClient.embed(query);
        if (qv == null || qv.length == 0) {
            markUnavailable("查询嵌入失败");
            return List.of();
        }
        ensureEmbedded();
        if (unavailable.get()) {
            return List.of();
        }
        record Hit(KnowledgeDoc doc, double score) {
        }
        List<Hit> hits = new ArrayList<>();
        for (Entry e : entries) {
            double s = Cosine.cosine(qv, e.vector());
            if (s > 0) {
                hits.add(new Hit(e.doc(), s));
            }
        }
        hits.sort((x, y) -> Double.compare(y.score(), x.score()));
        List<KnowledgeDoc> out = new ArrayList<>();
        for (Hit h : hits) {
            if (out.size() >= Math.max(1, topK)) {
                break;
            }
            // 用余弦分数作为文档得分（0~1，供上层参考；融合排序由 RRF 决定）
            out.add(new KnowledgeDoc(h.doc().getSource(), h.doc().getRefId(),
                    h.doc().getTitle(), h.doc().getSnippet(), h.score()));
        }
        return out;
    }

    /** 首次查询时批量嵌入全部文档（一次性成本，之后全内存） */
    private void ensureEmbedded() {
        if (entries.isEmpty() || entries.get(0).vector() != null) {
            return;
        }
        synchronized (lock) {
            if (entries.isEmpty() || entries.get(0).vector() != null) {
                return;
            }
            List<Entry> embedded = new ArrayList<>(entries.size());
            int failed = 0;
            for (Entry e : entries) {
                float[] v = embeddingClient.embed(e.text());
                if (v == null || v.length == 0) {
                    failed++;
                    embedded.add(e);
                } else {
                    embedded.add(e.withVector(v));
                }
            }
            if (failed == entries.size() && !entries.isEmpty()) {
                markUnavailable("文档嵌入全部失败");
                return;
            }
            if (failed > 0) {
                log.warn("[vector-rag] {} 条文档嵌入失败（这些文档不参与向量召回）", failed);
            }
            entries = embedded;
            log.info("[vector-rag] 文档嵌入完成：{} 条", entries.size());
        }
    }

    private void markUnavailable(String reason) {
        if (unavailable.compareAndSet(false, true)) {
            log.warn("[vector-rag] 向量检索不可用（{}），长期降级为纯关键词检索", reason);
        }
    }

    private static String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(p);
            }
        }
        return sb.toString();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }
}
