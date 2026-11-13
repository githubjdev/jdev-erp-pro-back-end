package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

	private Empresa empresa;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
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

	/** Pessoa como vem do banco, com todos os campos que o DTO copia. */
	private Pessoa salva(Long id) {
		Pessoa pessoa = pessoa(id, LocalDate.of(2025, 3, 10));
		pessoa.setRazaoSocial("Maria ME");
		pessoa.setNomeFantasia("Loja da Maria");
		pessoa.setInscricaoEstadual("123");
		pessoa.setCpf("059.486.784-85");
		pessoa.setTelefone("44 99999-0000");
		pessoa.setEmail("maria@teste.com.br");
		pessoa.setAtivo(true);
		pessoa.setObservacao("obs");
		pessoa.setCep("87000-000");
		pessoa.setLogradouro("Rua A");
		pessoa.setBairro("Centro");
		pessoa.setEstado("PR");
		pessoa.setCidade("Maringá");
		pessoa.setPais("Brasil");
		pessoa.setComplemento("Casa");
		pessoa.setEmpresa(empresa);
		return pessoa;
	}

	@Test
	public void testSalvarPreencheDataCadastro() {
		when(pessoaRepository.existePorNome("Maria da Silva", EMPRESA_ID)).thenReturn(false);
		when(pessoaRepository.saveAndFlush(any(Pessoa.class))).thenAnswer(i -> i.getArgument(0));

		/* A data do request é ignorada: o cadastro é sempre de hoje (P20) */
		PessoaDTO dto = pessoaService.salvar(pessoa(null, LocalDate.of(2000, 1, 1)));

		assertEquals(LocalDate.now(), dto.getDataCadastro());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testSalvarNomeDuplicado() {
		when(pessoaRepository.existePorNome("Maria da Silva", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.salvar(pessoa(null, null)));

		assertEquals("Já existe uma pessoa com o mesmo nome para a empresa logada.", erro.getMessage());
		verify(pessoaRepository, never()).saveAndFlush(any());
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
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.atualizar(pessoa(null, null)));

		assertEquals("Id da pessoa não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(pessoaRepository);
	}

	@Test
	public void testAtualizarPessoaDeOutraEmpresa() {
		when(pessoaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.atualizar(pessoa(99L, null)));

		assertEquals("Pessoa não encontrada para a empresa logada.", erro.getMessage());
	}

	@Test
	public void testAtualizarNomeDuplicado() {
		when(pessoaRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(pessoa(5L, LocalDate.now())));
		when(pessoaRepository.existePorNomeDiferenteId(5L, "Maria da Silva", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.atualizar(pessoa(5L, null)));

		assertEquals("Já existe outra pessoa com o mesmo nome para a empresa logada.", erro.getMessage());
		verify(pessoaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testFindAll() {
		when(pessoaRepository.findAll(EMPRESA_ID)).thenReturn(List.of(salva(1L)));

		PessoaDTO dto = pessoaService.findAll(EMPRESA_ID).get(0);

		/* Conversão completa para o DTO */
		assertEquals("Maria ME", dto.getRazaoSocial());
		assertEquals("Loja da Maria", dto.getNomeFantasia());
		assertEquals("123", dto.getInscricaoEstadual());
		assertEquals("059.486.784-85", dto.getCpf());
		assertEquals("maria@teste.com.br", dto.getEmail());
		assertEquals(TipoPessoa.FISICA, dto.getTipoPessoa());
		assertEquals(true, dto.getAtivo());
		assertEquals("Maringá", dto.getCidade());
		assertEquals("Casa", dto.getComplemento());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testBuscaPorNome() {
		when(pessoaRepository.buscaPorNome("maria", EMPRESA_ID)).thenReturn(List.of(salva(1L)));

		assertEquals(1L, pessoaService.buscaPorNome("maria", EMPRESA_ID).get(0).getId());
	}

	@Test
	public void testExistePorNome() {
		when(pessoaRepository.existePorNome("Maria", EMPRESA_ID)).thenReturn(true);

		assertTrue(pessoaService.existePorNome("Maria", EMPRESA_ID));
		assertFalse(pessoaService.existePorNome("João", EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(pessoaRepository.existePorNomeDiferenteId(1L, "Maria", EMPRESA_ID)).thenReturn(true);

		assertTrue(pessoaService.existePorNomeDiferenteId(1L, "Maria", EMPRESA_ID));
		assertFalse(pessoaService.existePorNomeDiferenteId(2L, "Maria", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(pessoaRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		pessoaService.deleteById(1L, EMPRESA_ID);

		verify(pessoaRepository).deleteById(1L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrada() {
		when(pessoaRepository.existsById(1L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.deleteById(1L, EMPRESA_ID));

		assertEquals("Pessoa não encontrada ou já foi deletada.", erro.getMessage());
		verify(pessoaRepository, never()).deleteById(anyLong(), anyLong());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pessoaRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(pessoaRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, pessoaService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pessoaRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhuma pessoa encontrada para deletar ou todas já foram deletadas.", erro.getMessage());
		verify(pessoaRepository, never()).deleteAll(anyLong());
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
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pessoaRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salva(1L), salva(2L)));

		pessoaService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(pessoaRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pessoaRepository.buscarPorIds(List.of(1L, 2L, 3L), EMPRESA_ID)).thenReturn(List.of(salva(2L)));

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> pessoaService.deletarAllById(List.of(1L, 2L, 3L), EMPRESA_ID));

		assertEquals("Pessoas não encontradas ou já deletadas: [1, 3]", erro.getMessage());
		verify(pessoaRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pessoaRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pessoaService.deletarAllById(List.of(1L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(pessoaRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(pessoaRepository.buscarPorIds(List.of(1L), EMPRESA_ID)).thenReturn(List.of(salva(1L)));

		assertEquals(1, pessoaService.buscarPorIds(List.of(1L), EMPRESA_ID).size());
	}

	@Test
	public void testExistsById() {
		when(pessoaRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(pessoaService.existsById(1L, EMPRESA_ID));
		assertFalse(pessoaService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(pessoaRepository.listar(EMPRESA_ID)).thenReturn(List.of(salva(1L), salva(2L)));

		assertEquals(2, pessoaService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		Pessoa pessoa = salva(1L);
		when(pessoaRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(pessoa));

		/* Devolve a entidade: é usado pelo EmpresaService */
		assertSame(pessoa, pessoaService.buscarPorId(1L, EMPRESA_ID).orElseThrow());
	}

	@Test
	public void testBuscarPorIdDto() {
		when(pessoaRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L)));
		when(pessoaRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals("Maria da Silva", pessoaService.buscarPorIdDto(1L, EMPRESA_ID).orElseThrow().getNome());
		assertTrue(pessoaService.buscarPorIdDto(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(pessoaRepository.total(EMPRESA_ID)).thenReturn(8L);

		assertEquals(8L, pessoaService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 10);
		when(pessoaRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(salva(1L)), pagina, 1));

		assertEquals(1L, pessoaService.listarPaginado(EMPRESA_ID, pagina).getContent().get(0).getId());
	}

}
