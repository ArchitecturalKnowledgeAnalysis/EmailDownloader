package nl.andrewl.apache_email_downloader;

import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

@CommandLine.Command(name = "ApacheEmailDownloader", mixinStandardHelpOptions = true)
public class ApacheEmailDownloader implements Runnable {
	@CommandLine.Option(names = "-l", description = "Name of the mailing list to download from.", required = true)
	public String listName;

	@CommandLine.Option(names = "-d", description = "The domain to download from. For example, \"hadoop.apache.org\"", required = true)
	public String domainName;

	@CommandLine.Option(names = "--from", description = "Latest period to download from, inclusive. Formatted as yyyy-mm.")
	public YearMonth fromDate;

	@CommandLine.Option(names = "--until", description = "Earliest period to download from, inclusive. Formatted as yyyy-mm.")
	public YearMonth untilDate;

	@CommandLine.Option(names = {"-o", "--output"}, description = "Directory to place downloaded files in. Will create it if it doesn't exist.", defaultValue = "./emails")
	public Path outputDir;

	public static void main(String[] args) {
		int result = new CommandLine(new ApacheEmailDownloader()).execute(args);
		System.exit(result);
	}

	@Override
	public void run() {
		if (Files.exists(outputDir) && !Files.isDirectory(outputDir)) {
			throw new IllegalArgumentException("Specified output directory is not a directory.");
		}

		System.out.printf("Downloading %s@%s to %s\n", listName, domainName, outputDir);
		var downloader = new Downloader(listName, domainName);
		YearMonth currentPeriod = fromDate == null ? YearMonth.now() : fromDate;
		YearMonth endPeriod = untilDate == null ? YearMonth.of(2000, 1) : untilDate;
		if (endPeriod.isAfter(currentPeriod)) throw new IllegalArgumentException("Invalid time range. 'until' date must be after 'from' date.");
		int fileCount = 0;
		int consecutiveEmptyFileCount = 0;
		try {
			Files.createDirectories(outputDir);
			while (currentPeriod.isAfter(endPeriod) || currentPeriod.equals(endPeriod)) {
				String fileName = String.format("%s_%s_%s.mbox", domainName, listName, currentPeriod);
				Path file = outputDir.resolve(fileName);
				try {
					downloader.downloadMboxToFile(file, currentPeriod);
					// The API can return empty files. Check for these and remove them.
					long fileSize = Files.size(file);
					if (fileSize == 0) {
						consecutiveEmptyFileCount++;
						Files.delete(file);
					} else {
						consecutiveEmptyFileCount = 0;
						fileCount++;
					}
					System.out.printf("Downloaded emails from %s to %s, total size of %.1f Kb.\n", currentPeriod, file, fileSize / 1024.0);
					if (consecutiveEmptyFileCount >= 5) {
						System.out.println("The last 5 periods were empty. Quitting the download now.");
						break;
					}
				} catch (Exception e) {
					System.err.println("An error occurred while downloading emails from period " + currentPeriod + ": " + e.getMessage());
				}
				currentPeriod = currentPeriod.minusMonths(1);
			}
			System.out.printf("Download complete. %d files downloaded.\n", fileCount);
		} catch (Exception e) {
			System.err.println("An error occurred during the download: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
