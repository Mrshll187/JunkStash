package com.junkStash.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.junkStash.config.DatabaseConfig;
import com.junkStash.services.SocketService.StatusType;
import com.junkStash.util.UserUtils;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.UpdateResult;

@Service
public class UserService {

	@Autowired
	private DatabaseConfig databaseService;
	
	@Autowired
	private FileService fileService;
	
	public static final int MAX_ATTEMPTS = 10;
	public static final int DAY_LIMIT = 1;
	
	public enum Status {APPROVED, PENDING, DENIED};
	
	public boolean userExists(String user){
		
		Document query = new Document();
		query.append("user", user);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext())
			return true;
		else
			return false;
	}
	
	public JsonArray getAllUsers(String userId){
		
		MongoCursor<Document> cursor = databaseService.getUserCollection().find().iterator();
		
		JsonArray jsonArray = new JsonArray();
		
		while(cursor.hasNext()){
			
			Document result = cursor.next();
			
			String user = result.getString("user");
			Date created = result.getDate("created");
			String status = result.getString("status");
			
			JsonObject json = new JsonObject();
			
			if(StringUtils.isNotEmpty(user))
				json.add("user", new JsonPrimitive(user));
			
			if(StringUtils.isNotEmpty(created.toString()))
				json.add("added", new JsonPrimitive(created.toString()));
			
			if(StringUtils.isNotEmpty(status))
				json.add("status", new JsonPrimitive(status));
			else
				json.add("status", new JsonPrimitive("Pending"));
				
			JsonObject diskSpace = fileService.getUserDiskSpace(user);
			
			json.add("space", new JsonPrimitive(diskSpace.get("normalized").getAsString()));
			
			jsonArray.add(json);
		}
		
		return jsonArray;
	}
	
	public long getTotalCapacityUsed(){
		
		MongoCursor<Document> cursor = databaseService.getUserCollection().find().iterator();
		
		long totalSize = 0;
		
		while(cursor.hasNext()){
			
			long userSpace = cursor.next().getLong("diskSpace");
			totalSize =+userSpace;
		}
		
		return totalSize;
	}
	
	public String getUserId (String userKey){
		
		Document query = new Document();
		query.append("userKey", userKey);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext())
			return results.iterator().next().getString("user");
		else
			return null;
	}
	
	public JsonArray findUsersLike(String user, int maxReturn){
		
		Document match = new Document();
		match.append("user",  Pattern.compile(user));
		
		MongoCursor<Document> cursor = databaseService.getUserCollection().find(match).limit(maxReturn).iterator();
		
		JsonArray jsonArray = new JsonArray();
		
		while(cursor.hasNext()){
			
			Document result = cursor.next();
			
			String userId = result.getString("user");
			JsonObject json = new JsonObject();
			
			if(StringUtils.isNotEmpty(userId))
				json.add("user", new JsonPrimitive(userId));
			
			jsonArray.add(json);
		}
		
		return jsonArray;
	}
	
	public boolean createUser(String userId, String password){
		
		Document document = new Document();
		document.append("user", userId);
		document.append("password", password);
		document.append("created", new Date());
		document.append("status", "Pending");
		document.append("diskSpace", FileService.USER_SPACE_SIZE);
		
		databaseService.getUserCollection().insertOne(document);
		addUserIdentifier(userId, password);
		
		return userExists(userId);
	}
	
	public String getUserKey(String user, String password){
		
		if(!userExists(user))
			return null;
		
		return addUserIdentifier(user, password);
	}
	
	public boolean isAuthenticated(String user, String password){
	
		Document query = new Document();
		query.append("user", user);
		query.append("password", password);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext())
			return true;
		else
			return false;
	}
	
	public boolean hasExhaustedLoginAttempts(String userId){
		
		incrementLoginAttempt(userId, new Date());
		
		int numberAttempts = getNumLoginAttempts(userId);
		boolean enoughTimeBetweenAttempts = hasEnoughTimeHasLapsed(userId);
		
		if(numberAttempts<MAX_ATTEMPTS)
			return false;
		else if(numberAttempts==MAX_ATTEMPTS && enoughTimeBetweenAttempts)
			return false;
		else if(numberAttempts==MAX_ATTEMPTS && !enoughTimeBetweenAttempts)
			return true;
		else
			return true;
	}
	
	public boolean hasEnoughTimeHasLapsed(String userId){
		
		Date lastLoginAttemptTime = getLastLoginAttemptTime(userId);
		DateTime last = new DateTime(lastLoginAttemptTime);
		
		DateTime now = new DateTime(new Date());
		
		int numberDays = Days.daysBetween(last, now).getDays();
		
		if(numberDays< DAY_LIMIT)
			return false;
		else
			return true;
	}
	
	public Date getLastLoginAttemptTime(String userId){
		
		Document query = new Document();
		query.append("user", userId);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		Document document = results.first();
		
		if(document.getDate("lastLoginAttempt")==null){
			
			Date now = new Date();
			
			Document match = new Document();
			match.append("user", userId);
			
			Document update = new Document();
			update.append("$set", new Document("lastLoginAttempt", now));
			
			databaseService.getUserCollection().updateOne(match, update);
			
			return now;
		}
		
		return document.getDate("lastLoginAttempt");
	}
	
	public int getNumLoginAttempts(String userId){
		
		Document query = new Document();
		query.append("user", userId);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		Document document = results.first();
		
		try{
			
			int count = document.getInteger("loginAttempts");
			
			return count;
		}
		catch(Exception e){
			
			Document match = new Document();
			match.append("user", userId);
			
			Document update = new Document();
			update.append("$set", new Document("loginAttempts", 0));
			
			databaseService.getUserCollection().updateOne(match, update);
			
			return 0;	
		}
	}
	
	public void incrementLoginAttempt(String userId, Date date){
		
		Document match = new Document();
		match.append("user", userId);
		
		Document update = new Document();
		update.append("$inc", new Document("loginAttempts", 1));
		update.append("$set", new Document("lastLoginAttempt", date));
		
		databaseService.getUserCollection().updateOne(match, update);
	}
	
	public void removeLoginAttempts(String userId){
		
		Document match = new Document();
		match.append("user", userId);
		
		Document update = new Document();
		update.append("$unset", new Document("loginAttempts", 1).append("lastLoginAttempt", 1));
		
		databaseService.getUserCollection().updateOne(match, update);
	}
	
	public boolean removeUser(String actionUser, String targetUser){
		
		ArrayList<String> fileIds = getUserFileIds(targetUser);
		
		if(!fileIds.isEmpty()){
			for(String fileId : fileIds){
				if(fileService.remove(fileId, actionUser)==false)
					return false;
			}
		}
		
		if(!userExists(targetUser))
			return false;
		
		databaseService.getUserCollection().deleteOne(new Document("user", targetUser));
		
		if(userExists(targetUser))
			return true;
		else
			return false;
	}
	
	public ArrayList<String> getUserFileIds(String userId){
		
		Document query = new Document();
		query.append("owner", userId);
		
		FindIterable<Document> results = databaseService.getFileCollection().find(query);
		
		ArrayList<String> fileIds = new ArrayList<>();
		
		results.forEach(new Block<Document>() {
		    
			@Override
		    public void apply(final Document document) {
		    	
				String fileId = document.get("_id").toString();
				
				fileIds.add(fileId);
		    }
		});
		
		return fileIds;
	}
	
	public boolean shareFile(String userId, String actionUserId, String fileId){
		
		Document shareInfo = new Document();
		shareInfo.append("_id", fileId);
		shareInfo.append("sharerId", actionUserId);
		shareInfo.append("shareDate", new Date());
		
		Document match = new Document();
		match.append("user", userId);
		
		Document update = new Document();
		update.append("$addToSet", new Document("shared", shareInfo));
		
		UpdateResult results = databaseService.getUserCollection().updateOne(match, update);
		
		if(results.getModifiedCount()>0)
			return true;
		else
			return false;
	}
	
	public boolean removeShare(String userId, String fileId){
		
		Document match = new Document();
		match.append("user", userId);
		
		Document update = new Document();
		update.append("$pull", new Document("shared", new Document("_id", fileId)));
		
		UpdateResult results = databaseService.getUserCollection().updateOne(match, update);
		
		if(results.getModifiedCount()>0)
			return true;
		else
			return false;
	}
	
	
	public boolean userHasFileAccess(String userId, String fileId){
		
		Document match = new Document();
		match.append("user", userId);
		match.append("shared", new Document("$elemMatch", new Document("_id", fileId)));
		
		FindIterable<Document> results = databaseService.getUserCollection().find(match);
		Document document = results.first();
		
		if(document == null || document.isEmpty())
			return false;
		else
			return true;
	}
	
	public boolean isUserAdmin(String user){
		
		Document query = new Document();
		query.append("user", user);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext())
			return results.iterator().next().getBoolean("admin", false);
		else
			return false;
	}
	
	private String addUserIdentifier(String user, String password){
		
		Document match = new Document();
		match.append("user", user);
		match.append("password", password);;
		
		String passwordHash = UserUtils.createSecureIdentifier();
		
		Document update = new Document();
		update.append("$set", new Document("userKey", passwordHash));
		
		databaseService.getUserCollection().updateOne(match, update);
		
		return passwordHash;
	}
	
	public boolean approveUser(String user){
		
		Document match = new Document();
		match.append("user", user);
		
		Document update = new Document();
		update.append("$set", new Document("status", StatusType.APPROVED.getStatus()));
		
		databaseService.getUserCollection().updateOne(match, update);
		
		SocketService.notifyAccessUpdate(user, StatusType.APPROVED);
		
		return isUserApproved(user);
	}
	
	public boolean denyUser(String user){
		
		Document match = new Document();
		match.append("user", user);
		
		Document update = new Document();
		update.append("$set", new Document("status", StatusType.DENIED.getStatus()));
		
		databaseService.getUserCollection().updateOne(match, update);
		
		SocketService.notifyAccessUpdate(user, StatusType.DENIED);
		
		return isUserDenied(user);
	}
	
	public boolean isUserApproved(String user){
		
		Document query = new Document();
		query.append("user", user);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext()){
			
			Document document = results.iterator().next();
			String status = document.getString("status");
			
			if(status == null || status.isEmpty() || !status.equalsIgnoreCase(StatusType.APPROVED.getStatus()))
				return false;
			else
				return true;
		}
		else
			return false;
	}
	
	public boolean isUserDenied(String user){
		
		Document query = new Document();
		query.append("user", user);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext()){
			
			Document document = results.iterator().next();
			String status = document.getString("status");
			
			if(status == null || status.isEmpty() || !status.equalsIgnoreCase(StatusType.DENIED.getStatus()))
				return false;
			else
				return true;
		}
		else
			return false;
	}
	
	private boolean userKeyExists(String user, String userKey){
		
		Document query = new Document();
		query.append("user", user);
		query.append("userKey", userKey);
		
		FindIterable<Document> results = databaseService.getUserCollection().find(query);
		
		if(results.iterator().hasNext())
			return true;
		else
			return false;
	}
	
	public boolean removeUserIdentifier(String user, String userKey){
		
		if(!userKeyExists(user, userKey))
			return false;
		
		Document match = new Document();
		match.append("user", user);
		match.append("userKey", userKey);;
		
		Document update = new Document();
		update.append("$unset", new Document("userKey", 1));
		
		databaseService.getUserCollection().updateOne(match, update);
		
		if(!userKeyExists(user, userKey))
			return true;
		else
			return false;
	}


}
