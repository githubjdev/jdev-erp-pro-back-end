package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.service.RoleService;

@RestController
@RequestMapping("/api/role")
public class RoleController {

        @Autowired
        private RoleService roleService;

        @GetMapping("/listar")
        public ResponseEntity<List<Role>> findAll() {
                return ResponseEntity.ok(roleService.findAll());
        }

        @GetMapping("/buscaPorAcesso/{acesso}")
        public ResponseEntity<List<Role>> buscaPorAcesso(@PathVariable String acesso) {
                return ResponseEntity.ok(roleService.buscaPorAcesso(acesso));
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
