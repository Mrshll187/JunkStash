package com.junkStash.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Controller;

import com.junkStash.services.UserService;
import com.junkStash.util.CacheUtil;
import com.junkStash.util.PropertyUtil;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

@Controller
@Configurable
public class IndexController {
	
	public IndexController(){
		setUpRoutes();
	}
	
	public void setUpRoutes(){
		
		Spark.before((request, response) -> {
			//Do authentication here
		}); 
		
		Spark.get("/", new Route() {
				
			@Override
			public Object handle(Request request, Response response) throws Exception {					
				return CacheUtil.getIndex();
			}
		});
		
		Spark.get("/socket", new Route() {
			
			@Override
			public Object handle(Request request, Response response) throws Exception {	
				return PropertyUtil.getWebSocketUrl();
			}
		});
	}
}
