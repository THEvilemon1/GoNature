package client;

import common.Order;
import common.VisitorLoginResult;

public interface ServerResponseListener {
    void onOrderExistsResult(boolean exists);
    void onOrderResult(Order order);
    void onUpdateOrderResult(boolean success);
    void onError(String errorMessage);

    default void onVisitorLoginResult(VisitorLoginResult result) {}
}
