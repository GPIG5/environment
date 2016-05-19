package comms;

import utility.ServiceInterface;
import utility.ServiceResponse;

import java.util.AbstractMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by hm649 on 18/05/16.
 */
public class MessageDispatcher implements Runnable {

    private Queue<ServiceResponse> responses;
    private AbstractMap<String, Future<?>> waitMap = new ConcurrentHashMap<>();
    private AbstractMap<String, ServiceResponse> responseMap = new ConcurrentHashMap<>();


    public MessageDispatcher(ServiceInterface si) {
        responses = si.getResponseQueue();
    }

    public ServiceResponse waitForResponse(String uuid, Future<?> future) throws InterruptedException {
        waitMap.put(uuid, future);
        future.wait();
        ServiceResponse sr = responseMap.remove(uuid);
        if (sr == null) {
            throw new IllegalStateException("No service response for drone after it woke");
        }
        return sr;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                synchronized (responses) {
                    responses.wait();
                }
                ServiceResponse sr;
                while ( (sr = responses.poll()) != null) {
                    if (responseMap.containsKey(sr.getUuid())) {
                        throw new IllegalStateException("Multiple service responses for same drone");
                    } else {
                        responseMap.put(sr.getUuid(), sr);
                        Future<?> future = waitMap.get(sr.getUuid());
                        synchronized (future) {
                            future.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                //meh
            }
        }
    }
}
