package gui.employee;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;

public class ParkUsageReportViewController {

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private TextArea reportArea;

    @FXML
    private BarChart<String, Number> usageChart;

    @FXML
    public void handleGenerateReport(ActionEvent event) {
        reportArea.setText(
                "Usage Report\n\n" +
                "2026-06-20\n" +
                "Visitors: 0\n" +
                "Usage: 0%");

        usageChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Usage %");
        series.getData().add(new XYChart.Data<>("2026-06-20", 0));

        usageChart.getData().add(series);
    }
}