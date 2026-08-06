package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revocado = true where r.tokenFamilyId = :familyId")
    void revokeFamily(String familyId);

    @Modifying
    void deleteByUsuarioId(Long usuarioId);
}
