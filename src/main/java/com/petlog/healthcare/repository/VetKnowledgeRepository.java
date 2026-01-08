package com.petlog.healthcare.repository;

import com.petlog.healthcare.entity.VetKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 수의사 지식 베이스 JPA Repository
 * WHY: PostgreSQL에 Q&A 데이터 저장 및 조회
 */
@Repository
public interface VetKnowledgeRepository extends JpaRepository<VetKnowledge, Long> {

    /**
     * 진료과별 조회
     */
    List<VetKnowledge> findByDepartment(String department);

    /**
     * 진료과 + 질병 조회
     */
    List<VetKnowledge> findByDepartmentAndDisease(String department, String disease);

    /**
     * 키워드 검색 (간단 LIKE 검색)
     */
    @Query("SELECT v FROM VetKnowledge v WHERE " +
            "v.question LIKE %:keyword% OR v.answer LIKE %:keyword%")
    List<VetKnowledge> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 진료과별 통계
     */
    @Query("SELECT v.department, COUNT(v) FROM VetKnowledge v GROUP BY v.department")
    List<Object[]> countByDepartment();

    /**
     * Milvus ID로 조회
     */
    VetKnowledge findByMilvusId(String milvusId);

    /**
     * Milvus ID 목록으로 조회
     */
    @Query("SELECT v FROM VetKnowledge v WHERE v.milvusId IN :milvusIds")
    List<VetKnowledge> findByMilvusIds(@Param("milvusIds") List<String> milvusIds);

    /**
     * 전체 개수
     */
    long count();
}
