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

import br.com.jdeverp.pro.dto.EmpresaDTO;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.service.EmpresaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {

        @Autowired
        private EmpresaService empresaService;

        @PostMapping("/salvar")
        public ResponseEntity<EmpresaDTO> salvar(@RequestBody @Valid Empresa empresa) {
                return ResponseEntity.ok(empresaService.salvar(empresa));
        }

        @PostMapping("/atualizar")
        public ResponseEntity<EmpresaDTO> atualizar(@RequestBody @Valid Empresa empresa) {
                return ResponseEntity.ok(empresaService.atualizar(empresa));
        }

        @GetMapping("/buscarPorId/{id}")
        public ResponseEntity<EmpresaDTO> buscarPorId(@PathVariable Long id) {
                return ResponseEntity.ok(empresaService.buscaPorId(id));
        }

        @GetMapping("/listar")
        public ResponseEntity<List<EmpresaDTO>> listar() {
                return ResponseEntity.ok(empresaService.findAll());
        }

        @GetMapping("/buscaPorNome/{nome}")
        public ResponseEntity<List<EmpresaDTO>> buscaPorNome(@PathVariable String nome) {
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