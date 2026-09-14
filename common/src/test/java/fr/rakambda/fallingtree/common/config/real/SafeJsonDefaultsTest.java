package fr.rakambda.fallingtree.common.config.real;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public class SafeJsonDefaultsTest {
 public static void main(String[] args) throws Exception {
  Path dir=Files.createTempDirectory("safe-json-test"); Path p=dir.resolve("config.json");
  String defaults="{\"nested\":{\"old\":1,\"new\":2},\"list\":[1,2],\"nil\":5,\"added\":true}";
  Files.writeString(p,"{\"nested\":{\"old\":9},\"list\":[99],\"nil\":null,\"unknown\":\"keep\"}");
  if(!SafeJsonDefaults.update(p,defaults)) throw new AssertionError("missing defaults not added");
  String first=Files.readString(p); var tree=com.google.gson.JsonParser.parseString(first).getAsJsonObject();
  if(tree.getAsJsonObject("nested").get("old").getAsInt()!=9 || !tree.getAsJsonObject("nested").has("new") || tree.getAsJsonArray("list").size()!=1 || !tree.get("nil").isJsonNull() || !tree.has("unknown")) throw new AssertionError("custom values lost");
  var time=Files.getLastModifiedTime(p); if(SafeJsonDefaults.update(p,defaults)|| !Files.readString(p).equals(first)||!Files.getLastModifiedTime(p).equals(time)) throw new AssertionError("no-op wrote");
  for(String invalid:new String[]{"{broken", "{\"a\":1,\"a\":2}","{} trailing","null","[]","{\"a\":NaN}","{\"a\":1,}","{a:1}",""}) {
   Files.writeString(p,invalid); try {SafeJsonDefaults.update(p,defaults);throw new AssertionError("accepted invalid: "+invalid);}catch(java.io.IOException expected){}
   if(!Files.readString(p).equals(invalid)) throw new AssertionError("invalid input changed");
  }
  Files.writeString(p,"{\"typed\":\"wrong\"}");
  String malformedType=Files.readString(p);
  try {SafeJsonDefaults.update(p, "{\"added\":1}", candidate -> {throw new IllegalArgumentException("invalid field type");});throw new AssertionError("validator ignored");}catch(java.io.IOException expected){}
  if(!Files.readString(p).equals(malformedType))throw new AssertionError("typed-invalid input changed");
  Files.writeString(p,"{\"entries\":{\"custom\":{\"x\":9}}}");
  SafeJsonDefaults.update(p,SafeJsonDefaults.retainEntries(p,"{\"entries\":{\"removed\":{\"x\":1}}}","entries"));
  if(Files.readString(p).contains("removed"))throw new AssertionError("removed registry record restored");
  Path fresh=dir.resolve("fresh.json"); if(!SafeJsonDefaults.update(fresh, defaults)||!Files.readString(fresh).equals(defaults)) throw new AssertionError("creation failed");
  System.out.println("SafeJsonDefaults: preservation, null, lists, nested defaults, no-op, malformed and duplicate input, creation PASS");
 }
}
