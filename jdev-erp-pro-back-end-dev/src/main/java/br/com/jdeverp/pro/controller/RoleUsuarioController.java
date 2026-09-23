package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.service.RoleUsuarioService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/roleUsuario")
public class RoleUsuarioController {

        @Autowired
        private RoleUsuarioService roleUsuarioService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @PostMapping("/salvar")
        public ResponseEntity<RoleUsuario> salvar(@RequestBody RoleUsuario roleUsuario) {
                return ResponseEntity.ok(roleUsuarioService.salvar(roleUsuario));
        }

        @GetMapping("/listarPorUsuario/{idUsuario}")
        public ResponseEntity<List<RoleUsuario>> findAllByUsuario(@PathVariable Long idUsuario) {
                return ResponseEntity.ok(roleUsuarioService.findAllByUsuario(idUsuario,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/listarPorRole/{idRole}")
        public ResponseEntity<List<RoleUsuario>> findAllByRoleAndEmpresa(@PathVariable Long idRole) {
                return ResponseEntity.ok(roleUsuarioService.findAllByRoleAndEmpresa(idRole,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorUsuarioERole/{idUsuario}/{idRole}")
        public ResponseEntity<Boolean> existePorUsuarioERole(
                        @PathVariable Long idUsuario, @PathVariable Long idRole) {
                return ResponseEntity.ok(roleUsuarioService.existePorUsuarioERole(idUsuario, idRole,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                roleUsuarioService.deleteById(id);
                return ResponseEntity.ok("Role do usuário deletado com sucesso.");
        }

        @DeleteMapping("/deletarPorUsuarioERole/{idUsuario}/{idRole}")
        public ResponseEntity<String> deleteByUsuarioAndRole(
                        @PathVariable Long idUsuario, @PathVariable Long idRole) {
                roleUsuarioService.deleteByUsuarioAndRole(idUsuario, idRole,
                                usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Role do usuário deletado com sucesso.");
        }
}
