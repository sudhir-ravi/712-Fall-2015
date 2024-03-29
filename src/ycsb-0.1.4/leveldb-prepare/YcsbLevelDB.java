import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.StringByteIterator;

public class YcsbLevelDB extends DB {
	public native int nativeInit();
	public native int nativeRead(String table, String key, StringBuilder jsonString);
	public native int nativeScan(String table, String key, int recordCount, Vector<String> jsonStrs);
	public native int nativeInsert(String table, String key, String jsonObject);
	public native int nativeDelete(String table, String key);
	
	static {
		System.loadLibrary("ycsbleveldb");
	}
	
	@Override
	public void init() {
		nativeInit();
	}
	
	@Override
	public int delete(String table, String key) {
		return nativeDelete(table, key);
	}

	@Override
	public int insert(String table, String key,
			HashMap<String, ByteIterator> insertValues) {
		JSONObject jsonObject = new JSONObject();
		Iterator<String> itr = insertValues.keySet().iterator();
		while(itr.hasNext()) {
			String field = itr.next();
			String value = insertValues.get(field).toString();
			jsonObject.put(field, value);
		}
		
		int ret = nativeInsert(table, key, jsonObject.toString());
		
		return ret;
	}

	@Override
	public int read(String table, String key, Set<String> fields,
			HashMap<String, ByteIterator> result) {
		StringBuilder jsonStr = new StringBuilder();
		JSONObject jsonObject = new JSONObject();
		int ret = nativeRead(table, key, jsonStr);
		if(ret != 0) {
			return ret;
		}
		
		result = new HashMap<String, ByteIterator>();
		try {
			jsonObject = new JSONObject(jsonStr);
			//Iterator<String> fieldItr = fields.iterator();
			@SuppressWarnings("unchecked")
			Iterator<String> fieldItr = jsonObject.keys();
			while(fieldItr.hasNext()) {
				String field = fieldItr.next();
				String value;
				try {
					value = jsonObject.getString(field);	
				} catch(JSONException e) {
					System.err.println(e.getMessage());
					continue;
				}
				result.put(field, new StringByteIterator(value));
			}	
		} catch(JSONException e) {
			result = new HashMap<String, ByteIterator>();
			System.err.println(e.getMessage());
			return -1;
		}
		
		return ret;
	}

	@Override
	public int scan(String table, String key, int recordCount, Set<String> fields,
			Vector<HashMap<String, ByteIterator>> result) {
		Vector<String> jsonStrs = new Vector<String>();

		int ret = nativeScan(table, key, recordCount, jsonStrs);
		if (ret != 0) {
			return ret;
		}

		result = new Vector<HashMap<String, ByteIterator>>();
		for(String jsonStr : jsonStrs) {
			HashMap<String, ByteIterator> hm = new HashMap<String, ByteIterator>();
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(jsonStr);
				//Iterator<String> fieldItr = fields.iterator();
				@SuppressWarnings("unchecked")
				Iterator<String> fieldItr = jsonObject.keys();
				while(fieldItr.hasNext()) {
					String field = fieldItr.next();
					String value;
					try {
						value = jsonObject.getString(field);	
					} catch(JSONException e) {
						System.err.println(e.getMessage());
						continue;
					}
					hm.put(field, new StringByteIterator(value));
				}	
			} catch(JSONException e) {
				System.err.println(e.getMessage());
				continue;
			}
			result.add(hm);
		}
		
		return ret;
	}

	@Override
	public int update(String table, String key,
			HashMap<String, ByteIterator> updateValues) {
		StringBuilder jsonStr = new StringBuilder();
		
		JSONObject jsonObject = new JSONObject();
		int ret = nativeRead(table, key, jsonStr);
		if(ret != 0) {
			return ret;
		}
		
		try {
			jsonObject = new JSONObject(jsonStr.toString());
			Iterator<String> fieldItr = updateValues.keySet().iterator();
			while(fieldItr.hasNext()) {
				String field = fieldItr.next();
				if(jsonObject.has(field)) {
					jsonObject.remove(field);
				}
				
				try {
					jsonObject.put(field, updateValues.get(field).toString());	
				} catch(JSONException e) {
					System.err.println(e.getMessage());
					continue;
				}
			}
			
			return nativeInsert(table, key, jsonObject.toString());
		} catch(JSONException e) {
			System.err.println(e.getMessage() + " " + jsonStr);
			return -1;
		}
	}
}
