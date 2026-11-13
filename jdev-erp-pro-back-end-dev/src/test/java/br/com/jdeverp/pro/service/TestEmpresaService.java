package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.EmpresaDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.repository.EmpresaRepository;
import br.com.jdeverp.pro.repository.PlanoRepository;
import jakarta.persistence.EntityManager;

/**
 * Empresa é dado global (P10): só ROLE_ADMIN cadastra, altera, apaga e lista.
 * O usuário comum consulta só a própria empresa.
 */
@ExtendWith(MockitoExtension.class)
public class TestEmpresaService {

	private static final Long EMPRESA_LOGADA = 10L;
	private static final Long OUTRA_EMPRESA = 20L;

	@Mock
	private EmpresaRepository empresaRepository;

	@Mock
	private PlanoRepository planoRepository;

	@Mock
	private PessoaService pessoaService;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private EmpresaService empresaService;

	private static Plano plano(Long id, String nome) {
		Plano plano = new Plano();
		plano.setId(id);
		plano.setNome(nome);
		return plano;
	}

	private static Pessoa pessoa(Long id, String nome) {
		Pessoa pessoa = new Pessoa();
		pessoa.setId(id);
		pessoa.setNome(nome);
		return pessoa;
	}

	private static Empresa empresa(Long id) {
		Empresa empresa = new Empresa();
		empresa.setId(id);
		empresa.setPlano(plano(1L, "PRO"));
		empresa.setPessoa(pessoa(5L, "Empresa " + id));
		return empresa;
	}

	/** Empresa como vem do request: plano e pessoa só com o id. */
	private static Empresa requisicao(Long id) {
		Empresa empresa = new Empresa();
		empresa.setId(id);
		empresa.setPlano(plano(1L, null));
		empresa.setPessoa(pessoa(5L, null));
		empresa.setTotalUsuario(3);
		empresa.setTotalCliente(7);
		empresa.setPlanoAtivo(true);
		empresa.setBloqueio(false);
		empresa.setLogoMarca("logo");
		empresa.setVigenciaPlano(LocalDate.of(2030, 1, 1));
		return empresa;
	}

	@Test
	public void testSalvar() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(false);
		when(planoRepository.findById(1L)).thenReturn(Optional.of(plano(1L, "PRO")));
		when(entityManager.getReference(Pessoa.class, 5L)).thenReturn(pessoa(5L, "JDev Treinamento"));
		when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> {
			Empresa salva = i.getArgument(0);
			salva.setId(30L);
			return salva;
		});

		EmpresaDTO dto = empresaService.salvar(requisicao(null));

		assertEquals(30L, dto.getId());
		/* Plano e pessoa trocados pelos registros do banco: o DTO sai com os nomes */
		assertEquals("PRO", dto.getPlanoNome());
		assertEquals("JDev Treinamento", dto.getPessoaNome());
		assertEquals(3, dto.getTotalUsuario());
		assertEquals(LocalDate.of(2030, 1, 1), dto.getVigenciaPlano());
	}

	@Test
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(empresa(null)));

		assertEquals("Apenas administradores podem cadastrar empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		/* A permissão vem antes de tudo: o usuário comum nem descobre se a pessoa já tem empresa */
		verifyNoInteractions(empresaRepository, pessoaService);
	}

	@Test
	public void testSalvarSemPessoa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Empresa empresa = requisicao(null);
		empresa.setPessoa(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(empresa));

		assertEquals("Pessoa deve ser informada para cadastrar a empresa.", erro.getMessage());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testSalvarPessoaJaTemEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(true);
		when(pessoaService.buscarPorId(5L, EMPRESA_LOGADA)).thenReturn(Optional.of(pessoa(5L, "Maria")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(requisicao(null)));

		assertEquals("Já existe uma empresa cadastrada para a pessoa: Maria", erro.getMessage());
		verify(empresaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarPessoaDeOutraEmpresaJaTemEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(true);
		when(pessoaService.buscarPorId(5L, EMPRESA_LOGADA)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(requisicao(null)));

		/* A pessoa é de outra empresa: o nome não aparece */
		assertEquals("Já existe uma empresa cadastrada para a pessoa: informada", erro.getMessage());
	}

	@Test
	public void testSalvarSemPlano() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(false);
		Empresa empresa = requisicao(null);
		empresa.setPlano(new Plano());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(empresa));

		assertEquals("Plano deve ser informado para a empresa.", erro.getMessage());
		verify(empresaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarPlanoInexistente() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(false);
		when(planoRepository.findById(1L)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(requisicao(null)));

		assertEquals("Plano com id: 1 não foi encontrado.", erro.getMessage());
		verify(empresaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		Empresa existente = empresa(OUTRA_EMPRESA);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(existente);
		when(planoRepository.findById(1L)).thenReturn(Optional.of(plano(1L, "PRO")));
		when(entityManager.getReference(Pessoa.class, 5L)).thenReturn(pessoa(5L, "Nova Pessoa"));
		when(empresaRepository.saveAndFlush(existente)).thenReturn(existente);
		Empresa alteracao = requisicao(OUTRA_EMPRESA);
		/* Bloquear outra empresa (um cliente do SaaS) é permitido, mesmo que ela tenha admins */
		alteracao.setBloqueio(true);

		EmpresaDTO dto = empresaService.atualizar(alteracao);

		assertEquals(OUTRA_EMPRESA, dto.getId());
		assertEquals("Nova Pessoa", dto.getPessoaNome());
		assertEquals(true, dto.getBloqueio());
		assertEquals(7, dto.getTotalCliente());
		assertEquals("logo", dto.getLogoMarca());
		verify(empresaRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarBloqueandoAPropriaEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);
		when(empresaRepository.buscarPorId(EMPRESA_LOGADA)).thenReturn(empresa(EMPRESA_LOGADA));
		Empresa bloqueio = requisicao(EMPRESA_LOGADA);
		bloqueio.setBloqueio(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.atualizar(bloqueio));

		/* P25: o admin logado não bloqueia a própria empresa */
		assertEquals(EmpresaService.MSG_BLOQUEAR_PROPRIA_EMPRESA, erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(empresaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarAPropriaEmpresaComBloqueioNulo() {
		Empresa existente = empresa(EMPRESA_LOGADA);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(EMPRESA_LOGADA)).thenReturn(existente);
		when(planoRepository.findById(1L)).thenReturn(Optional.of(plano(1L, "PRO")));
		when(entityManager.getReference(Pessoa.class, 5L)).thenReturn(pessoa(5L, "Nova Pessoa"));
		when(empresaRepository.saveAndFlush(existente)).thenReturn(existente);
		Empresa alteracao = requisicao(EMPRESA_LOGADA);
		alteracao.setBloqueio(null);

		/* Nulo vira false: editar a própria empresa sem bloqueá-la é permitido */
		EmpresaDTO dto = empresaService.atualizar(alteracao);

		assertEquals(false, existente.getBloqueio());
		assertEquals(false, dto.getBloqueio());
		assertEquals("logo", dto.getLogoMarca());
	}

	@Test
	public void testSalvarComBloqueioNulo() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(false);
		when(planoRepository.findById(1L)).thenReturn(Optional.of(plano(1L, "PRO")));
		when(entityManager.getReference(Pessoa.class, 5L)).thenReturn(pessoa(5L, "JDev Treinamento"));
		when(empresaRepository.saveAndFlush(any(Empresa.class))).thenAnswer(i -> i.getArgument(0));
		Empresa nova = requisicao(null);
		nova.setBloqueio(null);

		/* Nulo vira false (decidido em 2026-11-12) */
		assertEquals(false, empresaService.salvar(nova).getBloqueio());
		assertEquals(false, nova.getBloqueio());
	}

	@Test
	public void testAtualizarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.atualizar(requisicao(OUTRA_EMPRESA)));

		assertEquals("Apenas administradores podem atualizar empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testAtualizarSemId() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.atualizar(requisicao(null)));

		assertEquals("Id da empresa deve ser informado para edição.", erro.getMessage());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testAtualizarNaoEncontrada() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(77L)).thenReturn(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.atualizar(requisicao(77L)));

		assertEquals("Empresa com id: 77 não foi encontrada.", erro.getMessage());
		verify(empresaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemPessoa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(empresa(OUTRA_EMPRESA));
		Empresa alteracao = requisicao(OUTRA_EMPRESA);
		alteracao.setPessoa(new Pessoa());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.atualizar(alteracao));

		assertEquals("Pessoa deve ser informada para atualizar a empresa.", erro.getMessage());
		verify(empresaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testBuscaPorIdDaPropriaEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);
		when(empresaRepository.buscarPorId(EMPRESA_LOGADA)).thenReturn(empresa(EMPRESA_LOGADA));

		EmpresaDTO dto = empresaService.buscaPorId(EMPRESA_LOGADA);

		assertEquals(EMPRESA_LOGADA, dto.getId());
		assertEquals("PRO", dto.getPlanoNome());
	}

	@Test
	public void testBuscaPorIdDeOutraEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.buscaPorId(OUTRA_EMPRESA));

		assertEquals("Apenas administradores podem consultar outras empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(empresaRepository, never()).buscarPorId(anyLong());
	}

	@Test
	public void testBuscaPorIdAdministradorVeOutraEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(empresa(OUTRA_EMPRESA));

		assertEquals(OUTRA_EMPRESA, empresaService.buscaPorId(OUTRA_EMPRESA).getId());
	}

	@Test
	public void testBuscaPorIdNaoEncontrada() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.buscaPorId(OUTRA_EMPRESA));

		assertEquals("Empresa com id: 20 não foi encontrada.", erro.getMessage());
	}

	@Test
	public void testFindAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.findAll()).thenReturn(List.of(empresa(EMPRESA_LOGADA), empresa(OUTRA_EMPRESA)));

		List<EmpresaDTO> lista = empresaService.findAll();

		assertEquals(List.of(EMPRESA_LOGADA, OUTRA_EMPRESA), lista.stream().map(EmpresaDTO::getId).toList());
	}

	@Test
	public void testFindAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.findAll());

		assertEquals("Apenas administradores podem consultar empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testBuscaPorNome() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscaPorNome("emp")).thenReturn(List.of(empresa(EMPRESA_LOGADA)));

		List<EmpresaDTO> lista = empresaService.buscaPorNome("emp");

		assertEquals("Empresa 10", lista.get(0).getPessoaNome());
	}

	@Test
	public void testBuscaPorNomeSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.buscaPorNome("emp"));

		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testExistePorNome() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorNome("JDev")).thenReturn(true);

		assertTrue(empresaService.existePorNome("JDev"));
		assertFalse(empresaService.existePorNome("Outra"));
	}

	@Test
	public void testExistePorNomeSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		assertThrows(MsgApiException.class, () -> empresaService.existePorNome("JDev"));
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorNomeDiferenteId(1L, "JDev")).thenReturn(true);

		assertTrue(empresaService.existePorNomeDiferenteId(1L, "JDev"));
		assertFalse(empresaService.existePorNomeDiferenteId(2L, "JDev"));
	}

	@Test
	public void testExistePorNomeDiferenteIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		assertThrows(MsgApiException.class, () -> empresaService.existePorNomeDiferenteId(1L, "JDev"));
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testDeleteById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(empresa(OUTRA_EMPRESA));

		empresaService.deleteById(OUTRA_EMPRESA);

		verify(empresaRepository).deleteById(OUTRA_EMPRESA);
	}

	@Test
	public void testDeleteByIdDaPropriaEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_LOGADA);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.deleteById(EMPRESA_LOGADA));

		/* P25: o admin logado não apaga a própria empresa */
		assertEquals(EmpresaService.MSG_APAGAR_PROPRIA_EMPRESA, erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testDeleteByIdNaoEncontrada() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.deleteById(OUTRA_EMPRESA));

		assertEquals("Empresa não encontrada ou já foi deletada.", erro.getMessage());
		verify(empresaRepository, never()).deleteById(anyLong());
	}

	@Test
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.deleteById(OUTRA_EMPRESA));

		assertEquals("Apenas administradores podem deletar empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testSalvarPessoaSemId() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Empresa empresa = requisicao(null);
		empresa.setPessoa(new Pessoa());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(empresa));

		assertEquals("Pessoa deve ser informada para cadastrar a empresa.", erro.getMessage());
	}

	@Test
	public void testSalvarPlanoNulo() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.existePorPessoa(5L)).thenReturn(false);
		Empresa empresa = requisicao(null);
		empresa.setPlano(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.salvar(empresa));

		assertEquals("Plano deve ser informado para a empresa.", erro.getMessage());
		verifyNoInteractions(planoRepository);
	}

	@Test
	public void testAtualizarPessoaNula() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(empresaRepository.buscarPorId(OUTRA_EMPRESA)).thenReturn(empresa(OUTRA_EMPRESA));
		Empresa alteracao = requisicao(OUTRA_EMPRESA);
		alteracao.setPessoa(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.atualizar(alteracao));

		assertEquals("Pessoa deve ser informada para atualizar a empresa.", erro.getMessage());
	}

}
