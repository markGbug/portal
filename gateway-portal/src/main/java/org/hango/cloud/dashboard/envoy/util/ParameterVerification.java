package org.hango.cloud.ncegdashboard.envoy.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数校验工具类
 *
 * @Date: 创建时间: 2017/12/12 上午11:10.
 */
public class ParameterVerification {

	/**
	 * 判断apiPath是否合法
	 */
	public static boolean isApiPathValid(String apiPath) {
		if (StringUtils.isBlank(apiPath)) {
			return false;
		}

		//apiPath 只能包含字母、数字、/、{、}
		String pattern = "^[0-9A-Za-z\\/\\{\\}\\-\\_\\.]+$";

		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(apiPath);

		return m.matches();
	}

}
