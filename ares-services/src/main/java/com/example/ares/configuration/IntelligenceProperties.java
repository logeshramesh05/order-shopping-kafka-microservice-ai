package com.example.ares.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration for AI-backed incident analysis.
 */
@Validated
@ConfigurationProperties(prefix = "ares.intelligence")
public class IntelligenceProperties {

    @NotBlank
    private String provider = "groq";

    @NotBlank
    private String groqBaseUrl = "https://api.groq.com/openai/v1/chat/completions";

    @NotBlank
    private String groqModel = "openai/gpt-oss-120b";

    private String groqApiKey;

    @Min(250)
    private int timeoutMs = 5000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGroqBaseUrl() {
        return groqBaseUrl;
    }

    public void setGroqBaseUrl(String groqBaseUrl) {
        this.groqBaseUrl = groqBaseUrl;
    }

    public String getGroqModel() {
        return groqModel;
    }

    public void setGroqModel(String groqModel) {
        this.groqModel = groqModel;
    }

    public String getGroqApiKey() {
        return groqApiKey;
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
