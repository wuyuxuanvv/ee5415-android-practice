package com.practice.bmi;

import java.time.LocalDate;

// 这里负责运算和分级。切换界面架构时，公式和参考表继续走同一套逻辑。
final class BmiModel {
    enum Gender { MALE, FEMALE }

    enum Category {
        UNDERWEIGHT(R.string.underweight),
        NORMAL(R.string.normal),
        ACCEPTABLE(R.string.acceptable),
        OVERWEIGHT(R.string.overweight),
        OBESE(R.string.obese),
        SEVERE_UNDER(R.string.severe_under),
        SEVERE_OVER(R.string.severe_over),
        UNAVAILABLE(R.string.unavailable);

        final int label;

        Category(int label) {
            this.label = label;
        }
    }

    record Input(double height, double weight, int age, Gender gender) {}

    record Report(double bmi, Category category, int reference) {
        boolean isSevere() {
            return category == Category.SEVERE_UNDER || category == Category.SEVERE_OVER;
        }
    }

    // Each row gives the inclusive maximum, in tenths, of the first four categories.
    // Rows correspond to ages 6 through 18 in the HPB child chart.
    private static final int[][] BOYS = {
            {128, 131, 188, 214}, {130, 133, 198, 230}, {132, 136, 209, 246},
            {135, 138, 218, 260}, {138, 141, 227, 273}, {141, 145, 236, 283},
            {144, 148, 243, 292}, {147, 151, 250, 300}, {150, 154, 255, 306},
            {153, 158, 261, 312}, {156, 161, 265, 317}, {159, 163, 270, 321},
            {161, 166, 274, 324}
    };

    private static final int[][] GIRLS = {
            {126, 128, 183, 205}, {128, 131, 191, 218}, {131, 134, 201, 231},
            {134, 137, 210, 244}, {137, 141, 219, 256}, {141, 144, 227, 266},
            {144, 148, 234, 275}, {148, 152, 240, 283}, {151, 155, 246, 289},
            {154, 158, 250, 294}, {157, 161, 254, 297}, {159, 163, 257, 300},
            {161, 165, 259, 303}
    };

    private final BmiStorage storage;

    BmiModel(BmiStorage storage) {
        this.storage = storage;
    }

    Report calculate(Input input) {
        Report report = assess(input);
        // 日期按设备本地时间取值，数据库用日期覆盖同日的旧值。
        storage.saveResult(LocalDate.now(), report.bmi());
        return report;
    }

    static Report assess(Input input) {
        double metres = input.height() / 100;
        // 分级和显示用同一个一位小数值。用整数十分位比较儿童表的边界。
        long tenths = Math.round(10 * input.weight() / (metres * metres));
        double bmi = tenths / 10.0;
        if (input.age() > 18) {
            Category category;
            if (tenths < 185) {
                category = Category.UNDERWEIGHT;
            } else if (tenths < 230) {
                category = Category.NORMAL;
            } else if (tenths < 250) {
                category = Category.OVERWEIGHT;
            } else {
                category = Category.OBESE;
            }
            return new Report(bmi, category, R.string.adult_reference);
        }
        if (input.age() < 6) {
            // 儿童参考表从 6 岁起。更小的年龄仍可查看计算值。
            return new Report(bmi, Category.UNAVAILABLE, R.string.no_reference);
        }
        int[] limits = (input.gender() == Gender.MALE ? BOYS : GIRLS)[input.age() - 6];
        Category category;
        if (tenths <= limits[0]) {
            category = Category.SEVERE_UNDER;
        } else if (tenths <= limits[1]) {
            category = Category.UNDERWEIGHT;
        } else if (tenths <= limits[2]) {
            category = Category.ACCEPTABLE;
        } else if (tenths <= limits[3]) {
            category = Category.OVERWEIGHT;
        } else {
            category = Category.SEVERE_OVER;
        }
        return new Report(bmi, category, R.string.child_reference);
    }
}
