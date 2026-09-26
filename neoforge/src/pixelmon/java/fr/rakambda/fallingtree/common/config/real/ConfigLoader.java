package fr.rakambda.fallingtree.common.config.real;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;

/** NeoForge-only loader: preserve the operator tree and decoded state on failed writes. */
public final class ConfigLoader {
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private static final Map<Path,Object> LAST=new ConcurrentHashMap<>();
    private static final Map<Path,JsonObject> BASELINE=new ConcurrentHashMap<>();
    static <T> T loadConfig(T fallback,Class<T> type,Path path) throws IOException {
        Path key=path.toAbsolutePath().normalize(); T decoded; SafeJsonDefaults.Prepared prepared;
        try {
            prepared=SafeJsonDefaults.prepare(path,GSON.toJson(fallback),raw->decode(raw,type,fallback));
            decoded=decode(prepared.after(),type,fallback);
        } catch(Exception error) {
            LogManager.getLogger(ConfigLoader.class).error("Cannot load {}. Fix its JSON/settings and reload; keeping previous configuration.",path,error);
            Object previous=LAST.get(key); return type.isInstance(previous)?type.cast(previous):fallback;
        }
        try { SafeJsonDefaults.persist(path,prepared); }
        catch(Exception error) { LogManager.getLogger(ConfigLoader.class).error("Loaded {} but cannot persist missing defaults; decoded settings retained. Check file access and reload.",path,error); }
        LAST.put(key,decoded); BASELINE.put(key,GSON.toJsonTree(decoded).getAsJsonObject());
        return decoded;
    }
    private static <T> T decode(String raw,Class<T> type,T fallback) {
        JsonObject tree=JsonParser.parseString(raw).getAsJsonObject();
        validate(tree,type);
        T value=GSON.fromJson(tree,type);
        normalizeNulls(value,fallback);
        return value;
    }
    private static void validate(JsonElement value,Type type) {
        if(value.isJsonNull())return;
        if(type instanceof ParameterizedType p) {
            Class<?> raw=(Class<?>)p.getRawType();
            if(Collection.class.isAssignableFrom(raw)) {
                if(!value.isJsonArray())throw new IllegalArgumentException("Expected list for "+type);
                value.getAsJsonArray().forEach(v->{if(v.isJsonNull())throw new IllegalArgumentException("Null list member in "+type); validate(v,p.getActualTypeArguments()[0]);});return;
            }
            if(Map.class.isAssignableFrom(raw)) {
                if(!value.isJsonObject())throw new IllegalArgumentException("Expected map for "+type);
                value.getAsJsonObject().entrySet().forEach(e->validate(e.getValue(),p.getActualTypeArguments()[1]));return;
            }
        }
        if(!(type instanceof Class<?> cls))return;
        if(cls==String.class || cls.isEnum()) {
            if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Expected text for "+type);
            if(cls.isEnum() && Arrays.stream(cls.getEnumConstants()).noneMatch(e->((Enum<?>)e).name().equals(value.getAsString())))throw new IllegalArgumentException("Invalid "+type+": "+value);
        } else if(cls==boolean.class || cls==Boolean.class) {
            if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("Expected boolean");
        } else if(cls.isPrimitive() || Number.class.isAssignableFrom(cls)) {
            if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber() || !Double.isFinite(value.getAsDouble()))throw new IllegalArgumentException("Expected finite number");
            if((cls==float.class || cls==Float.class) && !Float.isFinite(value.getAsFloat()))throw new IllegalArgumentException("Expected finite float in range");
            if(cls==int.class && (value.getAsDouble()!=Math.rint(value.getAsDouble()) || value.getAsDouble()<Integer.MIN_VALUE || value.getAsDouble()>Integer.MAX_VALUE))throw new IllegalArgumentException("Expected integer in range");
        } else {
            if(!value.isJsonObject())throw new IllegalArgumentException("Expected object for "+cls);
            for(Field field:cls.getDeclaredFields())if(field.isAnnotationPresent(Expose.class)) {
                JsonElement child=value.getAsJsonObject().get(field.getName());
                if(child!=null)validate(child,field.getGenericType());
            }
        }
    }
    private static void normalizeNulls(Object value,Object fallback) {
        try {
            for(Field field:value.getClass().getDeclaredFields())if(field.isAnnotationPresent(Expose.class)) {
                field.setAccessible(true); Object current=field.get(value),defaultValue=field.get(fallback);
                if(current==null)field.set(value,defaultValue);
                else if(current instanceof Map<?,?> map)map.values().removeIf(Objects::isNull);
                else if(defaultValue!=null && field.getType().getPackageName().startsWith("fr.rakambda.fallingtree.common.config.real")) normalizeNulls(current,defaultValue);
            }
        } catch(ReflectiveOperationException e) { throw new IllegalArgumentException("Cannot decode configuration",e); }
    }
    static <T> T saveConfig(T config,Path path) throws IOException {
        Path key=path.toAbsolutePath().normalize();
        var snapshot=SafeJsonDefaults.prepare(path,"{}",raw->{});
        var live=JsonParser.parseString(snapshot.after()).getAsJsonObject();
        var current=GSON.toJsonTree(config).getAsJsonObject();
        overlayChanges(live,current,BASELINE.get(key));
        String after=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(live)+System.lineSeparator();
        SafeJsonDefaults.persist(path,new SafeJsonDefaults.Prepared(snapshot.before(),after));
        LAST.put(key,config); BASELINE.put(key,current); return config;
    }
    private static void overlayChanges(JsonObject live,JsonObject current,JsonObject baseline) {
        for(var entry:current.entrySet()) {
            JsonElement prior=baseline==null?null:baseline.get(entry.getKey());
            if(Objects.equals(prior,entry.getValue()))continue;
            JsonElement actual=live.get(entry.getKey());
            if(actual!=null && actual.isJsonObject() && entry.getValue().isJsonObject())
                overlayChanges(actual.getAsJsonObject(),entry.getValue().getAsJsonObject(),prior!=null && prior.isJsonObject()?prior.getAsJsonObject():null);
            else live.add(entry.getKey(),entry.getValue().deepCopy());
        }
    }
}
