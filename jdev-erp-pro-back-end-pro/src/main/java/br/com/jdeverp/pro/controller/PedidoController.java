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

import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.service.PedidoService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/pedido")
public class PedidoController {

        @Autowired
        private PedidoService pedidoService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @GetMapping("/listar")
        public ResponseEntity<List<Pedido>> findAll() {
                return ResponseEntity.ok(pedidoService.findAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNumeroPedido/{numeroPedido}")
        public ResponseEntity<List<Pedido>> buscaPorNumeroPedido(@PathVariable String numeroPedido) {
                return ResponseEntity.ok(pedidoService.buscaPorNumeroPedido(numeroPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNumeroPedido/{numeroPedido}")
        public ResponseEntity<Boolean> existePorNumeroPedido(@PathVariable String numeroPedido) {
                return ResponseEntity.ok(pedidoService.existePorNumeroPedido(numeroPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNumeroPedidoDiferenteId/{id}/{numeroPedido}")
        public ResponseEntity<Boolean> existePorNumeroPedidoDiferenteId(
                        @PathVariable Long id, @PathVariable String numeroPedido) {
                return ResponseEntity.ok(pedidoService.existePorNumeroPedidoDiferenteId(id, numeroPedido,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarTodos")
        public ResponseEntity<List<Pedido>> listar() {
                return ResponseEntity.ok(pedidoService.listar(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<Pedido> buscarPorId(@PathVariable Long id) {
                return pedidoService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada())
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/existePorId/{id}")
        public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
                return ResponseEntity.ok(pedidoService.existsById(id,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PostMapping("/buscarPorIds")
        public ResponseEntity<List<Pedido>> buscarPorIds(@RequestBody Iterable<Long> ids) {
                return ResponseEntity.ok(pedidoService.buscarPorIds(ids,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/total")
        public ResponseEntity<Long> total() {
                return ResponseEntity.ok(pedidoService.total(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/paginado")
        public ResponseEntity<Page<Pedido>> listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "asc") String direction) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                                ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                return ResponseEntity.ok(pedidoService.listarPaginado(
                                usuarioLogadoService.getEmpresaIdLogada(), pageable));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                pedidoService.deleteById(id, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Pedido deletado com sucesso.");
        }

        @DeleteMapping("/deletarTodos")
        public ResponseEntity<Long> deleteAll() {
                return ResponseEntity.ok(pedidoService.deleteAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorIds")
        public ResponseEntity<Void> deletarAllById(@RequestBody Iterable<Long> ids) {
                pedidoService.deletarAllById(ids,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }
}
