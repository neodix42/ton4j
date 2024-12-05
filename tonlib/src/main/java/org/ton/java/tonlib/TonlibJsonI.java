package org.ton.java.tonlib;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface TonlibJsonI extends Library {
  Pointer tonlib_client_json_create();

  void tonlib_client_json_destroy(Pointer tonlib);

  String tonlib_client_json_execute(Pointer tonlib, String query);

  String tonlib_client_json_receive(Pointer tonlib, Double size);

  void tonlib_client_json_send(Pointer tonlib, String query);

  void tonlib_client_set_verbosity_level(int verbosity_level);
}
