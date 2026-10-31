package edu.el3304.lab5.p1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MatchService {
    private final MatchRepository repository;
    private final ObjectMapper mapper;
    private final String apiUrl;

    public MatchService(MatchRepository repository, ObjectMapper mapper,
                        @Value("${matches.api.url}") String apiUrl) {
        this.repository = repository;
        this.mapper = mapper;
        this.apiUrl = apiUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadMatches() throws Exception {
        repository.initialize();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        Exception lastError = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(8)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) throw new IllegalStateException("Fuente HTTP " + response.statusCode());
                List<Match> matches = mapper.readValue(response.body(), new TypeReference<>() {});
                if (matches.isEmpty()) throw new IllegalStateException("La fuente no tiene partidos");
                repository.saveMatches(matches);
                return;
            } catch (Exception error) {
                lastError = error;
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("No se pudieron cargar los partidos", lastError);
    }

    public void predict(int matchId, int homeGoals, int awayGoals) {
        Match match = repository.match(matchId);
        if (match == null) throw new IllegalArgumentException("El partido seleccionado no existe.");
        if (homeGoals < 0 || homeGoals > 20 || awayGoals < 0 || awayGoals > 20)
            throw new IllegalArgumentException("Los goles deben estar entre 0 y 20.");
        String outcome = homeGoals == awayGoals ? "Empate"
            : homeGoals > awayGoals ? match.home() : match.away();
        repository.addPrediction(matchId, homeGoals, awayGoals, outcome);
    }
}
