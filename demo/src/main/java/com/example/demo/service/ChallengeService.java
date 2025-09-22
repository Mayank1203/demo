package com.example.demo.service;



import com.example.demo.dto.SolutionRequest;
import com.example.demo.dto.WebhookRequest;
import com.example.demo.dto.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChallengeService {

    private static final Logger logger = LoggerFactory.getLogger(ChallengeService.class);

    private final WebClient webClient;

    // Injecting user details from application.properties
    @Value("${challenge.user.name}")
    private String name;
    @Value("${challenge.user.reg-no}")
    private String regNo;
    @Value("${challenge.user.email}")
    private String email;

    public ChallengeService(WebClient.Builder webClientBuilder, @Value("${api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public void runChallenge() {
        logger.info("Starting Bajaj Finserv Health Challenge...");
        generateWebhook()
                .flatMap(this::submitSolution)
                .subscribe(
                        result -> logger.info("✅ Challenge Completed Successfully! Final Response: {}", result),
                        error -> logger.error("❌ Challenge Failed: {}", error.getMessage())
                );
    }

    private Mono<WebhookResponse> generateWebhook() {
        WebhookRequest request = new WebhookRequest(name, regNo, email);
        logger.info("Step 1: Generating webhook with details: {}", request);
        return webClient.post()
                .uri("/generateWebhook/JAVA")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WebhookResponse.class)
                .doOnSuccess(response -> logger.info("Webhook and accessToken received successfully."));
    }

    private Mono<String> submitSolution(WebhookResponse webhookResponse) {
        String sqlQuery = determineSqlQuery();
        SolutionRequest solution = new SolutionRequest(sqlQuery);

        logger.info("Step 2: Submitting solution to webhook URL: {}", webhookResponse.getWebhookUrl());
        logger.info("Authorization Token: Bearer {}", webhookResponse.getAccessToken().substring(0, 15) + "..."); // Log a snippet
        logger.info("Final SQL Query: {}", sqlQuery);

        // Note: The PDF shows a static URL, but logic implies using the dynamic one.
        // We will use the dynamic URL from the response as is standard practice.
        return webClient.post()
                .uri(webhookResponse.getWebhookUrl())
                .header("Authorization", "Bearer " + webhookResponse.getAccessToken())
                .bodyValue(solution)
                .retrieve()
                .bodyToMono(String.class);
    }

    private String determineSqlQuery() {
        // Extract the last two digits of the registration number
        int lastTwoDigits;
        try {
            lastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            logger.error("Could not parse last two digits of regNo: '{}'. Defaulting to 0 (Even).", regNo);
            lastTwoDigits = 0;
        }

        logger.info("Last two digits of registration number ({}) are {}.", regNo, lastTwoDigits);

        if (lastTwoDigits % 2 != 0) {
            // --- ODD QUESTION SQL ---
            // IMPORTANT: Replace this with the actual SQL query for the ODD question.
            logger.info("Condition: ODD. Using Query 1.");
            return "SELECT user_id, a.name, phone_number, city FROM accounts a JOIN user_profiles up ON a.id = up.account_id WHERE a.status = 'active' ORDER BY a.name;";
        } else {
            // --- EVEN QUESTION SQL ---
            // IMPORTANT: Replace this with the actual SQL query for the EVEN question.
            logger.info("Condition: EVEN. Using Query 2.");
            return "SELECT p.product_name, SUM(oi.quantity) as total_quantity, SUM(oi.price * oi.quantity) as total_revenue FROM products p JOIN order_items oi ON p.id = oi.product_id GROUP BY p.product_name ORDER BY total_revenue DESC LIMIT 5;";
        }
    }
}

