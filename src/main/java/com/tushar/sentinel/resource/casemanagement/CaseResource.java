package com.tushar.sentinel.resource.casemanagement;

import com.tushar.sentinel.model.response.casemanagement.AuditEntry;
import com.tushar.sentinel.model.response.casemanagement.CaseView;
import com.tushar.sentinel.repository.amlcase.CasePriority;
import com.tushar.sentinel.repository.amlcase.CaseStatus;
import com.tushar.sentinel.repository.amlcase.Disposition;
import com.tushar.sentinel.service.casemanagement.CaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Investigation workflow. The actor on every state change is the authenticated principal,
 * never a name supplied in the request, so the audit trail cannot be spoofed.
 */
@RestController
@RequestMapping("/api/v1/cases")
public class CaseResource {

    private static final int DEFAULT_SIZE = 50;

    private final CaseService caseService;

    public CaseResource(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseView open(@Valid @RequestBody OpenCaseRequest request, Authentication authentication) {
        return caseService.open(request.alertRefs(), request.priority(), authentication.getName());
    }

    @PostMapping("/{caseRef}/assignment")
    public CaseView assign(@PathVariable String caseRef, @RequestParam String assignee,
                           Authentication authentication) {
        return caseService.assign(caseRef, assignee, authentication.getName());
    }

    @PostMapping("/{caseRef}/disposition")
    public CaseView dispose(@PathVariable String caseRef, @Valid @RequestBody DisposeCaseRequest request,
                            Authentication authentication) {
        return caseService.dispose(caseRef, request.disposition(), request.reason(), authentication.getName());
    }

    @GetMapping
    public List<CaseView> queue(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size) {
        return caseService.queue(status, size);
    }

    @GetMapping("/{caseRef}")
    public CaseView detail(@PathVariable String caseRef) {
        return caseService.find(caseRef);
    }

    /** Immutable state transitions for the case and the alerts it bundles. */
    @GetMapping("/{caseRef}/audit")
    public List<AuditEntry> audit(@PathVariable String caseRef) {
        return caseService.trail(caseRef);
    }

    public record OpenCaseRequest(@NotEmpty List<String> alertRefs, @NotNull CasePriority priority) {
    }

    /** A disposition without a reason is not auditable, so the reason is mandatory. */
    public record DisposeCaseRequest(@NotNull Disposition disposition, @NotBlank String reason) {
    }
}
