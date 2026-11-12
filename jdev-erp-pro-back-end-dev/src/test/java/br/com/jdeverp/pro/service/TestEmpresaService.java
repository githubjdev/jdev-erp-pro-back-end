package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

	@InjectMocks
	private EmpresaService empresaService;

	private static Empresa empresa(Long id) {
		Plano plano = new Plano();
		plano.setId(1L);
		plano.setNome("PRO");
		Pessoa pessoa = new Pessoa();
		pessoa.setId(5L);
		pessoa.setNome("Empresa " + id);
		Empresa empresa = new Empresa();
		empresa.setId(id);
		empresa.setPlano(plano);
		empresa.setPessoa(pessoa);
		return empresa;
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
	public void testFindAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.findAll());

		assertEquals("Apenas administradores podem consultar empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

	@Test
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> empresaService.deleteById(OUTRA_EMPRESA));

		assertEquals("Apenas administradores podem deletar empresas.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(empresaRepository);
	}

}
