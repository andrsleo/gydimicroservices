package com.affiliate.rentals.properties.adapters.in.rest;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.affiliate.rentals.properties.application.dto.PropertyDto;
import com.affiliate.rentals.properties.application.usecase.GetAllPropertiesUseCase;
import com.affiliate.rentals.properties.application.usecase.GetPropertyByIdUseCase;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/properties")
@Validated
public class PropertyController {

    private final GetAllPropertiesUseCase getAll;
    private final GetPropertyByIdUseCase getById;

    public PropertyController(GetAllPropertiesUseCase getAll, GetPropertyByIdUseCase getById) {
        this.getAll = getAll;
        this.getById = getById;
    }

    // GET /api/v1/properties?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<PropertyDto>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<PropertyDto> result = getAll.execute(pageable);
        return ResponseEntity.ok(result);
    }

    // GET /api/v1/properties/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PropertyDto> getById(@PathVariable("id") UUID id) {
        PropertyDto dto = getById.execute(id);
        return ResponseEntity.ok(dto);
    }
}

