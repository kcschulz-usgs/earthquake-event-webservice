package gov.usgs.earthquake.event;

/**
 * Event ID class.
 */
public class EventID {

	private network, code;
	
	/**
	 * Construct a new EventID from a network and code.
	 * 
	 * @param [String] network
	 * @param [String] code
	 */
	public EventID(String network, String code) {
		this.network = network;
		this.code = code;
	}

	public String toString() {
		return this.network + this.code;
	}
}