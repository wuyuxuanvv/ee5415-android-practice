package com.practice.bmi;

import androidx.activity.ComponentActivity;

import java.util.function.Consumer;

final class ControllerFactory {
    static BmiController create(ComponentActivity owner, BmiModel model,
                                Consumer<BmiModel.Report> view) {
        return new BmiPresenter(model, view);
    }

    private record BmiPresenter(BmiModel model, Consumer<BmiModel.Report> view)
            implements BmiController {
        // Presenter 接收提交，调用 Model，然后直接通过 View 回调显示结果。
        @Override
        public void submit(BmiModel.Input input) {
            view.accept(model.calculate(input));
        }
    }
}
