package org.deeponion.walletTemplate;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.SocksMultiDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.DeterministicSeed;
import org.deeponion.net.SocksSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.torproject.android.binary.TorResourceInstaller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

/**
 * Copyright DeepOnion Developers 2020
 * Created by Nezero on 31/03/2020.
 */
public class BlockchainService extends Service {

    public static boolean isRunning = false;

    public static final Script.ScriptType PREFERRED_OUTPUT_SCRIPT_TYPE = Script.ScriptType.P2PKH;
    public static final String APP_NAME = "AndroidWalletTemplate";
    public static final String SOCKS_PROXY_ADDRESS = "127.0.0.1";
    public static final int SOCKS_PROXY_PORT = 9081;
    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    private static final String CHANNEL_ID = "CHANNEL_ID_DEEPONION";
    public static NetworkParameters params = MainNetParams.get();
    private static final String WALLET_FILE_NAME = APP_NAME.replaceAll("[^a-zA-Z0-9.-]", "_") + "-"
            + params.getPaymentProtocolId();
    public static WalletAppKit deepOnion;
    public static ProgressTracker progressTracker = new ProgressTracker();
    private File fileControlPort, filePid, fileTorRc;

    public BlockchainService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getString(R.string.deeponion))
                .setContentText(getString(R.string.notification_content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());

        // Don't block just get going.
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    TorResourceInstaller torResourceInstaller = new TorResourceInstaller(BlockchainService.this, getFilesDir());

                    File fileTorBin = torResourceInstaller.installResources();
                    fileTorRc = torResourceInstaller.getTorrcFile();
                    fileControlPort = new File(getFilesDir(), WalletTemplateConstants.TOR_CONTROL_PORT_FILE);
                    filePid = new File(getFilesDir(), WalletTemplateConstants.TOR_PID_FILE);

                    boolean success = fileTorBin != null && fileTorBin.canExecute();

                    String message = "Tor install success? " + success;
                    log.info(message);

                    if (success) {
                        runTorShellCmd(fileTorBin, fileTorRc);
                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }


                // Create the app kit. It won't do any heavyweight initialization until after we start it.
                try {
                    setupWalletKit(null);
                    if (deepOnion.isChainFileLocked()) {
                        stopSelf();
                    }
                    deepOnion.startAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deepOnion.stopAsync();
        deepOnion.awaitTerminated();
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setupWalletKit(@Nullable DeterministicSeed seed) throws IOException {
        // If seed is non-null it means we are restoring from backup.
        // Init with INFO logging
        BriefLogFormatter.initVerbose();
        // Init with default logging
        // BriefLogFormatter.init();
        initLogging();

        File appDataDirectory = getApplication().getFilesDir();
        final SocksSocketFactory socksSocketFactory = new SocksSocketFactory(SOCKS_PROXY_ADDRESS, SOCKS_PROXY_PORT);
        deepOnion = new AndroidWalletAppKit(params, PREFERRED_OUTPUT_SCRIPT_TYPE, null, appDataDirectory, WALLET_FILE_NAME, socksSocketFactory) {
            @Override
            protected void onSetupCompleted() {

            }
        };

        deepOnion.setDownloadListener(progressTracker)
                .setBlockingStartup(false)
                .setCheckpoints(getAssets().open("org.deeponion.production.checkpoints.txt"))
                .setDiscovery(new SocksMultiDiscovery(params))

                .setUserAgent(APP_NAME, "1.0");
        if (seed != null)
            deepOnion.restoreWalletFromSeed(seed);
    }

    private void initLogging() {
        final File logDir = getDir("log", MODE_PRIVATE);
        final File logFile = new File(logDir, "deeponion.log");

        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final PatternLayoutEncoder filePattern = new PatternLayoutEncoder();
        filePattern.setContext(context);
        filePattern.setPattern("%d{dd/MM/yyyy HH:mm:ss.SSS} [%thread] %logger{0} - %msg%n");
        filePattern.start();

        final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(context);
        fileAppender.setFile(logFile.getAbsolutePath());

        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logDir.getAbsolutePath() + "/deeponion.%d.log.gz");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.start();

        fileAppender.setEncoder(filePattern);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
        logcatTagPattern.setContext(context);
        logcatTagPattern.setPattern("%logger{0}");
        logcatTagPattern.start();

        final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
        logcatPattern.setContext(context);
        logcatPattern.setPattern("[%thread] %msg%n");
        logcatPattern.start();

        final ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);
        log.addAppender(fileAppender);

        // Switch on full logging in DeepOnionJ
//        log.setLevel(Level.ALL);
        log.setLevel(Level.INFO);
    }

    private File updateTorrcCustomFile() throws IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        StringBuffer extraLines = new StringBuffer();

        extraLines.append("\nRunAsDaemon 1\n");
        extraLines.append("ControlPortWriteToFile").append(' ').append(fileControlPort.getCanonicalPath()).append('\n');

        extraLines.append("PidFile").append(' ').append(filePid.getCanonicalPath()).append('\n');

        String socksPortPref = prefs.getString(WalletTemplateConstants.PREF_SOCKS, Integer.toString(BlockchainService.SOCKS_PROXY_PORT));

        if (socksPortPref.indexOf(':') != -1)
            socksPortPref = socksPortPref.split(":")[1];

        //socksPortPref = checkPortOrAuto(socksPortPref);

        String httpPortPref = prefs.getString(WalletTemplateConstants.PREF_HTTP, "0");

        if (httpPortPref.indexOf(':') != -1)
            httpPortPref = httpPortPref.split(":")[1];

        //httpPortPref = checkPortOrAuto(httpPortPref);

        String isolate = "";
        if (prefs.getBoolean(WalletTemplateConstants.PREF_ISOLATE_DEST, false)) {
            isolate += " IsolateDestAddr ";
        }

        String ipv6Pref = "";

        if (prefs.getBoolean(WalletTemplateConstants.PREF_PREFER_IPV6, false)) {
            ipv6Pref += " IPv6Traffic PreferIPv6 ";
        }

        if (prefs.getBoolean(WalletTemplateConstants.PREF_DISABLE_IPV4, false)) {
            ipv6Pref += " IPv6Traffic NoIPv4Traffic ";
        }

        extraLines.append("SOCKSPort ").append(socksPortPref).append(isolate).append(ipv6Pref).append('\n');
        extraLines.append("SocksTimeout 600").append('\n'); // Seconds
        extraLines.append("SafeSocks 0").append('\n');
        extraLines.append("TestSocks 0").append('\n');

        if (false)// (Prefs.openProxyOnAllInterfaces())
            extraLines.append("SocksListenAddress 0.0.0.0").append('\n');


        extraLines.append("HTTPTunnelPort ").append(httpPortPref).append('\n');

        if (prefs.getBoolean(WalletTemplateConstants.PREF_CONNECTION_PADDING, false)) {
            extraLines.append("ConnectionPadding 1").append('\n');
        }

        if (prefs.getBoolean(WalletTemplateConstants.PREF_REDUCED_CONNECTION_PADDING, true)) {
            extraLines.append("ReducedConnectionPadding 1").append('\n');
        }

        if (prefs.getBoolean(WalletTemplateConstants.PREF_CIRCUIT_PADDING, true)) {
            extraLines.append("CircuitPadding 1").append('\n');
        } else {
            extraLines.append("CircuitPadding 0").append('\n');
        }

        if (prefs.getBoolean(WalletTemplateConstants.PREF_REDUCED_CIRCUIT_PADDING, true)) {
            extraLines.append("ReducedCircuitPadding 1").append('\n');
        }

//        String transPort = prefs.getString("pref_transport", TorServiceConstants.TOR_TRANSPROXY_PORT_DEFAULT+"");
//        String dnsPort = prefs.getString("pref_dnsport", TorServiceConstants.TOR_DNS_PORT_DEFAULT+"");
//
//        extraLines.append("TransPort ").append(checkPortOrAuto(transPort)).append('\n');
//        extraLines.append("DNSPort ").append(checkPortOrAuto(dnsPort)).append('\n');

        extraLines.append("VirtualAddrNetwork 10.192.0.0/10").append('\n');
        extraLines.append("AutomapHostsOnResolve 1").append('\n');

        extraLines.append("DormantClientTimeout 10 minutes").append('\n');
        extraLines.append("DormantOnFirstStartup 0").append('\n');

        extraLines.append("DisableNetwork 0").append('\n');

//        if (Prefs.useDebugLogging())
//        {
        extraLines.append("Log debug syslog").append('\n');
        extraLines.append("Log info syslog").append('\n');
        extraLines.append("SafeLogging 0").append('\n');
//        }

//        extraLines = processSettingsImpl(extraLines);

        if (extraLines == null)
            return null;

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

    public boolean updateTorConfigCustom(File fileTorRcCustom, String extraLines) throws IOException {
        FileWriter fos = new FileWriter(fileTorRcCustom, false);
        PrintWriter ps = new PrintWriter(fos);
        ps.print(extraLines);
        ps.flush();
        ps.close();
        return true;
    }

    private boolean runTorShellCmd(File fileTor, File fileTorrc) throws Exception {
        File appCacheHome = getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
        File torrc = updateTorrcCustomFile();
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

    private int exec(String cmd, boolean wait) throws Exception {
        CommandResult shellResult = Shell.run("sh", cmd);

        //  debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());

        if (!shellResult.isSuccessful()) {
            throw new Exception("Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout());
        }

        return shellResult.exitCode;
    }

    private static class ProgressTracker extends DownloadProgressTracker {
        public double currentState = -1;

        @Override
        protected void progress(double pct, int blocksLeft, Date date) {
            super.progress(pct, blocksLeft, date);
            currentState = pct;
        }

        @Override
        protected void doneDownload() {
            super.doneDownload();
        }
    }

    // Notification

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}