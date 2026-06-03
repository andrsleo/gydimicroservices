package com.affiliate.rentals.gydi.calendar.infrastructure.in.rest;

import com.affiliate.rentals.gydi.calendar.application.dto.*;
import com.affiliate.rentals.gydi.calendar.domain.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin CRUD endpoints for managing season definitions.
 *
 * <p>Season definitions drive the automatic price multipliers on the property calendar.
 * They support a geographic scope hierarchy (GLOBAL → REGION → COUNTRY → SUBREGION)
 * so a single record can cover many countries without duplication.</p>
 */
@RestController
@Tag(name = "Season Definitions (Admin)", description = "Admin CRUD for season date ranges")
public class SeasonDefinitionController {

    private final CreateSeasonDefinitionUseCase createUseCase;
    private final UpdateSeasonDefinitionUseCase updateUseCase;
    private final DeleteSeasonDefinitionUseCase deleteUseCase;
    private final ListSeasonDefinitionsUseCase listUseCase;
    private final ListSeasonRegionsUseCase listRegionsUseCase;

    public SeasonDefinitionController(
            CreateSeasonDefinitionUseCase createUseCase,
            UpdateSeasonDefinitionUseCase updateUseCase,
            DeleteSeasonDefinitionUseCase deleteUseCase,
            ListSeasonDefinitionsUseCase listUseCase,
            ListSeasonRegionsUseCase listRegionsUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.listRegionsUseCase = listRegionsUseCase;
    }

    // ── Admin endpoints (ROLE_ADMIN required) ─────────────────────────────────

    @GetMapping("/api/v1/admin/seasons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all season definitions",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<SeasonDefinitionDto>> listAll() {
        return ResponseEntity.ok(listUseCase.execute());
    }

    @PostMapping("/api/v1/admin/seasons")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a season definition",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SeasonDefinitionDto> create(
            @RequestBody @Valid CreateSeasonDefinitionRequest request) {
        SeasonDefinitionDto created = createUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/v1/admin/seasons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a season definition",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SeasonDefinitionDto> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateSeasonDefinitionRequest request) {
        return ResponseEntity.ok(updateUseCase.execute(id, request));
    }

    @DeleteMapping("/api/v1/admin/seasons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a season definition",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    // ── Public / authenticated endpoints ──────────────────────────────────────

    @GetMapping("/api/v1/seasons/regions")
    @Operation(summary = "List available geographic regions for season definitions")
    public ResponseEntity<List<SeasonRegionDto>> listRegions() {
        return ResponseEntity.ok(listRegionsUseCase.execute());
    }
}
