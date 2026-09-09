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

import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.service.ClienteFuncionarioService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clienteFuncionario")
public class ClienteFuncionarioController {

        @Autowired
        private ClienteFuncionarioService clienteFuncionarioService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @PostMapping("/salvar")
        public ResponseEntity<ClienteFuncionario> salvar(
                        @RequestBody @Valid ClienteFuncionario clienteFuncionario) {
                return ResponseEntity.ok(clienteFuncionarioService.salvar(clienteFuncionario));
        }

        @GetMapping("/listar")
        public ResponseEntity<List<ClienteFuncionario>> listar() {
                return ResponseEntity.ok(clienteFuncionarioService.listar(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/findAll")
        public ResponseEntity<List<ClienteFuncionario>> findAll() {
                return ResponseEntity.ok(clienteFuncionarioService.findAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorNome/{nome}")
        public ResponseEntity<List<ClienteFuncionario>> buscaPorNome(@PathVariable String nome) {
                return ResponseEntity.ok(clienteFuncionarioService.buscaPorNome(nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNome/{nome}")
        public ResponseEntity<Boolean> existePorNome(@PathVariable String nome) {
                return ResponseEntity.ok(clienteFuncionarioService.existePorNome(nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorNomeDiferenteId/{id}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteId(
                        @PathVariable Long id, @PathVariable String nome) {
                return ResponseEntity.ok(clienteFuncionarioService.existePorNomeDiferenteId(id, nome,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<ClienteFuncionario> buscarPorId(@PathVariable Long id) {
                return clienteFuncionarioService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada())
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/existePorId/{id}")
        public ResponseEntity<Boolean> existePorId(@PathVariable Long id) {
                return ResponseEntity.ok(clienteFuncionarioService.existsById(id,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscarPorPessoa/{idPessoa}")
        public ResponseEntity<ClienteFuncionario> buscarPorPessoa(@PathVariable Long idPessoa) {
                return ResponseEntity.ok(clienteFuncionarioService.findByPessoa(idPessoa,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @PostMapping("/buscarPorIds")
        public ResponseEntity<List<ClienteFuncionario>> buscarPorIds(@RequestBody Iterable<Long> ids) {
                return ResponseEntity.ok(clienteFuncionarioService.buscarPorIds(ids,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/total")
        public ResponseEntity<Long> total() {
                return ResponseEntity.ok(clienteFuncionarioService.total(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/paginado")
        public ResponseEntity<Page<ClienteFuncionario>> listarPaginado(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sort,
                        @RequestParam(defaultValue = "asc") String direction) {
                Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                                ? Sort.Direction.ASC : Sort.Direction.DESC;
                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
                return ResponseEntity.ok(clienteFuncionarioService.listarPaginado(
                                usuarioLogadoService.getEmpresaIdLogada(), pageable));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                clienteFuncionarioService.deleteById(id, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Cliente funcionário deletado com sucesso.");
        }

        @DeleteMapping("/deletarTodos")
        public ResponseEntity<Long> deleteAll() {
                return ResponseEntity.ok(clienteFuncionarioService.deleteAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletarTodosPorIds")
        public ResponseEntity<Void> deletarAllById(@RequestBody Iterable<Long> ids) {
                clienteFuncionarioService.deletarAllById(ids,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/removerUsuario/{id}")
        public ResponseEntity<Void> removeUserClienteFuncionarioId(@PathVariable Long id) {
                clienteFuncionarioService.removeUserClienteFuncionarioId(id,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.noContent().build();
        }
}