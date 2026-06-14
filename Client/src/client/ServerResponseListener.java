package client;

import java.util.ArrayList;

import common.Booking;
import common.Employee;
import common.Order;
import common.VisitorLoginResult;

public interface ServerResponseListener {
    void onOrderExistsResult(boolean exists);
    void onOrderResult(Order order);
    void onUpdateOrderResult(boolean success);
    void onError(String errorMessage);

    default void onVisitorLoginResult(VisitorLoginResult result) {}
    default void onTravelerBookingsResult(ArrayList<Booking> bookings) {}
    default void onCreateBookingResult(Booking booking) {}
    default void onUpdateBookingResult(boolean success) {}
    default void onCancelBookingResult(boolean success) {}

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
}