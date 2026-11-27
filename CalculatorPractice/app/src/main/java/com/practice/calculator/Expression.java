package com.practice.calculator;

import java.math.BigDecimal;
import java.math.MathContext;

// 用两层解析实现运算优先级。sum 读取 product，product 读取 number。
final class Expression {
    private final String text;
    private int position;

    private Expression(String text) {
        this.text = text;
    }

    static String evaluate(String text) {
        Expression parser = new Expression(text);
        BigDecimal value = parser.sum();
        if (parser.position != text.length()) {
            throw new NumberFormatException("Unexpected input");
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal sum() {
        // 每一侧都会先把乘除算完，例如 2 + 3 * 4 的右侧先得到 12。
        BigDecimal value = product();
        while (position < text.length()) {
            if (take('+')) {
                value = value.add(product(), MathContext.DECIMAL64);
            } else if (take('-')) {
                value = value.subtract(product(), MathContext.DECIMAL64);
            } else {
                break;
            }
        }
        return value;
    }

    private BigDecimal product() {
        BigDecimal value = number();
        while (position < text.length()) {
            if (take('*')) {
                value = value.multiply(number(), MathContext.DECIMAL64);
            } else if (take('/')) {
                value = value.divide(number(), MathContext.DECIMAL64);
            } else {
                break;
            }
        }
        return value;
    }

    private BigDecimal number() {
        int start = position;
        // 数字前的减号表示负号，例如 2 * -3。
        take('-');
        while (position < text.length()) {
            char next = text.charAt(position);
            if (next != '.' && (next < '0' || next > '9')) {
                break;
            }
            position++;
        }
        return new BigDecimal(text.substring(start, position));
    }

    private boolean take(char value) {
        if (position < text.length() && text.charAt(position) == value) {
            position++;
            return true;
        }
        return false;
    }
}
