package org.deeponion.walletTemplate;

/**
 * Copyright DeepOnion Developers 2020
 * Created by Nezero on 03/04/2020.
 */
public interface WalletTemplateConstants {

    String TAG = "WalletTemplate";

    String PREF_OR = "pref_or";
    String PREF_OR_PORT = "pref_or_port";
    String PREF_OR_NICKNAME = "pref_or_nickname";
    String PREF_REACHABLE_ADDRESSES = "pref_reachable_addresses";
    String PREF_REACHABLE_ADDRESSES_PORTS = "pref_reachable_addresses_ports";

    String PREF_DISABLE_NETWORK = "pref_disable_network";

    String PREF_TOR_SHARED_PREFS = "org.torproject.android_preferences";

    String PREF_SOCKS = "pref_socks";

    String PREF_HTTP = "pref_http";

    String PREF_ISOLATE_DEST = "pref_isolate_dest";

    String PREF_CONNECTION_PADDING = "pref_connection_padding";
    String PREF_REDUCED_CONNECTION_PADDING = "pref_reduced_connection_padding";
    String PREF_CIRCUIT_PADDING = "pref_circuit_padding";
    String PREF_REDUCED_CIRCUIT_PADDING = "pref_reduced_circuit_padding";

    String PREF_PREFER_IPV6 = "pref_prefer_ipv6";
    String PREF_DISABLE_IPV4 = "pref_disable_ipv4";


    String APP_TOR_KEY = "_app_tor";
    String APP_DATA_KEY = "_app_data";
    String APP_WIFI_KEY = "_app_wifi";

    String TOR_CONTROL_PORT_FILE = "control.txt";
    String TOR_PID_FILE = "torpid";
}