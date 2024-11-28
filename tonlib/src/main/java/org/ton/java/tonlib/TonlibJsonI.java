package org.ton.java.tonlib;

import com.sun.jna.Library;

public interface TonlibJsonI extends Library {
  long tonlib_client_json_create();

  void tonlib_client_json_destroy(long tonlib);

  String tonlib_client_json_execute(long tonlib, String query);

  String tonlib_client_json_receive(long tonlib, Double size);

  void tonlib_client_json_send(long tonlib, String query);

  void tonlib_client_set_verbosity_level(long tonlib, int verbosity_level);
}
