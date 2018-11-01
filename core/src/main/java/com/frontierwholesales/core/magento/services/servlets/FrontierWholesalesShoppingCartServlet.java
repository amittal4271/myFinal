package com.frontierwholesales.core.magento.services.servlets;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.text.DecimalFormat;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontierwholesales.core.magento.services.FrontierWholesalesMagentoCommerceConnector;
import com.frontierwholesales.core.services.constants.FrontierWholesalesConstants;
import com.frontierwholesales.core.utils.FrontierWholesalesUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("serial")
@SlingServlet(label="FrontierWholesalesUserRegistration - Sling All Methods Servlet", 
description="FrontierWholesales User Registration Sling All Methods Servlet.", 
paths={"/services/cart"}, methods={"GET","POST"})
public class FrontierWholesalesShoppingCartServlet  extends SlingAllMethodsServlet{

	private static final Logger log = LoggerFactory.getLogger(FrontierWholesalesShoppingCartServlet.class);
	
	private FrontierWholesalesMagentoCommerceConnector commerceConnector = new FrontierWholesalesMagentoCommerceConnector();
	private ObjectMapper mapper = new ObjectMapper();
	private FrontierWholesalesUtils utils = new FrontierWholesalesUtils();
	
	
	
	@Override
	public void init() {
		 mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}
	
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		log.debug("FrontierWholesalesShoppingCartServlet doGet method Start");
		
		
		try {
			String action = request.getParameter("action");
			log.debug(" action is "+action);
			String token = request.getHeader("Authorization");
			
			if(token == null) {
			
				throw new Exception("token is null");
			
			}
		
			if(action.equals("remove")) {
				String itemId = request.getParameter("itemId");
				
				 commerceConnector.removeCartItem(token, itemId);
				
				
			}else if(action.equals("add")){
				String jsonData = request.getParameter("items");
			
				// create cart
				String cartId = commerceConnector.initCart(token);
				
				//update json structure with cartid
				String updatedData = FrontierWholesalesUtils.updateJsonObject(jsonData, "cartItem", "quote_id", cartId);
				//add item into the cart
				String cartItems = commerceConnector.addItemToCart(token, updatedData);
				
				commerceConnector.initCart(token);
			}
			String cartObject = commerceConnector.getCartTotalWithItems(token);
			String customerId = utils.getCustomerDetailsByParameter("id", token);
			String adminToken = commerceConnector.getAdminToken();
			String roleObject = commerceConnector.getUserRole(adminToken, customerId);
			
			String roleName = getRoleName(roleObject);
			
			
			String object = getValueFromJson(cartObject,request,roleName);
			
			log.debug("shopping cart operations end ");
			
			response.getOutputStream().write(object.getBytes("UTF-8"));
			
		}catch(Exception anyEx) {
			
			log.debug("Error "+anyEx.getMessage());
			
			JsonObject errorJsonObject = new JsonObject();
			errorJsonObject.addProperty("Fail", anyEx.getMessage());
			response.getOutputStream().println(errorJsonObject.toString());
			
		}
		log.debug("FrontierWholesalesShoppingCartServlet doGet method End");
	}
	
	
	
	private String getRoleName(String object) throws Exception{
		log.debug("getRoleName start");
		JsonParser parser = new JsonParser();
		JsonObject jsonObject = parser.parse(object).getAsJsonObject();
		String roleName="";
		JsonElement attribElement = jsonObject.get("extension_attributes");
		JsonElement roleObject = attribElement.getAsJsonObject().get("role_name");
		if(roleObject != null) {
			roleName = roleObject.getAsString();
		}
		log.debug("getRoleName end");
		return roleName;
	}
	
	private String getValueFromJson(String cartObject,SlingHttpServletRequest request,String roleName) throws Exception{
		log.debug("FrontierWholesalesShoppingCartServlet getValueFromJson method Start");
		Gson json = new Gson();
		JsonElement element = json.fromJson(cartObject,JsonElement.class);
		
		JsonObject object = element.getAsJsonObject();
		JsonElement quoteElement = object.get("quote");
		
		JsonObject quote = quoteElement.getAsJsonObject();
		FrontierWholesalesUtils utils = new FrontierWholesalesUtils();
		JsonArray array = object.getAsJsonArray("item_details");
	
		JsonArray updatedArray = new JsonArray();
		DecimalFormat priceFormat=new DecimalFormat("#0.00");
		
		for(int i=0;i<array.size();i++) {
			JsonElement jsonElement = array.get(i);
			JsonObject itemObject = jsonElement.getAsJsonObject();
			JsonElement price = itemObject.get("price");
			
			String sku = itemObject.get("sku").getAsString();
			
			String imgPath = utils.getImagePath(sku, request);
			itemObject.addProperty("imgPath", imgPath);
			

			itemObject.addProperty("price", "$"+priceFormat.format(price.getAsDouble()));
			JsonElement qtyObject = itemObject.get("qty");
			boolean bReturn = false;
			if(qtyObject.getAsInt() > 1) {
				bReturn = true;
			}
			itemObject.addProperty("quantities", bReturn);
			

			JsonElement updatedElement = json.fromJson(itemObject, JsonElement.class);
			updatedArray.add(updatedElement);
			

		}
		
		
			JsonElement arrayElement = json.fromJson(updatedArray, JsonElement.class);
		
			JsonElement subTotal = quote.get("subtotal");
			
			double subtotal = 0;
			if(subTotal != null) {
				
				subtotal = subTotal.getAsDouble();
			}
			
			if(subtotal == 0) {
				
				object.addProperty("noTotal", true);
			}else {
			
				object.addProperty("noTotal", false);
			}
			
			object.addProperty("subTotal", "$"+priceFormat.format(subtotal));
			
			object.add("items", arrayElement);
			
			object.addProperty("role_name", roleName);
			
			log.debug("FrontierWholesalesShoppingCartServlet getValueFromJson method End");
			return object.toString();
			
		
	}

}
