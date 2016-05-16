package utility;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServiceInterface {
	private ConcurrentLinkedQueue<ServiceRequest> requests;
	private ConcurrentLinkedQueue<ServiceResponse> responses;
	
	public ServiceInterface() {
		requests = new ConcurrentLinkedQueue<ServiceRequest>();
		responses = new ConcurrentLinkedQueue<ServiceResponse>();
	}
	
	public ConcurrentLinkedQueue<ServiceRequest> getRequestQueue() {
		return requests;
	}
	
	public ConcurrentLinkedQueue<ServiceResponse> getResponseQueue() {
		return responses;
	}
}
