package org.hango.cloud.ncegdashboard.envoy.exception;

/**
 *
 */
public class BadRequestException extends RuntimeException {

	private int code;

	public BadRequestException(String message, int code) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
