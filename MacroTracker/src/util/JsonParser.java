package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free JSON reader/writer.
 *
 * MacroTracker is a plain Eclipse project (no Maven/Gradle), and this
 * sandbox's network allowlist doesn't reach Maven Central, so pulling in
 * org.json (or similar) isn't practical here. The Anthropic API response
 * this is used for is bounded and predictable, so a minimal parser is a
 * fair trade-off — but it is a general JSON parser, not a hacky one-off.
 *
 * Parses into plain java.util types:
 *   object -> LinkedHashMap<String, Object>
 *   array  -> List<Object>
 *   string -> String
 *   number -> Double
 *   true/false -> Boolean
 *   null   -> null
 */
public final class JsonParser {

    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Object parse(String json) {
        JsonParser p = new JsonParser(json);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (p.pos != p.src.length()) {
            throw new JsonParseException("Unexpected trailing content at position " + p.pos);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object value = parse(json);
        if (!(value instanceof Map)) {
            throw new JsonParseException("Expected a JSON object at the top level");
        }
        return (Map<String, Object>) value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (pos >= src.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        char c = src.charAt(pos);
        switch (c) {
            case '{':
                return parseObjectValue();
            case '[':
                return parseArrayValue();
            case '"':
                return parseStringValue();
            case 't':
                expectLiteral("true");
                return Boolean.TRUE;
            case 'f':
                expectLiteral("false");
                return Boolean.FALSE;
            case 'n':
                expectLiteral("null");
                return null;
            default:
                return parseNumberValue();
        }
    }

    private Map<String, Object> parseObjectValue() {
        Map<String, Object> result = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            String key = parseStringValue();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            result.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == '}') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Expected ',' or '}' at position " + pos);
            }
        }
        return result;
    }

    private List<Object> parseArrayValue() {
        List<Object> result = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            Object value = parseValue();
            result.add(value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                pos++;
            } else if (next == ']') {
                pos++;
                break;
            } else {
                throw new JsonParseException("Expected ',' or ']' at position " + pos);
            }
        }
        return result;
    }

    private String parseStringValue() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsonParseException("Unterminated string");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= src.length()) {
                    throw new JsonParseException("Unterminated escape sequence");
                }
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > src.length()) {
                            throw new JsonParseException("Truncated unicode escape");
                        }
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default:
                        throw new JsonParseException("Invalid escape '\\" + esc + "'");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Double parseNumberValue() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
            pos++;
        }
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
        }
        if (pos == start) {
            throw new JsonParseException("Invalid number at position " + pos);
        }
        return Double.parseDouble(src.substring(start, pos));
    }

    private void expectLiteral(String literal) {
        if (pos + literal.length() > src.length() || !src.startsWith(literal, pos)) {
            throw new JsonParseException("Expected '" + literal + "' at position " + pos);
        }
        pos += literal.length();
    }

    private void expect(char c) {
        skipWhitespace();
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new JsonParseException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }

    private char peek() {
        if (pos >= src.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        return src.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    /** Escapes a string for embedding as a JSON string literal (without the surrounding quotes). */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }
}
