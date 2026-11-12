package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.jdeverp.pro.dto.ChamadoDTO;
import br.com.jdeverp.pro.enums.PrioridadeChamado;
import br.com.jdeverp.pro.enums.StatusChamado;
import br.com.jdeverp.pro.enums.TipoChamado;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class TestChamadoService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private ChamadoRepository chamadoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private ChamadoService chamadoService;

	private static Usuario usuario(Long id, String nome) {
		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setPessoa(pessoa);
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setClienteFuncionario(clienteFuncionario);
		return usuario;
	}

	private static Usuario referencia(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		return usuario;
	}

	@Test
	public void testSalvarBuscaUsuarioLogadoNaSessao() {
		Empresa empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		/* O usuário do JwtFilter vem sem o cadastro carregado (fora da sessão da requisição) */
		Usuario logadoDoFiltro = referencia(1L);
		Usuario logadoDoBanco = usuario(1L, "Ana Atendente");
		Usuario cliente = usuario(2L, "Carlos Cliente");

		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
		when(usuarioLogadoService.getUsuarioLogado()).thenReturn(logadoDoFiltro);
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(logadoDoBanco));
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(cliente));
		when(chamadoRepository.saveAndFlush(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));

		Chamado chamado = new Chamado();
		chamado.setTitulo("Impressora");
		chamado.setDescricao("Não imprime");
		chamado.setTipoChamado(TipoChamado.SUPORTE);
		chamado.setPrioridadeChamado(PrioridadeChamado.ALTA);
		chamado.setStatusChamado(StatusChamado.ABERTO);
		chamado.setAtendente(referencia(1L));
		chamado.setCliente(referencia(2L));

		ChamadoDTO dto = chamadoService.salvar(chamado);

		ArgumentCaptor<Chamado> salvo = ArgumentCaptor.forClass(Chamado.class);
		Mockito.verify(chamadoRepository).saveAndFlush(salvo.capture());
		assertSame(logadoDoBanco, salvo.getValue().getAbertoUser());
		assertEquals("Ana Atendente", dto.getAbertoUserNome());
		assertEquals("Carlos Cliente", dto.getClienteNome());
		assertEquals(LocalDate.now(), dto.getDataAbertura());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

}
