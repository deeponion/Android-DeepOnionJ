package org.deeponion.tor;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.deeponion.android.service.BlockchainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static org.deeponion.tor.TorConstants.DIRECTORY_TOR_DATA;

/**
 * Copyright DeepOnion Developers 2020
 * Created by Nezero on 07/05/2020.
 */
public class TorUtils {
    private static final Logger log = LoggerFactory.getLogger(TorUtils.class);

    static File updateTorrcCustomFile(SharedPreferences prefs, File fileControlPort, File  filePid, File fileTorRc) throws IOException {

        StringBuilder extraLines = new StringBuilder();

        extraLines.append("\nRunAsDaemon 1\n");
        extraLines.append("ControlPortWriteToFile").append(' ').append(fileControlPort.getCanonicalPath()).append('\n');

        extraLines.append("PidFile").append(' ').append(filePid.getCanonicalPath()).append('\n');

        String socksPortPref = prefs.getString(TorConstants.PREF_SOCKS, Integer.toString(BlockchainService.SOCKS_PROXY_PORT));

        if (socksPortPref.indexOf(':') != -1)
            socksPortPref = socksPortPref.split(":")[1];

        String httpPortPref = prefs.getString(TorConstants.PREF_HTTP, "0");

        if (httpPortPref.indexOf(':') != -1)
            httpPortPref = httpPortPref.split(":")[1];

        String isolate = "";
        if (prefs.getBoolean(TorConstants.PREF_ISOLATE_DEST, false)) {
            isolate += " IsolateDestAddr ";
        }

        String ipv6Pref = "";

        if (prefs.getBoolean(TorConstants.PREF_PREFER_IPV6, false)) {
            ipv6Pref += " IPv6Traffic PreferIPv6 ";
        }

        if (prefs.getBoolean(TorConstants.PREF_DISABLE_IPV4, false)) {
            ipv6Pref += " IPv6Traffic NoIPv4Traffic ";
        }

        extraLines.append("SOCKSPort ").append(socksPortPref).append(isolate).append(ipv6Pref).append('\n');
        extraLines.append("SocksTimeout 600").append('\n'); // Seconds
        extraLines.append("SafeSocks 0").append('\n');
        extraLines.append("TestSocks 0").append('\n');

        extraLines.append("HTTPTunnelPort ").append(httpPortPref).append('\n');

        if (prefs.getBoolean(TorConstants.PREF_CONNECTION_PADDING, false)) {
            extraLines.append("ConnectionPadding 1").append('\n');
        }

        if (prefs.getBoolean(TorConstants.PREF_REDUCED_CONNECTION_PADDING, true)) {
            extraLines.append("ReducedConnectionPadding 1").append('\n');
        }

        if (prefs.getBoolean(TorConstants.PREF_CIRCUIT_PADDING, true)) {
            extraLines.append("CircuitPadding 1").append('\n');
        } else {
            extraLines.append("CircuitPadding 0").append('\n');
        }

        if (prefs.getBoolean(TorConstants.PREF_REDUCED_CIRCUIT_PADDING, true)) {
            extraLines.append("ReducedCircuitPadding 1").append('\n');
        }

        extraLines.append("VirtualAddrNetwork 10.192.0.0/10").append('\n');
        extraLines.append("AutomapHostsOnResolve 1").append('\n');

        extraLines.append("DormantClientTimeout 10 minutes").append('\n');
        extraLines.append("DormantOnFirstStartup 0").append('\n');

        extraLines.append("DisableNetwork 0").append('\n');

//        if (Prefs.useDebugLogging()) FIXME
//        {
        extraLines.append("Log debug syslog").append('\n');
        extraLines.append("Log info syslog").append('\n');
        extraLines.append("SafeLogging 0").append('\n');
//        }

//        extraLines = processSettingsImpl(extraLines);

//        if (extraLines == null)
//            return null;

        extraLines.append('\n');
        extraLines.append(prefs.getString("pref_custom_torrc", "")).append('\n');

        log.info("updating torrc custom configuration...");

        log.info("torrc.custom=" + extraLines.toString());

        File fileTorRcCustom = new File(fileTorRc.getAbsolutePath() + ".custom");
        boolean success = updateTorConfigCustom(fileTorRcCustom, extraLines.toString());

        if (success && fileTorRcCustom.exists()) {
            return fileTorRcCustom;
        } else
            return null;

    }

    // Tor Helpers.

    public static boolean updateTorConfigCustom(File fileTorRcCustom, String extraLines) throws IOException {
        FileWriter fos = new FileWriter(fileTorRcCustom, false);
        PrintWriter ps = new PrintWriter(fos);
        ps.print(extraLines);
        ps.flush();
        ps.close();
        return true;
    }

    public static boolean runTorShellCmd(
            File fileTor,
            File fileTorrc,
            File appCacheHome,
            SharedPreferences prefs,
            File fileControlPort,
            File  filePid
    ) throws Exception {

        File torrc = updateTorrcCustomFile(prefs, fileControlPort, filePid, fileTorrc);
        if (torrc == null) {
            log.info("Custom torrc failed using default: " + fileTorrc.getCanonicalPath());
            torrc = fileTorrc;
        }

        if (!torrc.exists()) {
            log.error("torrc not installed: " + torrc.getCanonicalPath());
            return false;
        }

        String torCmdString = fileTor.getCanonicalPath()
                + " DataDirectory " + appCacheHome.getCanonicalPath()
                + " SocksPort " + BlockchainService.SOCKS_PROXY_PORT
                + " --defaults-torrc " + torrc;

        int exitCode = -1;

        try {
            exitCode = exec(torCmdString + " --verify-config", true);
        } catch (Exception e) {
            log.error("Tor configuration did not verify: " + e.getMessage(), e);
            return false;
        }

        try {
            exitCode = exec(torCmdString, true);
        } catch (Exception e) {
            log.error("Tor was unable to start: " + e.getMessage(), e);
            return false;
        }

        if (exitCode != 0) {
            log.error("Tor did not start. Exit:" + exitCode);
            return false;
        }

        return true;
    }

    private static int exec(String cmd, boolean wait) throws Exception {
        CommandResult shellResult = Shell.run("sh", cmd);

        if (!shellResult.isSuccessful()) {
            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
        }

        return shellResult.exitCode;
    }

}
