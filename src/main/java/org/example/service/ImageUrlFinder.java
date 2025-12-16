package org.example.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageUrlFinder {
    private static final String CATALOG_URL = "https://cat.mau.ru/2/";
    private final Map<String, String> breedToPageUrl = new HashMap<>();

    public ImageUrlFinder() {
        loadBreedCatalog();
    }

    private void loadBreedCatalog() {
        try {
            Document catalog = Jsoup.connect(CATALOG_URL)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Elements breedLinks = catalog.select("a.s1[href]");

            for (Element link : breedLinks) {
                String breedName = link.text().trim();
                String href = link.attr("href").trim();

                if (!breedName.equals("Все породы кошек") && !href.equals("/2/")) {
                    breedToPageUrl.put(breedName, "https://cat.mau.ru" + href);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Ошибка загрузки каталога: " + e.getMessage());
        }
    }


    public String findImageUrl(String breedName) throws IOException {
        String breedPageUrl = breedToPageUrl.get(breedName);
        if (breedPageUrl == null) {
            throw new IOException("Порода '" + breedName + "' не найдена в каталоге");
        }

        Document breedPage = Jsoup.connect(breedPageUrl)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();

        Element imgElement = null;


        if (imgElement == null) {
            imgElement = breedPage.selectFirst("img[src*=.jpg]");
        }

        if (imgElement == null) {
            imgElement = breedPage.selectFirst("img[src]");
        }

        if (imgElement == null) {
            throw new IOException("Изображение не найдено на странице породы");
        }

        String src = imgElement.attr("src");
        if (src.isEmpty()) {
            throw new IOException("Пустая ссылка на изображение");
        }

        return makeAbsoluteUrl(src, breedPageUrl);
    }

    private String makeAbsoluteUrl(String src, String baseUrl) {
        if (src.startsWith("http")) {
            return src;
        } else if (src.startsWith("/")) {
            return "https://cat.mau.ru" + src;
        } else {
            String base = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
            return base + src;
        }
    }
}