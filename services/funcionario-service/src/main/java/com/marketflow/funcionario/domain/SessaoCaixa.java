package com.marketflow.funcionario.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessoes_caixa")
public class SessaoCaixa {

    @Id
    private UUID id;

    @Column(name = "funcionario_id", nullable = false)
    private UUID funcionarioId;

    @Column(name = "token_id", nullable = false, unique = true, length = 80)
    private String tokenId;

    @Column(name = "iniciada_em", nullable = false)
    private Instant iniciadaEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean ativa;

    protected SessaoCaixa() {
    }

    public SessaoCaixa(UUID funcionarioId, String tokenId, Instant iniciadaEm, Instant expiraEm) {
        if (expiraEm == null || !expiraEm.isAfter(iniciadaEm)) {
            throw new IllegalArgumentException("expiraEm deve ser posterior a iniciadaEm");
        }
        this.id = UUID.randomUUID();
        this.funcionarioId = funcionarioId;
        this.tokenId = tokenId;
        this.iniciadaEm = iniciadaEm;
        this.expiraEm = expiraEm;
        this.ativa = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFuncionarioId() {
        return funcionarioId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public boolean estaValida(Instant agora) {
        return ativa && expiraEm.isAfter(agora);
    }

    public void encerrar() {
        this.ativa = false;
    }
}
