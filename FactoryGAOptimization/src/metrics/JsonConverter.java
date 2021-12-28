package metrics;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class JsonConverter {
	
	/**
	 * https://stackoverflow.com/questions/4795349/how-to-serialize-a-class-with-an-interface/9550086#9550086
	 */
		
	final static class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
		
	    public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context) {
	        final JsonObject wrapper = new JsonObject();
	        wrapper.addProperty("type", object.getClass().getName());
	        
	         wrapper.add("data", context.serialize(object));
	        // ^ An alternative to the original line above as suggested 
	        // in a subsequent SO answer to the same question by Victor Wong: 
		  //  wrapper.add("data", new Gson().toJsonTree(object));	        
	        return wrapper;
	    }

	    public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
	        final JsonObject wrapper = (JsonObject) elem;
	        final JsonElement typeName = get(wrapper, "type");
	        final JsonElement data = get(wrapper, "data");
	        final Type actualType = typeForName(typeName); 
	        return context.deserialize(data, actualType);
	    }

	    private Type typeForName(final JsonElement typeElem) {
	        try {
	            return Class.forName(typeElem.getAsString());
	        } catch (ClassNotFoundException e) {
	            throw new JsonParseException(e);
	        }
	    }

	    private JsonElement get(final JsonObject wrapper, String memberName) {
	        final JsonElement elem = wrapper.get(memberName);
	        if (elem == null) throw new JsonParseException( "wrapper: " + wrapper + ":\nno '" + memberName + "' member found in what was expected to be an interface wrapper");
	        return elem;
	    }
	}	

	
	///////////////////////////////

	private static Gson gson;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping().enableComplexMapKeySerialization();		
		gsonBuilder.registerTypeAdapter(SampleRate.class, new InterfaceAdapter<SampleRate>() );		
		gsonBuilder.registerTypeAdapter(ValueType.class, new InterfaceAdapter<ValueType>() );
		gsonBuilder.registerTypeAdapter(ConfigurationType.class, new InterfaceAdapter<ConfigurationType>() );
		gsonBuilder.registerTypeAdapter(Process.class, new InterfaceAdapter<Process>() );
		gsonBuilder.registerTypeAdapter(Value.class, new InterfaceAdapter<Value>() );
	//	gsonBuilder.registerTypeAdapter(RealValue.class, new InterfaceAdapter<RealValue>() );
	//	gsonBuilder.registerTypeAdapter(IntValue.class, new InterfaceAdapter<IntValue>() );
	//	gsonBuilder.registerTypeAdapter(NominalValue.class, new InterfaceAdapter<NominalValue>() );		
		gson = gsonBuilder.create();
	}
	
	///////////////////////////////
	
	public String toJson(Object x) {
		return gson.toJson(x);
	}

	public < T > T fromJson(String s, Class< T > typ) {
		return gson.fromJson(s,typ);
	}
	
	public < T > Object fromJson(String s, 	java.lang.reflect.Type typ) {
		return gson.fromJson(s,typ);		
	}
}

// End ///////////////////////////////////////////////////////////////

