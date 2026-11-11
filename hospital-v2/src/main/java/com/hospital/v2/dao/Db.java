package com.hospital.v2.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 统一 JDBC 访问助手：命名参数 + 主键回填 + Map 行。 */
@Repository
public class Db {
    private final NamedParameterJdbcTemplate np;

    public Db(JdbcTemplate jdbcTemplate) {
        this.np = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public List<Map<String, Object>> query(String sql, Map<String, Object> params) {
        return np.queryForList(sql, new MapSqlParameterSource(params));
    }

    public Map<String, Object> queryOne(String sql, Map<String, Object> params) {
        List<Map<String, Object>> rows = np.queryForList(sql, new MapSqlParameterSource(params));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int update(String sql, Map<String, Object> params) {
        return np.update(sql, new MapSqlParameterSource(params));
    }

    /** 插入并返回自增主键。sql 中的 :name 占位符走命名参数。 */
    public long insert(String sql, Map<String, Object> params) {
        MapSqlParameterSource src = new MapSqlParameterSource(params);
        KeyHolder kh = new GeneratedKeyHolder();
        np.update(sql, src, kh, new String[]{"id"});
        Number id = kh.getKey();
        return id == null ? -1 : id.longValue();
    }
}
