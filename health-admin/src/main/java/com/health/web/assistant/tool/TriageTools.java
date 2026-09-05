package com.health.web.assistant.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.domain.DiseaseLibrary;
import com.health.system.domain.Member;
import com.health.system.mapper.DiseaseLibraryMapper;
import com.health.system.mapper.MemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分诊数据服务：把疾病库/会员查询封装为纯查询方法（无 @Tool）。
 * 工具暴露统一走 AgentToolkit（每请求可观测，@Tool 带事件发布），
 * 与 AgentController 的 HTTP 端点共享底层数据，避免重复业务逻辑。
 */
@Service
public class TriageTools {

    private final DiseaseLibraryMapper diseaseLibraryMapper;
    private final MemberMapper memberMapper;

    public TriageTools(DiseaseLibraryMapper diseaseLibraryMapper, MemberMapper memberMapper) {
        this.diseaseLibraryMapper = diseaseLibraryMapper;
        this.memberMapper = memberMapper;
    }

    public List<DiseaseLibrary> searchDiseases(String keyword) {
        return diseaseLibraryMapper.selectList(new LambdaQueryWrapper<DiseaseLibrary>()
                .and(w -> w.like(DiseaseLibrary::getDiseaseName, keyword)
                        .or().like(DiseaseLibrary::getSymptoms, keyword)
                        .or().like(DiseaseLibrary::getCauses, keyword))
                .eq(DiseaseLibrary::getStatus, "ACTIVE")
                .last("LIMIT 20"));
    }

    public List<Member> searchMembers(String keyword) {
        return memberMapper.selectList(new LambdaQueryWrapper<Member>()
                .like(Member::getName, keyword)
                .or().like(Member::getPhone, keyword)
                .last("LIMIT 10"));
    }
}
