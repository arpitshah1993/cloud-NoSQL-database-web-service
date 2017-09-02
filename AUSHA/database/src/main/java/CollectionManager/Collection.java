package CollectionManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import CustomException.InvalidOperation;
import CustomException.InvalidSyntax;
import KeyCreator.iBigInteger;

public class Collection {
	class ObjectKey implements Comparable<ObjectKey> {
		Object obj;

		ObjectKey(Object obj) {
			this.obj = obj;
		}

		public boolean equals(ObjectKey o) {
			// the data type won't depends on input comparator, it will always
			// be input data type
			if (obj instanceof String)
				return this.obj.toString().compareToIgnoreCase(o.obj.toString()) == 0;
			else
				return Double.parseDouble((String) obj) == Double.parseDouble((String) o.obj);
		}

		public int hashCode() {
			if (this.obj instanceof String)
				return ((String) this.obj).hashCode();
			else
				return ((Double) this.obj).hashCode();
		}

		@Override
		public int compareTo(ObjectKey o) {
			if (obj instanceof String)
				return this.obj.toString().compareToIgnoreCase(o.obj.toString());
			else
				return (int) (Double.parseDouble((String) obj) - Double.parseDouble((String) o.obj));
		}
	}

	private String collectionName;
	private Map<String, ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>>> index;
	private Map<BigInteger, JSONObject> collection;
	private String databaseName;

	public Collection(String collectionName, String databaseName) throws InvalidSyntax {
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		collection = new ConcurrentHashMap<>();
		index = new ConcurrentHashMap<>();
		initializeCollection();
	}

	// insert the all the documents from folder of collection
	public void initializeCollection() throws InvalidSyntax {
		File directory = new File(databaseName + "/" + collectionName);
		if (!(directory.exists() && directory.isDirectory()))
			return;
		File[] textDox = directory.listFiles();
		for (File textFiles : textDox) {
			try (BufferedReader br = new BufferedReader(new FileReader(textFiles))) {
				Object[] lines = br.lines().toArray();
				String s = lines[lines.length - 1].toString();
				JSONObject obj = new JSONObject(s);
				String key = obj.getString("objectId");
				obj.remove("objectId");
				collection.put(new BigInteger(key), obj);
			} catch (Exception e) {
				throw new InvalidSyntax();
			}
		}
	}

	// insert the data entry set
	public boolean insert(String dataEntry) throws InvalidSyntax {
		try {
			JSONObject data = new JSONObject(dataEntry);
			BigInteger id = iBigInteger.getId();
			collection.put(id, data);

			JSONObject dataTemp = new JSONObject(dataEntry);
			dataTemp.put("objectId", id.toString());
			FileWriter out = new FileWriter(databaseName + "/" + collectionName + "/" + id + ".txt");
			out.write(dataTemp.toString() + "\n");
			out.close();

			// persisting index data if it is created for one of the
			// column(entity)
			String[] entities = JSONObject.getNames(data);
			for (String entity : entities) {
				if (index.containsKey(entity)) {
					ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>> indexMap = index.get(entity);
					Object obj = data.get(entity);
					indexMap.put(new ObjectKey(obj), indexMap.getOrDefault(obj, new ArrayList<BigInteger>()));
					indexMap.get(obj).add(id);
				}
			}

		} catch (Exception e) {
			throw new InvalidSyntax();
		}
		System.out.println("row Ineserted.");
		return true;
	}

	// check whether it is valid JSONObject
	private boolean isValidJson(String jsonStr) {
		boolean isValid = false;
		try {
			new JSONObject(jsonStr);
			isValid = true;
		} catch (JSONException je) {
			isValid = false;
		}
		return isValid;
	}

	// check whether it is valid JSONArray
	private boolean isValidJsonArray(String jsonStr) {
		boolean isValid = false;
		try {
			new JSONArray(jsonStr);
			isValid = true;
		} catch (JSONException je) {
			isValid = false;
		}
		return isValid;
	}

	// filter the data base on indexed entities
	public void indexedEntityFiltering(ArrayList<BigInteger> findingSet, String entity, JSONObject data)
			throws JSONException, InvalidOperation {
		ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>> indexTemp = index.get(entity);
		Object dataObj = data.get(entity);
		if (isValidJson(dataObj.toString())) {
			JSONObject obj = (JSONObject) dataObj;
			String relate = JSONObject.getNames(obj)[0];
			Object value = obj.get(relate);
			switch (relate) {
			// for greater than comparison
			case "gt":
				for (ArrayList<BigInteger> objectIds : indexTemp.tailMap(new ObjectKey(value)).values()) {
					findingSet.addAll(objectIds);
				}
				break;
				// for less than comparison
			case "lt":
				for (ArrayList<BigInteger> objectIds : indexTemp.headMap(new ObjectKey(value)).values()) {
					findingSet.addAll(objectIds);
				}
				break;
				// for in between comparison
			case "ib":
				String[] keys = obj.getString(relate).split("-");
				for (ArrayList<BigInteger> objectIds : indexTemp
						.subMap(new ObjectKey((Object) (keys[0])), new ObjectKey((Object) (keys[1]))).values()) {
					findingSet.addAll(objectIds);
				}
				break;
			default:
				throw new InvalidOperation();
			}
		} else {
			ArrayList<BigInteger> fileteredIds = indexTemp.get(dataObj);
			findingSet.addAll(fileteredIds);
		}
	}

	// filter the data on base of input query
	public ArrayList<BigInteger> find(String dataQuery) throws InvalidSyntax {
		ArrayList<BigInteger> ids = new ArrayList<>();
		try {
			JSONObject data = new JSONObject(dataQuery);
			String[] entities = JSONObject.getNames(data);
			if (entities == null)
				return new ArrayList<BigInteger>(collection.keySet());

			// spawning 4 thread to filter data for each indexed entity
			ExecutorService executorFilterWithIndexedEntity = Executors.newFixedThreadPool(4);
			ArrayList<BigInteger> findingSet = new ArrayList<BigInteger>();
			boolean isQueryContainsIndexedEntity = false;
			int l = entities.length;
			for (int i = 0; i < l; i++) {
				final int p = i;
				if (index.containsKey(entities[i])) {
					isQueryContainsIndexedEntity = true;
					executorFilterWithIndexedEntity.execute(() -> {
						try {
							indexedEntityFiltering(findingSet, entities[p], data);
						} catch (Exception e) {
							e.printStackTrace();
						}
						entities[p] = null;
						System.out.print("Filtered Set using Index:");
						System.out.println(findingSet);
					});
				}
			}
			executorFilterWithIndexedEntity.shutdown();
			executorFilterWithIndexedEntity.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			// if there is no entity in query which is indexed entity
			if (!isQueryContainsIndexedEntity)
				findingSet.addAll(collection.keySet());

			System.out.print("Final Filtered Set using Index:");
			System.out.println(findingSet);

			// spawning 4 thread to filter data for each filtered non indexed
			// entity
			ExecutorService executorFilterWithoutIndexedEntity = Executors.newFixedThreadPool(4);
			for (BigInteger keyId : findingSet) {
				executorFilterWithoutIndexedEntity.execute(() -> {
					try {
						JSONObject dataValue = collection.get(keyId);
						if (filterIdsOnRemainingEntity(entities, dataValue, data))
							ids.add(keyId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
			executorFilterWithoutIndexedEntity.shutdown();
			executorFilterWithoutIndexedEntity.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (Exception e) {
			throw new InvalidSyntax();
		}
		return ids;
	}

	// filtering data on based query with remaining entity
	boolean filterIdsOnRemainingEntity(String[] entities, JSONObject dataValue, JSONObject data)
			throws JSONException, InvalidOperation {
		boolean shouldAdd = true;
		for (String entity : entities) {
			if (dataValue.has(entity)) {
				Object dataObj = data.get(entity);
				if (isValidJson(dataObj.toString())) {
					JSONObject obj = (JSONObject) dataObj;
					String relate = JSONObject.getNames(obj)[0];
					switch (relate) {
					case "gt":
						if (dataValue.get(entity) instanceof String) {
							String value = obj.getString(relate);
							if (!((dataValue.getString(entity).compareToIgnoreCase(value)) > 0))
								shouldAdd = false;
						} else {
							double value = obj.getDouble(relate);
							if (!((dataValue.getDouble(entity) > value)))
								shouldAdd = false;
						}
						break;
					case "lt":
						if (dataValue.get(entity) instanceof String) {
							String value = obj.getString(relate);
							if (!((dataValue.getString(entity).compareToIgnoreCase(value)) < 0))
								shouldAdd = false;
						} else {
							double value = obj.getDouble(relate);
							if (!((dataValue.getDouble(entity) < value)))
								shouldAdd = false;
						}
						break;
					case "ib":
						String[] keys = obj.getString(relate).split("-");
						if (dataValue.get(entity) instanceof String) {
							if (!((dataValue.getString(entity).compareToIgnoreCase(keys[0])) > 0
									&& (dataValue.getString(entity).compareToIgnoreCase(keys[1])) < 0))
								shouldAdd = false;
						} else {
							if (!(dataValue.getDouble(entity) > Double.parseDouble(keys[0])
									&& dataValue.getDouble(entity) < Double.parseDouble(keys[1])))
								shouldAdd = false;
						}
						break;
					default:
						throw new InvalidOperation();
					}
				} else {
					if (dataValue.get(entity) instanceof String) {
						String value = data.getString(entity);
						if (!((dataValue.getString(entity).compareToIgnoreCase(value)) == 0))
							shouldAdd = false;
					} else {
						if (!((dataValue.getDouble(entity) == data.getDouble(entity))))
							shouldAdd = false;
					}
				}
			}
			if (!shouldAdd)
				break;
		}
		return shouldAdd;
	}

	// delete the data entry
	public String delete(String deleteData) throws InvalidSyntax {
		ArrayList<BigInteger> ids;
		try {
			ids = find(deleteData);
			for (BigInteger id : ids) {
				// update the indexed entities
				JSONObject data = collection.get(id);
				String[] entities = JSONObject.getNames(data);
				for (String entity : entities) {
					if (index.containsKey(entity)) {
						ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>> indexMap = index.get(entity);
						Object obj = data.get(entity);
						indexMap.get(obj).remove(id);
					}
				}

				collection.remove(id);
				File file = new File(databaseName + "/" + collectionName + "/" + id + ".txt");
				file.delete();
			}
		} catch (Exception e) {
			throw new InvalidSyntax();
		}
		System.out.println(ids.size() + " rows deleted.");
		return (ids.size() + " rows deleted.");
	}

	// create Index for given entity
	public String createIndex(String entity) {
		ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>> indexTemp = new ConcurrentSkipListMap<>();
		try {
			for (Map.Entry<BigInteger, JSONObject> currentvar : collection.entrySet()) {
				JSONObject data = (JSONObject) currentvar.getValue();
				Object key = data.get(entity);
				ArrayList<BigInteger> value = index.containsKey(key) ? value = indexTemp.get(key)
						: new ArrayList<BigInteger>();
				value.add(currentvar.getKey());
				indexTemp.put(new ObjectKey(key), value);
			}
			index.put(entity, indexTemp);
			return "Index is created for Entity: " + entity + ".";
		} catch (Exception e) {
			System.out.println("Exception occurred: " + e.getMessage());
		}
		return "Index couldn't be created for Entity: " + entity + ".";
	}

	// print the Tree Map
	public void print() {
		for (Entry<String, ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>>> indexTemp : index.entrySet()) {
			System.out.println("For index column:" + indexTemp.getKey());
			for (Entry<ObjectKey, ArrayList<BigInteger>> currentvar : indexTemp.getValue().entrySet()) {
				System.out.println("Key is " + currentvar.getKey().obj + "  and value " + currentvar.getValue());
			}
		}
	}

	// Select particular data entity and print the database
	public JSONArray Select(String selectData) throws InvalidSyntax {
		JSONArray arr = new JSONArray();
		StringBuffer buf = new StringBuffer();
		try {
			ArrayList<BigInteger> ids = new ArrayList<BigInteger>();
			String[] entity = null;
			if (selectData.equals("") || selectData.equals("{}")) {
				ids.addAll(collection.keySet());
			} else {
				JSONObject selectDataQuery = new JSONObject(selectData);
				String selectionBase;
				if (selectDataQuery.has("where"))
					selectionBase = selectDataQuery.get("where").toString();
				else
					selectionBase = "{}";
				ids.addAll(find(selectionBase));
				if (selectDataQuery.has("select")) {
					JSONObject selectedData = (JSONObject) selectDataQuery.get("select");
					entity = JSONObject.getNames(selectedData);
				}
			}
			for (BigInteger id : ids) {
				JSONObject data = collection.get(id);
				JSONObject dataResult = new JSONObject();

				if (entity == null)
					dataResult = data;
				else {
					dataResult.put("objectId", id);
					for (String entityName : entity) {
						if (data.has(entityName)) {
							dataResult.put(entityName, data.get(entityName));
						}
					}
				}
				buf.append(dataResult.toString() + "\n");
				arr.put(dataResult);
			}

		} catch (JSONException e) {
			throw new InvalidSyntax();
		}
		System.out.println(arr.length() + " rows found.");
		System.out.println(buf);
		return arr;
	}

	// update the data entry
	public String update(String updateQuery) throws InvalidSyntax {
		ArrayList<BigInteger> ids = new ArrayList<BigInteger>();
		try {
			JSONObject updateDataQuery = new JSONObject(updateQuery);
			String updateBase = updateDataQuery.get("where").toString();
			ids = find(updateBase);
			JSONObject updateData = (JSONObject) updateDataQuery.get("set");
			String[] entityNames = JSONObject.getNames(updateData);
			if (entityNames == null)
				return (0 + " rows updated.");
			for (BigInteger id : ids) {
				JSONObject idData = collection.get(id);
				for (String entity : entityNames) {
					// removing if has indexed entity with old value
					if (index.containsKey(entity)) {
						if (idData.has(entity)) {
							Object oldValue = idData.getString(entity);
							index.get(entity).get(oldValue).remove(id);
						}
					}

					idData.put(entity, updateData.get(entity));

					// updating new value
					if (index.containsKey(entity)) {
						ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>> indexedEnity = index.get(entity);
						indexedEnity.put(new ObjectKey(updateData.get(entity)),
								indexedEnity.getOrDefault(updateData.get(entity), new ArrayList<BigInteger>()));
						indexedEnity.get(updateData.get(entity)).add(id);
					}
				}
				collection.put(id, idData);

				// writing update to data
				BufferedWriter out = null;
				FileWriter fstream = new FileWriter(databaseName + "/" + collectionName + "/" + id + ".txt", true); // true
				// tells
				// to
				// append
				// data.
				out = new BufferedWriter(fstream);
				JSONObject idDataTemp = collection.get(id);
				idDataTemp.put("objectId", id.toString());
				out.write(idDataTemp.toString() + "\n");
				out.close();
			}
		} catch (Exception e) {
			throw new InvalidSyntax();
		}
		System.out.println(ids.size() + " rows updated.");
		return (ids.size() + " rows updated.");
	}

	// print the database
	public void printAllData() {
		for (Map.Entry<BigInteger, JSONObject> currentvar : collection.entrySet()) {
			System.out.println("Key is " + currentvar.getKey() + "  and value " + currentvar.getValue());
		}
	}
}
/*
// //find the collection of data on base of query
// public ArrayList<BigInteger> find(String dataQuery) throws InvalidSyntax{
// ArrayList<BigInteger> ids=new ArrayList<>();
// try{
// JSONObject data=new JSONObject(dataQuery);
// String[] entities=JSONObject.getNames(data);
// if(entities==null)
// return new ArrayList<BigInteger>(collection.keySet());
//
// ArrayList<BigInteger> findingSet=new ArrayList<BigInteger>();
// for(String entity:entities){
// if(entity.equals(indexCreated)){
// ArrayList<BigInteger> fileteredIds=index.get(data.get(entity));
// findingSet.addAll(fileteredIds);
// entity=null; //remove the filtered entity
// break;
// }
// }
// if(findingSet.size()==0)
// findingSet.addAll(collection.keySet());
//
// for(BigInteger keyId:findingSet){
// JSONObject dataValue=collection.get(keyId);
// boolean shouldAdd=true;
// for(String entity:entities){
// if(!((dataValue.has(entity)) &&
// (dataValue.get(entity).equals(data.get(entity))))){
// shouldAdd=false;
// break;
// }
// }
// if(shouldAdd)
// ids.add(keyId);
// }
// }
// catch(Exception e){
// throw new InvalidSyntax();
// }
// return ids;
// }

// //find the collection of data on base of query
// public ArrayList<BigInteger> find(String dataQuery) throws InvalidSyntax{
// ArrayList<BigInteger> ids=new ArrayList<>();
// try{
// JSONObject data=new JSONObject(dataQuery);
// String[] entities=JSONObject.getNames(data);
// if(entities==null)
// return new ArrayList<BigInteger>(collection.keySet());
//
// ArrayList<BigInteger> findingSet=new ArrayList<BigInteger>();
// for(String entity:entities){
// if(entity.equals(indexCreated)){
// //ensure the gt,lt,ib
// Object dataObj=data.get(entity);
// if(isValidJson(dataObj.toString())){
// JSONObject obj=(JSONObject)dataObj;
// String relate=JSONObject.getNames(obj)[0];
// //int value=obj.getInt(relate);
// int value;
// switch(relate){
// case "gt":
// value=obj.getInt(relate);
// for(ArrayList<BigInteger> objectIds:index.tailMap(value).values()){
// findingSet.addAll(objectIds);
// }
// entity=null; //remove the filtered entity
// break;
// case "lt":
// value=obj.getInt(relate);
// for(ArrayList<BigInteger> objectIds:index.headMap(value).values()){
// findingSet.addAll(objectIds);
// }
// entity=null;
// break;
// case "ib":
// String[] keys=obj.getString(relate).split("-");
// for(ArrayList<BigInteger> objectIds:index.subMap(Integer.parseInt(keys[0]),
// Integer.parseInt(keys[1])).values()){
// findingSet.addAll(objectIds);
// }
// entity=null;
// break;
// default:
// throw new InvalidOperation();
// }
// }
// else{
// ArrayList<BigInteger> fileteredIds=index.get(dataObj);
// findingSet.addAll(fileteredIds);
// entity=null; //remove the filtered entity
// break;
// }
// }
// }
// if(findingSet.size()==0)
// findingSet.addAll(collection.keySet());
//
// for(BigInteger keyId:findingSet){
// JSONObject dataValue=collection.get(keyId);
// boolean shouldAdd=true;
// for(String entity:entities){
// if(dataValue.has(entity)){// &&
// (dataValue.get(entity).equals(data.get(entity))))){
// Object dataObj=data.get(entity);
// if(isValidJson(dataObj.toString())){
// JSONObject obj=(JSONObject)dataObj;
// String relate=JSONObject.getNames(obj)[0];
// //int value=obj.getInt(relate);
// int value;
// switch(relate){
// case "gt":
// value=obj.getInt(relate);
// if(!(dataValue.getInt(entity)>value))
// shouldAdd=false;
// break;
// case "lt":
// value=obj.getInt(relate);
// if(!(dataValue.getInt(entity)<value))
// shouldAdd=false;
// break;
// case "ib":
// String[] keys=obj.getString(relate).split("-");
// if(!(dataValue.getInt(entity)<Integer.parseInt(keys[1]) &&
// dataValue.getInt(entity)>Integer.parseInt(keys[0])))
// shouldAdd=false;
// break;
// default:
// throw new InvalidOperation();
// }
// }
// else if(!(dataValue.get(entity).equals(data.get(entity)))){
// shouldAdd=false;
// }
// }
// if(!shouldAdd)
// break;
// }
// if(shouldAdd)
// ids.add(keyId);
// }
// }
// catch(Exception e){
// throw new InvalidSyntax();
// }
// return ids;
// }

// public double[] getDoubleValues(String str) throws JSONException{
//
// if(isValidJsonArray(str)){
// JSONArray arr=new JSONArray(str);
// int l=arr.length();
// double[] values=new double[l];
// for(int i=0;i<l;i++){
// values[i]=arr.getDouble(i);
// }
// }
// return new double[]{Double.parseDouble(str)};
// }
//
// public String[] getStringValues(String str) throws JSONException{
//
// if(isValidJsonArray(str)){
// JSONArray arr=new JSONArray(str);
// int l=arr.length();
// String[] values=new String[l];
// for(int i=0;i<l;i++){
// values[i]=arr.getString(i);
// }
// }
// return new String[]{str};
// }

// public void indexedEntityFiltering(ArrayList<BigInteger> findingSet,String
// entity, JSONObject data) throws JSONException, InvalidOperation{
// ConcurrentSkipListMap<ObjectKey,ArrayList<BigInteger>>
// indexTemp=index.get(entity);
// //ensure the gt,lt,ib
// Object dataObj=data.get(entity);
// if(isValidJson(dataObj.toString())){
// JSONObject obj=(JSONObject)dataObj;
// String relate=JSONObject.getNames(obj)[0];
// Object value;
// switch(relate){
// case "gt":
// value=obj.get(relate);
// for(ArrayList<BigInteger> objectIds:indexTemp.tailMap(new
// ObjectKey(value)).values()){
// findingSet.addAll(objectIds);
// }
// //entity=null; //remove the filtered entity
// break;
// case "lt":
// value=obj.get(relate);
// for(ArrayList<BigInteger> objectIds:indexTemp.headMap(new
// ObjectKey(value)).values()){
// findingSet.addAll(objectIds);
// }
// //entity=null;
// break;
// case "ib":
// String[] keys=obj.getString(relate).split("-");
// for(ArrayList<BigInteger> objectIds:indexTemp.subMap(new
// ObjectKey((Object)(keys[0])), new ObjectKey((Object)(keys[1]))).values()){
// findingSet.addAll(objectIds);
// }
// //entity=null;
// break;
// default:
// throw new InvalidOperation();
// }
// }
// else{
// ArrayList<BigInteger> fileteredIds=indexTemp.get(dataObj);
// findingSet.addAll(fileteredIds);
// //entity=null; //remove the filtered entity
// }
// }

// //find the collection of data on base of query
// public ArrayList<BigInteger> find(String dataQuery) throws InvalidSyntax{
// ArrayList<BigInteger> ids=new ArrayList<>();
// try{
// JSONObject data=new JSONObject(dataQuery);
// String[] entities=JSONObject.getNames(data);
// if(entities==null)
// return new ArrayList<BigInteger>(collection.keySet());
//
// ArrayList<BigInteger> findingSet=new ArrayList<BigInteger>();
// for(String entity:entities){
// if(index.containsKey(entity)){
// TreeMap<Object, ArrayList<BigInteger>> indexTemp=new
// TreeMap<>(index.get(entity));
// //ensure the gt,lt,ib
// Object dataObj=data.get(entity);
// if(isValidJson(dataObj.toString())){
// JSONObject obj=(JSONObject)dataObj;
// String relate=JSONObject.getNames(obj)[0];
// //int value=obj.getInt(relate);
// int value;
// switch(relate){
// case "gt":
// value=obj.getInt(relate);
// for(ArrayList<BigInteger> objectIds:indexTemp.tailMap(value).values()){
// findingSet.addAll(objectIds);
// }
// entity=null; //remove the filtered entity
// break;
// case "lt":
// value=obj.getInt(relate);
// for(ArrayList<BigInteger> objectIds:indexTemp.headMap(value).values()){
// findingSet.addAll(objectIds);
// }
// entity=null;
// break;
// case "ib":
// String[] keys=obj.getString(relate).split("-");
// for(ArrayList<BigInteger>
// objectIds:indexTemp.subMap(Integer.parseInt(keys[0]),
// Integer.parseInt(keys[1])).values()){
// findingSet.addAll(objectIds);
// }
// entity=null;
// break;
// default:
// throw new InvalidOperation();
// }
// }
// else{
// ArrayList<BigInteger> fileteredIds=indexTemp.get(dataObj);
// findingSet.addAll(fileteredIds);
// entity=null; //remove the filtered entity
// break;
// }
// }
// }
// if(findingSet.size()==0)
// findingSet.addAll(collection.keySet());
//
// for(BigInteger keyId:findingSet){
// JSONObject dataValue=collection.get(keyId);
// boolean shouldAdd=true;
// for(String entity:entities){
// if(dataValue.has(entity)){// &&
// (dataValue.get(entity).equals(data.get(entity))))){
// Object dataObj=data.get(entity);
// if(isValidJson(dataObj.toString())){
// JSONObject obj=(JSONObject)dataObj;
// String relate=JSONObject.getNames(obj)[0];
// //int value=obj.getInt(relate);
// int value;
// switch(relate){
// case "gt":
// value=obj.getInt(relate);
// if(!(dataValue.getInt(entity)>value))
// shouldAdd=false;
// break;
// case "lt":
// value=obj.getInt(relate);
// if(!(dataValue.getInt(entity)<value))
// shouldAdd=false;
// break;
// case "ib":
// String[] keys=obj.getString(relate).split("-");
// if(!(dataValue.getInt(entity)<Integer.parseInt(keys[1]) &&
// dataValue.getInt(entity)>Integer.parseInt(keys[0])))
// shouldAdd=false;
// break;
// default:
// throw new InvalidOperation();
// }
// }
// else if(!(dataValue.get(entity).equals(data.get(entity)))){
// shouldAdd=false;
// }
// }
// if(!shouldAdd)
// break;
// }
// if(shouldAdd)
// ids.add(keyId);
// }
// }
// catch(Exception e){
// throw new InvalidSyntax();
// }
// return ids;
// }

// find the collection of data on base of query
// public ArrayList<BigInteger> find(String dataQuery) throws InvalidSyntax{
// ArrayList<BigInteger> ids=new ArrayList<>();
// try{
// JSONObject data=new JSONObject(dataQuery);
// String[] entities=JSONObject.getNames(data);
// if(entities==null)
// return new ArrayList<BigInteger>(collection.keySet());
//
// ArrayList<BigInteger> findingSet=new ArrayList<BigInteger>();
// for(String entity:entities){
// if(index.containsKey(entity)){
// indexedEntityFiltering(findingSet, entity, data);
// entity=null;
// }
// }
//
// if(findingSet.size()==0)
// findingSet.addAll(collection.keySet());
//
// //filter the ids in findingSet(we got) now manually for the entities which
// were not indexed
// for(BigInteger keyId:findingSet){
// JSONObject dataValue=collection.get(keyId);
// boolean shouldAdd=true;
// for(String entity:entities){
// if(dataValue.has(entity)){// &&
// (dataValue.get(entity).equals(data.get(entity))))){
// Object dataObj=data.get(entity);
// if(isValidJson(dataObj.toString())){
// JSONObject obj=(JSONObject)dataObj;
// String relate=JSONObject.getNames(obj)[0];
// //int value=obj.getInt(relate);
// int value;
// switch(relate){
// case "gt":
// value=obj.getInt(relate);
// if(!(dataValue.getInt(entity)>value))
// shouldAdd=false;
// break;
// case "lt":
// value=obj.getInt(relate);
// if(!(dataValue.getInt(entity)<value))
// shouldAdd=false;
// break;
// case "ib":
// String[] keys=obj.getString(relate).split("-");
// if(!(dataValue.getInt(entity)<Integer.parseInt(keys[1]) &&
// dataValue.getInt(entity)>Integer.parseInt(keys[0])))
// shouldAdd=false;
//
// break;
// default:
// throw new InvalidOperation();
// }
// }
// else if(!(dataValue.get(entity).equals(data.get(entity)))){
// shouldAdd=false;
// }
// }
// if(!shouldAdd)
// break;
// }
// if(shouldAdd)
// ids.add(keyId);
// }
// }
// catch(Exception e){
// throw new InvalidSyntax();
// }
// return ids;
// }

// //filter the data on base of input query
// public ArrayList<BigInteger> find(String dataQuery) throws InvalidSyntax{
// ArrayList<BigInteger> ids=new ArrayList<>();
// try{
// JSONObject data=new JSONObject(dataQuery);
// String[] entities=JSONObject.getNames(data);
// if(entities==null)
// return new ArrayList<BigInteger>(collection.keySet());
//
// //spawning 4 thread to filter data for each indexed entity
// ExecutorService
// executorFilterWithIndexedEntity=Executors.newFixedThreadPool(4);
// ArrayList<BigInteger> findingSet=new ArrayList<BigInteger>();
// int l=entities.length;
// for(int i=0;i<l;i++){
// final int p=i;
// if(index.containsKey(entities[i])){
// executorFilterWithIndexedEntity.execute(()->{
// try {
// indexedEntityFiltering(findingSet, entities[p], data);
// } catch (Exception e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// entities[p]=null;
// //latch.countDown();
// System.out.print("Filtered Set using Index:");
// System.out.println(findingSet);
// });
// }
// }
//
// executorFilterWithIndexedEntity.shutdown();
// executorFilterWithIndexedEntity.awaitTermination(Long.MAX_VALUE,
// TimeUnit.NANOSECONDS);
//
// if(findingSet.size()==0)
// findingSet.addAll(collection.keySet());
//
// System.out.print("Final Filtered Set using Index:");
// System.out.println(findingSet);
//
//// //filter the ids in findingSet(we got) now manually for the entities which
// were not indexed
//// for(BigInteger keyId:findingSet){
//// JSONObject dataValue=collection.get(keyId);
//// boolean shouldAdd=false;
//// for(String entity:entities){
//// if(dataValue.has(entity)){// &&
// (dataValue.get(entity).equals(data.get(entity))))){
//// Object dataObj=data.get(entity);
//// if(isValidJson(dataObj.toString())){
//// JSONObject obj=(JSONObject)dataObj;
//// String relate=JSONObject.getNames(obj)[0];
//// //int value=obj.getInt(relate);
//// //int value;
//// switch(relate){
//// case "gt":
//// if(dataValue.get(entity) instanceof String){
//// String value=obj.getString(relate);
//// if((dataValue.getString(entity).compareToIgnoreCase(value))>0)
//// shouldAdd=true;
//// }
//// else{
//// double value=obj.getDouble(relate);
//// if((dataValue.getDouble(entity)>value))
//// shouldAdd=true;
//// }
//// break;
//// case "lt":
//// if(dataValue.get(entity) instanceof String){
//// String value=obj.getString(relate);
//// if((dataValue.getString(entity).compareToIgnoreCase(value))<0)
//// shouldAdd=true;
//// }
//// else{
//// double value=obj.getDouble(relate);
//// if((dataValue.getDouble(entity)<value))
//// shouldAdd=true;
//// }
//// break;
//// case "ib":
//// String[] keys=obj.getString(relate).split("-");
//// if(dataValue.get(entity) instanceof String){
//// if((dataValue.getString(entity).compareToIgnoreCase(keys[0]))>0 &&
// (dataValue.getString(entity).compareToIgnoreCase(keys[1]))<0)
//// shouldAdd=true;
//// }
//// else{
//// if(dataValue.getDouble(entity)>Double.parseDouble(keys[0]) &&
// dataValue.getDouble(entity)<Double.parseDouble(keys[1]))
//// shouldAdd=true;
//// }
//// break;
//// default:
//// throw new InvalidOperation();
//// }
//// }
//// else {
//// if(dataValue.get(entity) instanceof String){
//// String value=data.getString(entity);
//// if((dataValue.getString(entity).compareToIgnoreCase(value))==0)
//// shouldAdd=true;
//// }
//// else{
//// if((dataValue.getDouble(entity)==data.getDouble(entity)))
//// shouldAdd=true;
//// }
//// }
//// }
//// if(shouldAdd)
//// break;
//// }
//// if(shouldAdd)
//// ids.add(keyId);
//// }
//// }
//// catch(Exception e){
//// throw new InvalidSyntax();
//// }
//// return ids;
// //filter the ids in findingSet(we got) now manually for the entities which
// were not indexed
//
// //spawning 4 thread to filter data for each filtered non indexed entity
// ExecutorService
// executorFilterWithoutIndexedEntity=Executors.newFixedThreadPool(4);
// for(BigInteger keyId:findingSet){
//
//// boolean shouldAdd=true;
//// for(String entity:entities){
//// if(dataValue.has(entity)){// &&
// (dataValue.get(entity).equals(data.get(entity))))){
//// Object dataObj=data.get(entity);
//// if(isValidJson(dataObj.toString())){
//// JSONObject obj=(JSONObject)dataObj;
//// String relate=JSONObject.getNames(obj)[0];
//// //int value=obj.getInt(relate);
//// //int value;
//// switch(relate){
//// case "gt":
//// if(dataValue.get(entity) instanceof String){
//// String value=obj.getString(relate);
//// if(!((dataValue.getString(entity).compareToIgnoreCase(value))>0))
//// shouldAdd=false;
//// }
//// else{
//// double value=obj.getDouble(relate);
//// if(!((dataValue.getDouble(entity)>value)))
//// shouldAdd=false;
//// }
//// break;
//// case "lt":
//// if(dataValue.get(entity) instanceof String){
//// String value=obj.getString(relate);
//// if(!((dataValue.getString(entity).compareToIgnoreCase(value))<0))
//// shouldAdd=false;
//// }
//// else{
//// double value=obj.getDouble(relate);
//// if(!((dataValue.getDouble(entity)<value)))
//// shouldAdd=false;
//// }
//// break;
//// case "ib":
//// String[] keys=obj.getString(relate).split("-");
//// if(dataValue.get(entity) instanceof String){
//// if(!((dataValue.getString(entity).compareToIgnoreCase(keys[0]))>0 &&
// (dataValue.getString(entity).compareToIgnoreCase(keys[1]))<0))
//// shouldAdd=false;
//// }
//// else{
//// if(!(dataValue.getDouble(entity)>Double.parseDouble(keys[0]) &&
// dataValue.getDouble(entity)<Double.parseDouble(keys[1])))
//// shouldAdd=false;
//// }
//// break;
//// default:
//// throw new InvalidOperation();
//// }
//// }
//// else {
//// if(dataValue.get(entity) instanceof String){
//// String value=data.getString(entity);
//// if(!((dataValue.getString(entity).compareToIgnoreCase(value))==0))
//// shouldAdd=false;
//// }
//// else{
//// if(!((dataValue.getDouble(entity)==data.getDouble(entity))))
//// shouldAdd=false;
//// }
//// }
//// }
//// if(!shouldAdd)
//// break;
//// }executorFilterWithoutIndexedEntity.execute(()->{
// executorFilterWithoutIndexedEntity.execute(()->{
// try {
// JSONObject dataValue=collection.get(keyId);
// if(filterIdsOnRemainingEntity(entities, dataValue, data))
// ids.add(keyId);
// } catch (Exception e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
//// entities[p]=null;
//// //latch.countDown();
//// System.out.print("Filtered Set using Index:");
//// System.out.println(findingSet);
// });
// }
//
//
// executorFilterWithoutIndexedEntity.shutdown();
// executorFilterWithoutIndexedEntity.awaitTermination(Long.MAX_VALUE,
// TimeUnit.NANOSECONDS);
//
//
// }
//
// catch(Exception e){
// throw new InvalidSyntax();
// }
// return ids;
// }

// //update the data entry
// public String update(String updateQuery) throws InvalidSyntax{
// ArrayList<BigInteger> ids=new ArrayList<BigInteger>();
// try{
// JSONObject updateDataQuery=new JSONObject(updateQuery);
// String updateBase=updateDataQuery.get("where").toString();
// ids=find(updateBase);
// JSONObject updateData=(JSONObject)updateDataQuery.get("set");
// String[] entityNames=JSONObject.getNames(updateData);
// if(entityNames==null)
// return (0+" rows updated.");
// for(BigInteger id:ids){
// JSONObject idData=collection.get(id);
// for(String entity:entityNames){
// idData.put(entity,updateData.get(entity));
// }
// collection.put(id,idData);
//
// //writing update to data
// BufferedWriter out = null;
// FileWriter fstream = new
// FileWriter(databaseName+"/"+collectionName+"/"+id+".txt", true); //true tells
// to append data.
// out = new BufferedWriter(fstream);
// JSONObject idDataTemp=collection.get(id);
// idDataTemp.put("objectId", id.toString());
// out.write(idDataTemp.toString()+"\n");
// out.close();
// }
// }
// catch(Exception e){
// throw new InvalidSyntax();
// }
// System.out.println(ids.size()+" rows updated.");
// return (ids.size()+" rows updated.");
// }

// //Select particular data entity and print the database
// public JSONArray Select(String selectData) throws InvalidSyntax{
// JSONArray arr= new JSONArray();
// StringBuffer buf=new StringBuffer();
// try {
// ArrayList<BigInteger> ids=new ArrayList<BigInteger>();
// String[] entity=null;
// if(selectData.equals("") || selectData.equals("{}")){
// ids.addAll(collection.keySet());
// }
// else{
// JSONObject selectDataQuery=new JSONObject(selectData);
// String selectionBase;
// if(selectDataQuery.has("where"))
// selectionBase=selectDataQuery.get("where").toString();
// else
// selectionBase="{}";
// ids.addAll(find(selectionBase));
// if(selectDataQuery.has("select")){
// JSONObject selectedData=(JSONObject)selectDataQuery.get("select");
// entity=JSONObject.getNames(selectedData);
// }
// }
// for(BigInteger id:ids){
// JSONObject data=collection.get(id);
// JSONObject dataResult=new JSONObject();
//
// if(entity==null){
// dataResult=data;
// // for(String entityName: JSONObject.getNames(data)){
// // if(data.has(entityName)){
// // dataResult.put(entityName,data.get(entityName));
// // }
// // }
// }
// else{
// dataResult.put("objectId", id);
// for(String entityName: entity){
// if(data.has(entityName)){
// dataResult.put(entityName,data.get(entityName));
// }
// }
// }
// buf.append(dataResult.toString()+"\n");
// arr.put(dataResult);
// }
//
// }
// catch (JSONException e) {
// throw new InvalidSyntax();
// }
// System.out.println(arr.length()+" rows found.");
// System.out.println(buf);
// return arr;
// }

// //create Index for given entity
// public boolean createIndex(String entity){
// index.clear();
// try{
// for (Map.Entry<BigInteger,JSONObject> currentvar : collection.entrySet()){
// JSONObject data=(JSONObject)currentvar.getValue();
// Object key=data.get(entity);
// ArrayList<BigInteger> value=index.containsKey(key)?value=index.get(key):new
// ArrayList<BigInteger>();
// value.add(currentvar.getKey());
// index.put(key,value);
// }
// indexCreated=entity;
// return true;
// }
// catch(Exception e){
// System.out.println("Exception occurred: "+e.getMessage());
// }
// return false;
// }

// //create Index for given entity
// public String createIndex(String entity){
// // if(index.containsKey(entity)){
// // while(indexInUse.get(entity));
// // indexInUse.remove(entity);
// // index.remove(entity);
// // }
// ConcurrentSkipListMap<ObjectKey, ArrayList<BigInteger>> indexTemp=new
// ConcurrentSkipListMap<>();
// try{
// for (Map.Entry<BigInteger,JSONObject> currentvar : collection.entrySet()){
// JSONObject data=(JSONObject)currentvar.getValue();
// Object key=data.get(entity);
// ArrayList<BigInteger>
// value=index.containsKey(key)?value=indexTemp.get(key):new
// ArrayList<BigInteger>();
// value.add(currentvar.getKey());
// indexTemp.put(new ObjectKey(key),value);
// }
// // while(indexInUse.get(entity));
// index.put(entity, indexTemp);
// // indexInUse.put(entity, false);
// return "Index is created for Entity: "+entity+".";
// }
// catch(Exception e){
// System.out.println("Exception occurred: "+e.getMessage());
// }
// return "Index couldn't be created for Entity: "+entity+".";
// }
*/