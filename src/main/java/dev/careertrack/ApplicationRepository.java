package dev.careertrack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import dev.careertrack.Application.Stage;

@Repository
public class ApplicationRepository {
    private final JdbcTemplate jdbc;
    public ApplicationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Application> findAll() {
        return jdbc.query("SELECT * FROM applications ORDER BY created_at DESC, id", this::map);
    }
    public Optional<Application> findById(UUID id) {
        return jdbc.query("SELECT * FROM applications WHERE id = ?", this::map, id).stream().findFirst();
    }
    public Application insert(ApplicationRequest input) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO applications (id, company, role, location, stage, applied_on, interview_on, notes, version, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
            """, id, clean(input.company()), clean(input.role()), clean(input.location()), input.stage().name(),
            input.appliedOn(), input.interviewOn(), clean(input.notes()), Instant.now());
        return findById(id).orElseThrow();
    }
    public boolean update(UUID id, ApplicationRequest input) {
        return jdbc.update("""
            UPDATE applications SET company=?, role=?, location=?, stage=?, applied_on=?, interview_on=?, notes=?, version=version+1
            WHERE id=? AND version=?
            """, clean(input.company()), clean(input.role()), clean(input.location()), input.stage().name(),
            input.appliedOn(), input.interviewOn(), clean(input.notes()), id, input.version()) == 1;
    }
    public boolean delete(UUID id, int version) {
        return jdbc.update("DELETE FROM applications WHERE id=? AND version=?", id, version) == 1;
    }
    private String clean(String value) { return value == null ? "" : value.strip(); }
    private Application map(ResultSet rs, int row) throws SQLException {
        return new Application(rs.getObject("id", UUID.class), rs.getString("company"), rs.getString("role"),
            rs.getString("location"), Stage.valueOf(rs.getString("stage")),
            rs.getObject("applied_on", LocalDate.class), rs.getObject("interview_on", LocalDate.class),
            rs.getString("notes"), rs.getInt("version"), rs.getTimestamp("created_at").toInstant());
    }
}
