package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
	public void testSalvarValorForaDaFaixa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Plano barato = plano(null, "Barato");
		barato.setValorMensal(10.0);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(barato));

		assertEquals("O valor mensal do plano deve ficar entre R$ 49 e R$ 200.", erro.getMessage());
		verify(planoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarPlanoFreeComValorZero() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		Plano free = plano(1L, "Plano Free");
		free.setTipoPlano(TipoPlano.FREE);
		free.setValorMensal(0.0);
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
		Plano free = plano(1L, "Plano Free");
		free.setTipoPlano(TipoPlano.FREE);
		free.setValorMensal(10.0);
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
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.salvar(plano(null, "PRO")));

		assertEquals("Apenas administradores podem cadastrar planos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(planoRepository);
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
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> planoService.deleteById(1L));

		assertEquals("Apenas administradores podem deletar planos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(planoRepository, never()).deleteById(anyLong());
	}

}
