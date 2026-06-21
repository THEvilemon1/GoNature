package client;

import java.util.ArrayList;
import java.util.Map;

import common.Booking;
import common.Employee;
import common.Order;
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
    default void onParkPricesResult(Map<Integer, Integer> pricesByParkId) {}
    default void onCreateBookingResult(Booking booking) {}
    default void onUpdateBookingResult(boolean success) {}
    default void onCancelBookingResult(boolean success) {}
    default void onParkVisitorsReportResult(ParkVisitorsReportResult result) {}

    // Employee login
    default void onEmployeeLoginSuccess(Employee employee) {}
    default void onEmployeeLoginFailed(String reason) {}

    // Park worker operations
    default void onParkVisitorsResult(int currentVisitors) {}
    default void onEffectiveAvailableSpotsResult(int spots) {}
    default void onBookingResult(Booking booking) {}
    default void onCheckInResult(boolean success) {}
    default void onCheckOutResult(boolean success) {}
    default void onWalkInResult(Booking booking) {}
    
    default void onTodayBookingsResult(java.util.ArrayList<common.Booking> bookings) {}
    
    default void onParkChangeRequestResult(boolean success) {}
    default void onParkChangeApprovalResult(String requestId, boolean approved) {}
    default void onParkChangeRequestNotification(ParkChangeRequest request) {}
    
    default void onPromotionsListResult(java.util.ArrayList<Promotion> promotions) {}
    default void onPromotionRequestResult(boolean success) {}
    default void onPromotionApprovalResult(String requestId, boolean approved) {}
    default void onPromotionRequestNotification(PromotionRequest request) {}
}
