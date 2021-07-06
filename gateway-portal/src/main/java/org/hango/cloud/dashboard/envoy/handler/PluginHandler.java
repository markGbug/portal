package org.hango.cloud.dashboard.envoy.handler;

import org.hango.cloud.dashboard.envoy.web.dto.PluginOrderItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Type
 * @Desc
 * @date 2020/7/9
 */
public abstract class PluginHandler {

	public static final List<String> pluginIgnoreList = new ArrayList<>();

	public static final Map<String, PluginHandler> pluginUseSubNameList = new HashMap<>();

	static {
		pluginIgnoreList.add("org.hango.metadatahub");
		pluginIgnoreList.add("org.hango.metadataext");
	}

	static {
		pluginUseSubNameList.put("org.hango.resty", new RestyPluginHandler());
	}

	/**
	 * 获取插件名称
	 */
	public String getName(PluginOrderItemDto item) {
		return item.getName();
	}

}
