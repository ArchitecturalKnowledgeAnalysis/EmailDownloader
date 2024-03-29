package nl.andrewl.emaildownloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A mailing list fetcher designed to work with the Apache Software Foundation
 * mailing list archives.
 */
public class ApacheMailingListFetcher implements MailingListFetcher {
    private final String apiUrl;
    private final int maxConsecutiveFailures;
    private final int requestInterval;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ApacheMailingListFetcher(int maxConsecutiveFailures, int requestInterval, String apiUrl) {
        this.maxConsecutiveFailures = maxConsecutiveFailures;
        this.requestInterval = requestInterval;
        this.apiUrl = apiUrl;
    }

    @Override
    public CompletableFuture<Collection<Path>> download(
            Path dir,
            String domain,
            String listName,
            ZonedDateTime start,
            ZonedDateTime end,
            Consumer<String> messageConsumer
    ) {
        final YearMonth firstPeriod = YearMonth.from(start);
        final YearMonth lastPeriod = YearMonth.from(end);
        return CompletableFuture.supplyAsync(() -> fetchAll(firstPeriod, lastPeriod, dir, domain, listName, messageConsumer));
    }

    /**
     * Fetches all emails between the given periods (inclusive) from the given
     * domain and list name, and stores the files in dir. It traverses from the
     * most recent period to the oldest. The fetching is stopped if we don't
     * find any emails consecutively after a number of periods defined by
     * {@code MAX_CONSECUTIVE_EMPTY_FILES}. This is a synchronous operation,
     * meant to be submitted as a task to an executor for asynchronous
     * execution.
     * @param firstPeriod The earliest period to get emails from.
     * @param lastPeriod The latest period to get emails from.
     * @param dir The directory to store files in.
     * @param domain The domain of the mailing list.
     * @param listName The name of the mailing list.
     * @param messageConsumer The message consumer that's used to log messages.
     * @return The collection of files that were downloaded.
     */
    private Collection<Path> fetchAll(
            YearMonth firstPeriod,
            YearMonth lastPeriod,
            Path dir,
            String domain,
            String listName,
            Consumer<String> messageConsumer
    ) {
        YearMonth currentPeriod = lastPeriod;
        int emptyFileCount = 0;
        Instant lastRequestTimestamp = null;
        List<Path> files = new ArrayList<>();
        while (!currentPeriod.isBefore(firstPeriod) && emptyFileCount < maxConsecutiveFailures) {
            try {
                if (lastRequestTimestamp != null) {
                    long millisSinceLastRequest = Duration.between(lastRequestTimestamp, Instant.now()).toMillis();
                    if (millisSinceLastRequest < requestInterval) {
                        Thread.sleep(requestInterval - millisSinceLastRequest);
                    }
                }
                messageConsumer.accept("Fetching emails from %s@%s in period %s...".formatted(listName, domain, currentPeriod));
                lastRequestTimestamp = Instant.now();
                Path file = fetch(dir, domain, listName, currentPeriod);
                long delay = Duration.between(lastRequestTimestamp, Instant.now()).toMillis();
                long size = Files.size(file);
                if (size < 1) {
                    messageConsumer.accept("No emails were obtained from this period.");
                    emptyFileCount++;
                    Files.deleteIfExists(file);
                } else {
                    messageConsumer.accept("Successfully downloaded %.1f KB of emails to %s in %d ms.".formatted(size / 1024f, file, delay));
                    emptyFileCount = 0;
                    files.add(file);
                }
            } catch (IOException | InterruptedException e) {
                messageConsumer.accept("An error occurred: " + e.getMessage());
                e.printStackTrace();
                emptyFileCount++;
            }
            currentPeriod = currentPeriod.minusMonths(1);
        }
        return files;
    }

    private Path fetch(Path dir, String domain, String listName, YearMonth period) throws IOException, InterruptedException {
        Path file = buildFilePath(dir, domain, listName, period);
        HttpRequest request = HttpRequest.newBuilder(buildURI(domain, listName, period))
                .GET().timeout(Duration.ofSeconds(5)).build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(file));
        if (response.statusCode() >= 400) {
            throw new IOException("Received an invalid status code when downloading from " + request.uri() + " " + response.statusCode());
        }
        return file;
    }

    private Path buildFilePath(Path dir, String domain, String listName, YearMonth period) {
        return dir.resolve("%s_%s_%s.mbox".formatted(domain, listName, period));
    }

    private URI buildURI(String domain, String list, YearMonth period) {
        return URI.create(apiUrl + "?domain=" + domain + "&list=" + list + "&d=" + period);
    }
}
