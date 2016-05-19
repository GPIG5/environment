package utility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServiceInterface {
    private ConcurrentLinkedQueue<ServiceRequest> requests;

    public ServiceInterface() {
        requests = new ConcurrentLinkedQueue<ServiceRequest>();
    }

    public Queue<ServiceRequest> getRequestQueue() {
        return requests;
    }

}
