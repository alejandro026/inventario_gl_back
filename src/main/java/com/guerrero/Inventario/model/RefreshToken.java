package com.guerrero.Inventario.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_value", columnList = "token"),
    @Index(name = "idx_refresh_token_family", columnList = "tokenFamilyId")
})
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    private boolean revocado;
    private boolean used;

    @Column(nullable = false, length = 50)
    private String tokenFamilyId;
}
