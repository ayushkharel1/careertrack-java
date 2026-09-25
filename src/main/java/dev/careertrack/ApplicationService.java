package dev.careertrack;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;
    public ApplicationService(ApplicationRepository repository) { this.repository = repository; }
    public List<Application> list() { return repository.findAll(); }
    public Application get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }
    @Transactional
    public Application create(ApplicationRequest input) { validateDates(input); return repository.insert(input); }
    @Transactional
    public Application update(UUID id, ApplicationRequest input) {
        validateDates(input);
        get(id);
        if (input.version() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required for updates");
        if (!repository.update(id, input)) throw conflict();
        return get(id);
    }
    @Transactional
    public void delete(UUID id, int version) {
        get(id);
        if (!repository.delete(id, version)) throw conflict();
    }
    private void validateDates(ApplicationRequest input) {
        if (input.appliedOn() != null && input.interviewOn() != null && input.interviewOn().isBefore(input.appliedOn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview date cannot be before the application date");
        }
    }
    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "This application changed in another tab. Refresh and try again.");
    }
    public String exportCsv() {
        StringBuilder csv = new StringBuilder("Company,Role,Location,Stage,Applied on,Interview on,Notes\r\n");
        for (Application app : list()) {
            csv.append(String.join(",", cell(app.company()), cell(app.role()), cell(app.location()),
                cell(app.stage()), cell(app.appliedOn()), cell(app.interviewOn()), cell(app.notes()))).append("\r\n");
        }
        return csv.toString();
    }
    // Neutralize spreadsheet formulas, then quote delimiters, newlines and double quotes.
    static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.stripLeading().matches("(?s)^[=+@-].*") || text.startsWith("\t") || text.startsWith("\r")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
