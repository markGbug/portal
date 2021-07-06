package org.hango.cloud.ncegdashboard.envoy.web.filter;

import org.hango.cloud.ncegdashboard.envoy.web.holder.ProjectTraceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 项目Id过滤器，如果没有项目id，则使用默认项目id，方便后续程序进行处理
 */
public class ProjectTraceFilter implements Filter {

	private static final Logger logger = LoggerFactory.getLogger(ProjectTraceFilter.class);

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {
		try {
			ProjectTraceHolder.setProId(ProjectTraceHolder.DEFAULT_PROJECT_ID);
			ProjectTraceHolder.setTenantId(ProjectTraceHolder.DEFAULT_TENANT_ID);
		} catch (Exception e) {
			logger.info("projectHeader头填写异常");
			e.printStackTrace();
			return;
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		ProjectTraceHolder.removeProId();
		ProjectTraceHolder.removeTenantId();
	}

}
