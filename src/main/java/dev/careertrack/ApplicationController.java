package dev.careertrack;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service) { this.service = service; }
    @GetMapping public List<Application> list() { return service.list(); }
    @GetMapping("/{id}") public Application get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping public ResponseEntity<Application> create(@Valid @RequestBody ApplicationRequest input) {
        Application app = service.create(input);
        return ResponseEntity.created(URI.create("/api/applications/" + app.id())).body(app);
    }
    @PutMapping("/{id}") public Application update(@PathVariable UUID id, @Valid @RequestBody ApplicationRequest input) {
        return service.update(id, input);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam int version) {
        service.delete(id, version); return ResponseEntity.noContent().build();
    }
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=careertrack-applications.csv").body(service.exportCsv());
    }
}
