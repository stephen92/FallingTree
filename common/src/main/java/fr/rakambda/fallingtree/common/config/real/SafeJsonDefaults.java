package fr.rakambda.fallingtree.common.config.real;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

/** Adds absent object keys only. Arrays, nulls and existing values are owner-controlled. */
public final class SafeJsonDefaults {
    private SafeJsonDefaults() {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    public static boolean update(Path target, Path defaults) throws IOException {
        return update(target, Files.readString(defaults, StandardCharsets.UTF_8));
    }
    public static boolean update(Path target, InputStream defaults) throws IOException {
        if (defaults == null) throw new IOException("Missing bundled defaults for " + target);
        return update(target, new String(defaults.readAllBytes(), StandardCharsets.UTF_8));
    }
    public static boolean update(Path target, Path defaults, java.util.function.Consumer<String> validate) throws IOException {
        return update(target, Files.readString(defaults, StandardCharsets.UTF_8), validate);
    }
    public static boolean update(Path target, String defaults) throws IOException {
        return update(target, defaults, ignored -> {});
    }
    public static boolean update(Path target, String defaults, java.util.function.Consumer<String> validate) throws IOException {
        try {
            JsonObject template = parse(defaults);
            boolean exists = Files.exists(target);
            String before = exists ? Files.readString(target, StandardCharsets.UTF_8) : null;
            String after = defaults;
            if (exists) {
                JsonObject live = parse(before);
                if (!merge(live, template)) return false;
                after = GSON.toJson(live) + System.lineSeparator();
            }
            parse(after);
            validate.accept(after);
            Path absolute = target.toAbsolutePath();
            Files.createDirectories(absolute.getParent());
            Path temporary = Files.createTempFile(absolute.getParent(), ".config-defaults-", ".tmp");
            try {
                Files.writeString(temporary, after, StandardCharsets.UTF_8);
                // Refuse to overwrite an edit made while this update was being prepared.
                if (exists != Files.exists(absolute) || (exists && !before.equals(Files.readString(absolute, StandardCharsets.UTF_8))))
                    throw new IOException("Config changed during update; retry loading " + target);
                try { Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
            return true;
        } catch (RuntimeException e) {
            throw new IOException("Cannot update malformed JSON config " + target + "; fix it and reload. Original file was preserved.", e);
        }
    }
    /** Named-entry registries keep the owner's selection; only existing default entries grow fields. */
    public static String retainEntries(Path target, String defaults, String... registryKeys) throws IOException {
        JsonObject template = parse(defaults);
        if (!Files.exists(target)) return defaults;
        JsonObject live = parse(Files.readString(target, StandardCharsets.UTF_8));
        for (String key : registryKeys) {
            if (live.has(key) && live.get(key).isJsonObject() && template.has(key) && template.get(key).isJsonObject()) {
                JsonObject selected = live.getAsJsonObject(key);
                template.getAsJsonObject(key).entrySet().removeIf(entry -> !selected.has(entry.getKey()));
            }
        }
        return GSON.toJson(template);
    }
    private static boolean merge(JsonObject live, JsonObject defaults) {
        boolean changed = false;
        for (Map.Entry<String,JsonElement> entry : defaults.entrySet()) {
            String key=entry.getKey(); JsonElement value=entry.getValue();
            if (!live.has(key)) { live.add(key,value.deepCopy()); changed=true; }
            else if(live.get(key).isJsonObject() && value.isJsonObject()) changed |= merge(live.getAsJsonObject(key),value.getAsJsonObject());
        }
        return changed;
    }
    private static JsonObject parse(String source) throws IOException {
        try(JsonReader reader = new JsonReader(new StringReader(source))) {
            reader.setLenient(false);
            JsonElement value=read(reader);
            if(reader.peek()!=JsonToken.END_DOCUMENT || !value.isJsonObject()) throw new IOException("Expected exactly one JSON object");
            return value.getAsJsonObject();
        }
    }
    private static JsonElement read(JsonReader reader) throws IOException {
        switch(reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object=new JsonObject(); reader.beginObject();
                while(reader.hasNext()) {String key=reader.nextName(); if(object.has(key)) throw new IOException("Duplicate JSON key: "+key); object.add(key,read(reader));}
                reader.endObject(); return object;
            case BEGIN_ARRAY:
                JsonArray array=new JsonArray(); reader.beginArray(); while(reader.hasNext()) array.add(read(reader)); reader.endArray(); return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER: return JsonParser.parseString(reader.nextString());
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw new IOException("Invalid JSON token: "+reader.peek());
        }
    }
}
