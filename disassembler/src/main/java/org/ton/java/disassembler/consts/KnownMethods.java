package org.ton.java.disassembler.consts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KnownMethods {
    public static final Map<Integer, String> METHODS;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "recv_internal");
        map.put(-1, "recv_external");
        map.put(-2, "run_ticktock");
        map.put(66763, "get_full_domain");
        map.put(68445, "get_nft_content");
        map.put(69506, "get_telemint_token_name");
        map.put(72748, "get_sale_data");
        map.put(76407, "is_plugin_installed");
        map.put(78748, "get_public_key");
        map.put(80293, "get_owner");
        map.put(80697, "get_auction_info");
        map.put(81467, "get_subwallet_id");
        map.put(82320, "get_version");
        map.put(83229, "owner");
        map.put(85143, "seqno");
        map.put(85719, "royalty_params");
        map.put(90228, "get_editor");
        map.put(91689, "get_marketplace_address");
        map.put(92067, "get_nft_address_by_index");
        map.put(93270, "get_reveal_data");
        map.put(97026, "get_wallet_data");
        map.put(102351, "get_nft_data");
        map.put(102491, "get_collection_data");
        map.put(103289, "get_wallet_address");
        map.put(106029, "get_jetton_data");
        map.put(107279, "get_offer_data");
        map.put(107653, "get_plugin_list");
        map.put(110449, "get_is_closed");
        map.put(116695, "get_reveal_mode");
        map.put(118054, "get_username");
        map.put(122498, "get_telemint_auction_state");
        map.put(123660, "dnsresolve");
        map.put(128411, "get_royalty_params");
        map.put(129619, "get_telemint_auction_config");
        map.put(524286, "run_ticktock");

        METHODS = Collections.unmodifiableMap(map);
    }
}