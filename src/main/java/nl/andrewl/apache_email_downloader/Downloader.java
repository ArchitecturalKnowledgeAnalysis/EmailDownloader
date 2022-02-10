package nl.andrewl.apache_email_downloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;

/**
 * Simple component that downloads a month of emails in Mbox format from the
 * apache mailing lists API.
 */
public class Downloader {
	private static final String BASE_URL = "https://lists.apache.org/api/mbox.lua";

	private final HttpClient httpClient;
	private final String domain;
	private final String list;

	/**
	 * Constructs a new downloader.
	 * @param list The mailing list name to download from.
	 * @param domain The domain to download from.
	 */
	public Downloader(String list, String domain) {
		this.httpClient = HttpClient.newHttpClient();
		this.domain = domain;
		this.list = list;
	}

	/**
	 * Downloads a Mbox file containing emails from the given period, to the
	 * given file.
	 * @param file The file to place downloaded content in.
	 * @param period The period to download emails for.
	 * @throws IOException If an error occurs when downloading the emails.
	 * @throws InterruptedException If the process is interrupted while waiting
	 * for the download to complete.
	 */
	public void downloadMboxToFile(Path file, YearMonth period) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "?domain=" + domain + "&list=" + list + "&d=" + period))
				.GET()
				.timeout(Duration.ofSeconds(5))
				.build();
		HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(file));
		if (response.statusCode() >= 400) {
			throw new IllegalStateException("Error response code: " + response.statusCode());
		}
	}
}
