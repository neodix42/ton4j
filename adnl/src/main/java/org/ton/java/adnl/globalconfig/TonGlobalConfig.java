package org.ton.java.adnl.globalconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

@Builder
@Data
public class TonGlobalConfig implements Serializable {

  @SerializedName(value = "@type")
  String type;

  LiteServers[] liteservers;
  Validator validator;
  Dht dht;

  public static TonGlobalConfig loadFromPath(String path) {
    try {
      Gson gson =
          new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
      String globalConfigStr = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
      return gson.fromJson(globalConfigStr, TonGlobalConfig.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static TonGlobalConfig loadFromUrl(String url) {
    try {
      Gson gson =
          new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

      String globalConfigStr = IOUtils.toString(new URL(url), Charset.defaultCharset());
      return gson.fromJson(globalConfigStr, TonGlobalConfig.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
