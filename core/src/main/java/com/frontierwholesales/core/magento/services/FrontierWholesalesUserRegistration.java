package com.frontierwholesales.core.magento.services;

import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class FrontierWholesalesUserRegistration {

	 private static final Logger log = LoggerFactory.getLogger(FrontierWholesalesUserRegistration.class);
	
	 /**
	  * customer registration
	  * @param params
	  * @return
	  * @throws Exception
	  */
	public static String customerRegistration(JsonObject params) throws Exception{
		
		
		
		
		String server = FrontierWholesalesMagentoCommerceConnector.getServer();
		String customerId = Request.Post(server + "/rest/V1/customers")
                .bodyString(params.toString(),ContentType.APPLICATION_JSON)
                .execute().returnContent().asString();
		log.info("customer id is "+customerId);
		return customerId;
	}
	
	public static String companyRegistration(String pwd,JsonObject params) throws Exception{
		
		String server = FrontierWholesalesMagentoCommerceConnector.getServer();
		String userDetails = Request.Post(server + "/rest/all/V1/company")
				.addHeader("Authorization",pwd)
                .bodyString(params.toString(),ContentType.APPLICATION_JSON)
                .execute().returnContent().asString();
		log.info("customer id is "+userDetails);
		return userDetails;
	}
	
	public static String getCountriesWithRegions(String adminToken) throws Exception{
		
		String server = FrontierWholesalesMagentoCommerceConnector.getServer();
		String countryAndRegions = Request.Get(server + "/rest/all/V1/directory/countries")
				.addHeader("Authorization", adminToken)
               
                .execute().returnContent().asString();
		
		return countryAndRegions;
	}
	
}
