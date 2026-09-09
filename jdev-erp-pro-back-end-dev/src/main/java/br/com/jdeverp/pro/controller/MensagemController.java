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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.Mensagem;
import br.com.jdeverp.pro.service.MensagemService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/mensagem")
public class MensagemController {

        @Autowired
        private MensagemService mensagemService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @GetMapping("/listar")
        public ResponseEntity<List<Mensagem>> listar() {
                return ResponseEntity.ok(mensagemService.findAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorConteudo/{conteudo}")
        public ResponseEntity<List<Mensagem>> buscaPorConteudo(@PathVariable String conteudo) {
                return ResponseEntity.ok(mensagemService.buscaPorConteudo(conteudo,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorConteudo/{conteudo}")
        public ResponseEntity<Boolean> existePorConteudo(@PathVariable String conteudo) {
                return ResponseEntity.ok(mensagemService.existePorConteudo(conteudo,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorConteudoDiferenteId/{id}/{conteudo}")
        public ResponseEntity<Boolean> existePorConteudoDiferenteId(
                        @PathVariable Long id, @PathVariable String conteudo) {
                return ResponseEntity.ok(mensagemService.existePorConteudoDiferenteId(id, conteudo,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarTodos")
        public ResponseEntity<List<Mensagem>> listarTodos() {
                return ResponseEntity.ok(mensagemService.listar(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<Mensagem> buscarPorId(@PathVariable Long id) {
                return mensagemService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada())
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/existePorId/{id}")
        public ResponseEntity<Boolean> existePorId(@PathVariable Long id) {
                return ResponseEntity.ok(mensagemService.existsById(id,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PostMapping("/buscarPorIds")
        public ResponseEntity<List<Mensagem>> buscarPorIds(@RequestBody Iterable<Long> ids) {
                return ResponseEntity.ok(mensagemService.buscarPorIds(ids,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/total")
        public ResponseEntity<Long> total() {
                return ResponseEntity.ok(mensagemService.total(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/paginado")
        public ResponseEntity<Page<Mensagem>> listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "asc") String direction) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                                ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                return ResponseEntity.ok(mensagemService.listarPaginado(
                                usuarioLogadoService.getEmpresaIdLogada(), pageable));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                mensagemService.deleteById(id, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Mensagem deletada com sucesso.");
        }

        @DeleteMapping("/deletarTodos")
        public ResponseEntity<Long> deleteAll() {
                return ResponseEntity.ok(mensagemService.deleteAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorIds")
        public ResponseEntity<Void> deletarAllById(@RequestBody Iterable<Long> ids) {
                mensagemService.deletarAllById(ids,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/listarPorChamado/{idChamado}")
        public ResponseEntity<List<Mensagem>> findAllByChamado(@PathVariable Long idChamado) {
                return ResponseEntity.ok(mensagemService.findAllByChamado(idChamado,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorConteudoPorChamado/{idChamado}/{conteudo}")
        public ResponseEntity<List<Mensagem>> buscaPorConteudoByChamado(
                        @PathVariable Long idChamado, @PathVariable String conteudo) {
                return ResponseEntity.ok(mensagemService.buscaPorConteudoByChamado(conteudo, idChamado,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorConteudoPorChamado/{idChamado}/{conteudo}")
        public ResponseEntity<Boolean> existePorConteudoByChamado(
                        @PathVariable Long idChamado, @PathVariable String conteudo) {
                return ResponseEntity.ok(mensagemService.existePorConteudoByChamado(conteudo, idChamado,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorConteudoDiferenteIdPorChamado/{id}/{idChamado}/{conteudo}")
        public ResponseEntity<Boolean> existePorConteudoDiferenteIdByChamado(
                        @PathVariable Long id, @PathVariable Long idChamado, @PathVariable String conteudo) {
                return ResponseEntity.ok(mensagemService.existePorConteudoDiferenteIdByChamado(id, conteudo,
                                idChamado, usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/totalPorChamado/{idChamado}")
        public ResponseEntity<Long> countByChamado(@PathVariable Long idChamado) {
                return ResponseEntity.ok(mensagemService.countByChamado(idChamado,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorChamado/{idChamado}")
        public ResponseEntity<Void> deleteAllByChamado(@PathVariable Long idChamado) {
                mensagemService.deleteAllByChamado(idChamado,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/deletarPorChamado/{id}/{idChamado}")
        public ResponseEntity<Void> deleteByIdAndChamado(
                        @PathVariable Long id, @PathVariable Long idChamado) {
                mensagemService.deleteByIdAndChamado(id, idChamado,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/listarNaoLidas")
        public ResponseEntity<List<Mensagem>> findAllNaoLidas() {
                return ResponseEntity.ok(mensagemService.findAllNaoLidas(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarNaoLidasPorChamado/{idChamado}")
        public ResponseEntity<List<Mensagem>> findAllNaoLidasByChamado(@PathVariable Long idChamado) {
                return ResponseEntity.ok(mensagemService.findAllNaoLidasByChamado(idChamado,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/totalNaoLidasPorChamado/{idChamado}")
        public ResponseEntity<Long> countNaoLidasByChamado(@PathVariable Long idChamado) {
                return ResponseEntity.ok(mensagemService.countNaoLidasByChamado(idChamado,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PatchMapping("/atualizarLeitura/{id}/{lida}")
        public ResponseEntity<Void> updateLida(@PathVariable Long id, @PathVariable Boolean lida) {
                mensagemService.updateLida(id, lida, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/listarPorAtendente/{idAtendente}")
        public ResponseEntity<List<Mensagem>> findAllByAtendente(@PathVariable Long idAtendente) {
                return ResponseEntity.ok(mensagemService.findAllByAtendente(idAtendente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/totalPorAtendente/{idAtendente}")
        public ResponseEntity<Long> countByAtendente(@PathVariable Long idAtendente) {
                return ResponseEntity.ok(mensagemService.countByAtendente(idAtendente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarPorCliente/{idCliente}")
        public ResponseEntity<List<Mensagem>> findAllByCliente(@PathVariable Long idCliente) {
                return ResponseEntity.ok(mensagemService.findAllByCliente(idCliente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/totalPorCliente/{idCliente}")
        public ResponseEntity<Long> countByCliente(@PathVariable Long idCliente) {
                return ResponseEntity.ok(mensagemService.countByCliente(idCliente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarPorChamadoEAtendente/{idChamado}/{idAtendente}")
        public ResponseEntity<List<Mensagem>> findAllByChamadoAndAtendente(
                        @PathVariable Long idChamado, @PathVariable Long idAtendente) {
                return ResponseEntity.ok(mensagemService.findAllByChamadoAndAtendente(idChamado, idAtendente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarNaoLidasPorAtendente/{idAtendente}")
        public ResponseEntity<List<Mensagem>> findAllNaoLidasByAtendente(@PathVariable Long idAtendente) {
                return ResponseEntity.ok(mensagemService.findAllNaoLidasByAtendente(idAtendente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarNaoLidasPorCliente/{idCliente}")
        public ResponseEntity<List<Mensagem>> findAllNaoLidasByCliente(@PathVariable Long idCliente) {
                return ResponseEntity.ok(mensagemService.findAllNaoLidasByCliente(idCliente,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }
}