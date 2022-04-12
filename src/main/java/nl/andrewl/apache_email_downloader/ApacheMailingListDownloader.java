package nl.andrewl.apache_email_downloader;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

/**
 * The mailing list downloader offers methods to download the entirety of a
 * mailing list from an apache email archive, to a directory.
 */
public class ApacheMailingListDownloader {
	private static final String API_URL = "https://lists.apache.org/api/mbox.lua";

	private final String domain;
	private final String list;
	private final MboxDownloader downloader;

	public ApacheMailingListDownloader(String domain, String list) {
		this.domain = domain;
		this.list = list;
		this.downloader = new MboxDownloader();
	}

	/**
	 * Downloads all mbox files from the configured mailing list to the given
	 * directory.
	 * @param outputDir The directory to download the mbox files to.
	 * @param fromDate An (optional) lower bound date for downloading.
	 * @param untilDate An (optional) upper bound date for downloading.
	 * @return A completable future that completes when all files are
	 * downloaded, and returns a list containing the paths of all downloaded
	 * files.
	 * @throws IllegalArgumentException If the output directory is invalid (i.e.
	 * not a directory) or if the given "from" and "until" dates are invalid,
	 * such as if "until" is before "from".
	 */
	public CompletionStage<List<Path>> downloadAll(Path outputDir, YearMonth fromDate, YearMonth untilDate) {
		if (Files.exists(outputDir) && !Files.isDirectory(outputDir)) {
			throw new IllegalArgumentException("Specified output directory is not a directory.");
		}
		// We start at the most recent date and work our way back until we reach the end or run out of data.
		final YearMonth startPeriod = untilDate == null ? YearMonth.now() : untilDate;
		final YearMonth endPeriod = fromDate == null ? YearMonth.of(2000, 1) : fromDate;
		if (endPeriod.isAfter(startPeriod)) {
			throw new IllegalArgumentException("Invalid time range. 'until' date must be after 'from' date.");
		}

		CompletableFuture<List<Path>> cf = new CompletableFuture<>();
		ForkJoinPool.commonPool().submit(() -> {
			List<Path> files = new ArrayList<>();
			int consecutiveEmptyFileCount = 0;
			YearMonth currentPeriod = startPeriod;
			try {
				Files.createDirectories(outputDir);
				while (currentPeriod.isAfter(endPeriod) || currentPeriod.equals(endPeriod)) {
					String fileName = String.format("%s_%s_%s.mbox", domain, list, currentPeriod);
					Path file = outputDir.resolve(fileName);
					try {
						downloader.downloadMboxToFile(domain, list, file, currentPeriod);
						// The API can return empty files. Check for these and remove them.
						long fileSize = Files.size(file);
						if (fileSize == 0) {
							consecutiveEmptyFileCount++;
							Files.delete(file);
						} else {
							consecutiveEmptyFileCount = 0;
							files.add(file);
						}
						// If the last 5 periods were blank, assume we've hit the end, and quit.
						if (consecutiveEmptyFileCount >= 5) break;
					} catch (Exception e) {
						// If an exception occurs during single-file download, don't quit the whole thing; just log a message.
						System.err.println("An error occurred while downloading emails from period " + currentPeriod + ": " + e.getMessage());
					}
					currentPeriod = currentPeriod.minusMonths(1);
				}
				cf.complete(files);
			} catch (Exception e) {
				System.err.println("An error occurred during the download: " + e.getMessage());
				cf.completeExceptionally(e);
			}
		});
		return cf;
	}

	private URI buildURI(String domain, String list, YearMonth period) {
		return URI.create(API_URL + "?domain=" + domain + "&list=" + list + "&d=" + period);
	}
}
