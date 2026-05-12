package com.example.game_portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration des beans Web supplémentaires.
 *
 * =========================================================
 * SANS SPRING :
 * =========================================================
 *   Les appels HTTP externes utilisent HttpURLConnection directement :
 *
 *     URL url = new URL("https://api.example.com/data");
 *     HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *     conn.setRequestMethod("GET");
 *     conn.setRequestProperty("Accept", "application/json");
 *     int status = conn.getResponseCode();
 *
 *     BufferedReader reader = new BufferedReader(
 *         new InputStreamReader(conn.getInputStream()));
 *     StringBuilder response = new StringBuilder();
 *     String line;
 *     while ((line = reader.readLine()) != null) response.append(line);
 *     reader.close();
 *     conn.disconnect();
 *     // Puis parsing JSON manuel : new JSONObject(response.toString())
 *
 * =========================================================
 * AVEC SPRING (RestTemplate) :
 * =========================================================
 *   String result = restTemplate.getForObject(url, String.class);
 *   JsonNode json  = restTemplate.getForObject(url, JsonNode.class);
 *   // Jackson désérialise automatiquement la réponse JSON
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Une seule ligne pour appel HTTP + parsing JSON
 *   2. Gestion des codes d'erreur HTTP automatique (RestClientException)
 *   3. Bean singleton partagé → réutilisation de connexions (performance)
 *   4. Intercepteurs configurables (logging, auth, retry...)
 *   Note : En production, préférer WebClient (réactif) de Spring WebFlux
 *          pour les appels non-bloquants.
 */
@Configuration
public class WebConfig {

    /**
     * Bean RestTemplate pour les appels HTTP vers des APIs externes.
     * Déclaré comme @Bean pour permettre l'injection via @Autowired/@RequiredArgsConstructor.
     *
     * SANS SPRING : new HttpURLConnection() dans chaque classe qui en a besoin
     *   → pas de réutilisation, pas de configuration centralisée.
     * AVEC SPRING : un seul bean singleton, configurable (timeout, intercepteurs, etc.)
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — Appel HTTP externe avec HttpURLConnection
 * =====================================================================
 *
 * En J2EE, il n'existe pas de RestTemplate ni d'injection de bean HTTP.
 * Chaque appel vers une API externe utilise HttpURLConnection directement.
 *
 *   package com.example.util;
 *
 *   import java.io.*;
 *   import java.net.*;
 *   import java.nio.charset.StandardCharsets;
 *
 *   public class HttpClientJ2EE {
 *
 *       // Équivalent de restTemplate.getForObject(url, JsonNode.class)
 *       // Spring : 1 ligne ; J2EE : ~25 lignes de gestion de connexion
 *       public static String get(String apiUrl) throws IOException {
 *           URL url = new URL(apiUrl);
 *           HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *
 *           // Configuration de la requête
 *           conn.setRequestMethod("GET");
 *           conn.setRequestProperty("Accept",          "application/json");
 *           conn.setRequestProperty("Accept-Charset",  "UTF-8");
 *           conn.setConnectTimeout(5000);  // 5 secondes
 *           conn.setReadTimeout(10000);    // 10 secondes
 *
 *           // Lecture du code de statut HTTP
 *           int statusCode = conn.getResponseCode();
 *           if (statusCode < 200 || statusCode >= 300) {
 *               throw new IOException("HTTP " + statusCode + " depuis " + apiUrl);
 *           }
 *
 *           // Lecture du corps de la réponse
 *           // Spring RestTemplate fait cela avec les HttpMessageConverter
 *           InputStream is = conn.getInputStream();
 *           StringBuilder body = new StringBuilder();
 *           try (BufferedReader reader = new BufferedReader(
 *                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
 *               String line;
 *               while ((line = reader.readLine()) != null) body.append(line);
 *           }
 *
 *           conn.disconnect(); // fermeture manuelle obligatoire
 *           return body.toString(); // retourne le JSON brut comme String
 *       }
 *
 *       // Équivalent de restTemplate.postForObject(url, request, ResponseClass.class)
 *       public static String post(String apiUrl, String jsonBody) throws IOException {
 *           URL url = new URL(apiUrl);
 *           HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *
 *           conn.setRequestMethod("POST");
 *           conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
 *           conn.setRequestProperty("Accept",       "application/json");
 *           conn.setDoOutput(true);
 *           conn.setConnectTimeout(5000);
 *           conn.setReadTimeout(10000);
 *
 *           // Écriture du corps de la requête
 *           try (OutputStream os = conn.getOutputStream()) {
 *               byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
 *               os.write(input, 0, input.length);
 *           }
 *
 *           int statusCode = conn.getResponseCode();
 *
 *           // Lecture de la réponse ou de l'erreur
 *           InputStream is = statusCode < 300
 *               ? conn.getInputStream()
 *               : conn.getErrorStream();
 *
 *           StringBuilder response = new StringBuilder();
 *           try (BufferedReader reader = new BufferedReader(
 *                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
 *               String line;
 *               while ((line = reader.readLine()) != null) response.append(line);
 *           }
 *
 *           conn.disconnect();
 *           return response.toString();
 *       }
 *   }
 *
 *   // Utilisation dans GameIntegrationServiceJ2EE :
 *   // String jsonResponse = HttpClientJ2EE.get(apiUrl);
 *   // org.json.JSONArray root = new org.json.JSONArray(jsonResponse);
 *   // for (int i = 0; i < root.length(); i++) { ... }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring RestTemplate       : restTemplate.getForObject(url, JsonNode.class) — 1 ligne
 *   J2EE HttpURLConnection    : ~25 lignes de configuration, lecture, gestion d'erreur, fermeture
 *   Jackson Spring            : désérialise directement en JsonNode ou POJO
 *   J2EE parsing              : org.json.JSONArray/JSONObject — parsing manuel champ par champ
 *   Réutilisation connexion   : Spring bean singleton + pool ; J2EE : new connexion par appel
 *   Intercepteurs Spring      : logging, retry, auth configurables ; J2EE : aucun mécanisme intégré
 * =====================================================================
 */
