package edu.el3304.lab5.p1;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MatchController {
    private final MatchRepository repository;
    private final MatchService service;

    public MatchController(MatchRepository repository, MatchService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model, @RequestParam(required = false) String saved,
                        @RequestParam(required = false) String error) {
        model.addAttribute("matches", repository.matches());
        model.addAttribute("predictions", repository.predictions());
        if (saved != null) model.addAttribute("message", "Predicción guardada correctamente.");
        if (error != null) model.addAttribute("error", "Revisa el partido y escribe goles entre 0 y 20.");
        return "index";
    }

    @PostMapping("/predicciones")
    public String predict(@RequestParam int matchId, @RequestParam int homeGoals,
                          @RequestParam int awayGoals) {
        try {
            service.predict(matchId, homeGoals, awayGoals);
            return "redirect:/?saved=1";
        } catch (IllegalArgumentException error) {
            return "redirect:/?error=1";
        }
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, String> health() { return Map.of("status", "ok"); }
}
