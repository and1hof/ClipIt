package com.clipit;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This class has one purpose: to record the contents of the user's screen,
 * and store that recording as a video file.
 * <p>
 * It should contain a method startRecord() and stopRecord() for the InputManager to utilize.
 */
public class Recorder implements Runnable{
   long unixTime;
   private Robot myRobot;
   private Thread t;
   private boolean terminate_thread;

   public Recorder() {
      terminate_thread = false;
      try {
         myRobot = new Robot();
      } catch(AWTException ex) {
         System.out.println("robot was not created");
      } 
   } 
   public void run() {
      unixTime = System.currentTimeMillis() / 1000L;
      File file = new File("recordings/" + unixTime + ".mp4");
      try {
         boolean created = file.createNewFile();
      } catch (IOException e) {
         e.printStackTrace();
      }
      final IMediaWriter writer = ToolFactory.makeWriter("recordings/" + unixTime + ".mp4");
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4,screenSize.width, screenSize.height);
      long startTime = System.nanoTime();
      while(true) {
         if(terminate_thread)
            break;
         Rectangle captureRect = new Rectangle(0, 0, screenSize.width, screenSize.height);
         BufferedImage sourceImage = myRobot.createScreenCapture(captureRect);
         BufferedImage convertedImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
         convertedImage.getGraphics().drawImage(sourceImage, 0, 0, null);

         writer.encodeVideo(0, convertedImage, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         try {
            Thread.sleep(33);
         } catch(InterruptedException ie) {
         
         }
      }
      writer.close();

   }
   public void start()
   {
      if (t == null)
      {
         t = new Thread (this);
         t.start();
      }
   }
   public void stop() {
      terminate_thread = true;   
   }
    
}

