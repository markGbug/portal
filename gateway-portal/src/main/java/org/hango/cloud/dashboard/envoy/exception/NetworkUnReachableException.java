package org.hango.cloud.ncegdashboard.envoy.exception;

/**
 * remote host is unreachable.
 *
 * @version $Id: NetworkUnReachableException.java, v 0.1 2012-6-13 下午02:55:01
 */
public class NetworkUnReachableException extends Exception {

	/**
	 *
	 */
	public NetworkUnReachableException() {
	}

	/**
	 *
	 */
	public NetworkUnReachableException(String message) {
		super(message);
	}

	/**
	 *
	 */
	public NetworkUnReachableException(Throwable cause) {
		super(cause);
	}

	/**
	 *
	 */
	public NetworkUnReachableException(String message, Throwable cause) {
		super(message, cause);
	}

}
