package com.ideaforge.idea.repository;

import com.ideaforge.idea.entity.Idea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdeaRepository extends JpaRepository<Idea, Long> {

    /** 按 client_uuid 查找未软删的想法(同步幂等) */
    Optional<Idea> findByUserIdAndClientUuidAndDeletedAtIsNull(Long userId, String clientUuid);

    /**
     * 游标分页查询:created_at < cursor,按时间倒序。
     * cursor 为 null 时查首页。可选过滤分类/归档状态。
     */
    @Query("SELECT i FROM Idea i WHERE i.userId = :userId AND i.deletedAt IS NULL " +
           "AND (:cursor IS NULL OR i.createdAt < :cursor) " +
           "AND (:categoryId IS NULL OR i.categoryId = :categoryId) " +
           "AND (:archived IS NULL OR i.archived = :archived) " +
           "ORDER BY i.pinned DESC, i.createdAt DESC")
    Page<Idea> findByCursor(@Param("userId") Long userId,
                            @Param("cursor") LocalDateTime cursor,
                            @Param("categoryId") Short categoryId,
                            @Param("archived") Boolean archived,
                            Pageable pageable);

    /** 软删除(置 deleted_at),不物理删除,保留数据可恢复 */
    @Modifying
    @Query("UPDATE Idea i SET i.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE i.userId = :userId AND i.clientUuid = :clientUuid AND i.deletedAt IS NULL")
    int softDeleteByClientUuid(@Param("userId") Long userId, @Param("clientUuid") String clientUuid);
}
