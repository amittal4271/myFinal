package com.frontierwholesales.core.beans.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagentoSearchCondition {
	
	private static final Logger logger = LoggerFactory.getLogger( MagentoSearchCondition.class );

	private String field = null;
	private String value = null;
	private String type = "eq";
	
	public MagentoSearchCondition(String field, String value) {
		super();
		this.field = field;
		this.value = value;
	}

	public MagentoSearchCondition(String field, String value, String type) {
		super();
		this.field = field;
		this.value = value;
		this.type = type;
	}
	
	public String getField() {
		return field;
	}
	
	public void setField(String field) {
		this.field = field;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	String getQueryString(int group_index, int index) {
		String encodedValue = value;
		
		try {
			encodedValue = URLEncoder.encode(encodedValue, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn("Unable to encode search term {}", new Object[]{encodedValue}, e);
		}
		
		return "searchCriteria[filter_groups][" + group_index + "][filters][" + index + "][field]=" + field + "&" +
			   "searchCriteria[filter_groups][" + group_index + "][filters][" + index + "][value]=" + encodedValue + "&" + 
			   "searchCriteria[filter_groups][" + group_index + "][filters][" + index + "][condition_type]=" + type;
	}
}