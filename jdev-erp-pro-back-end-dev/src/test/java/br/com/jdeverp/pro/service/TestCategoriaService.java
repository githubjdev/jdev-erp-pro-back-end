package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.CategoriaDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.repository.CategoriaRepository;

@ExtendWith(MockitoExtension.class)
public class TestCategoriaService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private CategoriaRepository categoriaRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private CategoriaService categoriaService;

	private Empresa empresa;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
	}

	private Categoria categoria(Long id, String nome) {
		Categoria categoria = new Categoria();
		categoria.setId(id);
		categoria.setNome(nome);
		return categoria;
	}

	@Test
	public void testSalvar() {
		when(categoriaRepository.existePorNome("Games", EMPRESA_ID)).thenReturn(false);
		when(categoriaRepository.saveAndFlush(any(Categoria.class))).thenAnswer(i -> {
			Categoria salva = i.getArgument(0);
			salva.setId(1L);
			return salva;
		});

		CategoriaDTO dto = categoriaService.salvar(categoria(null, "Games"));

		assertEquals(1L, dto.getId());
		assertEquals("Games", dto.getNome());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testSalvarNomeDuplicado() {
		when(categoriaRepository.existePorNome("Games", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> categoriaService.salvar(categoria(null, "Games")));

		assertEquals("Já existe uma categoria com o mesmo nome para a empresa logada.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
		verify(categoriaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		Categoria existente = categoria(5L, "Nome antigo");
		existente.setEmpresa(empresa);
		when(categoriaRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(categoriaRepository.existePorNomeDiferenteId(5L, "Nome novo", EMPRESA_ID)).thenReturn(false);
		when(categoriaRepository.saveAndFlush(existente)).thenReturn(existente);

		CategoriaDTO dto = categoriaService.atualizar(categoria(5L, "Nome novo"));

		assertEquals(5L, dto.getId());
		assertEquals("Nome novo", dto.getNome());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
		/* Grava o registro da empresa logada, não o objeto do request */
		verify(categoriaRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> categoriaService.atualizar(categoria(null, "Nome")));

		assertEquals("Id da categoria não informado para atualizar.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
	}

	@Test
	public void testAtualizarCategoriaDeOutraEmpresa() {
		when(categoriaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> categoriaService.atualizar(categoria(99L, "Invasão")));

		assertEquals("Categoria não encontrada para a empresa logada.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
		verify(categoriaRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarNomeDuplicado() {
		when(categoriaRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(categoria(5L, "Antigo")));
		when(categoriaRepository.existePorNomeDiferenteId(5L, "Games", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> categoriaService.atualizar(categoria(5L, "Games")));

		assertEquals("Já existe uma categoria com o mesmo nome desta que sendo informada para atualizar, informe outro nome.",
				erro.getMessage());
		verify(categoriaRepository, never()).saveAndFlush(any());
	}

}
