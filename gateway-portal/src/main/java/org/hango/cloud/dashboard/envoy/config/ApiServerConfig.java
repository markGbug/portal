package org.hango.cloud.dashboard.envoy.config;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApiServerConfig {

	/**
	 * 插件配置额外信息
	 */
	private String pluginManagerExtra;


	public String getPluginManagerExtra() {
		return pluginManagerExtra;
	}

	public void setPluginManagerExtra(String pluginManagerExtra) {
		this.pluginManagerExtra = pluginManagerExtra;
	}

	public Map<String, String> getPluginManagerMap() {
		HashMap<String, String> pluginManagerMap = Maps.newHashMap();
		String pluginManagerExtra = getPluginManagerExtra();
		if (StringUtils.isBlank(pluginManagerExtra)) {
			return pluginManagerMap;
		}
		String[] split = pluginManagerExtra.split(",");
		for (String item : split) {
			String[] content = item.split(":");
			pluginManagerMap.put(content[0], content[1]);
		}
		return pluginManagerMap;
	}
}
