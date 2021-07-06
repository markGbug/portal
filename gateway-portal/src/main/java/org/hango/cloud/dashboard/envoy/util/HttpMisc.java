package org.hango.cloud.ncegdashboard.envoy.util;

public class HttpMisc {

	public static boolean isNormalCode(int code, int... exNormalList) {
		if (code >= 200 && code <= 300) {
			return true;
		}
		for (int except : exNormalList) {
			if (code == except) {
				return true;
			}
		}
		return false;
	}
}
