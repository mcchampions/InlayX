package me.qscbm.inlayx.util;

import java.io.*;
import java.nio.file.*;
import org.json.JSONObject;

public class MCAssetsUtils {

    public static final String VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    public static JSONObject getVersionManifest() {
        try {
            return new JSONObject(NetUtils.sendGetRequest(VERSION_MANIFEST));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject getClientJson(String version, String cacheDir) {
        String fileName = "client_" + version + ".json";
        File cacheFile = new File(cacheDir, fileName);
        return getCachedOrFetch(cacheFile, () -> {
            JSONObject manifest = getVersionManifest();
            if (manifest == null) return null;
            var versions = manifest.getJSONArray("versions");
            JSONObject target = null;
            for (int i = 0; i < versions.length(); i++) {
                JSONObject obj = versions.getJSONObject(i);
                if (obj.getString("id").equals(version)) {
                    target = obj;
                    break;
                }
            }
            if (target == null) return null;
            String url = target.getString("url");
            try {
                return new JSONObject(NetUtils.sendGetRequest(url));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static JSONObject getVersionAssetIndex(String version, String cacheDir) {
        String fileName = "asset_index_" + version + ".json";
        File cacheFile = new File(cacheDir, fileName);
        return getCachedOrFetch(cacheFile, () -> {
            JSONObject client = getClientJson(version, cacheDir);
            if (client == null) return null;
            JSONObject index = client.getJSONObject("assetIndex");
            String url = index.getString("url");
            try {
                return new JSONObject(NetUtils.sendGetRequest(url));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static JSONObject getLanguage(String version, String language, String cacheDir) {
        File cacheFile = new File(new File(new File(cacheDir, "lang"), version), language + ".json");

        if (language.equals("en_us")) {
            // 无法从 Asset Index 获取 en_us.json 从 https://github.com/misode/mcmeta/tree/assets 上获取
            return getCachedOrFetch(cacheFile, () -> {
                String url = "https://raw.githubusercontent.com/misode/mcmeta/" + version
                        + "-assets/assets/minecraft/lang/en_us.json";
                try {
                    return new JSONObject(NetUtils.sendGetRequest(url));
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });
        }

        return getCachedOrFetch(cacheFile, () -> {
            JSONObject assetIndex = getVersionAssetIndex(version, cacheDir);
            if (assetIndex == null) return null;
            JSONObject objects = assetIndex.optJSONObject("objects");
            if (objects == null) return null;
            String key = "minecraft/lang/" + language + ".json";
            JSONObject langObj = objects.optJSONObject(key);
            if (langObj == null) return null;
            String hash = langObj.getString("hash");
            String url = "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
            try {
                return new JSONObject(NetUtils.sendGetRequest(url));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private static JSONObject getCachedOrFetch(File cacheFile, Fetcher fetcher) {
        if (cacheFile.exists()) {
            try {
                String content = Files.readString(cacheFile.toPath());
                return new JSONObject(content);
            } catch (IOException | org.json.JSONException e) {
                e.printStackTrace();
            }
        }

        JSONObject result = fetcher.fetch();
        if (result != null) {
            try {
                File parent = cacheFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.writeString(cacheFile.toPath(), result.toString(2));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @FunctionalInterface
    private interface Fetcher {
        JSONObject fetch();
    }
}
