package com.practice.bmi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

// 管理输入、报告、趋势三个界面。运算由所选构建变体的 controller 触发。
public final class MainActivity extends ComponentActivity {
    private static final int BACKGROUND = 0xfff0f4ee;
    private static final int SURFACE = 0xffffffff;
    private static final int TEXT = 0xff172d25;
    private static final int MUTED = 0xff52675d;
    private static final int GREEN = 0xff006852;
    private static final int GREEN_LIGHT = 0xffdff3e8;
    private static final int WARNING = 0xff8d2518;
    private static final int WARNING_LIGHT = 0xffffe8e2;
    private static final int MENU_HOME = 1;
    private static final int MENU_HISTORY = 2;
    private static final int MENU_ENGLISH = 3;
    private static final int MENU_CHINESE = 4;
    private static final int MENU_COPY = 5;

    private enum Screen { ENTRY, REPORT, HISTORY }

    private BmiStorage storage;
    private BmiController controller;
    private BmiModel.Report report;
    private Screen screen = Screen.ENTRY;
    private Screen historyOrigin = Screen.ENTRY;
    private boolean landscape;
    private ScrollView scroll;
    private LinearLayout page;
    private EditText heightField;
    private EditText weightField;
    private EditText ageField;
    private RadioGroup genderField;
    private int maleId;
    private int femaleId;
    private String height = "";
    private String weight = "";
    private String age = "";
    private BmiModel.Gender gender;

    @Override
    protected void attachBaseContext(Context base) {
        // 创建界面前应用语言设置，后续 getString() 会读取对应的资源文件。
        String language = base.getSharedPreferences(BmiStorage.PREFERENCES, MODE_PRIVATE)
                .getString("language", "");
        if (language.isEmpty()) {
            super.attachBaseContext(base);
        } else {
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.setLocale(Locale.forLanguageTag(language));
            super.attachBaseContext(base.createConfigurationContext(configuration));
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        storage = new BmiStorage(this);
        landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        BmiStorage.Draft draft = storage.draft();
        height = draft.height();
        age = draft.age();
        gender = draft.gender();
        if (state != null) {
            restore(state);
        }

        LinearLayout root = column();
        root.setBackgroundColor(BACKGROUND);
        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitleTextColor(TEXT);
        toolbar.setContentInsetsRelative(dp(18), dp(12));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(52)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        page = column();
        page.setPadding(dp(16), dp(6), dp(16), dp(18));
        page.setFocusableInTouchMode(true);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(root);
        setActionBar(toolbar);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (screen == Screen.ENTRY) {
                    finish();
                } else {
                    navigateBack();
                }
            }
        });

        controller = ControllerFactory.create(this, new BmiModel(storage), result -> {
            report = result;
            navigate(Screen.REPORT);
        });
        render();
    }

    @Override
    protected void onPause() {
        readForm();
        storage.saveDraft(height, age, gender);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        storage.close();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        // 横竖屏或切换语言会重建 Activity。这里保留页面、报告与未提交的体重。
        super.onSaveInstanceState(state);
        readForm();
        state.putString("screen", screen.name());
        state.putString("historyOrigin", historyOrigin.name());
        state.putString("height", height);
        state.putString("weight", weight);
        state.putString("age", age);
        state.putString("gender", gender == null ? "" : gender.name());
        if (report != null) {
            state.putDouble("bmi", report.bmi());
            state.putString("category", report.category().name());
            state.putInt("reference", report.reference());
        }
    }

    private void restore(Bundle state) {
        screen = Screen.valueOf(state.getString("screen", Screen.ENTRY.name()));
        historyOrigin = Screen.valueOf(state.getString("historyOrigin", Screen.ENTRY.name()));
        height = state.getString("height", height);
        weight = state.getString("weight", weight);
        age = state.getString("age", age);
        String selected = state.getString("gender", "");
        gender = selected.isEmpty() ? null : BmiModel.Gender.valueOf(selected);
        String category = state.getString("category");
        if (category != null) {
            report = new BmiModel.Report(state.getDouble("bmi"),
                    BmiModel.Category.valueOf(category), state.getInt("reference"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 右上角菜单。每个条目的行为在 onOptionsItemSelected() 中。
        menu.add(0, MENU_HOME, 0, R.string.new_entry);
        menu.add(0, MENU_HISTORY, 1, R.string.history);
        SubMenu languages = menu.addSubMenu(R.string.language);
        languages.add(0, MENU_ENGLISH, 0, R.string.english);
        languages.add(0, MENU_CHINESE, 1, R.string.chinese);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home -> navigateBack();
            case MENU_HOME -> navigate(Screen.ENTRY);
            case MENU_HISTORY -> navigate(Screen.HISTORY);
            case MENU_ENGLISH -> setLanguage("en");
            case MENU_CHINESE -> setLanguage("zh");
            default -> { return super.onOptionsItemSelected(item); }
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        // 长按报告时显示，注册长按目标的位置在 showReport()。
        super.onCreateContextMenu(menu, view, info);
        if (report != null) {
            menu.setHeaderTitle(R.string.result_title);
            menu.add(0, MENU_COPY, 0, R.string.copy);
            menu.add(0, MENU_HISTORY, 1, R.string.history);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_COPY && report != null) {
            ClipboardManager clipboard = getSystemService(ClipboardManager.class);
            clipboard.setPrimaryClip(ClipData.newPlainText("BMI", bmiText(report.bmi())));
            if (android.os.Build.VERSION.SDK_INT < 33) {
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (item.getItemId() == MENU_HISTORY) {
            navigate(Screen.HISTORY);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void setLanguage(String language) {
        getSharedPreferences(BmiStorage.PREFERENCES, MODE_PRIVATE).edit()
                .putString("language", language).apply();
        recreate();
    }

    private void navigateBack() {
        navigate(screen == Screen.HISTORY ? historyOrigin : Screen.ENTRY);
    }

    private void navigate(Screen destination) {
        if (screen == destination) {
            return;
        }
        if (destination == Screen.HISTORY) {
            historyOrigin = screen;
        }
        readForm();
        getSystemService(InputMethodManager.class)
                .hideSoftInputFromWindow(page.getWindowToken(), 0);
        screen = destination;
        render();
        scroll.scrollTo(0, 0);
    }

    private void render() {
        page.removeAllViews();
        heightField = null;
        weightField = null;
        ageField = null;
        genderField = null;
        int title = switch (screen) {
            case ENTRY -> R.string.new_entry;
            case REPORT -> R.string.report;
            case HISTORY -> R.string.history;
        };
        if (getActionBar() != null) {
            getActionBar().setTitle(title);
            getActionBar().setDisplayHomeAsUpEnabled(screen != Screen.ENTRY);
            getActionBar().setHomeActionContentDescription(R.string.back);
        }
        switch (screen) {
            case ENTRY -> showEntry();
            case REPORT -> showReport();
            case HISTORY -> showHistory();
        }
        page.requestFocus();
    }

    private void showEntry() {
        // 这里控制输入项、单位文字和输入页的两个按钮。
        LinearLayout form = card(SURFACE);
        LinearLayout firstRow = row();
        heightField = input(firstRow, R.string.height, height, true);
        weightField = input(firstRow, R.string.weight, weight, true);
        form.addView(firstRow);

        LinearLayout ageRow = row();
        ageField = input(ageRow, R.string.age, age, false);
        form.addView(ageRow);

        TextView genderLabel = text(R.string.gender, 14, MUTED, true);
        add(form, genderLabel, 8);
        genderField = new RadioGroup(this);
        genderField.setOrientation(RadioGroup.HORIZONTAL);
        maleId = View.generateViewId();
        femaleId = View.generateViewId();
        addGender(R.string.male, maleId);
        addGender(R.string.female, femaleId);
        if (gender != null) {
            genderField.check(gender == BmiModel.Gender.MALE ? maleId : femaleId);
        }
        form.addView(genderField);
        add(form, button(R.string.calculate, true, view -> submit()), 10);
        add(form, button(R.string.view_trend, false, view -> navigate(Screen.HISTORY)), 6);

        LinearLayout explanation = card(GREEN_LIGHT);
        explanation.addView(text(R.string.result_title, 23, GREEN, true));
        add(explanation, text(R.string.result_hint, 15, TEXT, false), 6);
        place(form, explanation);
    }

    private void showReport() {
        // 这里调整结果文字、严重等级的配色以及长按区域。
        LinearLayout result = card(report.isSevere() ? WARNING_LIGHT : GREEN_LIGHT);
        int accent = report.isSevere() ? WARNING : GREEN;
        result.addView(text(R.string.result_title, 15, accent, true));
        TextView score = text(0, 56, accent, true);
        score.setText(bmiText(report.bmi()));
        result.addView(score);
        add(result, text(report.category().label, 23, accent, true), 2);
        if (report.isSevere()) {
            add(result, text(R.string.severe_warning, 15, WARNING, true), 8);
        }
        add(result, text(report.reference(), 14, TEXT, false), 12);
        add(result, text(R.string.context_hint, 13, MUTED, false), 10);
        registerForContextMenu(result);
        for (int i = 0; i < result.getChildCount(); i++) {
            registerForContextMenu(result.getChildAt(i));
        }

        LinearLayout actions = card(SURFACE);
        actions.addView(button(R.string.calculate_again, true, view -> navigate(Screen.ENTRY)));
        add(actions, button(R.string.view_trend, false, view -> navigate(Screen.HISTORY)), 8);
        place(result, actions);
    }

    private void showHistory() {
        // 仓库按日期升序查询。图按升序绘制，下面的列表从最新一天开始显示。
        List<BmiStorage.Entry> entries = storage.history();
        LinearLayout graph = card(SURFACE);
        graph.addView(text(R.string.daily_latest, 17, TEXT, true));
        if (entries.isEmpty()) {
            add(graph, text(R.string.empty, 15, MUTED, false), 12);
            page.addView(graph, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        TrendChart chart = new TrendChart(this, entries, locale(), GREEN, MUTED);
        chart.setContentDescription(getString(R.string.chart_description, entries.size(),
                bmiText(entries.get(entries.size() - 1).bmi())));
        chart.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        graph.addView(chart, new LinearLayout.LayoutParams(-1, dp(landscape ? 175 : 220)));

        LinearLayout saved = card(SURFACE);
        saved.addView(text(R.string.measurements, 17, TEXT, true));
        DateTimeFormatter dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale());
        for (int i = entries.size() - 1; i >= 0; i--) {
            BmiStorage.Entry entry = entries.get(i);
            LinearLayout line = row();
            TextView day = text(0, 15, MUTED, false);
            day.setText(dates.format(entry.date()));
            line.addView(day, new LinearLayout.LayoutParams(0, -2, 1));
            TextView value = text(0, 15, GREEN, true);
            value.setText(getString(R.string.entry_value, bmiText(entry.bmi())));
            line.addView(value);
            add(saved, line, 12);
        }
        place(graph, saved);
    }

    private void place(View first, View second) {
        // 两张卡片在竖屏上下排，在横屏左右排。三个页面共用此布局方法。
        if (landscape) {
            LinearLayout split = row();
            LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
            left.setMarginEnd(dp(12));
            split.addView(first, left);
            split.addView(second, new LinearLayout.LayoutParams(0, -2, 1));
            page.addView(split, new LinearLayout.LayoutParams(-1, -2));
        } else {
            page.addView(first, new LinearLayout.LayoutParams(-1, -2));
            add(page, second, 12);
        }
    }

    private void addGender(int label, int id) {
        RadioButton option = new RadioButton(this);
        option.setId(id);
        option.setText(label);
        option.setTextColor(TEXT);
        option.setMinHeight(dp(44));
        genderField.addView(option, new RadioGroup.LayoutParams(0, -2, 1));
    }

    private EditText input(LinearLayout row, int label, String value, boolean decimal) {
        LinearLayout wrap = column();
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
        if (row.getChildCount() > 0) {
            size.setMarginStart(dp(12));
        }
        row.addView(wrap, size);
        TextView caption = text(label, 14, MUTED, true);
        EditText field = new EditText(this);
        field.setId(View.generateViewId());
        caption.setLabelFor(field.getId());
        wrap.addView(caption);
        field.setInputType(InputType.TYPE_CLASS_NUMBER
                | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        field.setSingleLine();
        field.setTextColor(TEXT);
        field.setTextSize(19);
        field.setText(value);
        field.setMinHeight(dp(48));
        wrap.addView(field, new LinearLayout.LayoutParams(-1, -2));
        return field;
    }

    private void submit() {
        // 先检查输入，再进入 controller。横屏重建和语言切换不会执行这条保存路径。
        readForm();
        double centimetres = number(height);
        double kilograms = number(weight);
        int years;
        try {
            years = Integer.parseInt(age);
        } catch (NumberFormatException failure) {
            years = 0;
        }
        if (!(centimetres >= 30 && centimetres <= 300)) {
            invalid(heightField, R.string.invalid_height);
        } else if (!(kilograms >= 1 && kilograms <= 700)) {
            invalid(weightField, R.string.invalid_weight);
        } else if (years < 1 || years > 120) {
            invalid(ageField, R.string.invalid_age);
        } else if (gender == null) {
            Toast.makeText(this, R.string.invalid_gender, Toast.LENGTH_SHORT).show();
        } else {
            storage.saveDraft(height, age, gender);
            getSystemService(InputMethodManager.class)
                    .hideSoftInputFromWindow(page.getWindowToken(), 0);
            controller.submit(new BmiModel.Input(centimetres, kilograms, years, gender));
        }
    }

    private void readForm() {
        if (heightField == null) {
            return;
        }
        height = heightField.getText().toString().trim();
        weight = weightField.getText().toString().trim();
        age = ageField.getText().toString().trim();
        int id = genderField.getCheckedRadioButtonId();
        gender = id == maleId ? BmiModel.Gender.MALE
                : id == femaleId ? BmiModel.Gender.FEMALE : null;
    }

    private static double number(String input) {
        try {
            return Double.parseDouble(input.replace(',', '.'));
        } catch (NumberFormatException failure) {
            return Double.NaN;
        }
    }

    private void invalid(EditText field, int message) {
        field.requestFocus();
        field.setError(getString(message));
    }

    private Button button(int label, boolean primary, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setMinHeight(dp(48));
        button.setTextColor(primary ? SURFACE : GREEN);
        button.setBackgroundTintList(ColorStateList.valueOf(primary ? GREEN : GREEN_LIGHT));
        button.setOnClickListener(click);
        return button;
    }

    private TextView text(int label, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        if (label != 0) {
            view.setText(label);
        }
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout card(int color) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(18));
        card.setBackground(background);
        return card;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.TOP);
        return layout;
    }

    private void add(LinearLayout layout, View view, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topMargin);
        layout.addView(view, params);
    }

    private Locale locale() {
        return getResources().getConfiguration().getLocales().get(0);
    }

    private String bmiText(double bmi) {
        return String.format(locale(), "%.1f", bmi);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
