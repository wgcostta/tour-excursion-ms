package br.com.tourapp.tourapp.repository;


import br.com.tourapp.tourapp.entity.Notificacao;
import br.com.tourapp.tourapp.enums.TipoNotificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    Page<Notificacao> findByOrganizadorId(UUID organizadorId, Pageable pageable);

    List<Notificacao> findByOrganizadorIdAndEnviadaFalse(UUID organizadorId);

    @Query("SELECT n FROM Notificacao n WHERE n.organizador.id = :organizadorId AND n.id = :notificacaoId")
    Notificacao findByIdAndOrganizadorId(@Param("notificacaoId") UUID notificacaoId,
                                         @Param("organizadorId") UUID organizadorId);

    List<Notificacao> findByTipo(TipoNotificacao tipo);

    @Query("SELECT n FROM Notificacao n WHERE n.enviada = false AND n.createdAt < :tempo")
    List<Notificacao> findNotificacoesPendentes(@Param("tempo") LocalDateTime tempo);

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.organizador.id = :organizadorId AND n.enviada = true")
    Long countEnviadasByOrganizadorId(@Param("organizadorId") UUID organizadorId);
}