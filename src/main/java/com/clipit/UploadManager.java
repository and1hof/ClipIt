package com.clipit;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

/**
 * This class is responsible for passing a video file to the YouTube API,
 * grabbing the resulting URL and storing it in a variable which will than be moved
 * to the user's clipboard.
 */
public class UploadManager {
    private static String token = null;

    public static void uploadVideo() {
    }

    /**
     * Authenticate the user against the auth server. If successful, we should store a token for later use.
     */
    public static void basicAuth(String email, String password) {
        try {
            HttpResponse<JsonNode> response = Unirest.get("http://cryologic-andhofmt.c9users.io/token").basicAuth(email, password).asJson();
            if (response.getStatus() == 401) { // do something
                System.out.println("Log in failed. Please check your credentials and try again.");
            } else if (response.getStatus() == 200) {
                // it worked!
                token = response.getBody().getObject().getString("token");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
