package nl.andrewl.apache_email_downloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;

/**
 * Simple component that downloads a month of emails in Mbox format from the
 * apache mailing lists API.
 */
public class MboxDownloader {
	private static final String BASE_URL = "https://lists.apache.org/api/mbox.lua";

	private final HttpClient httpClient;

	/**
	 * Constructs a new downloader.
	 */
	public MboxDownloader() {
		this.httpClient = HttpClient.newHttpClient();
	}

	/**
	 * Downloads a Mbox file containing emails from the given period, to the
	 * given file.
	 * @param domain The domain to download the mbox file from.
	 * @param list The name of the mailing list to download from.
	 * @param file The file to place downloaded content in.
	 * @param period The period to download emails for.
	 * @return A future that completes when the request is complete.
	 */
	public CompletableFuture<HttpResponse<Path>> downloadMboxToFile(String domain, String list, Path file, YearMonth period) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "?domain=" + domain + "&list=" + list + "&d=" + period))
				.GET()
				.timeout(Duration.ofSeconds(5))
				.build();
		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(file));
	}
}
