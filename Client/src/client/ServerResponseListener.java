package client;

import java.util.ArrayList;

import common.ParkSubmittedReport;
import common.Booking;
import common.Employee;
import common.Order;
import common.TravelerProfile;
import common.ParkOption;
import java.util.Map;
import common.VisitorLoginResult;
import common.ParkChangeRequest;
import common.Promotion;
import common.PromotionRequest;
import common.ParkVisitorsReportResult;

public interface ServerResponseListener {
    void onOrderExistsResult(boolean exists);
    void onOrderResult(Order order);
    void onUpdateOrderResult(boolean success);
    void onError(String errorMessage);

    default void onVisitorLoginResult(VisitorLoginResult result) {}
    default void onTravelerBookingsResult(ArrayList<Booking> bookings) {}
    default void onParkPricesResult(Map<Integer, Double> pricesByParkId) {}
    default void onTravelerProfileResult(TravelerProfile profile) {}
    default void onUpdateTravelerProfileResult(boolean success) {}
    default void onParksResult(ArrayList<ParkOption> parks) {}
    default void onCreateBookingResult(Booking booking) {}
    default void onCreateBookingRequiresWaitlistConfirmation(Booking booking, String message) {}
    default void onUpdateBookingResult(Booking booking) {}
    default void onCancelBookingResult(boolean success) {}
    default void onParkVisitorsReportResult(ParkVisitorsReportResult result) {}
    default void onParkUsageReportResult(java.util.ArrayList<common.ParkUsageReportResult> results) {}
    default void onConfirmBookingResult(boolean success) {}

    // Employee login
    default void onEmployeeLoginSuccess(Employee employee) {}
    default void onEmployeeLoginFailed(String reason) {}

    // Park worker operations
    default void onParkVisitorsResult(int currentVisitors) {}
    default void onEffectiveAvailableSpotsResult(int spots) {}
    default void onParkSettingsResult(Integer maxCapacity, Integer gap, Integer defaultStayTime) {}
    default void onBookingResult(Booking booking) {}
    default void onCheckInResult(boolean success) {}
    default void onCheckOutResult(boolean success) {}
    default void onWalkInResult(Booking booking) {}
    default void onVisitsReportResult(java.util.ArrayList<common.VisitsReportResult> result) {}
    default void onTodayBookingsResult(java.util.ArrayList<common.Booking> bookings) {}
    
    default void onParkChangeRequestResult(boolean success) {}
    default void onParkChangeApprovalResult(String requestId, boolean approved) {}
    default void onParkChangeRequestNotification(ParkChangeRequest request) {}
    
    default void onPromotionsListResult(java.util.ArrayList<Promotion> promotions) {}
    default void onPromotionRequestResult(boolean success) {}
    default void onPromotionApprovalResult(String requestId, boolean approved) {}
    default void onPromotionRequestNotification(PromotionRequest request) {}
    default void onSubmittedReportsResult(ArrayList<ParkSubmittedReport> reports) {}

    // Department manager reports
    default void onAllParksVisitorsResult(ArrayList<common.ParkVisitorsCount> parks) {}
    default void onCancellationsReportResult(ArrayList<common.CancellationsReportResult> results) {}
}
