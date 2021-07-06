package org.hango.cloud.dashboard.envoy.exception;

/**
 * remote host is unreachable.
 *
 * @version $Id: HostUnReachableException.java, v 1.0 2013-8-2 下午04:02:15
 */
public class HostUnReachableException extends Exception {

	private static final long serialVersionUID = -3316039062933001109L;

	public HostUnReachableException() {
	}

	/**
	 *
	 */
	public HostUnReachableException(String message) {
		super(message);
	}

	/**
	 *
	 */
	public HostUnReachableException(Throwable cause) {
		super(cause);
	}

	/**
	 *
	 */
	public HostUnReachableException(String message, Throwable cause) {
		super(message, cause);
	}

}
