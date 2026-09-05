package com.health.web.assistant.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.DiseaseLibrary;
import com.health.system.domain.KnowledgeArticle;
import com.health.system.mapper.DiseaseLibraryMapper;
import com.health.system.mapper.KnowledgeArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL 关键词/词频混合检索（Phase3 默认实现，零新增依赖）。
 * 中文按相邻二元组（bigram）+ 英文/数字 token 拆词，对疾病库与科普文章做 OR-LIKE 候选召回，
 * 再按「标题命中权重高 + 命中次数」评分排序。规模小，纯 SQL+内存足够；
 * 后续接入向量（VectorRetriever）时仅替换实现类。
 */
@Component
public class KeywordKnowledgeRetriever implements KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(KeywordKnowledgeRetriever.class);
    private static final int CANDIDATE_LIMIT = 60;
    private static final int MAX_TERMS = 8;
    private static final Pattern ASCII_WORD = Pattern.compile("[a-zA-Z0-9]{2,}");

    private final DiseaseLibraryMapper diseaseLibraryMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;

    public KeywordKnowledgeRetriever(DiseaseLibraryMapper diseaseLibraryMapper,
                                     KnowledgeArticleMapper knowledgeArticleMapper) {
        this.diseaseLibraryMapper = diseaseLibraryMapper;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
    }

    @Override
    public List<KnowledgeDoc> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Set<String> terms = extractTerms(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        List<KnowledgeDoc> docs = new ArrayList<>();
        docs.addAll(searchDiseases(terms));
        docs.addAll(searchArticles(terms));
        docs.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        log.debug("知识检索: query={}, terms={}, hits={}", query, terms, docs.size());
        return docs.size() <= topK ? docs : docs.subList(0, topK);
    }

    private List<KnowledgeDoc> searchDiseases(Set<String> terms) {
        List<DiseaseLibrary> rows = diseaseLibraryMapper.selectList(
                new LambdaQueryWrapper<DiseaseLibrary>()
                        .and(w -> {
                            int i = 0;
                            for (String t : terms) {
                                if (i++ > 0) {
                                    w.or();
                                }
                                w.like(DiseaseLibrary::getDiseaseName, t)
                                        .or().like(DiseaseLibrary::getSymptoms, t)
                                        .or().like(DiseaseLibrary::getCauses, t);
                            }
                        })
                        .eq(DiseaseLibrary::getStatus, "ACTIVE")
                        .last("LIMIT " + CANDIDATE_LIMIT));
        List<KnowledgeDoc> docs = new ArrayList<>();
        for (DiseaseLibrary d : rows) {
            double score = scoreOf(3.0, 1.2, 1.0,
                    d.getDiseaseName(), d.getSymptoms(), d.getCauses(), terms);
            String body = firstNonBlank(d.getSymptoms(), d.getTreatment());
            docs.add(new KnowledgeDoc("疾病库", d.getId(), d.getDiseaseName(),
                    truncate(body, 90), score));
        }
        return docs;
    }

    private List<KnowledgeDoc> searchArticles(Set<String> terms) {
        List<KnowledgeArticle> rows = knowledgeArticleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .and(w -> {
                            int i = 0;
                            for (String t : terms) {
                                if (i++ > 0) {
                                    w.or();
                                }
                                w.like(KnowledgeArticle::getTitle, t)
                                        .or().like(KnowledgeArticle::getContent, t);
                            }
                        })
                        .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                        .last("LIMIT " + CANDIDATE_LIMIT));
        List<KnowledgeDoc> docs = new ArrayList<>();
        for (KnowledgeArticle a : rows) {
            double score = scoreOf(3.0, 1.2, 1.0,
                    a.getTitle(), a.getContent(), null, terms);
            docs.add(new KnowledgeDoc("科普文章", a.getId(), a.getTitle(),
                    truncate(a.getContent(), 90), score));
        }
        return docs;
    }

    /**
     * 词频评分：命中 titleWeight 加权最高的字段最相关；同一词多次命中递增少量分。
     */
    private static double scoreOf(double titleWeight, double bodyWeight, double extraWeight,
                                  String title, String body, String body2, Set<String> terms) {
        double score = 0;
        for (String t : terms) {
            score += hitScore(title, t, titleWeight, extraWeight)
                    + hitScore(body, t, bodyWeight, extraWeight)
                    + hitScore(body2, t, bodyWeight, extraWeight);
        }
        return score;
    }

    private static double hitScore(String text, String term, double weight, double extraWeight) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int idx = text.indexOf(term);
        if (idx < 0) {
            return 0;
        }
        int count = 1;
        int from = idx + term.length();
        while (count < 3) {
            int next = text.indexOf(term, from);
            if (next < 0) {
                break;
            }
            count++;
            from = next + term.length();
        }
        return weight + (count - 1) * extraWeight;
    }

    /** 中文 → 相邻二元组；英文/数字 → 原词；共取至多 8 个去重词 */
    static Set<String> extractTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        StringBuilder cjk = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjk.append(c);
            } else {
                addCjkBigrams(cjk, terms);
                cjk.setLength(0);
            }
        }
        addCjkBigrams(cjk, terms);
        Matcher m = ASCII_WORD.matcher(query);
        while (m.find()) {
            terms.add(m.group().toLowerCase());
        }
        if (terms.size() <= MAX_TERMS) {
            return terms;
        }
        // 保留前 MAX_TERMS 个（已按出现顺序 + ascii 在后）
        Set<String> cut = new LinkedHashSet<>();
        int n = 0;
        for (String t : terms) {
            if (n++ >= MAX_TERMS) {
                break;
            }
            cut.add(t);
        }
        return cut;
    }

    private static void addCjkBigrams(StringBuilder buf, Set<String> terms) {
        if (buf.length() < 2) {
            return;
        }
        for (int i = 0; i + 2 <= buf.length(); i++) {
            terms.add(buf.substring(i, i + 2));
        }
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    private static String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String s = text.trim().replaceAll("\\s+", " ");
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
