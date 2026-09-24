package com.kidxoxo.talenteditor.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A deliberately small, deterministic numeric expression language.
 *
 * <p>It supports arithmetic, parentheses, named runtime values and a fixed
 * function whitelist. It never reflects into Java and cannot call commands or
 * access the file system, which makes expressions safe to load from data packs.</p>
 */
public final class NumericExpression {
    public static final int MAX_LENGTH = 512;
    private final String source;
    private final Node root;

    private NumericExpression(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    public static NumericExpression compile(String source) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Expression must not be blank");
        if (source.length() > MAX_LENGTH) throw new IllegalArgumentException("Expression is longer than " + MAX_LENGTH);
        Parser parser = new Parser(source);
        Node root = parser.expression();
        parser.skipWhitespace();
        if (!parser.end()) throw parser.error("Unexpected token");
        return new NumericExpression(source, root);
    }

    public double evaluate(Resolver resolver) {
        double value = root.evaluate(Objects.requireNonNull(resolver, "resolver"));
        return Double.isFinite(value) ? value : 0.0D;
    }

    public String source() {
        return source;
    }

    public interface Resolver {
        double variable(String name);

        default double lookup(String function, String key) {
            return 0.0D;
        }

        default double random() {
            return 0.0D;
        }
    }

    private interface Node {
        double evaluate(Resolver resolver);
    }

    private record Constant(double value) implements Node {
        @Override public double evaluate(Resolver resolver) { return value; }
    }

    private record Variable(String name) implements Node {
        @Override public double evaluate(Resolver resolver) { return resolver.variable(name); }
    }

    private record Unary(char operation, Node value) implements Node {
        @Override public double evaluate(Resolver resolver) {
            double number = value.evaluate(resolver);
            return operation == '-' ? -number : number;
        }
    }

    private record Binary(char operation, Node left, Node right) implements Node {
        @Override public double evaluate(Resolver resolver) {
            double first = left.evaluate(resolver);
            double second = right.evaluate(resolver);
            return switch (operation) {
                case '+' -> first + second;
                case '-' -> first - second;
                case '*' -> first * second;
                case '/' -> Math.abs(second) < 1.0E-12D ? 0.0D : first / second;
                case '%' -> Math.abs(second) < 1.0E-12D ? 0.0D : first % second;
                case '^' -> Math.pow(first, second);
                default -> 0.0D;
            };
        }
    }

    private record Function(String name, List<Node> arguments) implements Node {
        @Override public double evaluate(Resolver resolver) {
            double first = arguments.isEmpty() ? 0.0D : arguments.get(0).evaluate(resolver);
            double second = arguments.size() < 2 ? 0.0D : arguments.get(1).evaluate(resolver);
            return switch (name) {
                case "min" -> Math.min(first, second);
                case "max" -> Math.max(first, second);
                case "pow" -> Math.pow(first, second);
                case "gt" -> first > second ? 1.0D : 0.0D;
                case "gte" -> first >= second ? 1.0D : 0.0D;
                case "lt" -> first < second ? 1.0D : 0.0D;
                case "lte" -> first <= second ? 1.0D : 0.0D;
                case "eq" -> Double.compare(first, second) == 0 ? 1.0D : 0.0D;
                case "neq" -> Double.compare(first, second) != 0 ? 1.0D : 0.0D;
                case "clamp" -> Math.max(second, Math.min(arguments.get(2).evaluate(resolver), first));
                case "sqrt" -> Math.sqrt(Math.max(0.0D, first));
                case "abs" -> Math.abs(first);
                case "floor" -> Math.floor(first);
                case "ceil" -> Math.ceil(first);
                case "round" -> Math.rint(first);
                case "random" -> resolver.random();
                default -> 0.0D;
            };
        }
    }

    private record Lookup(String function, String key) implements Node {
        @Override public double evaluate(Resolver resolver) { return resolver.lookup(function, key); }
    }

    private static final class Parser {
        private static final List<String> LOOKUPS = List.of(
                "counter", "state", "target_state", "attribute", "target_attribute",
                "attribute_base", "attribute_ratio_bonus",
                "category_count", "rarity_count", "ever_category_count", "ever_rarity_count",
                "has_talent", "has_ever_talent", "has_item", "nearby_count"
        );
        private final String source;
        private int position;
        private int nodes;

        private Parser(String source) {
            this.source = source;
        }

        private Node expression() {
            return addSubtract();
        }

        private Node addSubtract() {
            Node value = multiplyDivide();
            while (true) {
                skipWhitespace();
                if (!peek('+') && !peek('-')) return value;
                char operation = source.charAt(position++);
                value = node(new Binary(operation, value, multiplyDivide()));
            }
        }

        private Node multiplyDivide() {
            Node value = power();
            while (true) {
                skipWhitespace();
                if (!peek('*') && !peek('/') && !peek('%')) return value;
                char operation = source.charAt(position++);
                value = node(new Binary(operation, value, power()));
            }
        }

        private Node power() {
            Node value = unary();
            skipWhitespace();
            if (peek('^')) {
                position++;
                value = node(new Binary('^', value, power()));
            }
            return value;
        }

        private Node unary() {
            skipWhitespace();
            if (peek('+') || peek('-')) {
                char operation = source.charAt(position++);
                return node(new Unary(operation, unary()));
            }
            return atom();
        }

        private Node atom() {
            skipWhitespace();
            if (peek('(')) {
                position++;
                Node value = expression();
                require(')');
                return value;
            }
            if (!end() && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                return number();
            }
            String name = identifier().toLowerCase(Locale.ROOT);
            skipWhitespace();
            if (!peek('(')) return node(new Variable(name));
            position++;
            if (LOOKUPS.contains(name)) {
                String key = stringLiteral();
                require(')');
                if (name.equals("nearby_count")) NearbyEntityQuery.parse(key);
                return node(new Lookup(name, key));
            }
            ArrayList<Node> arguments = new ArrayList<>();
            skipWhitespace();
            if (!peek(')')) {
                do {
                    arguments.add(expression());
                    skipWhitespace();
                    if (!peek(',')) break;
                    position++;
                } while (true);
            }
            require(')');
            int expected = switch (name) {
                case "sqrt", "abs", "floor", "ceil", "round" -> 1;
                case "min", "max", "pow", "gt", "gte", "lt", "lte", "eq", "neq" -> 2;
                case "clamp" -> 3;
                case "random" -> 0;
                default -> throw error("Unknown function '" + name + "'");
            };
            if (arguments.size() != expected) {
                throw error("Function '" + name + "' requires " + expected + " argument(s)");
            }
            return node(new Function(name, List.copyOf(arguments)));
        }

        private Node number() {
            int start = position;
            boolean exponent = false;
            while (!end()) {
                char value = source.charAt(position);
                if (Character.isDigit(value) || value == '.') {
                    position++;
                } else if ((value == 'e' || value == 'E') && !exponent) {
                    exponent = true;
                    position++;
                    if (!end() && (source.charAt(position) == '+' || source.charAt(position) == '-')) position++;
                } else break;
            }
            try {
                double value = Double.parseDouble(source.substring(start, position));
                if (!Double.isFinite(value)) throw error("Number must be finite");
                return node(new Constant(value));
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private String identifier() {
            skipWhitespace();
            int start = position;
            while (!end()) {
                char value = source.charAt(position);
                if (!Character.isLetterOrDigit(value) && value != '_') break;
                position++;
            }
            if (start == position || !Character.isLetter(source.charAt(start)) && source.charAt(start) != '_') {
                throw error("Expected a value");
            }
            return source.substring(start, position);
        }

        private String stringLiteral() {
            skipWhitespace();
            if (end() || source.charAt(position) != '"') throw error("Expected a quoted resource or state key");
            position++;
            StringBuilder value = new StringBuilder();
            while (!end()) {
                char next = source.charAt(position++);
                if (next == '"') return value.toString();
                if (next == '\\') {
                    if (end()) throw error("Unterminated escape");
                    char escaped = source.charAt(position++);
                    if (escaped != '\\' && escaped != '"') throw error("Only quote and backslash may be escaped");
                    value.append(escaped);
                } else {
                    value.append(next);
                }
                if (value.length() > 256) throw error("Lookup key is too long");
            }
            throw error("Unterminated string");
        }

        private <T extends Node> T node(T value) {
            if (++nodes > 256) throw error("Expression is too complex");
            return value;
        }

        private void require(char expected) {
            skipWhitespace();
            if (!peek(expected)) throw error("Expected '" + expected + "'");
            position++;
        }

        private boolean peek(char expected) {
            return !end() && source.charAt(position) == expected;
        }

        private void skipWhitespace() {
            while (!end() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean end() {
            return position >= source.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + position);
        }
    }
}
