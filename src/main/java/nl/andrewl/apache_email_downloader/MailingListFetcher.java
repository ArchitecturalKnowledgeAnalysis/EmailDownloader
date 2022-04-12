package nl.andrewl.apache_email_downloader;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Generic interface for any component that can fetch email data from somewhere
 * on the internet.
 */
public interface MailingListFetcher {
    /**
     * Downloads emails from a given domain and mailing list to a file in the
     * given directory.
     * @param dir The directory to download to.
     * @param domain The domain in which the mailing list exists.
     * @param listName The name of the mailing list.
     * @param start The earliest time to fetch emails from.
     * @param end The latest time to fetch emails from.
     * @return A future that completes when the download is complete.
     */
    CompletableFuture<Collection<Path>> download(Path dir, String domain, String listName, ZonedDateTime start, ZonedDateTime end);

    default CompletableFuture<Collection<Path>> download(Path dir, String domain, String listName, ZonedDateTime start) {
        return download(dir, domain, listName, start, ZonedDateTime.now());
    }

    default CompletableFuture<Collection<Path>> download(Path dir, String domain, String listName) {
        return download(dir, domain, listName, epochStart());
    }

    /**
     * Defines the starting epoch, which sets the default starting date from
     * which we start downloading emails.
     * @return The start epoch.
     */
    default ZonedDateTime epochStart() {
        return ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
    }
}
