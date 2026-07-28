package com.marketflow.funcionario.domain;

import java.time.Instant;
import java.util.UUID;

import com.marketflow.funcionario.security.SenhaHash;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "funcionarios")
public class Funcionario {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String matricula;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 80)
    private String cargo;

    @Column(name = "senha_hash", nullable = false, length = 128)
    private String senhaHash;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Funcionario() {
    }

    public Funcionario(String matricula, String nome, String cargo, String senha, boolean ativo) {
        this.id = UUID.randomUUID();
        this.matricula = matricula;
        this.nome = nome;
        this.cargo = cargo;
        this.senhaHash = SenhaHash.gerar(senha);
        this.ativo = ativo;
        this.criadoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public String getNome() {
        return nome;
    }

    public String getCargo() {
        return cargo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean senhaConfere(String senha) {
        return SenhaHash.verificar(senha, senhaHash);
    }
}
