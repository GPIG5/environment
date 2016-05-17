package utility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServiceInterface {
    private ConcurrentLinkedQueue<ServiceRequest> requests;
    private ConcurrentLinkedQueue<ServiceResponse> responses;

    public ServiceInterface() {
        requests = new ConcurrentLinkedQueue<ServiceRequest>();
        responses = new ConcurrentLinkedQueue<ServiceResponse>();
    }

    public Queue<ServiceRequest> getRequestQueue() {
        return requests;
    }

    public Queue<ServiceResponse> getResponseQueue() {
        return responses;
    }
}
