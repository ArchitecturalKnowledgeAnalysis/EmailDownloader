package nl.andrewl.apache_email_downloader;

import picocli.CommandLine;

import java.nio.file.Path;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The command-line interface to the mailing list downloader.
 */
@CommandLine.Command(name = "EmailDownloader", mixinStandardHelpOptions = true)
public class EmailDownloaderCommand implements Runnable {
	@CommandLine.Option(names = "-l", description = "Name of the mailing list to download from.", required = true)
	public String listName;

	@CommandLine.Option(names = "-d", description = "The domain to download from. For example, \"hadoop.apache.org\"", required = true)
	public String domainName;

	@CommandLine.Option(names = "--from", description = "Earliest period to download from, inclusive. Formatted as yyyy-mm.")
	public YearMonth fromDate;

	@CommandLine.Option(names = "--until", description = "Latest period to download from, inclusive. Formatted as yyyy-mm.")
	public YearMonth untilDate;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Directory to place downloaded files in. Will create it if it doesn't exist.", defaultValue = "./emails")
	public Path outputDir;

	public static void main(String[] args) {
		int result = new CommandLine(new EmailDownloaderCommand()).execute(args);
		System.exit(result);
	}

	@Override
	public void run() {
		var downloader = new ApacheMailingListFetcher();
		downloader.download(
				outputDir,
				domainName,
				listName,
				fromDate.atDay(1).atStartOfDay(ZoneId.systemDefault()),
				untilDate.atEndOfMonth().atStartOfDay(ZoneId.systemDefault()),
				System.out::println
		).thenAccept(paths -> {
			System.out.println("Successfully downloaded " + paths.size() + " files to " + outputDir);
		}).toCompletableFuture().join();
	}
}
