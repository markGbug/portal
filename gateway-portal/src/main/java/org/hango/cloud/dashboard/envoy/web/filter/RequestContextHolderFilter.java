package org.hango.cloud.dashboard.envoy.web.filter;

import org.hango.cloud.dashboard.envoy.web.holder.RequestContextHolder;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 */
public class RequestContextHolderFilter extends RequestContextHolder implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
		throws IOException, ServletException {
		RequestContextHolder.values.set(new HashMap<String, Object>());
		RequestContextHolder.setValue(RequestContextHolder.REQUEST_KEY, servletRequest);
		RequestContextHolder.setValue(RequestContextHolder.RESPONSE_KEY, servletResponse);
		filterChain.doFilter(servletRequest, servletResponse);
		RequestContextHolder.values.set(null);
	}

	@Override
	public void destroy() {

	}

}
