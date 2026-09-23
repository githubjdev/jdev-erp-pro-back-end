package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.MovimentacaoProduto;
import br.com.jdeverp.pro.service.MovimentacaoProdutoService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/movimentacaoProduto")
public class MovimentacaoProdutoController {

        @Autowired
        private MovimentacaoProdutoService movimentacaoProdutoService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @GetMapping("/listar")
        public ResponseEntity<List<MovimentacaoProduto>> findAll() {
                return ResponseEntity.ok(movimentacaoProdutoService.findAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNome/{nome}")
        public ResponseEntity<List<MovimentacaoProduto>> buscaPorNome(@PathVariable String nome) {
                return ResponseEntity.ok(movimentacaoProdutoService.buscaPorNome(nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNome/{nome}")
        public ResponseEntity<Boolean> existePorNome(@PathVariable String nome) {
                return ResponseEntity.ok(movimentacaoProdutoService.existePorNome(nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomeDiferenteId/{id}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteId(
                        @PathVariable Long id, @PathVariable String nome) {
                return ResponseEntity.ok(movimentacaoProdutoService.existePorNomeDiferenteId(id, nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarTodos")
        public ResponseEntity<List<MovimentacaoProduto>> listar() {
                return ResponseEntity.ok(movimentacaoProdutoService.listar(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<MovimentacaoProduto> buscarPorId(@PathVariable Long id) {
                return movimentacaoProdutoService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada())
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/existePorId/{id}")
        public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
                return ResponseEntity.ok(movimentacaoProdutoService.existsById(id,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PostMapping("/buscarPorIds")
        public ResponseEntity<List<MovimentacaoProduto>> buscarPorIds(@RequestBody Iterable<Long> ids) {
                return ResponseEntity.ok(movimentacaoProdutoService.buscarPorIds(ids,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/total")
        public ResponseEntity<Long> total() {
                return ResponseEntity.ok(movimentacaoProdutoService.total(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/paginado")
        public ResponseEntity<Page<MovimentacaoProduto>> listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "asc") String direction) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                                ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                return ResponseEntity.ok(movimentacaoProdutoService.listarPaginado(
                                usuarioLogadoService.getEmpresaIdLogada(), pageable));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                movimentacaoProdutoService.deleteById(id, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Movimentação de produto deletada com sucesso.");
        }

        @DeleteMapping("/deletarTodos")
        public ResponseEntity<Long> deleteAll() {
                return ResponseEntity.ok(movimentacaoProdutoService.deleteAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorIds")
        public ResponseEntity<Void> deletarAllById(@RequestBody Iterable<Long> ids) {
                movimentacaoProdutoService.deletarAllById(ids,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/listarPorPedido/{idPedido}")
        public ResponseEntity<List<MovimentacaoProduto>> findAllByPedido(@PathVariable Long idPedido) {
                return ResponseEntity.ok(movimentacaoProdutoService.findAllByPedido(idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNomePorPedido/{idPedido}/{nome}")
        public ResponseEntity<List<MovimentacaoProduto>> buscaPorNomeByPedido(
                        @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(movimentacaoProdutoService.buscaPorNomeByPedido(nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomePorPedido/{idPedido}/{nome}")
        public ResponseEntity<Boolean> existePorNomeByPedido(
                        @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(movimentacaoProdutoService.existePorNomeByPedido(nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomeDiferenteIdPorPedido/{id}/{idPedido}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteIdByPedido(
                        @PathVariable Long id, @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(movimentacaoProdutoService.existePorNomeDiferenteIdByPedido(id, nome,
                                idPedido, usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarPorPedido/{id}/{idPedido}")
        public ResponseEntity<String> deleteByIdAndPedido(
                        @PathVariable Long id, @PathVariable Long idPedido) {
                movimentacaoProdutoService.deleteByIdAndPedido(id, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Movimentação de produto deletada com sucesso.");
        }
}
