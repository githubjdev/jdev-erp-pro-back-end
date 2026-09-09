package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.service.EmpresaService;

@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {

        @Autowired
        private EmpresaService empresaService;

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<Empresa> buscarPorId(@PathVariable Long id) {
                return ResponseEntity.ok(empresaService.buscaPorId(id));
        }

        @GetMapping("/listar")
        public ResponseEntity<List<Empresa>> listar() {
                return ResponseEntity.ok(empresaService.findAll());
        }

        @GetMapping("/buscaPorNome/{nome}")
        public ResponseEntity<List<Empresa>> buscaPorNome(@PathVariable String nome) {
                return ResponseEntity.ok(empresaService.buscaPorNome(nome));
        }

        @GetMapping("/existePorNome/{nome}")
        public ResponseEntity<Boolean> existePorNome(@PathVariable String nome) {
                return ResponseEntity.ok(empresaService.existePorNome(nome));
        }

        @GetMapping("/existePorNomeDiferenteId/{id}/{nome}")
        public ResponseEntity<Boolean> existePorNomeDiferenteId(
                        @PathVariable Long id, @PathVariable String nome) {
                return ResponseEntity.ok(empresaService.existePorNomeDiferenteId(id, nome));
        }

        @DeleteMapping("/deletar/{id}")
        public ResponseEntity<String> deleteById(@PathVariable Long id) {
                empresaService.deleteById(id);
                return ResponseEntity.ok("Empresa deletada com sucesso.");
        }
}