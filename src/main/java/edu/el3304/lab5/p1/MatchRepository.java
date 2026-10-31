package edu.el3304.lab5.p1;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MatchRepository {
    private final JdbcTemplate jdbc;

    public MatchRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void initialize() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS matches (
              id INTEGER PRIMARY KEY, home TEXT NOT NULL, away TEXT NOT NULL,
              played_at TEXT NOT NULL)
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS predictions (
              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              match_id INTEGER NOT NULL REFERENCES matches(id),
              home_goals INTEGER NOT NULL, away_goals INTEGER NOT NULL,
              outcome TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now())
            """);
    }

    public void saveMatches(List<Match> matches) {
        for (Match match : matches) {
            jdbc.update("""
                INSERT INTO matches(id, home, away, played_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET home=excluded.home,
                  away=excluded.away, played_at=excluded.played_at
                """, match.id(), match.home(), match.away(), match.playedAt());
        }
    }

    public List<Match> matches() {
        return jdbc.query("SELECT id, home, away, played_at FROM matches ORDER BY id",
            (rs, row) -> new Match(rs.getInt("id"), rs.getString("home"),
                rs.getString("away"), rs.getString("played_at")));
    }

    public Match match(int id) {
        return jdbc.query("SELECT id, home, away, played_at FROM matches WHERE id=?",
            (rs, row) -> new Match(rs.getInt("id"), rs.getString("home"),
                rs.getString("away"), rs.getString("played_at")), id)
            .stream().findFirst().orElse(null);
    }

    public void addPrediction(int matchId, int homeGoals, int awayGoals, String outcome) {
        jdbc.update("INSERT INTO predictions(match_id,home_goals,away_goals,outcome) VALUES(?,?,?,?)",
            matchId, homeGoals, awayGoals, outcome);
    }

    public List<Prediction> predictions() {
        return jdbc.query("""
            SELECT p.id, m.home, m.away, p.home_goals, p.away_goals, p.outcome
            FROM predictions p JOIN matches m ON m.id=p.match_id ORDER BY p.id DESC
            """, (rs, row) -> new Prediction(rs.getLong("id"), rs.getString("home"),
                rs.getString("away"), rs.getInt("home_goals"),
                rs.getInt("away_goals"), rs.getString("outcome")));
    }
}
