package client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import common.VisitorLoginResult;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import ocsf.client.AbstractClient;

public class ParkClient extends AbstractClient {

    private ServerResponseListener listener;
    private boolean intentionalDisconnect = false;

    private ParkClient(String host, int port) throws IOException {
        super(host, port);
    }

    private static ParkClient instance;

    public static synchronized void connect(String host, int port) throws IOException {
        if (instance != null && instance.isConnected()) {
            return;
        }
        ParkClient newInstance = new ParkClient(host, port);
        newInstance.openConnection();
        instance = newInstance;
    }

    public static ParkClient getInstance() {
        return instance;
    }

    private static synchronized void clearInstance(ParkClient client) {
        if (instance == client) {
            instance = null;
        }
    }

    public void setListener(ServerResponseListener listener) {
        this.listener = listener;
    }

    public void setIntentionalDisconnect() {
        this.intentionalDisconnect = true;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Message && listener != null) {
            Message message = (Message) msg;
            switch (message.getCommand()) {
            case "VISITS_REPORT_RESULT":
                listener.onVisitsReportResult(
                    (java.util.ArrayList<common.VisitsReportResult>) message.getData()
                );
                break;
                case "ORDER_EXISTS_RESULT":
                    listener.onOrderExistsResult((boolean) message.getData());
                    break;
                case "ORDER_RESULT":
                    listener.onOrderResult((Order) message.getData());
                    break;
                case "UPDATE_ORDER_RESULT":
                    listener.onUpdateOrderResult((boolean) message.getData());
                    break;
                case "VISITOR_LOGIN_RESULT":
                    listener.onVisitorLoginResult((VisitorLoginResult) message.getData());
                    break;
                case "TRAVELER_BOOKINGS_RESULT":
                    listener.onTravelerBookingsResult((ArrayList<Booking>) message.getData());
                    break;
                case "PARK_PRICES_RESULT":
                    listener.onParkPricesResult((Map<Integer, Integer>) message.getData());
                    break;
                case "CREATE_BOOKING_RESULT":
                    listener.onCreateBookingResult((Booking) message.getData());
                    break;
                case "CREATE_BOOKING_REQUIRES_WAITLIST_CONFIRMATION":
                    Object[] waitlistPayload = (Object[]) message.getData();
                    listener.onCreateBookingRequiresWaitlistConfirmation((Booking) waitlistPayload[0], (String) waitlistPayload[1]);
                    break;
                case "UPDATE_BOOKING_RESULT":
                    listener.onUpdateBookingResult((boolean) message.getData());
                    break;
                case "CANCEL_BOOKING_RESULT":
                    listener.onCancelBookingResult((boolean) message.getData());
                    break;
                case "CONFIRM_BOOKING_RESULT":
                    listener.onConfirmBookingResult((boolean) message.getData());
                    break;
                case "EMPLOYEE_LOGIN_SUCCESS":
                    listener.onEmployeeLoginSuccess((Employee) message.getData());
                    break;
                case "EMPLOYEE_LOGIN_FAILED":
                    listener.onEmployeeLoginFailed((String) message.getData());
                    break;
                case "FORCE_LOGOUT":
                    handleForceLogout((String) message.getData());
                    break;
                case "ERROR":
                    listener.onError((String) message.getData());
                    break;

                case "PARK_VISITORS_RESULT":
                    listener.onParkVisitorsResult((int) message.getData());
                    break;
                case "EFFECTIVE_AVAILABLE_SPOTS_RESULT":
                    listener.onEffectiveAvailableSpotsResult((int) message.getData());
                    break;
                case "BOOKING_RESULT":
                    listener.onBookingResult((common.Booking) message.getData());
                    break;
                case "CHECK_IN_RESULT":
                    listener.onCheckInResult((boolean) message.getData());
                    break;
                case "CHECK_OUT_RESULT":
                    listener.onCheckOutResult((boolean) message.getData());
                    break;
                case "WALK_IN_RESULT":
                    listener.onWalkInResult((common.Booking) message.getData());
                    break;
                case "TODAY_BOOKINGS_RESULT":
                    listener.onTodayBookingsResult((java.util.ArrayList<common.Booking>) message.getData());
                    break;    
                    
                  
            }
        }
    }

    @Override
    protected void connectionClosed() {
        clearInstance(this);
        if (!intentionalDisconnect) {
            System.out.println("Server closed the connection.");
            showServerDisconnectedAlert("The server has shut down.");
        }
    }

    @Override
    protected void connectionException(Exception exception) {
        clearInstance(this);
        if (!intentionalDisconnect) {
            System.out.println("Lost connection to server: " + exception.getMessage());
            showServerDisconnectedAlert("Connection to server was lost.");
        }
    }

    private void showServerDisconnectedAlert(String reason) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Server Disconnected");
            alert.setHeaderText("Connection Lost");
            alert.setContentText(reason + "\nPlease restart the client.");
            alert.showAndWait();
            System.exit(0);
        });
    }

    private void handleForceLogout(String reason) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Logged Out");
            alert.setHeaderText("You have been logged out");
            alert.setContentText(reason + "\nPlease log in again.");
            alert.showAndWait();

            SessionManager.getInstance().logout();
            this.setIntentionalDisconnect();
            try {
                this.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        });
    }
}
