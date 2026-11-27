package com.practice.calculator;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

// Activity 负责键盘、算式编辑和显示。运算规则放在 Expression.java。
public final class MainActivity extends Activity {
    private static final int BACKGROUND = 0xff141817;
    private static final int DISPLAY = 0xff202723;
    private static final int KEY = 0xff29322e;
    private static final int OPERATOR = 0xff344a3b;
    private static final int ACCENT = 0xffbaedb0;
    private static final int TEXT = 0xfff1f5ef;
    private static final int MUTED = 0xffaab9ad;
    private static final int MAX_INPUT_LENGTH = 72;

    private String expression = "";
    private String previous = "";
    private String error = "";
    private boolean evaluated;
    private TextView detail;
    private TextView output;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) {
            expression = state.getString("expression", "");
            previous = state.getString("previous", "");
            error = state.getString("error", "");
            evaluated = state.getBoolean("evaluated");
        }
        createLayout();
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        // 保存输入和输出，使旋转后可以接着操作。
        super.onSaveInstanceState(state);
        state.putString("expression", expression);
        state.putString("previous", previous);
        state.putString("error", error);
        state.putBoolean("evaluated", evaluated);
    }

    private void createLayout() {
        // 竖屏显示区在上，横屏显示区在左。两种方向复用同一套按钮。
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(BACKGROUND);

        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setGravity(Gravity.BOTTOM);
        display.setPadding(dp(20), dp(12), dp(20), dp(16));
        display.setBackground(shape(DISPLAY, 22));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(16);
        title.setTextColor(ACCENT);
        title.setTypeface(null, Typeface.BOLD);
        display.addView(title);

        detail = new TextView(this);
        detail.setTextSize(17);
        detail.setTextColor(MUTED);
        detail.setGravity(Gravity.END | Gravity.BOTTOM);
        detail.setSingleLine();
        detail.setEllipsize(TextUtils.TruncateAt.START);
        display.addView(detail, new LinearLayout.LayoutParams(-1, 0, 1));

        output = new TextView(this);
        output.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        output.setTextColor(TEXT);
        output.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        output.setMaxLines(2);
        output.setEllipsize(TextUtils.TruncateAt.START);
        output.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        output.setAutoSizeTextTypeUniformWithConfiguration(17, 58, 1, TypedValue.COMPLEX_UNIT_SP);
        display.addView(output, new LinearLayout.LayoutParams(-1, dp(94)));

        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        // 修改这些行可以调整按键位置。0 键的双倍宽度设在 addRow()。
        addRow(pad, "AC", "DEL", "±", "÷");
        addRow(pad, "7", "8", "9", "×");
        addRow(pad, "4", "5", "6", "−");
        addRow(pad, "1", "2", "3", "+");
        addRow(pad, "0", ".", "=");

        LinearLayout.LayoutParams displaySize = landscape
                ? new LinearLayout.LayoutParams(0, -1, 0.85f)
                : new LinearLayout.LayoutParams(-1, 0, 1);
        if (landscape) {
            displaySize.setMarginEnd(dp(12));
        } else {
            displaySize.bottomMargin = dp(12);
        }
        root.addView(display, displaySize);
        root.addView(pad, landscape ? new LinearLayout.LayoutParams(0, -1, 1.15f)
                : new LinearLayout.LayoutParams(-1, 0, 2.2f));
        setContentView(root);
    }

    private void addRow(LinearLayout pad, String... keys) {
        LinearLayout row = new LinearLayout(this);
        pad.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
        for (String key : keys) {
            Button button = new Button(this);
            int color = "=".equals(key) ? ACCENT : isOperatorKey(key) ? OPERATOR : KEY;
            button.setText(key);
            button.setTextSize(key.length() > 1 ? 17 : 25);
            button.setTypeface(null, Typeface.BOLD);
            button.setTextColor("=".equals(key) ? BACKGROUND : TEXT);
            button.setAllCaps(false);
            button.setMinWidth(0);
            button.setMinHeight(0);
            button.setMinimumWidth(0);
            button.setMinimumHeight(0);
            button.setPadding(0, 0, 0, 0);
            button.setStateListAnimator(null);
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30ffffff),
                    shape(color, 15), null));
            int description = switch (key) {
                case "AC" -> R.string.clear;
                case "DEL" -> R.string.delete;
                case "±" -> R.string.sign;
                case "÷" -> R.string.divide;
                case "×" -> R.string.multiply;
                case "−" -> R.string.subtract;
                case "+" -> R.string.add;
                case "=" -> R.string.equals;
                case "." -> R.string.decimal;
                default -> 0;
            };
            if (description != 0) {
                button.setContentDescription(getString(description));
            }
            button.setOnClickListener(view -> press(key));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -1,
                    key.equals("0") ? 2 : 1);
            size.setMargins(dp(3), dp(3), dp(3), dp(3));
            row.addView(button, size);
        }
    }

    private void press(String key) {
        // 按键入口。数字和小数点交给 appendNumber()，二元运算符交给 appendOperator()。
        switch (key) {
            case "AC" -> {
                expression = "";
                previous = "";
                error = "";
                evaluated = false;
            }
            case "DEL" -> {
                if (!expression.isEmpty()) {
                    expression = expression.substring(0, expression.length() - 1);
                }
                previous = "";
                error = "";
                evaluated = false;
            }
            case "±" -> {
                error = "";
                previous = "";
                int start = operandStart();
                if (start > 0 && expression.charAt(start - 1) == '-'
                        && (start == 1 || isOperator(expression.charAt(start - 2)))) {
                    expression = expression.substring(0, start - 1) + expression.substring(start);
                } else if (expression.length() < MAX_INPUT_LENGTH) {
                    expression = expression.substring(0, start) + "-" + expression.substring(start);
                }
            }
            case "=" -> calculate();
            case "÷", "×", "−", "+" -> appendOperator(key);
            default -> appendNumber(key);
        }
        render();
    }

    private void appendOperator(String key) {
        if (!error.isEmpty()) {
            error = "";
        }
        // 界面显示数学符号，内部用 ASCII 运算符供 Expression 解析。
        char operator = switch (key) {
            case "÷" -> '/';
            case "×" -> '*';
            case "−" -> '-';
            default -> '+';
        };
        if (expression.isEmpty()) {
            if (operator == '-') {
                expression = "-";
            }
            return;
        }
        if (endsWithOperator()) {
            int end = expression.length() - 1;
            if (end == 0) {
                return;
            }
            if (isOperator(expression.charAt(end - 1))) {
                end--;
            }
            expression = expression.substring(0, end) + operator;
        } else if (expression.length() < MAX_INPUT_LENGTH) {
            expression += operator;
        }
        evaluated = false;
        previous = "";
    }

    private void appendNumber(String key) {
        if (evaluated || !error.isEmpty()) {
            expression = "";
            previous = "";
            error = "";
            evaluated = false;
        }
        String operand = expression.substring(operandStart());
        if (key.equals(".") && operand.contains(".")) {
            return;
        }
        String addition = key.equals(".") && operand.isEmpty() ? "0." : key;
        if (expression.length() + addition.length() <= MAX_INPUT_LENGTH) {
            expression += addition;
        }
    }

    private void calculate() {
        if (expression.isEmpty()) {
            return;
        }
        try {
            // 计算结果成为当前值，按运算符可续算，按数字可直接开始下一条算式。
            String result = Expression.evaluate(expression);
            previous = readable(expression) + " =";
            expression = result;
            error = "";
            evaluated = true;
        } catch (ArithmeticException failure) {
            error = getString(R.string.divide_by_zero);
        } catch (NumberFormatException failure) {
            error = getString(R.string.invalid_expression);
        }
    }

    private int operandStart() {
        // 找到末尾数字段的开始位置，供插入负号和阻止重复小数点使用。
        int start = expression.length();
        while (start > 0 && !isOperator(expression.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    private boolean endsWithOperator() {
        return isOperator(expression.charAt(expression.length() - 1));
    }

    private static boolean isOperator(char value) {
        return value == '+' || value == '-' || value == '*' || value == '/';
    }

    private static boolean isOperatorKey(String key) {
        return key.equals("÷") || key.equals("×") || key.equals("−") || key.equals("+");
    }

    private static String readable(String value) {
        return value.replace('*', '×').replace('/', '÷').replace('-', '−');
    }

    private void render() {
        detail.setText(error.isEmpty() ? previous : readable(expression));
        output.setTextColor(error.isEmpty() ? TEXT : 0xffffb7a7);
        output.setText(error.isEmpty() ? expression.isEmpty() ? "0" : readable(expression) : error);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
