package com.clipit;

import com.dropbox.core.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * This class is responsible for passing a video file to the YouTube API,
 * grabbing the resulting URL and storing it in a variable which will than be moved
 * to the user's clipboard.
 */
public class UploadManager {

    public static void upload(File theFile) throws IOException, DbxException {
        final String DROP_BOX_APP_KEY = "oi7o6vknwiuinyf";
        final String DROP_BOX_APP_SECRET = "ayrhpxsaw1e9u75";

        String rootDir = "recordings";

        DbxAppInfo dbxAppInfo = new DbxAppInfo(DROP_BOX_APP_KEY, DROP_BOX_APP_SECRET);

        DbxRequestConfig reqConfig = new DbxRequestConfig("javarootsDropbox/1.0",
                Locale.getDefault().toString());
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(reqConfig, dbxAppInfo);


//        String authorizeUrl = webAuth.start();
//        System.out.println("1. Go to this URL : " + authorizeUrl);
//        System.out.println("2. Click \"Allow\" (you might have to log in first)");
//        System.out.println("3. Copy the authorization code and paste here ");
//        String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();


//        DbxAuthFinish authFinish = webAuth.finish(code);
//        String accessToken = authFinish.accessToken;
        String accessToken = "KFB8qu9OsNAAAAAAAAAAFM1UdqjBQNyvyk5vSyXebuu8hRr0wEIiHA-ekh8ds52v";
        DbxClient client = new DbxClient(reqConfig, accessToken);

        //System.out.println("account name is : " + client.getAccountInfo().displayName);


        File inputFile = theFile;
        FileInputStream inputStream = new FileInputStream(inputFile);
        try {

            DbxEntry.File uploadedFile = client.uploadFile("/" + theFile.getName(),
                    DbxWriteMode.add(), inputFile.length(), inputStream);
            String sharedUrl = client.createShareableUrl("/" + "/" + theFile.getName());
            //System.out.println("Uploaded: " + uploadedFile.toString() + " URL " + sharedUrl);
            System.out.println(sharedUrl);
            StringSelection stringSelection = new StringSelection(sharedUrl);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
            System.out.println("The clip was uploaded successfully. The link has been copied to your clipboard.");
        } finally {
            inputStream.close();
        }
    }
}