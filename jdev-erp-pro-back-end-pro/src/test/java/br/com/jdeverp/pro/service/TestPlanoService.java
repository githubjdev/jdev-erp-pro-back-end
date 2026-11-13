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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.PlanoDTO;
import br.com.jdeverp.pro.enums.TipoPlano;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.repository.PlanoRepository;

/** Plano é dado global: só ROLE_ADMIN cadastra, altera ou apaga (P10). */
@ExtendWith(MockitoExtension.class)
public class TestPlanoService {

	@Mock
	private PlanoRepository planoRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private PlanoService planoService;

	private static Plano plano(Long id, String nome) {
		Plano plano = new Plano();
		plano.setId(id);
		plano.setNome(nome);
		plano.setDescricao("Plano de teste");
		plano.setValorMensal(99.0);
		plano.setLimiteUsuario(5);
		plano.setLimiteCliente(10);
		plano.setTipoPlano(TipoPlano.PRO);
		return plano;
	}

	private static Plano free(Long id, double valor) {
		Plano free = plano(id, "Plano Free");
		free.setTipoPlano(TipoPlano.FREE);
		free.setValorMensal(valor);
		return free;
	}

	private void salvarDevolveOProprio() {
		when(planoRepository.saveAndFlush(any(Plano.class))).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	public void testSalvar() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.existePorNome("PRO")).thenReturn(false);
		when(planoRepository.saveAndFlush(any(Plano.class))).thenAnswer(i -> {
			Plano salvo = i.getArgument(0);
			salvo.setId(3L);
			return salvo;
		});

		PlanoDTO dto = planoService.salvar(plano(null, "PRO"));

		assertEquals(3L, dto.getId());
		assertEquals("PRO", dto.getNome());
		assertEquals(99.0, dto.getValorMensal());
		assertEquals(5, dto.getLimiteUsuario());
		assertEquals(10, dto.getLimiteCliente());
		assertEquals(TipoPlano.PRO, dto.getTipoPlano());
	}

	@Test
	public void testSalvarValorNosLimites() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		salvarDevolveOProprio();

		for (double valor : new double[] { 49.0, 200.0 }) {
			Plano plano = plano(null, "Limite " + valor);
			plano.setValorMensal(valor);
			assertEquals(valor, planoService.salvar(plano).getValorMensal());
		}
	}

	@Test
	public void testSalvarPlanoFree() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.existePorTipo(TipoPlano.FREE)).thenReturn(true);
		Plano free = plano(null, "Outro Free");
		free.setTipoPlano(TipoPlano.FREE);
		free.setValorMensal(0.0);

		/* O FREE é inserido direto no banco: não se cadastra outro (P11) */
		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(free));

		assertEquals("Já existe o plano FREE: ele é cadastrado direto no banco e não pode haver outro.", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarPrimeiroPlanoFree() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.existePorTipo(TipoPlano.FREE)).thenReturn(false);
		salvarDevolveOProprio();

		assertEquals(0.0, planoService.salvar(free(null, 0.0)).getValorMensal());
	}

	@Test
	public void testSalvarPlanoFreeComValor() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.existePorTipo(TipoPlano.FREE)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(free(null, 10.0)));

		assertEquals("O plano FREE deve ter valor mensal 0.", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarValorForaDaFaixa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);

		for (double valor : new double[] { 10.0, 48.99, 200.01 }) {
			Plano plano = plano(null, "Fora " + valor);
			plano.setValorMensal(valor);

			MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(plano));

			assertEquals("O valor mensal do plano deve ficar entre R$ 49 e R$ 200.", erro.getMessage());
		}
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarValorNulo() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Plano plano = plano(null, "Sem valor");
		plano.setValorMensal(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(plano));

		assertEquals("Valor mensal não pode ser nulo", erro.getMessage());
	}

	@Test
	public void testSalvarNomeDuplicado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.existePorNome("PRO")).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(plano(null, "PRO")));

		assertEquals("Já existe um plano com o mesmo nome: PRO", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(plano(null, "PRO")));

		assertEquals("Apenas administradores podem cadastrar planos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(planoRepository);
	}

	@Test
	public void testAtualizar() {
		Plano plano = plano(2L, "PRO");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.findById(2L)).thenReturn(Optional.of(plano(2L, "Antigo")));
		when(planoRepository.existePorNomeDiferenteId(2L, "PRO")).thenReturn(false);
		when(planoRepository.saveAndFlush(plano)).thenReturn(plano);

		PlanoDTO dto = planoService.atualizar(plano);

		assertEquals(2L, dto.getId());
		assertEquals("PRO", dto.getNome());
	}

	@Test
	public void testAtualizarPlanoFreeComValorZero() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Plano free = free(1L, 0.0);
		when(planoRepository.findById(1L)).thenReturn(Optional.of(free));
		when(planoRepository.existePorTipoDiferenteId(1L, TipoPlano.FREE)).thenReturn(false);
		when(planoRepository.existePorNomeDiferenteId(1L, "Plano Free")).thenReturn(false);
		when(planoRepository.saveAndFlush(free)).thenReturn(free);

		/* O próprio plano FREE pode ser editado, com valor 0, como o CHECK do banco */
		assertEquals(0.0, planoService.atualizar(free).getValorMensal());
	}

	@Test
	public void testAtualizarPlanoFreeComValor() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Plano free = free(1L, 10.0);
		when(planoRepository.findById(1L)).thenReturn(Optional.of(free));
		when(planoRepository.existePorTipoDiferenteId(1L, TipoPlano.FREE)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(free));

		assertEquals("O plano FREE deve ter valor mensal 0.", erro.getMessage());
	}

	@Test
	public void testAtualizarOutroPlanoParaFree() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Plano virarFree = plano(2L, "Plano Pro");
		virarFree.setTipoPlano(TipoPlano.FREE);
		virarFree.setValorMensal(0.0);
		when(planoRepository.findById(2L)).thenReturn(Optional.of(plano(2L, "Plano Pro")));
		when(planoRepository.existePorTipoDiferenteId(2L, TipoPlano.FREE)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(virarFree));

		assertEquals("Já existe o plano FREE: ele é cadastrado direto no banco e não pode haver outro.", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemId() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(plano(null, "PRO")));

		assertEquals("Id do plano deve ser informado para edição.", erro.getMessage());
		verifyNoInteractions(planoRepository);
	}

	@Test
	public void testAtualizarNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.findById(9L)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(plano(9L, "PRO")));

		assertEquals("Plano com id: 9 não foi encontrado.", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarValorForaDaFaixa() {
		Plano plano = plano(2L, "PRO");
		plano.setValorMensal(300.0);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.findById(2L)).thenReturn(Optional.of(plano(2L, "PRO")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(plano));

		assertEquals("O valor mensal do plano deve ficar entre R$ 49 e R$ 200.", erro.getMessage());
	}

	@Test
	public void testAtualizarNomeDuplicado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.findById(2L)).thenReturn(Optional.of(plano(2L, "Antigo")));
		when(planoRepository.existePorNomeDiferenteId(2L, "PRO")).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(plano(2L, "PRO")));

		assertEquals("Já existe outro plano com o mesmo nome: PRO", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.atualizar(plano(1L, "PRO")));

		assertEquals("Apenas administradores podem atualizar planos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testBuscarPorId() {
		when(planoRepository.findById(2L)).thenReturn(Optional.of(plano(2L, "PRO")));
		when(planoRepository.findById(3L)).thenReturn(Optional.empty());

		assertEquals("PRO", planoService.buscarPorId(2L).orElseThrow().getNome());
		assertTrue(planoService.buscarPorId(3L).isEmpty());
	}

	@Test
	public void testFindAll() {
		when(planoRepository.findAll()).thenReturn(List.of(free(1L, 0.0), plano(2L, "PRO")));

		assertEquals(List.of("Plano Free", "PRO"), planoService.findAll().stream().map(PlanoDTO::getNome).toList());
	}

	@Test
	public void testBuscaPorNome() {
		when(planoRepository.buscaPorNome("pr")).thenReturn(List.of(plano(2L, "PRO")));

		assertEquals(2L, planoService.buscaPorNome("pr").get(0).getId());
	}

	@Test
	public void testExistePorNome() {
		when(planoRepository.existePorNome("PRO")).thenReturn(true);

		assertTrue(planoService.existePorNome("PRO"));
		assertFalse(planoService.existePorNome("Outro"));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(planoRepository.existePorNomeDiferenteId(1L, "PRO")).thenReturn(true);

		assertTrue(planoService.existePorNomeDiferenteId(1L, "PRO"));
		assertFalse(planoService.existePorNomeDiferenteId(2L, "PRO"));
	}

	@Test
	public void testDeleteById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.findById(2L)).thenReturn(Optional.of(plano(2L, "PRO")));

		planoService.deleteById(2L);

		verify(planoRepository).deleteById(2L);
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(planoRepository.findById(2L)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.deleteById(2L));

		assertEquals("Plano não encontrado ou já foi deletado.", erro.getMessage());
		verify(planoRepository, never()).deleteById(anyLong());
	}

	@Test
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.deleteById(1L));

		assertEquals("Apenas administradores podem deletar planos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(planoRepository, never()).deleteById(anyLong());
	}

}
