package org.hango.cloud.ncegdashboard.envoy.config;

import org.hango.cloud.ncegdashboard.envoy.web.filter.LogUUIDFilter;
import org.hango.cloud.ncegdashboard.envoy.web.filter.ProjectTraceFilter;
import org.hango.cloud.ncegdashboard.envoy.web.filter.RequestContextHolderFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @version $Id: WebServletConfig.java, v 1.0 2017年3月24日 下午4:46:11
 */
@Configuration
public class WebServletConfig extends WebMvcConfigurerAdapter {

	@Bean
	public FilterRegistrationBean requestContextFilterReg() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new RequestContextHolderFilter());
		registration.addUrlPatterns("/*");
		registration.setName(RequestContextHolderFilter.class.getSimpleName());
		registration.setOrder(1);
		return registration;
	}

	@Bean
	public FilterRegistrationBean logUuidFilterReg() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new LogUUIDFilter());
		registration.addUrlPatterns("/*");
		registration.setName(LogUUIDFilter.class.getSimpleName());
		registration.setOrder(2);
		return registration;
	}

	@Bean
	public FilterRegistrationBean projectFilterReg() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(new ProjectTraceFilter());
		registration.addUrlPatterns("/*");
		registration.setName(ProjectTraceFilter.class.getSimpleName());
		registration.setOrder(3);
		return registration;
	}

}
