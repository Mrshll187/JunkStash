package com.spark.controllers;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.spark.services.DatabaseService;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

@Controller
@Configurable
public class AuxController {

	@Autowired
	private DatabaseService databaseService;
	
	public AuxController() {	
		setUpRoutes();
	}
	
	private void setUpRoutes(){
	
		 Spark.get("/getAll", new Route() {
	
				@Override
				public Object handle(Request request, Response response) throws Exception {
					
					JsonObject payload = new JsonObject();
					
					System.out.println("User Request at Path : ("+request.pathInfo()+") "+new Date());
					
					JsonArray jsonArray = databaseService.getAllDocuments();
					
					if(jsonArray.isJsonNull() || !jsonArray.iterator().hasNext()){
						
						payload.add("message", new JsonPrimitive("Request Was Empty"));
			         	payload.add("success", new JsonPrimitive(false));
		         		
		         		return payload;
					}
					else{
						
						payload.add("message", new JsonPrimitive("Successfully Found "+jsonArray.size()));
			         	payload.add("success", new JsonPrimitive(true));
			         	payload.add("payload", jsonArray);
			         	
			         	return payload;
					}
				}
	     });
		 
		 Spark.post("/submit", new Route() {
		     	
		     	@Override
		         public Object handle(Request request, Response response) {
		            
		     		JsonObject payload = new JsonObject();
		     		
		         	String data = request.body();
		         	
		         	if(data.isEmpty()){
			         	
		         		payload.add("message", new JsonPrimitive("Request Was Empty"));
			         	payload.add("success", new JsonPrimitive(false));
		         		
		         		return payload;
		         	}
		         	
		         	System.out.println("Server Recieved Payload : "+data);
		         	
		         	boolean saved = databaseService.save(data);
		         	
		         	if(saved){
		         		
		         		payload.add("message", new JsonPrimitive("Entry Successfully Saved"));
			         	payload.add("success", new JsonPrimitive(true));
		         		
		         		return payload;
		         	}
		         	else{
			         	
		         		payload.add("message", new JsonPrimitive("Entry Already Exists"));
			         	payload.add("success", new JsonPrimitive(false));
		         		
		         		return payload;
		         	}	
		          }
	     });
		 
		 Spark.post("/remove", new Route() {
		     	
		     	@Override
		         public Object handle(Request request, Response response) {
		             
		     		JsonObject payload = new JsonObject();
		     		
		         	String data = request.body();
		         	
		         	if(data.isEmpty()){
			         	
		         		payload.add("message", new JsonPrimitive("Request Was Empty"));
			         	payload.add("success", new JsonPrimitive(false));
		         		
		         		return payload;
		         	}
		         	
		         	JsonParser jsonParser = new JsonParser();
		         	JsonObject json = jsonParser.parse(data).getAsJsonObject();
		         	
		         	System.out.println("Server Attempting To Remove Entry : "+json.get("message").getAsString());
		         	
		         	String message = json.get("message").getAsString();
		         			
		         	boolean removed = databaseService.remove(message);
		         	
		         	if(removed){
		         		
		         		payload.add("message", new JsonPrimitive("Entry Successfully Removed"));
			         	payload.add("success", new JsonPrimitive(true));
		         		
		         		return payload;
		         	}
		         	else{
			         	
		         		payload.add("message", new JsonPrimitive("Entry Failed To Remove"));
			         	payload.add("success", new JsonPrimitive(false));
		         		
		         		return payload;
		         	}
		          }
	     });
		 
		 Spark.post("/upload", new Route() {
		     	
			 	JsonObject payload = new JsonObject();
			 
		     	@Override
		         public Object handle(Request request, Response response) {
		            
		     		try{
		     			
			            MultipartConfigElement multipartConfigElement = new MultipartConfigElement("/tmp");
			            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
			            
			            Part filePart = request.raw().getPart("file");
			           
			            String fileName = filePart.getSubmittedFileName();
			            InputStream fileStream = filePart.getInputStream();
			            
			            File uploadFile = new File("/tmp/"+fileName);
			            
			            FileUtils.copyInputStreamToFile(fileStream, uploadFile);
			            IOUtils.closeQuietly(fileStream);
			            
			            System.out.println("Successfully File Upload : "+fileName);
		     		}
		     		
		     		catch(Exception e){
		     			
		     			payload.add("message", new JsonPrimitive("Failed Uploading File "+e.getMessage()));
			         	payload.add("success", new JsonPrimitive(false));
		         		
		         		return payload;
		     		}
		         	
		     		payload.add("message", new JsonPrimitive("Successfully Uploaded File"));
		         	payload.add("success", new JsonPrimitive(true));
		         	
		         	return payload;
		     	}
		         	
	     });
	     
		 Spark.post("/search", new Route() {
	     	
	     	@Override
	         public Object handle(Request request, Response response) {
	             
	     		JsonObject payload = new JsonObject();
	     		
	         	String data = request.body();
	         	
	         	if(data.isEmpty()){
		         	
	         		payload.add("message", new JsonPrimitive("Request Was Empty"));
		         	payload.add("success", new JsonPrimitive(false));
	         		
	         		return payload;
	         	}
	         	
	         	System.out.println("Server Recieved Payload : "+data);
	         	
	         	Document document = databaseService.find(data);
	      	
	         	if(document.isEmpty()){
	         		
	         		payload.add("message", new JsonPrimitive("No Entry Found"));
		         	payload.add("success", new JsonPrimitive(false));
	         		
	         		return payload;
	         	}
	         	else{
	         		
	         		payload.add("message", new JsonPrimitive("Entry Found : "+document.getDate("time")));
		         	payload.add("success", new JsonPrimitive(true));
	         		
	         		return payload;
	         		
	         	}
	          }
	     });
	}
}
