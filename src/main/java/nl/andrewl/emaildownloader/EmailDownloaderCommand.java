package nl.andrewl.emaildownloader;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * The command-line interface to the mailing list downloader.
 */
@CommandLine.Command(name = "EmailDownloader", mixinStandardHelpOptions = true)
public class EmailDownloaderCommand implements Runnable {
	@CommandLine.Option(names = {"-l", "--list-name"}, description = "Name of the mailing list to download from.", required = true)
	public String listName;

	@CommandLine.Option(names = {"-d", "--list-domain"}, description = "The domain to download from. For example, \"hadoop.apache.org\"", required = true)
	public String domainName;

	@CommandLine.Option(names = "--from", description = "Earliest period to download from, inclusive. Formatted as yyyy-mm. Defaults to ten years before today.")
	public YearMonth fromDate;

	@CommandLine.Option(names = "--until", description = "Latest period to download from, inclusive. Formatted as yyyy-mm. Defaults to the current year and month.")
	public YearMonth untilDate;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Directory to place downloaded files in. Will create it if it doesn't exist. Defaults to ./emails/", defaultValue = "./emails/")
	public Path outputDir;

	@CommandLine.Option(names = {"--apache-max-failures"}, description = "The maximum number of consecutive failures in email fetching to tolerate before quitting the download early.", defaultValue = "10")
	public int apacheMaxConsecutiveFailures;

	@CommandLine.Option(names = {"--apache-request-interval"}, description = "The number of milliseconds to wait between each API request, to avoid rate-limiting repercussions.", defaultValue = "1000")
	public int apacheRequestInterval;

	@CommandLine.Option(names = {"--apache-api-url"}, description = "The URL to Apache's MBox API.", defaultValue = "https://lists.apache.org/api/mbox.lua")
	public String apacheApiUrl;

	public static void main(String[] args) {
		int result = new CommandLine(new EmailDownloaderCommand()).execute(args);
		System.exit(result);
	}

	@Override
	public void run() {
		var downloader = new ApacheMailingListFetcher(apacheMaxConsecutiveFailures, apacheRequestInterval, apacheApiUrl);
		if (fromDate == null) {
			fromDate = YearMonth.now().minusYears(10);
			System.out.println("--from has not been set; will use " + fromDate + ".");
		}
		if (untilDate == null) {
			untilDate = YearMonth.now();
			System.out.println("--until has not been set; will use " + untilDate + ".");
		}
		if (!Files.exists(outputDir)) {
			try {
				Files.createDirectories(outputDir);
			} catch (IOException e) {
				System.err.println("Couldn't create missing output directories: " + e.getMessage());
				System.exit(1);
			}
		}
		if (!Files.isDirectory(outputDir)) {
			System.err.println("The specified output directory " + outputDir.toAbsolutePath() + " is not a directory.");
			System.exit(1);
		}
		downloader.download(
				outputDir,
				domainName,
				listName,
				fromDate.atDay(1).atStartOfDay(ZoneId.systemDefault()),
				untilDate.atEndOfMonth().atStartOfDay(ZoneId.systemDefault()),
				System.out::println
		).handle((paths, throwable) -> {
			if (throwable != null) {
				System.err.println("An error occurred while downloading: " + throwable.getMessage());
				System.exit(1);
			} else if (paths.isEmpty()) {
				System.out.println("""
					Couldn't download any emails. Please check the --from and --until times.
					If you're downloading Apache mailing lists, consider increasing the
					--apache-max-failures value to search through a larger time span before quitting.""");
			} else {
				System.out.println("Successfully downloaded " + paths.size() + " files to " + outputDir.toAbsolutePath() + ".");
			}
			return null;
		}).join();
	}
}
