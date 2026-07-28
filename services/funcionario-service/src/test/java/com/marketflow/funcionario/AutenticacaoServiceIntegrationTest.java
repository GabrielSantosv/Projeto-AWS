package com.marketflow.funcionario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.marketflow.funcionario.domain.Funcionario;
import com.marketflow.funcionario.domain.SessaoCaixa;
import com.marketflow.funcionario.exception.OperadorNaoAutorizadoException;
import com.marketflow.funcionario.repository.FuncionarioRepository;
import com.marketflow.funcionario.repository.SessaoCaixaRepository;
import com.marketflow.funcionario.service.AutenticacaoService;
import com.marketflow.funcionario.security.TokenOpaco;

@SpringBootTest
@ActiveProfiles("test")
class AutenticacaoServiceIntegrationTest {

    @Autowired
    private AutenticacaoService autenticacaoService;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private SessaoCaixaRepository sessaoCaixaRepository;

    @BeforeEach
    void setUp() {
        sessaoCaixaRepository.deleteAll();
        funcionarioRepository.deleteAll();
    }

    @Test
    void loginComFuncionarioAtivoECargoAutorizadoCriaSessao() {
        Funcionario funcionario = funcionarioRepository.save(
                new Funcionario("OP-001", "Ana Operadora", "OPERADOR", "segredo", true)
        );

        AutenticacaoService.ResultadoLogin resultado = autenticacaoService.login("OP-001", "segredo");

        assertThat(resultado.token()).isNotBlank();
        assertThat(resultado.funcionarioId()).isEqualTo(funcionario.getId());
        assertThat(sessaoCaixaRepository.findByTokenId(TokenOpaco.identificar(resultado.token()))).isPresent();
        assertThat(sessaoCaixaRepository.findByTokenId(resultado.token())).isEmpty();
    }

    @Test
    void loginComFuncionarioInativoRetornaNaoAutorizadoESemSessao() {
        funcionarioRepository.save(new Funcionario("OP-002", "Ana Inativa", "OPERADOR", "segredo", false));

        assertThatThrownBy(() -> autenticacaoService.login("OP-002", "segredo"))
                .isInstanceOf(OperadorNaoAutorizadoException.class);
        assertThat(sessaoCaixaRepository.findAll()).isEmpty();
    }

    @Test
    void loginComFuncionarioInexistenteRetornaNaoAutorizado() {
        assertThatThrownBy(() -> autenticacaoService.login("OP-404", "segredo"))
                .isInstanceOf(OperadorNaoAutorizadoException.class);
        assertThat(sessaoCaixaRepository.findAll()).isEmpty();
    }

    @Test
    void tokenValidoEhConfirmado() {
        funcionarioRepository.save(new Funcionario("OP-003", "Ana Valida", "SUPERVISOR", "segredo", true));
        AutenticacaoService.ResultadoLogin login = autenticacaoService.login("OP-003", "segredo");

        AutenticacaoService.ResultadoValidacao validacao = autenticacaoService.validar(login.token());

        assertThat(validacao.valido()).isTrue();
        assertThat(validacao.matricula()).isEqualTo("OP-003");
    }

    @Test
    void tokenExpiradoOuInvalidoEhRejeitado() {
        Funcionario funcionario = funcionarioRepository.save(
                new Funcionario("OP-004", "Ana Expirada", "OPERADOR", "segredo", true)
        );
        SessaoCaixa expirada = sessaoCaixaRepository.save(new SessaoCaixa(
                funcionario.getId(),
                TokenOpaco.identificar("token-expirado"),
                Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS)
        ));

        AutenticacaoService.ResultadoValidacao expirado = autenticacaoService.validar("token-expirado");
        AutenticacaoService.ResultadoValidacao inexistente = autenticacaoService.validar("token-inexistente");

        assertThat(expirado.valido()).isFalse();
        assertThat(inexistente.valido()).isFalse();
        assertThat(sessaoCaixaRepository.findByTokenId(TokenOpaco.identificar("token-expirado")).orElseThrow().isAtiva()).isFalse();
    }
}
