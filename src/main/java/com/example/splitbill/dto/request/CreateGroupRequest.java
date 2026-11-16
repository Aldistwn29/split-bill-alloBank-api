package com.example.splitbill.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
    @NotBlank(message = "Nama group wajib diisi")
    String name,

    @NotNull(message = "Daftar participant wajib diisi")
    @Size(min = 2, message = "Minimal dua participant diperlukan")
    List<@NotBlank(message = "Nama participant wajib diisi") String> participants
) {}
