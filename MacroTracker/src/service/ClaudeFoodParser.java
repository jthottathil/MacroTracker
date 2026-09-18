package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.ParsedFoodItem;
import util.JsonParser;

/**
 * Turns a free-text meal description ("2 eggs, a slice of toast, and black
 * coffee") into structured ParsedFoodItem rows using the Anthropic Messages
 * API with a forced tool call, so the model's reply is always well-formed
 * JSON matching LOG_FOOD_ITEMS_SCHEMA rather than prose Claude has to parse
 * itself.
 *
 * Requires the ANTHROPIC_API_KEY environment variable to be set to a valid
 * Anthropic API key (console.anthropic.com). It is intentionally never
 * hardcoded here — see the note in DatabaseConnection about why that
 * matters for a project pushed to a public GitHub repo.
 */
public class ClaudeFoodParser {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    // Update this if a newer model is preferred; see
    // https://docs.claude.com/en/docs/about-claude/models for current IDs.
    private static final String MODEL = "claude-sonnet-4-5-20250929";

    private static final int MAX_TOKENS = 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final String SYSTEM_PROMPT =
            "You are a nutrition-logging assistant inside a calorie tracking app. "
            + "The user will describe what they ate in plain language, possibly "
            + "listing several foods in one sentence and possibly giving rough "
            + "quantities (\"a bowl of\", \"two slices of\", \"a handful of\"). "
            + "Break the description into one item per distinct food, and estimate "
            + "calories and macros (protein, carbs, fat in grams) for each using "
            + "standard nutrition data for the quantity described — make a "
            + "reasonable estimate rather than refusing when exact numbers aren't "
            + "given. Infer the most likely meal_type (Breakfast, Lunch, Dinner, or "
            + "Snack) from context (time of day mentioned, the foods themselves); if "
            + "genuinely ambiguous, use \"Snack\". Always call the log_food_items tool "
            + "with your best answer.";

    private final String apiKey;
    private final HttpClient httpClient;

    public ClaudeFoodParser() {
        this(System.getenv("ANTHROPIC_API_KEY"));
    }

    public ClaudeFoodParser(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    /**
     * Parses a free-text meal description into structured food items.
     * Runs a blocking network call — callers on the JavaFX Application
     * Thread must invoke this from a background Task, not directly.
     *
     * @throws FoodParseException on a missing API key, network failure,
     *         API error response, or an unexpected response shape.
     */
    public List<ParsedFoodItem> parse(String mealDescription) throws FoodParseException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new FoodParseException(
                    "ANTHROPIC_API_KEY is not set. Set it as an environment variable "
                    + "before launching MacroTracker to use AI food logging.");
        }
        if (mealDescription == null || mealDescription.isBlank()) {
            throw new FoodParseException("Enter a description of what you ate first.");
        }

        String requestBody = buildRequestBody(mealDescription.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new FoodParseException("Could not reach the Claude API: " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            throw new FoodParseException(
                    "Claude API returned an error (HTTP " + response.statusCode() + "): "
                    + extractErrorMessage(response.body()));
        }

        return extractItems(response.body());
    }

    private String buildRequestBody(String mealDescription) {
        String toolSchema =
                "{"
                + "\"name\":\"log_food_items\","
                + "\"description\":\"Log one or more food items extracted from a natural-language meal description.\","
                + "\"input_schema\":{"
                    + "\"type\":\"object\","
                    + "\"properties\":{"
                        + "\"items\":{"
                            + "\"type\":\"array\","
                            + "\"items\":{"
                                + "\"type\":\"object\","
                                + "\"properties\":{"
                                    + "\"food_name\":{\"type\":\"string\"},"
                                    + "\"calories\":{\"type\":\"integer\"},"
                                    + "\"protein_g\":{\"type\":\"number\"},"
                                    + "\"carbs_g\":{\"type\":\"number\"},"
                                    + "\"fat_g\":{\"type\":\"number\"},"
                                    + "\"meal_type\":{\"type\":\"string\",\"enum\":[\"Breakfast\",\"Lunch\",\"Dinner\",\"Snack\"]}"
                                + "},"
                                + "\"required\":[\"food_name\",\"calories\",\"protein_g\",\"carbs_g\",\"fat_g\",\"meal_type\"]"
                            + "}"
                        + "}"
                    + "},"
                    + "\"required\":[\"items\"]"
                + "}"
                + "}";

        return "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_tokens\":" + MAX_TOKENS + ","
                + "\"system\":\"" + JsonParser.escape(SYSTEM_PROMPT) + "\","
                + "\"tools\":[" + toolSchema + "],"
                + "\"tool_choice\":{\"type\":\"tool\",\"name\":\"log_food_items\"},"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + JsonParser.escape(mealDescription) + "\"}]"
                + "}";
    }

    @SuppressWarnings("unchecked")
    private List<ParsedFoodItem> extractItems(String responseBody) throws FoodParseException {
        Map<String, Object> root;
        try {
            root = JsonParser.parseObject(responseBody);
        } catch (Exception e) {
            throw new FoodParseException("Claude API response wasn't valid JSON.", e);
        }

        Object contentObj = root.get("content");
        if (!(contentObj instanceof List)) {
            throw new FoodParseException("Claude API response had no content.");
        }

        for (Object blockObj : (List<Object>) contentObj) {
            if (!(blockObj instanceof Map)) {
                continue;
            }
            Map<String, Object> block = (Map<String, Object>) blockObj;
            if (!"tool_use".equals(block.get("type"))) {
                continue;
            }
            Object inputObj = block.get("input");
            if (!(inputObj instanceof Map)) {
                continue;
            }
            Object itemsObj = ((Map<String, Object>) inputObj).get("items");
            if (!(itemsObj instanceof List)) {
                continue;
            }

            List<ParsedFoodItem> result = new ArrayList<>();
            for (Object itemObj : (List<Object>) itemsObj) {
                if (!(itemObj instanceof Map)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) itemObj;
                String foodName = asString(item.get("food_name"), "Unknown food");
                int calories = (int) Math.round(asDouble(item.get("calories"), 0));
                double protein = asDouble(item.get("protein_g"), 0);
                double carbs = asDouble(item.get("carbs_g"), 0);
                double fat = asDouble(item.get("fat_g"), 0);
                String mealType = asString(item.get("meal_type"), "Snack");
                result.add(new ParsedFoodItem(foodName, calories, protein, carbs, fat, mealType));
            }

            if (result.isEmpty()) {
                throw new FoodParseException("Claude didn't return any food items for that description.");
            }
            return result;
        }

        throw new FoodParseException("Claude API response didn't include the expected tool call.");
    }

    @SuppressWarnings("unchecked")
    private String extractErrorMessage(String responseBody) {
        try {
            Map<String, Object> root = JsonParser.parseObject(responseBody);
            Object errorObj = root.get("error");
            if (errorObj instanceof Map) {
                Object message = ((Map<String, Object>) errorObj).get("message");
                if (message != null) {
                    return message.toString();
                }
            }
        } catch (Exception ignored) {
            // fall through to returning the raw body below
        }
        return responseBody;
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private double asDouble(Object value, double fallback) {
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    /** Thrown for any failure to turn a meal description into food items. */
    public static class FoodParseException extends Exception {
        public FoodParseException(String message) {
            super(message);
        }

        public FoodParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
