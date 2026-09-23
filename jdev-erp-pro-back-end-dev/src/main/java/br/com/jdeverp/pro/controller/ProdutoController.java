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

import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.service.ProdutoService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/produto")
public class ProdutoController {

        @Autowired
        private ProdutoService produtoService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @GetMapping("/listar")
        public ResponseEntity<List<Produto>> findAll() {
                return ResponseEntity.ok(produtoService.findAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNome/{nome}")
        public ResponseEntity<List<Produto>> buscaPorNome(@PathVariable String nome) {
                return ResponseEntity.ok(produtoService.buscaPorNome(nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNome/{nome}")
        public ResponseEntity<Boolean> existePorNome(@PathVariable String nome) {
                return ResponseEntity.ok(produtoService.existePorNome(nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomeDiferenteId/{id}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteId(
                        @PathVariable Long id, @PathVariable String nome) {
                return ResponseEntity.ok(produtoService.existePorNomeDiferenteId(id, nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarTodos")
        public ResponseEntity<List<Produto>> listar() {
                return ResponseEntity.ok(produtoService.listar(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
                return produtoService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada())
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/existePorId/{id}")
        public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
                return ResponseEntity.ok(produtoService.existsById(id,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PostMapping("/buscarPorIds")
        public ResponseEntity<List<Produto>> buscarPorIds(@RequestBody Iterable<Long> ids) {
                return ResponseEntity.ok(produtoService.buscarPorIds(ids,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/total")
        public ResponseEntity<Long> total() {
                return ResponseEntity.ok(produtoService.total(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/paginado")
        public ResponseEntity<Page<Produto>> listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "asc") String direction) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                                ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                return ResponseEntity.ok(produtoService.listarPaginado(
                                usuarioLogadoService.getEmpresaIdLogada(), pageable));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                produtoService.deleteById(id, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Produto deletado com sucesso.");
        }

        @DeleteMapping("/deletarTodos")
        public ResponseEntity<Long> deleteAll() {
                return ResponseEntity.ok(produtoService.deleteAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorIds")
        public ResponseEntity<Void> deletarAllById(@RequestBody Iterable<Long> ids) {
                produtoService.deletarAllById(ids,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }
}
