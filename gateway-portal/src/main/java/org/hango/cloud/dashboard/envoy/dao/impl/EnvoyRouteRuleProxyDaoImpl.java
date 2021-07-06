package org.hango.cloud.dashboard.envoy.dao.impl;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hango.cloud.dashboard.envoy.util.Const;
import org.hango.cloud.dashboard.envoy.dao.IEnvoyRouteRuleProxyDao;
import org.hango.cloud.dashboard.envoy.meta.EnvoyDestinationInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleMapMatchInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteRuleProxyInfo;
import org.hango.cloud.dashboard.envoy.meta.EnvoyRouteStringMatchInfo;
import org.hango.cloud.dashboard.envoy.web.dto.HttpRetryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路由规则发布信息Dao层实现类
 * <p>
 * 2019-09-18
 */
@Component
public class EnvoyRouteRuleProxyDaoImpl extends BaseDao implements IEnvoyRouteRuleProxyDao {

	private static final Logger logger = LoggerFactory.getLogger(EnvoyRouteRuleProxyDaoImpl.class);

	@Override
	public long add(EnvoyRouteRuleProxyInfo envoyRouteRuleProxyInfo) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		String sql =
			"insert into apigw_envoy_route_rule_proxy (route_rule_id, gw_id, destination_services, service_id, "
			+ "priority, orders, enable_state, create_time, update_time, project_id, hosts, timeout, http_retry, uri, "
			+ "method, host, header, query_param, virtual_cluster) "
			+ " values (:routeRuleId, :gwId, :destinationServices, :serviceId, :priority, :orders, :enableState, "
			+ ":createTime, :updateTime, :projectId, :hosts, :timeout, :httpRetry, :uri, :method, :host, :header, "
			+ ":queryParam, :virtualCluster)";
		SqlParameterSource ps = new BeanPropertySqlParameterSource(envoyRouteRuleProxyInfo);
		namedParameterJdbcTemplate.update(sql, ps, keyHolder);
		logger.info("add envoyRouteRuleProxyInfo: {}",
		            ReflectionToStringBuilder.toString(envoyRouteRuleProxyInfo, ToStringStyle.SIMPLE_STYLE));
		return keyHolder.getKey().intValue();
	}

	@Override
	public long update(EnvoyRouteRuleProxyInfo envoyRouteRuleProxyInfo) {
		String sql = "update apigw_envoy_route_rule_proxy set update_time=:updateTime, "
		             + "destination_services=:destinationServices, service_id=:serviceId, enable_state=:enableState, "
		             + "hosts=:hosts, timeout=:timeout,"
		             + "http_retry=:httpRetry, uri=:uri, method=:method, host=:host, header=:header, "
		             + "query_param=:queryParam, "
		             + "priority=:priority, orders=:orders, virtual_cluster=:virtualCluster where id=:id";
		SqlParameterSource ps = new BeanPropertySqlParameterSource(envoyRouteRuleProxyInfo);
		logger.info("update envoyRouteRuleProxyInfo: {}",
		            ReflectionToStringBuilder.toString(envoyRouteRuleProxyInfo, ToStringStyle.SIMPLE_STYLE));
		return namedParameterJdbcTemplate.update(sql, ps);
	}

	@Override
	public int delete(EnvoyRouteRuleProxyInfo envoyRouteRuleProxyInfo) {
		String sql = "delete from apigw_envoy_route_rule_proxy where id=:id";
		SqlParameterSource ps = new BeanPropertySqlParameterSource(envoyRouteRuleProxyInfo);
		logger.info("delete envoyRouteRuleProxyInfo: {}",
		            ReflectionToStringBuilder.toString(envoyRouteRuleProxyInfo, ToStringStyle.SIMPLE_STYLE));
		return namedParameterJdbcTemplate.update(sql, ps);
	}

	@Override
	public EnvoyRouteRuleProxyInfo get(long id) {
		String sql = "select * from apigw_envoy_route_rule_proxy where id=:id";
		return queryForObject(sql, new MapSqlParameterSource("id", id), new EnvoyRouteRuleProxyInfoRowMapper());
	}

	@Override
	public List<EnvoyRouteRuleProxyInfo> findAll() {
		String sql = "select * from apigw_envoy_route_rule_proxy";
		return namedParameterJdbcTemplate.query(sql, new EnvoyRouteRuleProxyInfoRowMapper());
	}

	@Override
	public List<EnvoyRouteRuleProxyInfo> getRecordsByField(Map<String, Object> params) {
		String head = "select * from apigw_envoy_route_rule_proxy where ";
		String sql = getQueryCondition(head, params);
		return namedParameterJdbcTemplate.query(sql, params, new EnvoyRouteRuleProxyInfoRowMapper());
	}

	@Override
	public int getCountByFields(Map<String, Object> params) {
		String head = "select count(*) from apigw_envoy_route_rule_proxy where ";
		String sql = getQueryCondition(head, params);
		return namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
	}

	@Override
	public List<EnvoyRouteRuleProxyInfo> getRouteRuleProxyList(long gwId, long serviceId, long projectId,
	                                                           String sortKey, String sortValue, long offset,
	                                                           long limit) {
		Map<String, Object> params = new HashMap<>(Const.DEFAULT_MAP_SIZE);
		params.put("offset", offset);
		params.put("limit", limit);
		String sql;
		if (Const.CONST_PRIORITY.equals(sortKey)) {
			sortKey = "orders";
		}
		//已发布页面，默认按照路由规则排序
		String orderString = StringUtils.isBlank(sortKey) ? "orders desc , create_time asc" :
		                     sortKey + " " + sortValue;
		if (Const.CONST_PRIORITY.equals(sortKey) && Const.CONST_DESC.equals(sortValue)) {
			orderString += orderString + ", create_time asc";
		} else if (Const.CONST_PRIORITY.equals(sortKey) && Const.CONST_DESC.equals(sortValue)) {
			orderString += orderString + ", create_time desc";
		}
		if (0 != gwId && 0 != serviceId) {
			sql = "select * from apigw_envoy_route_rule_proxy where service_id=:serviceId and gw_id=:gwId order by "
			      + orderString + " limit :limit offset :offset";
			params.put("serviceId", serviceId);
			params.put("gwId", gwId);
		} else if (0 == gwId && 0 != serviceId) {
			sql = "select * from apigw_envoy_route_rule_proxy where service_id=:serviceId and project_id=:projectId "
			      + "order by " + orderString + " limit :limit offset :offset";
			params.put("serviceId", serviceId);
			params.put("projectId", projectId);
		} else if (0 != gwId && 0 == serviceId) {
			sql = "select * from apigw_envoy_route_rule_proxy where gw_id=:gwId and project_id=:projectId order by "
			      + orderString + " limit :limit offset :offset";
			params.put("gwId", gwId);
			params.put("projectId", projectId);
		} else {
			sql = "select * from apigw_envoy_route_rule_proxy where project_id=:projectId order by " + orderString
			      + " limit :limit offset :offset";
			params.put("projectId", projectId);
		}
		return namedParameterJdbcTemplate.query(sql, params, new EnvoyRouteRuleProxyInfoRowMapper());
	}

	@Override
	public List<EnvoyRouteRuleProxyInfo> getRouteRuleProxyList(long serviceId) {
		String sql = "select * from apigw_envoy_route_rule_proxy where service_id=:serviceId";
		return namedParameterJdbcTemplate.query(sql, new MapSqlParameterSource("serviceId", serviceId),
		                                        new EnvoyRouteRuleProxyInfoRowMapper());

	}

	@Override
	public long getRouteRuleProxyCount(long gwId, long serviceId, long projectId) {
		String sql;
		Map<String, Object> params = new HashMap<String, Object>();
		if (0 != gwId && 0 != serviceId) {
			sql = "select count(*) from apigw_envoy_route_rule_proxy where service_id=:serviceId and gw_id=:gwId";
			params.put("serviceId", serviceId);
			params.put("gwId", gwId);
		} else if (0 == gwId && 0 != serviceId) {
			sql = "select count(*) from apigw_envoy_route_rule_proxy where service_id=:serviceId and "
			      + "project_id=:projectId";
			params.put("serviceId", serviceId);
			params.put("projectId", projectId);
		} else if (0 != gwId && 0 == serviceId) {
			sql = "select count(*) from apigw_envoy_route_rule_proxy where gw_id=:gwId and project_id=:projectId";
			params.put("gwId", gwId);
			params.put("projectId", projectId);
		} else {
			sql = "select count(*) from apigw_envoy_route_rule_proxy where project_id=:projectId";
			params.put("projectId", projectId);
		}
		return namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
	}

	class EnvoyRouteRuleProxyInfoRowMapper implements RowMapper<EnvoyRouteRuleProxyInfo> {

		@Override
		public EnvoyRouteRuleProxyInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			EnvoyRouteRuleProxyInfo routeRuleProxyInfo = new EnvoyRouteRuleProxyInfo();
			routeRuleProxyInfo.setId(rs.getLong("id"));
			routeRuleProxyInfo.setGwId(rs.getLong("gw_id"));
			routeRuleProxyInfo.setCreateTime(rs.getLong("create_time"));
			routeRuleProxyInfo.setUpdateTime(rs.getLong("update_time"));
			routeRuleProxyInfo.setRouteRuleId(rs.getLong("route_rule_id"));
			routeRuleProxyInfo.setDestinationServices(rs.getString("destination_services"));
			routeRuleProxyInfo.setProjectId(rs.getLong("project_id"));
			routeRuleProxyInfo.setServiceId(rs.getLong("service_id"));
			routeRuleProxyInfo.setPriority(rs.getLong("priority"));
			routeRuleProxyInfo.setOrders(rs.getLong("orders"));
			routeRuleProxyInfo.setEnableState(rs.getString("enable_state"));
			List<EnvoyDestinationInfo> envoyDestinationInfos = JSON.parseArray(
				routeRuleProxyInfo.getDestinationServices(), EnvoyDestinationInfo.class);
			routeRuleProxyInfo.setDestinationServiceList(envoyDestinationInfos);
			routeRuleProxyInfo.setHosts(rs.getString("hosts"));
			routeRuleProxyInfo.setTimeout(rs.getLong("timeout"));
			//构造HttpRetry
			routeRuleProxyInfo.setHttpRetry(rs.getString("http_retry"));
			if (StringUtils.isNotBlank(routeRuleProxyInfo.getHttpRetry())) {
				routeRuleProxyInfo.setHttpRetryDto(
					JSON.parseObject(routeRuleProxyInfo.getHttpRetry(), HttpRetryDto.class));
			}
			routeRuleProxyInfo.setUri(rs.getString("uri"));
			routeRuleProxyInfo.setUriMatchInfo(
				JSON.parseObject(routeRuleProxyInfo.getUri(), EnvoyRouteStringMatchInfo.class));

			routeRuleProxyInfo.setMethod(rs.getString("method"));
			routeRuleProxyInfo.setMethodMatchInfo(
				JSON.parseObject(routeRuleProxyInfo.getMethod(), EnvoyRouteStringMatchInfo.class));

			routeRuleProxyInfo.setHost(rs.getString("host"));
			routeRuleProxyInfo.setHostMatchInfo(
				JSON.parseObject(routeRuleProxyInfo.getHost(), EnvoyRouteStringMatchInfo.class));

			routeRuleProxyInfo.setQueryParam(rs.getString("query_param"));
			routeRuleProxyInfo.setQueryParamList(
				JSON.parseArray(routeRuleProxyInfo.getQueryParam(), EnvoyRouteRuleMapMatchInfo.class));

			routeRuleProxyInfo.setHeader(rs.getString("header"));
			routeRuleProxyInfo.setHeaderList(
				JSON.parseArray(routeRuleProxyInfo.getHeader(), EnvoyRouteRuleMapMatchInfo.class));
			routeRuleProxyInfo.setVirtualCluster(rs.getString("virtual_cluster"));
			return routeRuleProxyInfo;
		}

	}

}
