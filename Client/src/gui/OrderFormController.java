package gui;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ResourceBundle;

import client.ParkClient;
import client.ServerResponseListener;
import common.Message;
import common.Order;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OrderFormController implements Initializable {

    private Order order;
    private ParkClient client;

    @FXML private TextField txtOrderNumber;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> spinnerVisitors;
    @FXML private TextField txtConfirmationCode;
    @FXML private TextField txtSubscriberId;
    @FXML private TextField txtDateOfPlacingOrder;
    @FXML private Label lblStatus;
    @FXML private Button btnSave;
    @FXML private Button btnClose;

    public void loadOrder(Order o, ParkClient parkClient) {
        this.order = o;
        this.client = parkClient;

        txtOrderNumber.setText(String.valueOf(o.getOrderNumber()));
        txtConfirmationCode.setText(String.valueOf(o.getConfirmationCode()));
        txtSubscriberId.setText(String.valueOf(o.getSubscriberId()));
        txtDateOfPlacingOrder.setText(
            o.getDateOfPlacingOrder() != null ? o.getDateOfPlacingOrder().toString() : ""
        );

        // Set DatePicker value
        if (o.getOrderDate() != null)
            datePicker.setValue(o.getOrderDate().toLocalDate());

        // Set Spinner value
        spinnerVisitors.getValueFactory().setValue(o.getNumberOfVisitors());

        // Read only fields
        txtOrderNumber.setEditable(false);
        txtConfirmationCode.setEditable(false);
        txtSubscriberId.setEditable(false);
        txtDateOfPlacingOrder.setEditable(false);
    }

    public void Save(ActionEvent event) {
        LocalDate selectedDate = datePicker.getValue();
        int newVisitors = spinnerVisitors.getValue();

        if (selectedDate == null) {
            lblStatus.setText("Please select a date.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }
        
        if (selectedDate.isBefore(LocalDate.now())) {
            lblStatus.setText("Please select a future date.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        if (newVisitors < 1 || newVisitors > 15) {
            lblStatus.setText("Number of visitors must be between 1 and 15.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        Date newDate = Date.valueOf(selectedDate);

        Order updatedOrder = new Order(
            order.getOrderNumber(),
            newDate,
            newVisitors,
            order.getConfirmationCode(),
            order.getSubscriberId(),
            order.getDateOfPlacingOrder()
        );

        client.setListener(new ServerResponseListener() {
            @Override
            public void onOrderExistsResult(boolean exists) {}

            @Override
            public void onOrderResult(Order order) {}

            @Override
            public void onUpdateOrderResult(boolean success) {
                Platform.runLater(() -> {
                    if (success) {
                        lblStatus.setText("Order updated successfully.");
                        lblStatus.setStyle("-fx-text-fill: green;");
                    } else {
                        lblStatus.setText("ERROR: Update failed.");
                        lblStatus.setStyle("-fx-text-fill: red;");
                    }
                });
            }

            @Override
            public void onError(String errorMsg) {
                Platform.runLater(() -> {
                    lblStatus.setText("Server error: " + errorMsg);
                    lblStatus.setStyle("-fx-text-fill: red;");
                });
            }
        });

        try {
            client.sendToServer(new Message("UPDATE_ORDER", updatedOrder));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeWindow(ActionEvent event) throws Exception {
        ((Node) event.getSource()).getScene().getWindow().hide();
        Stage primaryStage = new Stage();

        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ParkClientView.fxml"));
        Parent root = loader.load();
        ParkClientController controller = loader.getController();
        controller.setClient(client);

        Scene scene = new Scene(root);
        java.net.URL css = getClass().getResource("/gui/ParkClientView.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        primaryStage.setTitle("Park Management Tool");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Spinner range 1 to 15, default 1
        SpinnerValueFactory<Integer> valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 15, 1);
        spinnerVisitors.setValueFactory(valueFactory);
        datePicker.getEditor().setEditable(false);
        lblStatus.setText("");
    }
}