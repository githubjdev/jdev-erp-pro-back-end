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

import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.service.ItemPedidoService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/itemPedido")
public class ItemPedidoController {

        @Autowired
        private ItemPedidoService itemPedidoService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @GetMapping("/listar/{idPedido}")
        public ResponseEntity<List<ItemPedido>> listar(@PathVariable Long idPedido) {
                return ResponseEntity.ok(itemPedidoService.findAll(idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNome/{idPedido}/{nome}")
        public ResponseEntity<List<ItemPedido>> buscaPorNome(
                        @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(itemPedidoService.buscaPorNome(nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNome/{idPedido}/{nome}")
        public ResponseEntity<Boolean> existePorNome(
                        @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(itemPedidoService.existePorNome(nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomeDiferenteId/{id}/{idPedido}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteId(
                        @PathVariable Long id, @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(itemPedidoService.existePorNomeDiferenteId(id, nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarTodos")
        public ResponseEntity<List<ItemPedido>> listarTodos() {
                return ResponseEntity.ok(itemPedidoService.listar(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<ItemPedido> buscarPorId(@PathVariable Long id) {
                return itemPedidoService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada())
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/existePorId/{id}")
        public ResponseEntity<Boolean> existePorId(@PathVariable Long id) {
                return ResponseEntity.ok(itemPedidoService.existsById(id,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PostMapping("/buscarPorIds")
        public ResponseEntity<List<ItemPedido>> buscarPorIds(@RequestBody Iterable<Long> ids) {
                return ResponseEntity.ok(itemPedidoService.buscarPorIds(ids,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/total")
        public ResponseEntity<Long> total() {
                return ResponseEntity.ok(itemPedidoService.total(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/paginado")
        public ResponseEntity<Page<ItemPedido>> listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "asc") String direction) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                                ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                return ResponseEntity.ok(itemPedidoService.listarPaginado(
                                usuarioLogadoService.getEmpresaIdLogada(), pageable));
        }

        @DeleteMapping("/deletar/{id}/{idPedido}")
        public ResponseEntity<String> deleteById(
                        @PathVariable Long id, @PathVariable Long idPedido) {
                itemPedidoService.deleteById(id, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Item do pedido deletado com sucesso.");
        }

        @DeleteMapping("/deletarTodos")
        public ResponseEntity<Long> deleteAll() {
                return ResponseEntity.ok(itemPedidoService.deleteAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorIds")
        public ResponseEntity<Void> deletarAllById(@RequestBody Iterable<Long> ids) {
                itemPedidoService.deletarAllById(ids,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/listarPorPedido/{idPedido}")
        public ResponseEntity<List<ItemPedido>> findAllByPedido(@PathVariable Long idPedido) {
                return ResponseEntity.ok(itemPedidoService.findAllByPedido(idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNomePorPedido/{idPedido}/{nome}")
        public ResponseEntity<List<ItemPedido>> buscaPorNomePorPedido(
                        @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(itemPedidoService.buscaPorNomePorPedido(nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomePorPedido/{idPedido}/{nome}")
        public ResponseEntity<Boolean> existePorNomePorPedido(
                        @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(itemPedidoService.existePorNomePorPedido(nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomeDiferenteIdPorPedido/{id}/{idPedido}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteIdPorPedido(
                        @PathVariable Long id, @PathVariable Long idPedido, @PathVariable String nome) {
                return ResponseEntity.ok(itemPedidoService.existePorNomeDiferenteIdPorPedido(id, nome, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarPorPedido/{id}/{idPedido}")
        public ResponseEntity<String> deleteByIdAndPedido(
                        @PathVariable Long id, @PathVariable Long idPedido) {
                itemPedidoService.deleteByIdAndPedido(id, idPedido,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Item do pedido deletado com sucesso.");
        }
}