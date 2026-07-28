package com.marketflow.funcionario.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketflow.funcionario.domain.Funcionario;
import com.marketflow.funcionario.domain.SessaoCaixa;
import com.marketflow.funcionario.exception.OperadorNaoAutorizadoException;
import com.marketflow.funcionario.repository.FuncionarioRepository;
import com.marketflow.funcionario.repository.SessaoCaixaRepository;
import com.marketflow.funcionario.security.TokenOpaco;

@Service
public class AutenticacaoService {

    private final FuncionarioRepository funcionarioRepository;
    private final SessaoCaixaRepository sessaoCaixaRepository;
    private final long duracaoSessaoMinutos;
    private final Set<String> cargosAutorizados;
    private final Clock clock;
    @Autowired

    public AutenticacaoService(
            FuncionarioRepository funcionarioRepository,
            SessaoCaixaRepository sessaoCaixaRepository,
            @Value("${auth.session-duration-minutes:60}") long duracaoSessaoMinutos,
            @Value("${auth.cargos-autorizados:OPERADOR,SUPERVISOR,GERENTE}") String cargosAutorizados
    ) {
        this(funcionarioRepository, sessaoCaixaRepository, duracaoSessaoMinutos, cargosAutorizados, Clock.systemUTC());
    }

    AutenticacaoService(
            FuncionarioRepository funcionarioRepository,
            SessaoCaixaRepository sessaoCaixaRepository,
            long duracaoSessaoMinutos,
            String cargosAutorizados,
            Clock clock
    ) {
        this.funcionarioRepository = funcionarioRepository;
        this.sessaoCaixaRepository = sessaoCaixaRepository;
        this.duracaoSessaoMinutos = duracaoSessaoMinutos;
        this.cargosAutorizados = Arrays.stream(cargosAutorizados.split(","))
                .map(String::trim)
                .filter(cargo -> !cargo.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toUnmodifiableSet());
        this.clock = clock;
    }

    @Transactional
    public ResultadoLogin login(String matricula, String senha) {
        Funcionario funcionario = funcionarioRepository.findByMatricula(matricula)
                .filter(Funcionario::isAtivo)
                .filter(this::cargoAutorizado)
                .filter(funcionarioEncontrado -> funcionarioEncontrado.senhaConfere(senha))
                .orElseThrow(OperadorNaoAutorizadoException::new);

        Instant iniciadaEm = Instant.now(clock);
        Instant expiraEm = iniciadaEm.plus(duracaoSessaoMinutos, ChronoUnit.MINUTES);
        String token = TokenOpaco.gerar();
        SessaoCaixa sessao = sessaoCaixaRepository.save(new SessaoCaixa(
                funcionario.getId(),
                TokenOpaco.identificar(token),
                iniciadaEm,
                expiraEm
        ));
        return new ResultadoLogin(
                token,
                funcionario.getId(),
                funcionario.getMatricula(),
                funcionario.getNome(),
                funcionario.getCargo(),
                sessao.getExpiraEm()
        );
    }

    @Transactional
    public ResultadoValidacao validar(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return ResultadoValidacao.invalido();
        }

        SessaoCaixa sessao = sessaoCaixaRepository.findByTokenId(TokenOpaco.identificar(tokenId)).orElse(null);
        if (sessao == null || !sessao.estaValida(Instant.now(clock))) {
            if (sessao != null && sessao.isAtiva()) {
                sessao.encerrar();
                sessaoCaixaRepository.save(sessao);
            }
            return ResultadoValidacao.invalido();
        }

        Funcionario funcionario = funcionarioRepository.findById(sessao.getFuncionarioId()).orElse(null);
        if (funcionario == null || !funcionario.isAtivo()) {
            return ResultadoValidacao.invalido();
        }
        return new ResultadoValidacao(
                true,
                funcionario.getId(),
                funcionario.getMatricula(),
                funcionario.getNome(),
                funcionario.getCargo(),
                sessao.getExpiraEm()
        );
    }

    private boolean cargoAutorizado(Funcionario funcionario) {
        return cargosAutorizados.contains(funcionario.getCargo().trim().toUpperCase());
    }

    public record ResultadoLogin(
            String token,
            UUID funcionarioId,
            String matricula,
            String nome,
            String cargo,
            Instant expiraEm
    ) {
    }

    public record ResultadoValidacao(
            boolean valido,
            UUID funcionarioId,
            String matricula,
            String nome,
            String cargo,
            Instant expiraEm
    ) {
        public static ResultadoValidacao invalido() {
            return new ResultadoValidacao(false, null, null, null, null, null);
        }
    }
}
