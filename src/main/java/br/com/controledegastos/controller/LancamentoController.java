package br.com.controledegastos.controller;

import br.com.controledegastos.model.Lancamento;
import br.com.controledegastos.model.TipoLancamento;
import br.com.controledegastos.repository.LancamentoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class LancamentoController {

    @Autowired
    private LancamentoRepository lancamentoRepository;

    private void carregarDados(Model model, int page) {
        Pageable pageable = PageRequest.of(page, 5, Sort.by("data").descending());
        Page<Lancamento> lancamentosPage = lancamentoRepository.findAll(pageable);

        List<Lancamento> todosLancamentos = lancamentoRepository.findAll();
        BigDecimal saldo = todosLancamentos.stream()
                .map(l -> l.getTipo() == TipoLancamento.RECEITA ? l.getValor() : l.getValor().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("lancamentos", lancamentosPage.getContent());
        model.addAttribute("saldo", saldo);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", lancamentosPage.getTotalPages());
    }

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        carregarDados(model, page);
        model.addAttribute("novoLancamento", new Lancamento());
        model.addAttribute("tipos", TipoLancamento.values());
        model.addAttribute("lancamentoParaEditar", new Lancamento());
        return "index";
    }

    @PostMapping("/lancamentos")
    public String addLancamento(@Valid @ModelAttribute("novoLancamento") Lancamento novoLancamento,
            BindingResult result, Model model) {
        if (result.hasErrors()) {
            carregarDados(model, 0);
            model.addAttribute("tipos", TipoLancamento.values());
            model.addAttribute("lancamentoParaEditar", new Lancamento());
            return "index";
        }
        lancamentoRepository.save(novoLancamento);
        carregarDados(model, 0);
        return "index :: lista-lancamentos";
    }

    @DeleteMapping("/lancamentos/{id}")
    public String deleteLancamento(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, Model model) {
        lancamentoRepository.deleteById(id);
        carregarDados(model, page);
        return "index :: lista-lancamentos";
    }

    @GetMapping("/lancamentos/editar/{id}")
    public String editLancamento(@PathVariable Long id, Model model) {
        Optional<Lancamento> lancamentoOpt = lancamentoRepository.findById(id);
        if (lancamentoOpt.isPresent()) {
            model.addAttribute("lancamentoParaEditar", lancamentoOpt.get());
            model.addAttribute("tipos", TipoLancamento.values());
            return "index :: form-edicao";
        }
        return "index :: lista-lancamentos";
    }

    @PutMapping("/lancamentos/{id}")
    public String updateLancamento(@PathVariable Long id,
            @Valid @ModelAttribute("lancamentoParaEditar") Lancamento lancamentoAtualizado, BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            carregarDados(model, 0);
            model.addAttribute("tipos", TipoLancamento.values());
            return "index";
        }
        Optional<Lancamento> lancamentoOpt = lancamentoRepository.findById(id);
        if (lancamentoOpt.isPresent()) {
            Lancamento lancamentoExistente = lancamentoOpt.get();
            lancamentoExistente.setDescricao(lancamentoAtualizado.getDescricao());
            lancamentoExistente.setValor(lancamentoAtualizado.getValor());
            lancamentoExistente.setTipo(lancamentoAtualizado.getTipo());
            lancamentoExistente.setData(lancamentoAtualizado.getData());
            lancamentoRepository.save(lancamentoExistente);
        }
        carregarDados(model, 0);
        return "index :: lista-lancamentos";
    }
}
