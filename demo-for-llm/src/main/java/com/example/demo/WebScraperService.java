package com.example.demo;

import com.example.demo.dto.UnifiedAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
@Slf4j
public class WebScraperService {

    @Value("${web.scraper.timeout:10000}")
    private int timeoutMs;

    @Value("${web.scraper.userAgent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36}")
    private String userAgent;

    public Mono<WebScrapingResult> scrapeUrl(String url) {

        return Mono.fromCallable(() -> {

            long startTime = System.currentTimeMillis();

            log.info("Starting URL scraping: {}", url);

            try {

                Connection.Response response = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(timeoutMs)
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(false)
                        .execute();

                long responseTime = System.currentTimeMillis() - startTime;

                int statusCode = response.statusCode();
                String finalUrl = response.url().toString();

                log.info("HTTP {} received for {}", statusCode, finalUrl);

                if (statusCode < 200 || statusCode >= 300) {

                    UnifiedAnalysisResponse.UrlMetadata metadata =
                            UnifiedAnalysisResponse.UrlMetadata.builder()
                                    .originalUrl(url)
                                    .finalUrl(finalUrl)
                                    .domain(extractDomain(finalUrl))
                                    .title("")
                                    .responseTime((int) responseTime)
                                    .isAccessible(false)
                                    .build();

                    return new WebScrapingResult("", metadata);
                }

                Document doc = response.parse();

                String title = doc.title();
                if (title == null || title.isBlank()) {
                    title = extractTitleFromH1(doc);
                }

                String content = extractMainContent(doc);

                UnifiedAnalysisResponse.UrlMetadata metadata =
                        UnifiedAnalysisResponse.UrlMetadata.builder()
                                .originalUrl(url)
                                .finalUrl(finalUrl)
                                .domain(extractDomain(finalUrl))
                                .title(title)
                                .responseTime((int) responseTime)
                                .isAccessible(true)
                                .build();

                log.info(
                        "Successfully scraped {} characters from {} in {} ms",
                        content.length(),
                        finalUrl,
                        responseTime
                );

                return new WebScrapingResult(content, metadata);

            } catch (IOException e) {

                long responseTime = System.currentTimeMillis() - startTime;

                log.error("Failed to scrape URL: {}", url, e);

                UnifiedAnalysisResponse.UrlMetadata metadata =
                        UnifiedAnalysisResponse.UrlMetadata.builder()
                                .originalUrl(url)
                                .finalUrl(url)
                                .domain(extractDomain(url))
                                .title("")
                                .responseTime((int) responseTime)
                                .isAccessible(false)
                                .build();

                return new WebScrapingResult("", metadata);
            }

        }).onErrorResume(e -> {

            log.error("Unexpected error while scraping {}: {}", url, e.getMessage(), e);

            UnifiedAnalysisResponse.UrlMetadata metadata =
                    UnifiedAnalysisResponse.UrlMetadata.builder()
                            .originalUrl(url)
                            .finalUrl(url)
                            .domain(extractDomain(url))
                            .title("")
                            .responseTime(0)
                            .isAccessible(false)
                            .build();

            return Mono.just(new WebScrapingResult("", metadata));
        });
    }

    private String extractMainContent(Document doc) {

        String content;

        if (!doc.select("main").isEmpty()) {
            content = doc.select("main").text();

        } else if (!doc.select("article").isEmpty()) {
            content = doc.select("article").text();

        } else if (!doc.select("div[role=main]").isEmpty()) {
            content = doc.select("div[role=main]").text();

        } else if (!doc.select(".content").isEmpty()) {
            content = doc.select(".content").text();

        } else if (!doc.select(".main-content").isEmpty()) {
            content = doc.select(".main-content").text();

        } else if (!doc.select(".post-content").isEmpty()) {
            content = doc.select(".post-content").text();

        } else {
            content = doc.body().text();
        }

        return cleanText(content);
    }

    private String extractTitleFromH1(Document doc) {

        if (doc.selectFirst("h1") != null) {
            return doc.selectFirst("h1").text();
        }

        return "";
    }

    private String cleanText(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractDomain(String url) {

        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    public record WebScrapingResult(
            String content,
            UnifiedAnalysisResponse.UrlMetadata metadata
    ) {
    }
}