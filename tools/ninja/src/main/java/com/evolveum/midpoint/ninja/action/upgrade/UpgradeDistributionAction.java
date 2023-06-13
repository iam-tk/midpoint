package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.Action;

import com.evolveum.midpoint.ninja.action.DataSourceAction;

import com.evolveum.midpoint.ninja.util.NinjaUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class UpgradeDistributionAction extends Action<UpgradeDistributionOptions, Void> {

    @Override
    public Void execute() throws Exception {
        File tempDirectory = options.getTempDirectory() != null ?
                options.getTempDirectory() : new File(FileUtils.getTempDirectory(), UpgradeConstants.UPGRADE_TEMP_DIRECTORY);

        FileUtils.forceMkdir(tempDirectory);

        // download distribution
        DownloadDistributionOptions downloadOptions = new DownloadDistributionOptions();
        downloadOptions.setTempDirectory(tempDirectory);
        downloadOptions.setDistributionArchive(options.getDistributionArchive());

        DownloadDistributionAction downloadAction = new DownloadDistributionAction();
        downloadAction.init(context, downloadOptions);
        DownloadDistributionResult downloadResult = downloadAction.execute();

        // todo next actions should be executed from downloaded ninja (as not to replace ninja.jar that's currently running), or maybe not?
//        log.info("Starting ninja");
//        new ProcessBuilder(
//                "../../_mess/mid8842/.upgrade-process/1685390031006-midpoint-latest-dist/bin/ninja.sh -v --offline -h".split(" ")
//        ).inheritIO().start();
//        System.out.println("Finished main");

        // upgrade installation
        UpgradeInstallationOptions installationOptions = new UpgradeInstallationOptions();
        installationOptions.setDistributionDirectory(downloadResult.getDistributionDirectory());
        installationOptions.setBackup(options.isBackupMidpointDirectory());
        installationOptions.setInstallationDirectory(options.getInstallationDirectory());

        UpgradeInstallationAction installationAction = new UpgradeInstallationAction();
        installationAction.init(context, installationOptions);
        installationAction.execute();

        // upgrade database
        UpgradeDatabaseOptions databaseOptions = new UpgradeDatabaseOptions();
        File installationDirectory = NinjaUtils.computeInstallationDirectory(options.getInstallationDirectory(), context);
        databaseOptions.setScriptsDirectory(new File(installationDirectory, databaseOptions.getScriptsDirectory().getPath()));

        UpgradeDatabaseAction databaseAction = new UpgradeDatabaseAction();
        databaseAction.init(context, databaseOptions);
        databaseAction.execute();

        return null;
    }
}
