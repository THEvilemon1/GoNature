package client;

import java.util.ArrayList;

import common.Booking;
import common.Employee;
import common.Order;
import common.TravelerProfile;
import common.ParkOption;
import java.util.Map;
import common.VisitorLoginResult;

public interface ServerResponseListener {
    void onOrderExistsResult(boolean exists);
    void onOrderResult(Order order);
    void onUpdateOrderResult(boolean success);
    void onError(String errorMessage);

    default void onVisitorLoginResult(VisitorLoginResult result) {}
    default void onTravelerBookingsResult(ArrayList<Booking> bookings) {}
    default void onParkPricesResult(Map<Integer, Integer> pricesByParkId) {}
    default void onTravelerProfileResult(TravelerProfile profile) {}
    default void onUpdateTravelerProfileResult(boolean success) {}
    default void onParksResult(ArrayList<ParkOption> parks) {}
    default void onCreateBookingResult(Booking booking) {}
    default void onCreateBookingRequiresWaitlistConfirmation(Booking booking, String message) {}
    default void onUpdateBookingResult(boolean success) {}
    default void onCancelBookingResult(boolean success) {}
    default void onConfirmBookingResult(boolean success) {}

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
    
    default void onRegisterTravelerResult(String result) {}
}
