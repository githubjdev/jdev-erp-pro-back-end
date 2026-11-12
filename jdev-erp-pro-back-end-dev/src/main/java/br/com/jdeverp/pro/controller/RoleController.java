package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.dto.RoleDTO;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.service.RoleService;
import jakarta.validation.Valid;

@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RestController
@RequestMapping("/api/role")
public class RoleController {

        @Autowired
        private RoleService roleService;
        
        @PostMapping("/salvar")
        public ResponseEntity<RoleDTO> salvar(@RequestBody @Valid Role role){
        		return ResponseEntity.ok(roleService.salvar(role));
        }
        
        
        @PostMapping("/atualizar")
        public ResponseEntity<RoleDTO> atualizar(@RequestBody @Valid Role role){
        		return ResponseEntity.ok(roleService.atualizar(role));
        }

        @GetMapping("/listar")
        public ResponseEntity<List<RoleDTO>> findAll() {
                return ResponseEntity.ok(roleService.listar());
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<RoleDTO> buscarPorId(@PathVariable Long id) {
                return roleService.buscarPorId(id)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/buscaPorAcesso/{acesso}")
        public ResponseEntity<List<RoleDTO>> buscaPorAcesso(@PathVariable String acesso) {
                return ResponseEntity.ok(roleService.buscaPorAcessoDto(acesso));
        }

        @GetMapping("/existePorAcesso/{acesso}")
        public ResponseEntity<Boolean> existePorAcesso(@PathVariable String acesso) {
                return ResponseEntity.ok(roleService.existePorAcesso(acesso));
        }

        @GetMapping("/existePorAcessoDiferenteId/{id}/{acesso}")
        public ResponseEntity<Boolean> existePorAcessoDiferenteId(
                        @PathVariable Long id, @PathVariable String acesso) {
                return ResponseEntity.ok(roleService.existePorAcessoDiferenteId(id, acesso));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                roleService.deleteById(id);
                return ResponseEntity.ok("Role deletado com sucesso.");
        }
}
