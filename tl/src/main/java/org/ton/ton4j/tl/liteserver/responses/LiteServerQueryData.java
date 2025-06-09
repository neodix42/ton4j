package org.ton.ton4j.tl.liteserver.responses;

public interface LiteServerQueryData {
  String getQueryName();

  byte[] getQueryData();
}
