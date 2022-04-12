package nl.andrewl.apache_email_downloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

public class ApacheMailingListFetcher implements MailingListFetcher {
    private static final String API_URL = "https://lists.apache.org/api/mbox.lua";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Override
    public CompletableFuture<Collection<Path>> download(Path dir, String domain, String listName, ZonedDateTime start, ZonedDateTime end) {
        final YearMonth firstPeriod = YearMonth.from(start);
        final YearMonth lastPeriod = YearMonth.from(end);
        List<CompletableFuture<Path>> futures = new ArrayList<>();
        YearMonth currentPeriod = lastPeriod;
        int delay = 0;
        while (!currentPeriod.isBefore(firstPeriod)) {
            futures.add(fetch(dir, domain, listName, currentPeriod, delay));

        }
    }

    private CompletableFuture<Path> fetch(Path dir, String domain, String listName, YearMonth period, int delay) {
        Path file = buildFilePath(dir, domain, listName, period);
        HttpRequest request = HttpRequest.newBuilder(buildURI(domain, listName, period))
                .GET().timeout(Duration.ofSeconds(5)).build();
        Executor delayed = CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS, executor);

        var future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(file)).thenApply(HttpResponse::body);
        return CompletableFuture.supplyAsync(() -> null, delayed).thenCompose(o -> future);
    }

    private Path buildFilePath(Path dir, String domain, String listName, YearMonth period) {
        return dir.resolve("%s_%s_%s.mbox".formatted(domain, listName, period));
    }

    private URI buildURI(String domain, String list, YearMonth period) {
        return URI.create(API_URL + "?domain=" + domain + "&list=" + list + "&d=" + period);
    }
}
