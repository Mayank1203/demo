
package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {
    private String name;
    @JsonProperty("regNo") // Matches the exact JSON key from the PDF
    private String regNo;
    private String email;
}
