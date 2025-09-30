package com.example.demo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerAgentService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAgentService.class);

    private final ChatClient chatClient;
    private final CustomerRepository customerRepository;

    public CustomerAgentService(ChatClient chatClient, CustomerRepository customerRepository) {
        this.chatClient = chatClient;
        this.customerRepository = customerRepository;
    }

    public String generateCustomerResponse(String customerId, String inquiry) {
        logger.info("=== Starting LLM call for customer: {} ===", customerId);
        logger.debug("Customer inquiry: {}", inquiry);

        long startTime = System.currentTimeMillis();

        String systemPrompt = """
                You are a helpful and professional customer service agent.
                Your name is Amanda.
                Always be polite, empathetic, and solution-oriented.
                Personalize your responses based on the customer information provided.
                Keep responses concise but comprehensive.

                The Company Name: LoFi Tech Solutions
                Business Hours: Monday to Friday, 9 AM to 6 PM (EST)
                Contact Email: support@lofi-tech.com

                Always get the customer details by customer ID using the tool provided.

                Summarize the customer details in following format:

                - ID: {customerId}
                - Name: {customerName}
                - Email: {customerEmail}
                - Phone: {customerPhone}
                - Preferred Language: {preferredLanguage}
                - Customer Segment: {customerSegment}
                - Last Interaction: {lastInteraction}

                """;

        String userPrompt = """
                Customer Information:
                - ID: {customerId}

                Customer Inquiry: {inquiry}

                Please provide a personalized and helpful response to this customer.
                """;

        PromptTemplate promptTemplate = new PromptTemplate(userPrompt);

        Map<String, Object> promptVariables = Map.of(
                "customerId", customerId,
                "inquiry", inquiry);

        Prompt prompt = promptTemplate.create(promptVariables);

        // Log the final prompt that will be sent to the LLM
        logger.debug("System prompt: {}", systemPrompt);
        logger.debug("Final user prompt: {}", prompt.getContents());
        logger.debug("Prompt variables: {}", promptVariables);

        try {
            logger.info("Calling LLM with model: gpt-oss:20b");

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt.getContents())
                    .advisors(new SimpleLoggerAdvisor())
                    .tools(customerRepository)
                    .call()
                    .content();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.info("LLM call completed successfully in {} ms", duration);

            if (response != null) {
                logger.debug("LLM response length: {} characters", response.length());
                logger.debug("LLM response: {}", response);
            } else {
                logger.warn("LLM returned null response");
                response = "I apologize, but I didn't receive a proper response. Please try again.";
            }

            logger.info("=== Finished LLM call for customer: {} ===", customerId);

            return response;

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.error("LLM call failed after {} ms for customer: {}", duration, customerId, e);
            logger.error("Error details: {}", e.getMessage());
            return "I apologize, but I'm experiencing technical difficulties. Please try again later or contact our support team directly.";
        }
    }
}