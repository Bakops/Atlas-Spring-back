package com.example.operation_atlas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JoinRequest {
    @NotBlank(message = "Pseudo requie")
    @Size(max = 50, message = "Pseudo trop long")
    private String pseudo;

    @NotBlank(message = "Join code required")
    @Size(min = 6, max = 6, message = "Join code must be 6 characters")
    private String joinCode;

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }
}