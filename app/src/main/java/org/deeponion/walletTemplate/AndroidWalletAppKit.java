package org.deeponion.walletTemplate;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.BlockingClientManager;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.KeyChainGroupStructure;
import org.deeponion.net.SocksSocketFactory;

import java.io.File;

import javax.annotation.Nullable;


/**
 * Copyright DeepOnion Developers 2020
 * Created by Nezero on 31/03/2020.
 */
public class AndroidWalletAppKit extends WalletAppKit {
    final SocksSocketFactory socksSocketFactory;
    public AndroidWalletAppKit(NetworkParameters params, File directory, String filePrefix, SocksSocketFactory socksSocketFactory) {
        super(params, directory, filePrefix);
        this.socksSocketFactory = socksSocketFactory;
    }

    public AndroidWalletAppKit(NetworkParameters params, Script.ScriptType preferredOutputScriptType, @Nullable KeyChainGroupStructure structure, File directory, String filePrefix, SocksSocketFactory socksSocketFactory) {
        super(params, preferredOutputScriptType, structure, directory, filePrefix);
        this.socksSocketFactory = socksSocketFactory;
    }

    public AndroidWalletAppKit(Context context, Script.ScriptType preferredOutputScriptType, @Nullable KeyChainGroupStructure structure, File directory, String filePrefix, SocksSocketFactory socksSocketFactory) {
        super(context, preferredOutputScriptType, structure, directory, filePrefix);
        this.socksSocketFactory = socksSocketFactory;
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        if(vPeerGroup != null) {
            // Example of how to configure the default DeepOnionJ peer group
            vPeerGroup.setMaxConnections(8);
            vPeerGroup.setPingIntervalMsec(60 * 1000); // 60s
        }
    }

    @Override
    protected PeerGroup createPeerGroup() {
        BlockingClientManager cm  = new BlockingClientManager(socksSocketFactory);
        cm.setConnectTimeoutMillis(60000);
        return new PeerGroup(params, vChain, cm);
    }
}