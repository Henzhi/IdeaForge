package com.ideaforge.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 第三方账号绑定表。provider: google/apple/wechat/qq/github。
 */
@Entity
@Table(name = "user_auth",
    uniqueConstraints = @UniqueConstraint(name = "uk_provider_pid", columnNames = {"provider", "providerId"}),
    indexes = @Index(name = "idx_user_auth_user", columnList = "user_id"))
@Data
@NoArgsConstructor
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "access_token")
    private String accessToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
