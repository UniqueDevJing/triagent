package com.hospital.v2.service;

import com.hospital.v2.config.EmbeddingGateway;
import com.hospital.v2.config.HospitalProps;
import com.hospital.v2.dao.Db;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 医学知识库 RAG：上传/切片/向量化 / 按库收窄 + 内存余弦 TopK。 */
@Service
public class KnowledgeService {
    private final Db db;
    private final EmbeddingGateway embed;
    private final HospitalProps props;

    public KnowledgeService(Db db, EmbeddingGateway embed, HospitalProps props) {
        this.db = db;
        this.embed = embed;
        this.props = props;
    }

    public List<Map<String, Object>> listBases() {
        return db.query("SELECT * FROM knowledge_base ORDER BY id", Map.of());
    }

    public List<Map<String, Object>> listDocs(Long baseId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("base_id", baseId);
        return db.query("SELECT * FROM knowledge_doc WHERE base_id=:base_id ORDER BY id", p);
    }

    /** 新增文档并立即切片 + 向量化（单片段简化版，演示全链路）。 */
    public long addDoc(long baseId, String title, String content) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("base_id", baseId);
        d.put("title", title);
        long docId = db.insert("INSERT INTO knowledge_doc (base_id, title, source_type, status) VALUES (:base_id,:title,'text','READY')", d);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("doc_id", docId);
        s.put("base_id", baseId);
        s.put("content", content);
        long segId = db.insert("INSERT INTO knowledge_segment (doc_id, base_id, content, vector_status) VALUES (:doc_id,:base_id,:content,'PENDING')", s);
        embedSegment(segId, content);
        return docId;
    }

    /** 把一条 PENDING 片段向量化并落库。 */
    private void embedSegment(long segId, String content) {
        float[] v = embed.embed(content);
        Map<String, Object> u = new LinkedHashMap<>();
        u.put("emb", EmbeddingGateway.toJson(v));
        u.put("id", segId);
        db.update("UPDATE knowledge_segment SET embedding=:emb, vector_status='READY' WHERE id=:id", u);
    }

    /** 应用启动时补齐所有 PENDING 向量。 */
    public void embedPending() {
        List<Map<String, Object>> pending = db.query(
                "SELECT id, content FROM knowledge_segment WHERE vector_status='PENDING'", Map.of());
        for (Map<String, Object> row : pending) {
            embedSegment(((Number) row.get("id")).longValue(), (String) row.get("content"));
        }
    }

    /** 检索：按知识库收窄候选 → 余弦 TopK → 阈值过滤。 */
    public List<Map<String, Object>> retrieve(String query, Long baseId, int topK) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (baseId != null) {
            p.put("base_id", baseId);
        }
        String sql = "SELECT id, doc_id, base_id, content, embedding FROM knowledge_segment WHERE vector_status='READY'";
        if (baseId != null) sql += " AND base_id=:base_id";
        List<Map<String, Object>> cands = db.query(sql, p);

        float[] q = embed.embed(query);
        int k = Math.min(topK <= 0 ? props.getRag().getTopK() : topK, cands.size());
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> c : cands) {
            float[] v = EmbeddingGateway.fromJson((String) c.get("embedding"));
            double sim = EmbeddingGateway.cosine(q, v);
            Map<String, Object> item = new LinkedHashMap<>(c);
            item.put("score", Math.round(sim * 1000.0) / 1000.0);
            scored.add(item);
        }
        scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        List<Map<String, Object>> out = new ArrayList<>();
        double th = props.getRag().getScoreThreshold();
        for (Map<String, Object> m : scored) {
            if (out.size() >= k) break;
            if ((Double) m.get("score") >= th) out.add(m);
        }
        return out;
    }
}
