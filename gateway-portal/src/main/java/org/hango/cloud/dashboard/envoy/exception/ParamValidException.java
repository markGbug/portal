package org.hango.cloud.ncegdashboard.envoy.exception;

import org.hango.cloud.ncegdashboard.envoy.meta.errorcode.ErrorCode;

/**
 * @version $Id: ParamValidException.java, v 1.0 2018年07月26日 10:17
 */
public class ParamValidException extends RuntimeException {

	private static final long serialVersionUID = 7502581117241462842L;

	public ParamValidException(ErrorCode errorCode) {
		super(errorCode.getCode() + "," + errorCode.getMessage() + "," + errorCode.getStatusCode());
	}

}
