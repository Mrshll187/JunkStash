package com.spark.services;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.result.UpdateResult;
import com.spark.config.DatabaseConfig;

@Service
public class DatabaseService {
	
	@Autowired
	private DatabaseConfig databaseService;
	
	private static String cachedIndex;
	
	public DatabaseService() throws Exception{
		cacheResource();
	}
		
	public MongoDatabase getMongoDatabase(){
		return databaseService.getMongoDatabase();
	}
	
	public MongoClient getMongoClient(){
		return databaseService.getMongoClient();
	}
	
	public GridFSBucket getGridFSBucket(){
		return databaseService.getGridFSBucket();
	}
	
	public boolean exists(String fileId){
		
		ObjectId objectId = new ObjectId(fileId);
		
		Document query = new Document();
		query.append("_id", objectId);
		
		FindIterable<Document> results = databaseService.getFileCollection().find(query);
		
		if(results.iterator().hasNext())
			return true;
		
		else
			return false;
	}
	
	public boolean remove(String fileId){
		
		try{
			getGridFSBucket().delete(new ObjectId(fileId));
		}
		catch(Exception e){
			return false;
		}
		
		return true;
	}
	
	public JsonArray getAllDocuments(){
		
		MongoCursor<Document> cursor = databaseService.getFileCollection().find().iterator();
		
		JsonArray jsonArray = new JsonArray();
		
		while(cursor.hasNext()){
			
			Document result = cursor.next();
			
			String messsage = result.getString("message");
			String type = result.getString("type");
			String id = result.get("_id").toString();
			String name = result.getString("filename");
			String time = result.getDate("uploadDate").toString();
			long size = result.getLong("length");
			
			JsonObject json = new JsonObject();
			
			if(StringUtils.isNotEmpty(messsage))
				json.add("message", new JsonPrimitive(messsage));
			
			if(StringUtils.isNotEmpty(time))
				json.add("time", new JsonPrimitive(time));
			
			if(StringUtils.isNotEmpty(type))
				json.add("type", new JsonPrimitive(type));
			
			if(StringUtils.isNotEmpty(id))
				json.add("id", new JsonPrimitive(id));
			
			if(StringUtils.isNotEmpty(name))
				json.add("name", new JsonPrimitive(name));
			
			json.add("size", new JsonPrimitive(FileUtils.byteCountToDisplaySize(size)));
			
			jsonArray.add(json);
		}
		
		return jsonArray;
	}
	
	public boolean setFileType(String fileId, String fileType){
		
		Document query = new Document();
		query.append("_id", new ObjectId(fileId));
		
		Document update = new Document();
		update.append("$set", new Document("type", fileType));
		
		UpdateResult results = databaseService.getFileCollection().updateOne(query, update);
		
		if(results.getModifiedCount()>0)
			return true;
		else
			return false;
	}
	
	public Document find(String fileId){
		
		ObjectId objectId = new ObjectId(fileId);
		
		Document query = new Document();
		query.append("_id", objectId);
		
		FindIterable<Document> results = databaseService.getFileCollection().find(query);
		
		if(results.iterator().hasNext())
			return results.iterator().next();
		
		else
			return new Document();
	}
	
	public String getFileName(String fileId){
		
		ObjectId objectId = new ObjectId(fileId);
		
		Document query = new Document();
		query.append("_id", objectId);
		
		FindIterable<Document> results = databaseService.getFileCollection().find(query);
		
		if(results.iterator().hasNext())
			return results.iterator().next().getString("filename");
		
		else
			return null;
	}
	
	private void cacheResource() throws Exception{
		
		URL indexFile = getClass().getClassLoader().getResource("index.html");
		cachedIndex = FileUtils.readFileToString(new File(indexFile.toURI()));
		
		System.out.println("Resource Cached : "+indexFile);
	}
	
	public String getIndex(){
		return cachedIndex;
	}
}
