package com.practice.bmi;

import androidx.activity.ComponentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.function.Consumer;

final class ControllerFactory {
    static BmiController create(ComponentActivity owner, BmiModel model,
                                Consumer<BmiModel.Report> view) {
        ReportViewModel viewModel = new ViewModelProvider(owner).get(ReportViewModel.class);
        // View 观察 LiveData。报告交给 Activity 保存到 Bundle，消费后清除观察值。
        viewModel.report.observe(owner, result -> {
            if (result != null) {
                view.accept(result);
                viewModel.report.setValue(null);
            }
        });
        return input -> viewModel.submit(model, input);
    }

    public static final class ReportViewModel extends ViewModel {
        private final MutableLiveData<BmiModel.Report> report = new MutableLiveData<>();

        void submit(BmiModel model, BmiModel.Input input) {
            report.setValue(model.calculate(input));
        }
    }
}
