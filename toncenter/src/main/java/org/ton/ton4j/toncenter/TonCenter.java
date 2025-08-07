package org.ton.ton4j.toncenter;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class TonCenter {

  public String apiKey = "to-be-added";
  public String endpoint = "https://toncenter.com/api/v2/";

  public static class TonCenterBuilder {}

  public static TonCenterBuilder builder() {
    return new CustomTonCenterBuilder();
  }

  private static class CustomTonCenterBuilder extends TonCenterBuilder {

    @Override
    public TonCenter build() {
      return super.build();
    }
  }
}
