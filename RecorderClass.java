/*
 * Event Listeners TCSS 305
 */

//package view;
import java.util.*;
import java.awt.image.BufferedImage;
import java.awt.*;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.concurrent.TimeUnit;

import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.ICodec;

public class RecorderClass implements Runnable{
   private Robot myRobot;
   private Thread t;
   private boolean terminate_thread;
   public RecorderClass() {
      terminate_thread = false;
      try {
         myRobot = new Robot();
      } catch(AWTException ex) {
         System.out.println("robot was not created");
      } 
   } 
   public void run() {
      int counter = 0;
      final IMediaWriter writer = ToolFactory.makeWriter("test.mp4");
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
         counter++; 
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
class TestThread {
   public static void main(String args[]) {
      RecorderClass myRecorder = new RecorderClass();
      myRecorder.start();
      try {
         Thread.sleep(15000); 
      } catch(InterruptedException ie) {
         
      }
      
      myRecorder.stop();
      
   }   
}
