package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.service.ChamadoService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;

@RestController
@RequestMapping("/api/chamado")
public class ChamadoController {

        @Autowired
        private ChamadoService chamadoService;

        @Autowired
        private UsuarioLogadoService usuarioLogadoService;

        @GetMapping("/listar")
        public ResponseEntity<List<Chamado>> listar() {
                return ResponseEntity.ok(chamadoService.findAll(
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/buscaPorTitulo/{titulo}")
        public ResponseEntity<List<Chamado>> buscaPorTitulo(@PathVariable String titulo) {
                return ResponseEntity.ok(chamadoService.buscaPorTitulo(titulo,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorTitulo/{titulo}")
        public ResponseEntity<Boolean> existePorTitulo(@PathVariable String titulo) {
                return ResponseEntity.ok(chamadoService.existePorTitulo(titulo,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @GetMapping("/existePorTituloDiferenteId/{id}/{titulo}")
        public ResponseEntity<Boolean> existePorTituloDiferenteId(
                        @PathVariable Long id, @PathVariable String titulo) {
                return ResponseEntity.ok(chamadoService.existePorTituloDiferenteId(id, titulo,
                                usuarioLogadoService.getEmpresaIdLogada()));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deletePorId(@PathVariable Long id) {
                chamadoService.deleteById(id, usuarioLogadoService.getEmpresaIdLogada());
                return ResponseEntity.ok("Chamado deletado com sucesso.");
        }
}