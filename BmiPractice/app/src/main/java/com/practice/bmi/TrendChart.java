package com.practice.bmi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// 自定义 View 负责绘图，历史数据和界面导航由调用者提供。
final class TrendChart extends View {
    private static final int GRID = 0xffdbe5df;
    private final List<BmiStorage.Entry> entries;
    private final DateTimeFormatter dateFormat;
    private final Locale locale;
    private final int accent;
    private final int muted;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    TrendChart(Context context, List<BmiStorage.Entry> entries, Locale locale, int accent, int muted) {
        super(context);
        this.entries = entries;
        this.locale = locale;
        this.accent = accent;
        this.muted = muted;
        dateFormat = DateTimeFormatter.ofPattern("M/d", locale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(38);
        float right = getWidth() - dp(8);
        float top = dp(22);
        float bottom = getHeight() - dp(28);
        if (right <= left || bottom <= top) {
            return;
        }

        double minimum = entries.get(0).bmi();
        double maximum = minimum;
        for (BmiStorage.Entry entry : entries) {
            minimum = Math.min(minimum, entry.bmi());
            maximum = Math.max(maximum, entry.bmi());
        }
        // 用四个纵轴间隔覆盖数据范围，一个记录也能显示为独立的点。
        double step = Math.max(1, Math.ceil((maximum - minimum + 1) / 4));
        double floor = Math.max(0, Math.floor((minimum - 0.5) / step) * step);
        if (floor + step * 4 < maximum) {
            step = Math.ceil((maximum - floor) / 4);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(dp(11));
        paint.setStrokeWidth(dp(1));
        for (int i = 0; i <= 4; i++) {
            float y = bottom - i * (bottom - top) / 4;
            paint.setColor(GRID);
            canvas.drawLine(left, y, right, y, paint);
            paint.setColor(muted);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format(locale, "%.0f", floor + i * step),
                    left - dp(6), y + dp(4), paint);
        }

        BmiStorage.Entry first = entries.get(0);
        BmiStorage.Entry last = entries.get(entries.size() - 1);
        long firstDay = first.date().toEpochDay();
        long span = last.date().toEpochDay() - firstDay;
        Path path = new Path();
        for (int i = 0; i < entries.size(); i++) {
            BmiStorage.Entry entry = entries.get(i);
            float x = x(entry, firstDay, span, left, right);
            float y = y(entry, floor, step, top, bottom);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        paint.setColor(accent);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(dp(2.5f));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        for (BmiStorage.Entry entry : entries) {
            canvas.drawCircle(x(entry, firstDay, span, left, right),
                    y(entry, floor, step, top, bottom), dp(3.5f), paint);
        }

        paint.setColor(muted);
        if (span == 0) {
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(dateFormat.format(first.date()), (left + right) / 2, bottom + dp(19), paint);
        } else {
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(dateFormat.format(first.date()), left, bottom + dp(19), paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(dateFormat.format(last.date()), right, bottom + dp(19), paint);
        }
    }

    private static float x(BmiStorage.Entry entry, long first, long span, float left, float right) {
        // 按真实日期间隔定位。例如隔三天的两点，间距是隔一天的三倍。
        if (span == 0) {
            return (left + right) / 2;
        }
        return left + (right - left) * (entry.date().toEpochDay() - first) / span;
    }

    private static float y(BmiStorage.Entry entry, double floor, double step, float top, float bottom) {
        return bottom - (float) ((entry.bmi() - floor) / (step * 4)) * (bottom - top);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
