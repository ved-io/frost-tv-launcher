package com.flankes.glasslauncher.util;

import java.util.HashMap;
import java.util.Map;

public class Localization {

    private static final Map<String, Map<String, String>> STRINGS = new HashMap<>();

    static {
        // --- GERMAN (DE) ---
        Map<String, String> de = new HashMap<>();
        de.put("popup_launcher_settings", "Launcher-Einstellungen");
        de.put("popup_tv_settings", "TV-Systemeinstellungen");
        de.put("sidebar_header", "EINSTELLUNGEN");
        de.put("tab_lang", "Sprache");
        de.put("tab_wallpaper", "Hintergrund");
        de.put("tab_weather", "Wetter");
        de.put("tab_about", "Über");
        de.put("btn_close", "Schließen");

        de.put("lang_title", "Spracheinstellungen");
        de.put("lang_desc", "Wähle die Sprache für die Benutzeroberfläche, Datum und Wetter:");
        de.put("lang_status_prefix", "Aktuelle Sprache: ");

        de.put("wp_title", "Hintergrund & Tiefeneffekt");
        de.put("wp_depth_label", "Uhr-Tiefeneffekt (hinter Bergen):");
        de.put("btn_enabled", "Aktiviert");
        de.put("btn_disabled", "Deaktiviert");
        de.put("wp_source_label", "Hintergrundbild-Quelle:");
        de.put("wp_source_default", "Alpenpanorama");
        de.put("wp_source_custom", "Aus /sdcard/ laden");
        de.put("wp_open_files", "Dateien öffnen");
        de.put("wp_hint", "Eigene Bilder: 'wallpaper.jpg' und 'wallpaper_fg.png' in Ordner /sdcard/Wallpapers/ ablegen.");

        de.put("weather_title", "Wetteranzeige (Open-Meteo)");
        de.put("weather_toggle_label", "Wetter:");
        de.put("btn_on", "Ein");
        de.put("btn_off", "Aus");
        de.put("weather_unit_label", "Einheit:");
        de.put("weather_interval_label", "Intervall:");
        de.put("weather_current_city_prefix", "Aktuelle Stadt: ");
        de.put("weather_search_label", "Stadt suchen:");
        de.put("weather_search_hint", "Stadtname eingeben...");
        de.put("btn_search", "Suchen");
        de.put("weather_searching", "Suche Städte...");
        de.put("weather_no_results", "Keine Städte gefunden.");
        de.put("weather_results_title", "Gefundene Städte (D-Pad zum Auswählen):");

        de.put("about_title", "Über Frost TV Launcher");
        de.put("about_version", "Version: v1.3.0 Native Edition");
        de.put("about_arch", "100% Native Java & AndroidX • Keine Web-Engine • Keine Tracker");
        de.put("about_desc", "Inspiriert von der minimalistischen Designphilosophie von Couch TV (GitHub: couch-tv/couch-tv). Von Grund auf eigenständig und nativ für Android TV und Google TV entwickelt.");
        STRINGS.put("de", de);

        // --- ENGLISH (EN) ---
        Map<String, String> en = new HashMap<>();
        en.put("popup_launcher_settings", "Launcher Settings");
        en.put("popup_tv_settings", "TV System Settings");
        en.put("sidebar_header", "SETTINGS");
        en.put("tab_lang", "Language");
        en.put("tab_wallpaper", "Wallpaper");
        en.put("tab_weather", "Weather");
        en.put("tab_about", "About");
        en.put("btn_close", "Close");

        en.put("lang_title", "Language Settings");
        en.put("lang_desc", "Choose the language for the user interface, date, and weather:");
        en.put("lang_status_prefix", "Current language: ");

        en.put("wp_title", "Wallpaper & Depth Effect");
        en.put("wp_depth_label", "Clock Depth Effect (behind peaks):");
        en.put("btn_enabled", "Enabled");
        en.put("btn_disabled", "Disabled");
        en.put("wp_source_label", "Wallpaper Source:");
        en.put("wp_source_default", "Alpine Panorama");
        en.put("wp_source_custom", "Load from /sdcard/");
        en.put("wp_open_files", "Open Files");
        en.put("wp_hint", "Custom images: Place 'wallpaper.jpg' and 'wallpaper_fg.png' in /sdcard/Wallpapers/.");

        en.put("weather_title", "Weather Forecast (Open-Meteo)");
        en.put("weather_toggle_label", "Weather:");
        en.put("btn_on", "On");
        en.put("btn_off", "Off");
        en.put("weather_unit_label", "Unit:");
        en.put("weather_interval_label", "Interval:");
        en.put("weather_current_city_prefix", "Current City: ");
        en.put("weather_search_label", "Search City:");
        en.put("weather_search_hint", "Enter city name...");
        en.put("btn_search", "Search");
        en.put("weather_searching", "Searching cities...");
        en.put("weather_no_results", "No cities found.");
        en.put("weather_results_title", "Found cities (D-Pad to select):");

        en.put("about_title", "About Frost TV Launcher");
        en.put("about_version", "Version: v1.3.0 Native Edition");
        en.put("about_arch", "100% Native Java & AndroidX • No Web Engine • Zero Trackers");
        en.put("about_desc", "Inspired by the minimalist design philosophy of Couch TV (GitHub: couch-tv/couch-tv). Built completely from scratch as an independent native Android TV and Google TV launcher.");
        STRINGS.put("en", en);

        // --- SPANISH (ES) ---
        Map<String, String> es = new HashMap<>();
        es.put("popup_launcher_settings", "Ajustes del Launcher");
        es.put("popup_tv_settings", "Ajustes del Sistema TV");
        es.put("sidebar_header", "AJUSTES");
        es.put("tab_lang", "Idioma");
        es.put("tab_wallpaper", "Fondo");
        es.put("tab_weather", "Clima");
        es.put("tab_about", "Acerca de");
        es.put("btn_close", "Cerrar");

        es.put("lang_title", "Ajustes de Idioma");
        es.put("lang_desc", "Elige el idioma para la interfaz de usuario, fecha y clima:");
        es.put("lang_status_prefix", "Idioma actual: ");

        es.put("wp_title", "Fondo y Efecto de Profundidad");
        es.put("wp_depth_label", "Efecto profundidad reloj (tras montañas):");
        es.put("btn_enabled", "Activado");
        es.put("btn_disabled", "Desactivado");
        es.put("wp_source_label", "Origen del fondo de pantalla:");
        es.put("wp_source_default", "Panorama Alpino");
        es.put("wp_source_custom", "Cargar desde /sdcard/");
        es.put("wp_open_files", "Abrir Archivos");
        es.put("wp_hint", "Imágenes propias: Guarda 'wallpaper.jpg' y 'wallpaper_fg.png' en /sdcard/Wallpapers/.");

        es.put("weather_title", "Pronóstico del Tiempo (Open-Meteo)");
        es.put("weather_toggle_label", "Clima:");
        es.put("btn_on", "Sí");
        es.put("btn_off", "No");
        es.put("weather_unit_label", "Unidad:");
        es.put("weather_interval_label", "Intervalo:");
        es.put("weather_current_city_prefix", "Ciudad actual: ");
        es.put("weather_search_label", "Buscar ciudad:");
        es.put("weather_search_hint", "Ingresa el nombre de la ciudad...");
        es.put("btn_search", "Buscar");
        es.put("weather_searching", "Buscando ciudades...");
        es.put("weather_no_results", "No se encontraron ciudades.");
        es.put("weather_results_title", "Ciudades encontradas (D-Pad para elegir):");

        es.put("about_title", "Acerca de Frost TV Launcher");
        es.put("about_version", "Versión: v1.3.0 Native Edition");
        es.put("about_arch", "100% Java Nativo y AndroidX • Sin motor web • Cero rastreadores");
        es.put("about_desc", "Inspirado en la filosofía de diseño minimalista de Couch TV (GitHub: couch-tv/couch-tv). Construido desde cero de forma nativa para Android TV y Google TV.");
        STRINGS.put("es", es);

        // --- FRENCH (FR) ---
        Map<String, String> fr = new HashMap<>();
        fr.put("popup_launcher_settings", "Paramètres du Launcher");
        fr.put("popup_tv_settings", "Paramètres Système TV");
        fr.put("sidebar_header", "PARAMÈTRES");
        fr.put("tab_lang", "Langue");
        fr.put("tab_wallpaper", "Fond d'écran");
        fr.put("tab_weather", "Météo");
        fr.put("tab_about", "À propos");
        fr.put("btn_close", "Fermer");

        fr.put("lang_title", "Paramètres de Langue");
        fr.put("lang_desc", "Choisissez la langue pour l'interface, la date et la météo :");
        fr.put("lang_status_prefix", "Langue actuelle : ");

        fr.put("wp_title", "Fond d'écran et Effet de Profondeur");
        fr.put("wp_depth_label", "Effet profondeur horloge (derrière les cimes) :");
        fr.put("btn_enabled", "Activé");
        fr.put("btn_disabled", "Désactivé");
        fr.put("wp_source_label", "Source du fond d'écran :");
        fr.put("wp_source_default", "Panorama Alpin");
        fr.put("wp_source_custom", "Charger depuis /sdcard/");
        fr.put("wp_open_files", "Ouvrir Fichiers");
        fr.put("wp_hint", "Images personnalisées : Déposez 'wallpaper.jpg' et 'wallpaper_fg.png' dans /sdcard/Wallpapers/.");

        fr.put("weather_title", "Affichage Météo (Open-Meteo)");
        fr.put("weather_toggle_label", "Météo :");
        fr.put("btn_on", "Oui");
        fr.put("btn_off", "Non");
        fr.put("weather_unit_label", "Unité :");
        fr.put("weather_interval_label", "Intervalle :");
        fr.put("weather_current_city_prefix", "Ville actuelle : ");
        fr.put("weather_search_label", "Rechercher une ville :");
        fr.put("weather_search_hint", "Entrez le nom de la ville...");
        fr.put("btn_search", "Chercher");
        fr.put("weather_searching", "Recherche en cours...");
        fr.put("weather_no_results", "Aucune ville trouvée.");
        fr.put("weather_results_title", "Villes trouvées (D-Pad pour choisir) :");

        fr.put("about_title", "À propos de Frost TV Launcher");
        fr.put("about_version", "Version : v1.3.0 Native Edition");
        fr.put("about_arch", "100% Java Natif & AndroidX • Pas de moteur web • Zéro traceur");
        fr.put("about_desc", "Inspiré par la philosophie de design minimaliste de Couch TV (GitHub: couch-tv/couch-tv). Conçu de zéro comme un lanceur natif pour Android TV et Google TV.");
        STRINGS.put("fr", fr);

        // --- PORTUGUESE (PT) ---
        Map<String, String> pt = new HashMap<>();
        pt.put("popup_launcher_settings", "Configurações do Launcher");
        pt.put("popup_tv_settings", "Configurações do Sistema TV");
        pt.put("sidebar_header", "CONFIGURAÇÕES");
        pt.put("tab_lang", "Idioma");
        pt.put("tab_wallpaper", "Plano de fundo");
        pt.put("tab_weather", "Clima");
        pt.put("tab_about", "Sobre");
        pt.put("btn_close", "Fechar");

        pt.put("lang_title", "Configurações de Idioma");
        pt.put("lang_desc", "Escolha o idioma para a interface, data e previsão do tempo:");
        pt.put("lang_status_prefix", "Idioma atual: ");

        pt.put("wp_title", "Plano de Fundo e Efeito de Profundidade");
        pt.put("wp_depth_label", "Efeito profundidade relógio (atrás dos picos):");
        pt.put("btn_enabled", "Ativado");
        pt.put("btn_disabled", "Desativado");
        pt.put("wp_source_label", "Origem do plano de fundo:");
        pt.put("wp_source_default", "Panorama Alpino");
        pt.put("wp_source_custom", "Carregar de /sdcard/");
        pt.put("wp_open_files", "Abrir Arquivos");
        pt.put("wp_hint", "Imagens personalizadas: Coloque 'wallpaper.jpg' e 'wallpaper_fg.png' em /sdcard/Wallpapers/.");

        pt.put("weather_title", "Previsão do Tempo (Open-Meteo)");
        pt.put("weather_toggle_label", "Clima:");
        pt.put("btn_on", "Ligado");
        pt.put("btn_off", "Desligado");
        pt.put("weather_unit_label", "Unidade:");
        pt.put("weather_interval_label", "Intervalo:");
        pt.put("weather_current_city_prefix", "Cidade atual: ");
        pt.put("weather_search_label", "Buscar cidade:");
        pt.put("weather_search_hint", "Digite o nome da cidade...");
        pt.put("btn_search", "Buscar");
        pt.put("weather_searching", "Buscando cidades...");
        pt.put("weather_no_results", "Nenhuma cidade encontrada.");
        pt.put("weather_results_title", "Cidades encontradas (D-Pad para selecionar):");

        pt.put("about_title", "Sobre o Frost TV Launcher");
        pt.put("about_version", "Versão: v1.3.0 Native Edition");
        pt.put("about_arch", "100% Java Nativo & AndroidX • Sem motor web • Zero rastreadores");
        pt.put("about_desc", "Inspirado na filosofia de design minimalista do Couch TV (GitHub: couch-tv/couch-tv). Desenvolvido do zero de forma nativa para Android TV e Google TV.");
        STRINGS.put("pt", pt);

        // --- ITALIAN (IT) ---
        Map<String, String> it = new HashMap<>();
        it.put("popup_launcher_settings", "Impostazioni Launcher");
        it.put("popup_tv_settings", "Impostazioni di Sistema TV");
        it.put("sidebar_header", "IMPOSTAZIONI");
        it.put("tab_lang", "Lingua");
        it.put("tab_wallpaper", "Sfondo");
        it.put("tab_weather", "Meteo");
        it.put("tab_about", "Info");
        it.put("btn_close", "Chiudi");

        it.put("lang_title", "Impostazioni Lingua");
        it.put("lang_desc", "Scegli la lingua per l'interfaccia utente, la data e il meteo:");
        it.put("lang_status_prefix", "Lingua corrente: ");

        it.put("wp_title", "Sfondo ed Effetto Profondità");
        it.put("wp_depth_label", "Effetto profondità orologio (dietro le vette):");
        it.put("btn_enabled", "Attivato");
        it.put("btn_disabled", "Disattivato");
        it.put("wp_source_label", "Sorgente dello sfondo:");
        it.put("wp_source_default", "Panorama Alpino");
        it.put("wp_source_custom", "Carica da /sdcard/");
        it.put("wp_open_files", "Apri File");
        it.put("wp_hint", "Immagini personalizzate: Posiziona 'wallpaper.jpg' e 'wallpaper_fg.png' in /sdcard/Wallpapers/.");

        it.put("weather_title", "Previsioni Meteo (Open-Meteo)");
        it.put("weather_toggle_label", "Meteo:");
        it.put("btn_on", "Sì");
        it.put("btn_off", "No");
        it.put("weather_unit_label", "Unità:");
        it.put("weather_interval_label", "Intervallo:");
        it.put("weather_current_city_prefix", "Città corrente: ");
        it.put("weather_search_label", "Cerca città:");
        it.put("weather_search_hint", "Inserisci il nome della città...");
        it.put("btn_search", "Cerca");
        it.put("weather_searching", "Ricerca città in corso...");
        it.put("weather_no_results", "Nessuna città trovata.");
        it.put("weather_results_title", "Città trovate (D-Pad per selezionare):");

        it.put("about_title", "Info su Frost TV Launcher");
        it.put("about_version", "Versione: v1.3.0 Native Edition");
        it.put("about_arch", "100% Java Nativo e AndroidX • Nessun motore web • Zero tracker");
        it.put("about_desc", "Ispirato alla filosofia di design minimalista di Couch TV (GitHub: couch-tv/couch-tv). Sviluppato da zero come launcher nativo per Android TV e Google TV.");
        STRINGS.put("it", it);
    }

    public static String get(String langCode, String key) {
        String code = (langCode == null) ? "de" : langCode.toLowerCase();
        Map<String, String> map = STRINGS.get(code);
        if (map == null) {
            map = STRINGS.get("de");
        }
        if (map != null && map.containsKey(key)) {
            return map.get(key);
        }
        // Fallback to German
        Map<String, String> de = STRINGS.get("de");
        return (de != null) ? de.get(key) : "";
    }
}
