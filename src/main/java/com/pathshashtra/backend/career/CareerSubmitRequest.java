package com.pathshashtra.backend.career;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CareerSubmitRequest {
    @NotEmpty(message = "Answers cannot be empty")
    private List<String> answers;
}
