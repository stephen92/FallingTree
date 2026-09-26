package fr.rakambda.fallingtree.common.config.real;
import com.google.gson.annotations.Expose;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class LoaderPreservationTest {
    @TempDir Path root;
    static final class Settings {
        @Expose int amount=1;
        @Expose boolean enabled=true;
        @Expose List<String> selected=new ArrayList<>();
    }
    @Test void failedDefaultPersistenceRetainsDecodedSettings() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        Path file=root.resolve("config.json"); Files.writeString(file,"{\"amount\":71}");
        try(var lock=Files.newByteChannel(file,Set.of(StandardOpenOption.READ,com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE))) {
            assertEquals(71,ConfigLoader.loadConfig(new Settings(),Settings.class,file).amount);
            assertEquals("{\"amount\":71}",Files.readString(file));
        }
    }
    @Test void malformedReloadRetainsPriorAndDoesNotTouchFile() throws Exception {
        Path file=root.resolve("config.json"); Files.writeString(file,"{\"amount\":71}");
        var first=ConfigLoader.loadConfig(new Settings(),Settings.class,file);
        Files.writeString(file,"{bad");
        assertSame(first,ConfigLoader.loadConfig(new Settings(),Settings.class,file));
        assertEquals("{bad",Files.readString(file));
    }
    @Test void noOpPreservesNullUnknownAndSelectedList() throws Exception {
        Path file=root.resolve("config.json"); Files.writeString(file,"{\"amount\":71,\"enabled\":null,\"selected\":[\"custom\"],\"owner\":true}");
        var cfg=ConfigLoader.loadConfig(new Settings(),Settings.class,file);
        assertEquals(List.of("custom"),cfg.selected); String once=Files.readString(file);
        ConfigLoader.loadConfig(new Settings(),Settings.class,file); assertEquals(once,Files.readString(file));
    }
    @Test void savingOneSettingPreservesOtherNullAndUnknownFields() throws Exception {
        Path file=root.resolve("save.json"); Files.writeString(file,"{\"amount\":71,\"enabled\":null,\"selected\":[],\"owner\":{\"note\":\"keep\"}}");
        var settings=ConfigLoader.loadConfig(new Settings(),Settings.class,file); settings.amount=99;
        ConfigLoader.saveConfig(settings,file);
        var tree=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertTrue(tree.get("enabled").isJsonNull()); assertEquals(99,tree.get("amount").getAsInt());
        assertEquals("keep",tree.getAsJsonObject("owner").get("note").getAsString());
    }
    @Test void maintainedServerConfigCopyRetainsAllOperatorFields() throws Exception {
        var constructor=Configuration.class.getDeclaredConstructor(); constructor.setAccessible(true);
        Path file=root.resolve("real.json"); Files.copy(Path.of("../config/server/fallingtree.json"),file);
        var original=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        ConfigLoader.loadConfig(constructor.newInstance(),Configuration.class,file);
        var updated=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertOriginal(original,updated); String once=Files.readString(file);
        ConfigLoader.loadConfig(constructor.newInstance(),Configuration.class,file); assertEquals(once,Files.readString(file));
    }
    private static void assertOriginal(com.google.gson.JsonObject before,com.google.gson.JsonObject after) {
        before.entrySet().forEach(e->{
            if(e.getValue().isJsonObject())assertOriginal(e.getValue().getAsJsonObject(),after.getAsJsonObject(e.getKey()));
            else assertEquals(e.getValue(),after.get(e.getKey()),e.getKey());
        });
    }
    @Test void invalidTypesEnumsDuplicateKeysAndFractionalIntegersStayUntouched() throws Exception {
        var constructor=Configuration.class.getDeclaredConstructor();constructor.setAccessible(true);
        Path file=root.resolve("invalid.json");
        for(String json:List.of("{\"trees\":{\"breakMode\":\"INVALID\"}}","{\"trees\":{\"maxScanSize\":1.5}}","{\"trees\":{\"allowedLogs\":[4]}}","{\"trees\":true}","{\"breakInCreative\":false,\"breakInCreative\":true}")) {
            Files.writeString(file,json); ConfigLoader.loadConfig(constructor.newInstance(),Configuration.class,file); assertEquals(json,Files.readString(file));
        }
    }
    @Test void floatOverflowRetainsPriorAndDoesNotPersistDefaults() throws Exception {
        var constructor=Configuration.class.getDeclaredConstructor();constructor.setAccessible(true);
        Path file=root.resolve("overflow.json");Files.writeString(file,"{\"trees\":{\"trunkLootPercentage\":0.75}}");
        var previous=ConfigLoader.loadConfig(constructor.newInstance(),Configuration.class,file);
        String invalid="{\"trees\":{\"trunkLootPercentage\":1e100}}";Files.writeString(file,invalid);
        assertSame(previous,ConfigLoader.loadConfig(constructor.newInstance(),Configuration.class,file));
        assertEquals(invalid,Files.readString(file));
    }
}
