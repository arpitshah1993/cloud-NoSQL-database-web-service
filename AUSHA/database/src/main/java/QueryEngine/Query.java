package QueryEngine;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import CustomException.CollectionAlreadyPresent;
import CustomException.GException;
import CustomException.InvalidCollection;
import CustomException.InvalidDatabse;
import CustomException.InvalidOperation;
import CustomException.InvalidSyntax;
import Database.Database;
import Database.DatabaseFactory;
import KeyCreator.iBigInteger;

public class Query {

	static Database db;
	
	public Query(){
		try {
			String text = new String(Files.readAllBytes(Paths.get("Key.txt")), StandardCharsets.UTF_8);
			//previously it was just this
			//new iBigInteger(text.replace("\n", "").replace("\r", ""));;
			//so, took it into one reference, so that It can call disposable for clean up and our logic got executed.
			new iBigInteger(text.replace("\n", "").replace("\r", ""));
		} catch (Exception e) {
			try {
				FileWriter out = new FileWriter("Key.txt");
				out.write("0");
				out.close();
				String text = new String(Files.readAllBytes(Paths.get("Key.txt")), StandardCharsets.UTF_8);
				new iBigInteger(text.replace("\n", "").replace("\r", ""));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
	}
	
	//parsing the query to decide the operation
	public static String queryParser(String query) throws Exception{
		System.out.println(query);
		String operationName,collectionName="",JsonArguments;
		try{
		int firstDivider=query.indexOf('.');
		if(firstDivider==-1){
			int firstBracket=query.indexOf('(');
			int lastBracket=query.lastIndexOf(')');
			operationName=query.substring(0,firstBracket);
			JsonArguments=query.substring(firstBracket+1,lastBracket);
		}
		else{
			int secondDivider=query.indexOf('.',firstDivider+1);
			String datbaseName=query.substring(0, firstDivider);	
			
			if(secondDivider==-1){
				int firstBracket=query.indexOf('(', firstDivider+1);
				int lastBracket=query.lastIndexOf(')');
				 operationName=query.substring(firstDivider+1,firstBracket);
				 if(!operationName.equals("createDatabase"))
					 db=DatabaseFactory.getDatabase(datbaseName,false);
				 JsonArguments=query.substring(firstBracket+1,lastBracket);
			}
			else{
			db=DatabaseFactory.getDatabase(datbaseName,false);
			collectionName=query.substring(firstDivider+1, secondDivider);
			db.isCollectionExist(collectionName);
			int firstBracket=query.indexOf('(', secondDivider+1);
			int lastBracket=query.lastIndexOf(')');
			 operationName=query.substring(secondDivider+1,firstBracket);
			 JsonArguments=query.substring(firstBracket+1,lastBracket);
			}
		}
		
		switch(operationName){
		case "createDatabase":
			DatabaseFactory.getDatabase(JsonArguments,true);
			return "Database created.";
		case "deleteDatabase":
			DatabaseFactory.deleteDatabase(JsonArguments);
			return "Database deleted.";
		case "createIndex":
			String[] entityNames=JsonArguments.split(",");
			StringBuffer sb=new StringBuffer();
			for(String entity:entityNames){
				sb.append(db.createIndex(collectionName, entity)+System.lineSeparator());
			}
			return sb.toString();
		case "insert":
			if(db.insert(collectionName, JsonArguments))
			return "row Ineserted.";
		case "update":
			return db.update(collectionName, JsonArguments);
		case "delete":
			return db.delete(collectionName, JsonArguments);
		case "print":
			 db.printIndedx(collectionName);
			 break;
		case "select":
			return db.select(collectionName, JsonArguments).toString();
		case "deleteCollection":
			if(db.deleteCollection(collectionName))
			return "Collection removed.";
			break;
		case "createCollection":
			if(db.createCollection(JsonArguments))
				return "Collection created.";
			break;
		default:
			throw new InvalidOperation();
		}
		return "No operation Performed";
		}
		catch(InvalidOperation e){
			throw e;
		}
		catch(InvalidDatabse e){
			throw e;
		}
		catch(CollectionAlreadyPresent e){
			throw e;
		}
		catch(GException e){
			throw e;
		}
		catch(InvalidCollection e){
			throw e;
		}
		catch(InvalidSyntax e){
			throw e;
		}
		catch(Exception e){
			throw new InvalidOperation();
		}
	}

	public static void main(String [] args){
		try{
			new Query();
			String s=new String("");
			System.out.println("Database Started");
			while(!s.equals("quit")){
				try{
					//System.out.print(s+""+s.equals("quit"));
					System.out.print(">>");
					Scanner in=new Scanner(System.in);
					s=in.nextLine();
					long startTime=System.currentTimeMillis();
					System.out.println(String.valueOf(Query.queryParser(s)+" "+(((long)System.currentTimeMillis())-startTime))+" ms");
				}
				catch(Exception e){
					System.out.println(e.getMessage());
				}
			}
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
		finally{
			iBigInteger.terminate();
		}

	}
}


