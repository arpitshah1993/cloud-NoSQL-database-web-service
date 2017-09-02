package Authorizor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

import CustomException.InvalidSyntax;

public class UserData {
	private HashMap<String,String> adminUserInformation=new HashMap<>();
	private HashMap<String,String> userInformation=new HashMap<>();
	private static UserData user=null;
	public static UserData getInstance(){
		if(user==null)
			user = new UserData("privateAUSHA/Userdata/users.txt");
		return user;
	}
	private UserData(String databaseName){
		System.out.println("User Constructed!");
//		try {  
			initialize(databaseName);
			try (BufferedReader br = new BufferedReader(new FileReader(databaseName))) {
				Object[] lines = br.lines().toArray();
				for(Object line:lines){
					String s = line.toString();
					JSONObject jsonObj = new JSONObject(s);
					String us = jsonObj.getString("username");
					String pw = jsonObj.getString("password");
					if(jsonObj.getString("permission").equals("Admin"))
						adminUserInformation.put(us, pw);
					else
						userInformation.put(us, pw);
				
				}
			}
			catch (Exception e) {
				//throw new InvalidSyntax();
			}
//			String content=new String(Files.readAllBytes(Paths.get(databaseName)));
//			JSONArray jArray = new JSONArray(content);
//			for (int i = 0; i < jArray.length(); i++) {       
//				JSONObject jsonObj = jArray.getJSONObject(i);
//				String us = jsonObj.getString("username");
//				String pw = jsonObj.getString("password");
//				if(jsonObj.getString("permission").equals("Admin"))
//					adminUserInformation.put(us, pw);
//				else
//					userInformation.put(us, pw);
//			}
//		}
//		catch(Exception e){
//		}
	}
	
	public String createUser(String userName, String password, String permission) throws IOException{
		if(permission.equals("Admin")){
			if(adminUserInformation.containsKey(userName))
				return "Username is already exists!";
				else
					adminUserInformation.put(userName, password);
		}
		if(userInformation.containsKey(userName))
			return "Username is already exists!";
			else
				userInformation.put(userName, password);
		
		// writing update to data
		BufferedWriter out = null;
		FileWriter fstream = new FileWriter("privateAUSHA/Userdata/users.txt", true); // true
		out = new BufferedWriter(fstream);
		out.write("{\"username\":\""+userName+"\", \"password\":\""+password+"\",\"permission\":\""+permission+"\"}" + "\n");
		out.close();
		return "User is sucessfully created!";
	}

	//this will create the private data folder if not is there
	//Ex. this file contains data about users,we have to have that file. If someone has installed this database
	//first time, it wont have that folers. So, this will create when it start the database first time and put 
	//Admin user
	private void initialize(String databaseName){ 
		File dir1 = new File("privateAUSHA");
		if(!dir1.exists()){
			dir1.mkdir();
			File dir2 = new File("privateAUSHA/Userdata");
			dir2.mkdir();
			FileWriter out;
			try {
				out = new FileWriter(databaseName);
				out.write("{\"username\":\"Admin\", \"password\":\"1234\",\"permission\":\"Admin\"}"+ "\n");
				out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public boolean isUserAuthorized(String userName,String password){
		boolean b=userInformation.containsKey(userName) && userInformation.get(userName).equals(password);
		return b;
	}
	
	public boolean isAdminUserAuthorized(String userName,String password){
		boolean b=adminUserInformation.containsKey(userName) && adminUserInformation.get(userName).equals(password);
		return b;
	}
}

