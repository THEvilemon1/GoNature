package client;

import java.io.IOException;

import java.util.ArrayList;

import common.ParkSubmittedReport;
import common.ParkChangeRequest;
import common.Booking;
import common.Employee;
import common.Message;
import common.Order;
import common.ParkOption;
import common.VisitorLoginResult;
import common.Promotion;
import common.PromotionRequest;
import common.ParkVisitorsReportResult;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import ocsf.client.AbstractClient;


public class ParkClient extends AbstractClient {

    private ServerResponseListener listener;
    // Unsolicited notifications for the department manager's overview (live visitor
    // counts, pending change/promotion requests) are delivered here instead of through
    // the regular request/response listener. The overview is a long-lived screen, but
    // the transient report windows it opens call setListener() and would otherwise take
    // over the only listener, causing the manager to stop receiving live pushes.
    private ServerResponseListener notificationListener;
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

    /**
     * Registers a listener for unsolicited server notifications pushed to the
     * department manager (live visitor counts and pending requests). Unlike
     * {@link #setListener}, this is not replaced when other screens swap the
     * regular listener, so the manager keeps receiving live updates.
     */
    public void setNotificationListener(ServerResponseListener notificationListener) {
        this.notificationListener = notificationListener;
    }

    public void clearNotificationListener(ServerResponseListener notificationListener) {
        if (this.notificationListener == notificationListener) {
            this.notificationListener = null;
        }
    }

    public void setIntentionalDisconnect() {
        this.intentionalDisconnect = true;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (!(msg instanceof Message)) return;
        Message message = (Message) msg;

        // Server-initiated notifications for the department manager's overview are
        // delivered to a dedicated listener that transient screens never replace,
        // so live visitor counts keep updating even while a report window is open.
        if (notificationListener != null) {
            switch (message.getCommand()) {
                case "ALL_PARKS_VISITORS_RESULT":
                    notificationListener.onAllParksVisitorsResult(
                        (ArrayList<common.ParkVisitorsCount>) message.getData());
                    return;
                case "PARK_CHANGE_REQUEST_NOTIFICATION":
                    notificationListener.onParkChangeRequestNotification(
                        (ParkChangeRequest) message.getData());
                    return;
                case "PROMOTION_REQUEST_NOTIFICATION":
                    notificationListener.onPromotionRequestNotification(
                        (PromotionRequest) message.getData());
                    return;
                default:
                    break;
            }
        }

        if (listener != null) {
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
                case "PARKS_RESULT":
                    listener.onParksResult((ArrayList<ParkOption>) message.getData());
                    break;
                case "TRAVELER_PROFILE_RESULT":
                    listener.onTravelerProfileResult((common.TravelerProfile) message.getData());
                    break;
                case "UPDATE_TRAVELER_PROFILE_RESULT":
                    listener.onUpdateTravelerProfileResult((boolean) message.getData());
                    break;
                case "CREATE_BOOKING_RESULT":
                    listener.onCreateBookingResult((Booking) message.getData());
                    break;
                case "CREATE_BOOKING_REQUIRES_WAITLIST_CONFIRMATION":
                    Object[] waitlistPayload = (Object[]) message.getData();
                    listener.onCreateBookingRequiresWaitlistConfirmation((Booking) waitlistPayload[0], (String) waitlistPayload[1]);
                    break;
                case "UPDATE_BOOKING_RESULT":
                    listener.onUpdateBookingResult((Booking) message.getData());
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
                case "PARK_SETTINGS_RESULT":
                    Object[] parkSettings = (Object[]) message.getData();
                    listener.onParkSettingsResult((Integer) parkSettings[0], (Integer) parkSettings[1], (Integer) parkSettings[2]);
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
                case "PARK_CHANGE_REQUEST_RESULT":
                    listener.onParkChangeRequestResult((boolean) message.getData());
                    break;

                case "PARK_CHANGE_APPROVAL_RESULT":
                    Object[] approvalData = (Object[]) message.getData();
                    listener.onParkChangeApprovalResult((String) approvalData[0], (boolean) approvalData[1]);
                    break;

                case "PARK_CHANGE_REQUEST_NOTIFICATION":
                    ParkChangeRequest incomingRequest = (ParkChangeRequest) message.getData();
                    listener.onParkChangeRequestNotification(incomingRequest);
                    break;

                case "PROMOTIONS_LIST_RESULT":
                    listener.onPromotionsListResult((java.util.ArrayList<Promotion>) message.getData());
                    break;

                case "PROMOTION_REQUEST_RESULT":
                    listener.onPromotionRequestResult((boolean) message.getData());
                    break;

                case "PROMOTION_APPROVAL_RESULT":
                    Object[] promoApprovalData = (Object[]) message.getData();
                    listener.onPromotionApprovalResult((String) promoApprovalData[0], (boolean) promoApprovalData[1]);
                    break;

                case "PROMOTION_REQUEST_NOTIFICATION":
                    listener.onPromotionRequestNotification((PromotionRequest) message.getData());
                    break;
                case "PARK_VISITORS_REPORT_RESULT":
                    listener.onParkVisitorsReportResult((ParkVisitorsReportResult) message.getData());
                    break;
                case "PARK_USAGE_REPORT_RESULT":
                    listener.onParkUsageReportResult((ArrayList<common.ParkUsageReportResult>) message.getData());
                    break;
                case "SUBMITTED_REPORTS_RESULT":
                    listener.onSubmittedReportsResult((ArrayList<ParkSubmittedReport>) message.getData());
                    break;
                    
                case "ALL_PARKS_VISITORS_RESULT":
                    listener.onAllParksVisitorsResult(
                        (ArrayList<common.ParkVisitorsCount>) message.getData());
                    break;

                case "CANCELLATIONS_REPORT_RESULT":
                    listener.onCancellationsReportResult(
                        (ArrayList<common.CancellationsReportResult>) message.getData());
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
