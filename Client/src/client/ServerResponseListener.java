package client;

public interface ServerResponseListener {
    void onOrderExistsResult(boolean exists);
    void onUpdateOrderResult(boolean success);
    void onError(String errorMessage);
}
