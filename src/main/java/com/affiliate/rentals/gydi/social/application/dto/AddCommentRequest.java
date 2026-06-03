package com.affiliate.rentals.gydi.social.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank(message = "El comentario no puede estar vacío")
        @Size(min = 3, max = 300, message = "El comentario debe tener entre 3 y 300 caracteres")
        String body,
        Long parentCommentId
) {}
