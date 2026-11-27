package com.practice.bmi;

import androidx.activity.ComponentActivity;

import java.util.function.Consumer;

final class ControllerFactory {
    static BmiController create(ComponentActivity owner, BmiModel model,
                                Consumer<BmiModel.Report> view) {
        return new BmiStore(model, state -> view.accept(state.report()));
    }

    private record CalculateIntent(BmiModel.Input input) {}
    private record ReportState(BmiModel.Report report) {}

    private record BmiStore(BmiModel model, Consumer<ReportState> render)
            implements BmiController {
        @Override
        public void submit(BmiModel.Input input) {
            dispatch(new CalculateIntent(input));
        }

        private void dispatch(CalculateIntent intent) {
            // 提交变为 Intent，Store 生成新 State，最后把 State 交给界面渲染。
            render.accept(new ReportState(model.calculate(intent.input())));
        }
    }
}
