package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.PessoaDTO;
import br.com.jdeverp.pro.enums.TipoPessoa;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
public class TestPessoaService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private PessoaRepository pessoaRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private PessoaService pessoaService;

	@BeforeEach
	public void configurar() {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
	}

	private static Pessoa pessoa(Long id, LocalDate dataCadastro) {
		Pessoa pessoa = new Pessoa();
		pessoa.setId(id);
		pessoa.setNome("Maria da Silva");
		pessoa.setTipoPessoa(TipoPessoa.FISICA);
		pessoa.setDataCadastro(dataCadastro);
		return pessoa;
	}

	@Test
	public void testSalvarPreencheDataCadastro() {
		when(pessoaRepository.existePorNome("Maria da Silva", EMPRESA_ID)).thenReturn(false);
		when(pessoaRepository.saveAndFlush(any(Pessoa.class))).thenAnswer(i -> i.getArgument(0));

		/* A data do request é ignorada: o cadastro é sempre de hoje (P20) */
		PessoaDTO dto = pessoaService.salvar(pessoa(null, LocalDate.of(2000, 1, 1)));

		assertEquals(LocalDate.now(), dto.getDataCadastro());
	}

	@Test
	public void testAtualizarMantemDataCadastro() {
		LocalDate cadastro = LocalDate.of(2025, 3, 10);
		when(pessoaRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(pessoa(5L, cadastro)));
		when(pessoaRepository.existePorNomeDiferenteId(5L, "Maria da Silva", EMPRESA_ID)).thenReturn(false);
		when(pessoaRepository.saveAndFlush(any(Pessoa.class))).thenAnswer(i -> i.getArgument(0));

		/* O request vem sem a data (ou com outra): a edição mantém a do cadastro */
		PessoaDTO dto = pessoaService.atualizar(pessoa(5L, null));

		assertEquals(cadastro, dto.getDataCadastro());
	}

	@Test
	public void testAtualizarPessoaDeOutraEmpresa() {
		when(pessoaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.atualizar(pessoa(99L, null)));

		assertEquals("Pessoa não encontrada para a empresa logada.", erro.getMessage());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(pessoaRepository);
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.deletarAllById(List.of(1L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(pessoaRepository);
	}

}
