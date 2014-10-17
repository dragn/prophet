package me.prophet;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import me.prophet.data.Catalogue;
import me.prophet.prov.SiteDownloadDataProvider;
import me.prophet.tag.Tagger;
import me.prophet.util.CatalogueFetcher;
import me.prophet.util.YandexCatalogueFetcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class Prophet {

    private final JCommander jc;

    @Parameter(names = {"-v", "--verbose"})
    private boolean verbose;

    /**
     * 'help' command
     */
    @Parameters(commandNames = "help", commandDescription = "Displays help for various commands")
    private class CommandHelp implements Runnable {
        @Parameter()
        private List<String> command;

        @Override
        public void run() {
            if (command != null && !command.isEmpty()) {
                JCommander c = jc.getCommands().get(command.get(0));
                if (c != null) {
                    c.usage();
                } else {
                    log("No such command '" + command.get(0) + "'. Type 'prophet help' for a list of commands.");
                }
            } else {
                jc.usage();
            }
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

            logVerbose("Loading knowledge...");
            try {
                Tagger tagger = Tagger.fromFile(knowledgeFile);

                logVerbose("Loaded %d tags", tagger.getKeywords().tags().size());

                for (String url : urls) {
                    List<String> tags = tagger.multitagText(tagger.getDocument(url));
                    if (tags != null && !tags.isEmpty()) {
                        log("%s: %s", url, tags);
                    } else {
                        log("%s: could not determine...", url);
                    }
                }
            } catch (IOException e) {
                logError("error reading knowledge file", e);
            }
        }
    }

    /**
     * 'learn' command
     */
    @Parameters(commandNames = "learn", commandDescription = "Gather the knowledge for catalogue")
    private class CommandLearn implements Runnable {

        @Parameter(names = {"-c", "--catalogue"}, required = true, description = "Catalogue file path.")
        private String catalogueFile;

        @Parameter(names = {"-kn", "--knowledge"}, required = true, description = "File, where to put learned knowledge")
        private String knowledgeFile;

        @Parameter(names = {"-d", "--crawl-depth"}, description = "Site crawling depth. How deep to follow child pages.")
        private Integer crawlDepth = 1;

        @Parameter(names = {"-t", "--type"}, description = "Classifier type. Supported: 'MNB', 'CNB', 'TWCNB' (default)")
        private String type = "TWCNB";

        @Override
        public void run() {
            Tagger tagger = taggerByName(type);
            if (tagger == null) return;
            tagger.setProvider(new SiteDownloadDataProvider(crawlDepth));
            tagger.setVerbose(verbose);
            try {
                tagger.learn(Catalogue.fromFile(catalogueFile));
                log("Saving knowledge to file: %s", knowledgeFile);
                tagger.toFile(knowledgeFile);
            } catch (IOException e) {
                logError("IO Error", e);
            }
        }
    }

    /**
     * 'fetch-catalogue' command
     */
    @Parameters(commandNames = "fetch-catalogue", commandDescription = "Extra utility to build websites catalogue by parsing remote catalogue site.")
    private class CommandFetchCatalogue implements Runnable {

        @Parameter(names = {"-t", "--type"}, required = true, description = "Remote catalogue type. Supported: .")
        private String type;

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
                logError("IO Error", e);
            }
        }
    }

    /**
     * 'benchmark' command
     */
    @Parameters(commandNames = "benchmark", commandDescription = "Run prediction quality evaluation against provided data")
    private class CommandBenchmark implements Runnable {
        @Parameter(names = {"-c", "--catalogue"}, required = true, description = "Where to store parsed catalogue.")
        private String catalogueFile;

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

            logVerbose("Loading knowledge...");
            try {
                Tagger tagger = Tagger.fromFile(knowledgeFile);
                tagger.setProvider(new SiteDownloadDataProvider(crawlDepth));
                logVerbose("Loaded %d tags", tagger.getKeywords().tags().size());

                try {
                    tagger.test(Catalogue.fromFile(catalogueFile));
                } catch (IOException e) {
                    logError("error reading catalogue file", e);
                }
            } catch (IOException e) {
                logError("error reading knowledge file", e);
            }
        }
    }

    private Tagger taggerByName(String name) {
        try {
            Class<?> cl = getClass().getClassLoader().loadClass(Tagger.class.getPackage().getName() + "." +
                    name.toUpperCase() + "Tagger");
            return (Tagger) cl.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logError("Error loading tagger class", e);
        }
        return null;
    }

    private void log(String format, Object... args) {
        System.out.format(format + "\n", args);
    }

    private void logError(String format, Object... args) {
        System.err.format("ERROR: " + format + "\n", args);
    }

    private void logError(String format, Throwable e) {
        logError(format);
        if (verbose) e.printStackTrace();
    }

    private void logVerbose(String format, Object... args) {
        if (verbose) log(format, args);
    }

    private Prophet(String... args) {
        jc = new JCommander(this);
        jc.setProgramName(getClass().getSimpleName().toLowerCase());
        jc.addCommand(new CommandRun());
        jc.addCommand(new CommandLearn());
        jc.addCommand(new CommandFetchCatalogue());
        jc.addCommand(new CommandBenchmark());
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
        new Prophet(args);
    }
}
