package me.prophet;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import me.prophet.prov.SiteDownloadDataProvider;
import me.prophet.tag.TWCNBTagger;

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

        @Parameter(names = {"-kn", "--knowledge"}, description = "File, containing prophet's knowledge")
        private String knowledgeFile = "./knowledge.txt";

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
        CommandHelp help = new CommandHelp();
        CommandRun run = new CommandRun();

        jc = new JCommander(this);
        jc.addCommand(help);
        jc.addCommand(run);

        jc.parse(args);

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
