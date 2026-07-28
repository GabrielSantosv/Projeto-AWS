package com.marketflow.funcionario.controller;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

import com.marketflow.funcionario.service.AutenticacaoService;

public final class AutenticacaoDtos {

    private AutenticacaoDtos() {
    }

    public record LoginRequest(
            @NotBlank String matricula,
            @NotBlank String senha
    ) {
    }

    public record LoginResponse(
            String token,
            String tipo,
            UUID funcionarioId,
            String matricula,
            String nome,
            String cargo,
            Instant expiraEm
    ) {
        public static LoginResponse from(AutenticacaoService.ResultadoLogin resultado) {
            return new LoginResponse(
                    resultado.token(),
                    "Bearer",
                    resultado.funcionarioId(),
                    resultado.matricula(),
                    resultado.nome(),
                    resultado.cargo(),
                    resultado.expiraEm()
            );
        }
    }

    public record ValidacaoResponse(
            boolean valido,
            UUID funcionarioId,
            String matricula,
            String nome,
            String cargo,
            Instant expiraEm
    ) {
        public static ValidacaoResponse from(AutenticacaoService.ResultadoValidacao resultado) {
            return new ValidacaoResponse(
                    resultado.valido(),
                    resultado.funcionarioId(),
                    resultado.matricula(),
                    resultado.nome(),
                    resultado.cargo(),
                    resultado.expiraEm()
            );
        }
    }
}
