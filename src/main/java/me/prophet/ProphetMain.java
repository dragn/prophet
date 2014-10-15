package me.prophet;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import me.prophet.data.Catalogue;
import me.prophet.prov.SiteDownloadDataProvider;
import me.prophet.tag.TWCNBTagger;
import me.prophet.tag.Tagger;
import me.prophet.util.CatalogueFetcher;
import me.prophet.util.YandexCatalogueFetcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class ProphetMain {

    private final JCommander jc;

    @Parameter(names = {"-v", "--verbose"})
    private boolean verbose;

    @Parameters(commandNames = "help", commandDescription = "Displays help for various commands")
    private class CommandHelp implements Runnable {
        @Override
        public void run() {
            jc.usage();
        }
    }

    /**
     * 'run' command
     */
    @Parameters(commandNames = "run", commandDescription = "Run detection for specified URLs")
    private class CommandRun implements Runnable {

        @Parameter(description = "URLs")
        private List<String> urls;

        @Parameter(names = {"-kn", "--knowledge"}, required = true, description = "File, containing prophet's knowledge")
        private String knowledgeFile;

        @Parameter(names = {"-d", "--crawl-depth"}, description = "Site crawling depth. How deep to follow child pages.")
        private Integer crawlDepth = 2;

        @Override
        public void run() {
            logVerbose("Checking knowledge file %s for existence...", knowledgeFile);

            if (!new File(knowledgeFile).exists()) {
                logError("Knowledge file %s not found (check the -kn option)", knowledgeFile);
                return;
            }

            TWCNBTagger tagger = new TWCNBTagger(new SiteDownloadDataProvider(crawlDepth));

            logVerbose("Loading knowledge...");
            tagger.fromFile(knowledgeFile);
            logVerbose("Loaded %d tags", tagger.getKeywords().tags().size());

            for (String url : urls) {
                List<String> tags = tagger.multitagText(tagger.getDocument(url));
                if (tags != null && !tags.isEmpty()) {
                    log("%s: %s", url, tags);
                } else {
                    log("%s: could not determine...", url);
                }
            }
        }
    }

    @Parameters(commandNames = "learn", commandDescription = "Gather the knowledge for catalogue")
    private class CommandLearn implements Runnable {

        @Parameter(names = {"-c", "--catalogue"}, required = true, description = "Catalogue file path.")
        private String catalogueFile;

        @Parameter(names = {"-kn", "--knowledge"}, required = true, description = "File, where to put learned knowledge")
        private String knowledgeFile;

        @Parameter(names = {"-d", "--crawl-depth"}, description = "Site crawling depth. How deep to follow child pages.")
        private Integer crawlDepth = 1;

        @Override
        public void run() {
            Tagger tagger = new TWCNBTagger(new SiteDownloadDataProvider(crawlDepth));
            tagger.setVerbose(verbose);
            try {
                tagger.learn(Catalogue.fromFile(catalogueFile));
                log("Saving knowledge to file: %s", knowledgeFile);
                tagger.toFile(knowledgeFile);
            } catch (IOException e) {
                logError("IO Error: %s", e.getMessage());
                if (verbose) e.printStackTrace();
            }
        }
    }

    @Parameters(commandNames = "fetch-catalogue", commandDescription = "Extra utility to build websites catalogue by parsing remote catalogue site.")
    private class CommandFetchCatalogue implements Runnable {

        @Parameter(names = {"-t", "--type"}, description = "Remote catalogue type. Supported: 'yandex' (default).")
        private String type = "yandex";

        @Parameter(names = {"-l", "--links"}, required = true, description = "File, containing definition of tags and their appropriate catalogue section URLs.")
        private String linksFile;

        @Parameter(names = {"-c", "--catalogue"}, required = true, description = "Where to store parsed catalogue.")
        private String catalogueFile;

        @Parameter(names = {"-p", "--pages"}, description = "Maximum number of pages to parse.")
        private Integer maxPages = 20;

        @Override
        public void run() {
            CatalogueFetcher cf;
            switch (type) {
            case "yandex":
                cf = new YandexCatalogueFetcher();
                break;
            default:
                logError("Unsupported type: %s", type);
                return;
            }
            cf.setMaxPages(maxPages);
            try {
                cf.readFile(linksFile);
                cf.fetch().toFile(catalogueFile);
            } catch (IOException e) {
                logError("IO Error: %s", e.getMessage());
                if (verbose) e.printStackTrace();
            }
        }
    }

    private void log(String format, Object... args) {
        System.out.format(format + "\n", args);
    }

    private void logError(String format, Object... args) {
        System.err.format("ERROR: " + format + "\n", args);
    }

    private void logVerbose(String format, Object... args) {
        if (verbose) log(format, args);
    }

    private ProphetMain(String... args) {
        jc = new JCommander(this);
        jc.addCommand(new CommandRun());
        jc.addCommand(new CommandLearn());
        jc.addCommand(new CommandFetchCatalogue());
        jc.addCommand(new CommandHelp());

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            log(e.getMessage());
            log("");
            jc.usage(jc.getParsedCommand());
            return;
        }

        if (jc.getParsedCommand() == null) {
            jc.usage();
        } else {
            JCommander command = jc.getCommands().get(jc.getParsedCommand());
            if (!command.getObjects().isEmpty()) {
                Object cmdObj = command.getObjects().get(0);
                if (cmdObj instanceof Runnable) {
                    ((Runnable) cmdObj).run();
                }
            }
        }
    }

    public static void main(String... args) throws IOException {
        new ProphetMain(args);
    }
}
