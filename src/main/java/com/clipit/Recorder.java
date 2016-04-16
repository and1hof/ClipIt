package com.clipit;

import com.dropbox.core.DbxException;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
//import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.*;

/*import java.awt.*;//old code
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;*/
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
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
   private String audioFileName;
   private String videoFileName;
   private volatile boolean safeCombine;
   public Recorder() {
      audioFileName = null;
      videoFileName = null;
      terminate_thread = false;
      safeCombine = false;

      try {
         myRobot = new Robot();
      } catch(AWTException ex) {
         System.out.println("robot was not created");
      } 
   }
   public void run() {
      unixTime = System.currentTimeMillis() / 1000L;
      videoFileName = "recordings/" + unixTime + ".mp4";
      File videoFile = new File(videoFileName);
      try {
         boolean created = videoFile.createNewFile();
      } catch (IOException e) {
         e.printStackTrace();
      }


      final IMediaWriter writer = ToolFactory.makeWriter("recordings/" + unixTime + ".mp4");
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4,screenSize.width, screenSize.height);
      //myFormat.getEncoding()
      //writer.addAudioStream(0, 0, ICodec.ID.CODEC_ID_MP3, myFormat.getChannels(), (int)myFormat.getSampleRate());

      long startTime = System.nanoTime();
      RecordAudio myAudio = new RecordAudio();
      myAudio.start();
      while(true) {
         if(terminate_thread) {
            myAudio.stop();
            break;
         }
         Rectangle captureRect = new Rectangle(0, 0, screenSize.width, screenSize.height);
         BufferedImage sourceImage = myRobot.createScreenCapture(captureRect);
         BufferedImage convertedImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
         convertedImage.getGraphics().drawImage(sourceImage, 0, 0, null);

         writer.encodeVideo(0, convertedImage, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         try {
            Thread.sleep(20);
         } catch(InterruptedException ie) {
         
         }
      }
      writer.close();
      while(!safeCombine){
         //wait for audio thread
      }
      File uploadFile = createMergedFile(audioFileName, videoFileName);

      try {
         UploadManager.upload(uploadFile);
      } catch (IOException e) {
         e.printStackTrace();
      } catch (DbxException e) {
         e.printStackTrace();
      }

   }
   public File createMergedFile(String audioFileName, String videoFileName) {
      long unixTime = System.currentTimeMillis() / 1000L;
      String mergedFileName = "recordings/merged" + unixTime + ".mp4";
      File myFile = new File(mergedFileName);
      try {
         boolean created = myFile.createNewFile();
      } catch (IOException e) {

      }
      IMediaWriter comboWriter = ToolFactory.makeWriter(mergedFileName);
      IContainer videoContainer = IContainer.make();
      IContainer audioContainer = IContainer.make();
      videoContainer.open(videoFileName, IContainer.Type.READ, null);
      audioContainer.open(audioFileName, IContainer.Type.READ, null);
      int numStreamVideo = videoContainer.getNumStreams();
      int numStreamAudio = audioContainer.getNumStreams();
      IStreamCoder videoCoder = null;
      IStreamCoder audioCoder = null;
      int audioId = -1;
      int videoId = -1;
      for(int i=0; i<numStreamVideo; i++){
         IStream stream = videoContainer.getStream(i);
         IStreamCoder videoCode = stream.getStreamCoder();

         if(videoCode.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
         {
            videoId = i;
            videoCoder = videoCode;
            videoCode.close();
            stream.delete();
            break;
         }

      }

      for(int i=0; i<numStreamAudio; i++){
         IStream stream = audioContainer.getStream(i);
         IStreamCoder audioCode = stream.getStreamCoder();

         if(audioCode.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
         {
            audioId = i;
            audioCoder = audioCode;
            audioCode.close();
            stream.delete();
            break;
         }
      }
      if(videoCoder == null || audioCoder == null) {
         throw new RuntimeException("audio or video stream is missing");
      }
      videoCoder.open(null,null);
      audioCoder.open(null,null);
      comboWriter.addAudioStream(0, 0, audioCoder.getChannels(), audioCoder.getSampleRate());
      comboWriter.addVideoStream(1, 1, videoCoder.getWidth(), videoCoder.getHeight());
      IPacket audioPacket = IPacket.make();
      IPacket videoPacket = IPacket.make();
      while(audioContainer.readNextPacket(audioPacket) >= 0 || videoContainer.readNextPacket(videoPacket) >= 0){

         if (videoPacket.getStreamIndex() == videoId) {

            //video packet
            IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
                    videoCoder.getWidth(),
                    videoCoder.getHeight());
            int offset = 0;
            while (offset < videoPacket.getSize()) {
               int bytesDecoded = videoCoder.decodeVideo(picture, videoPacket, offset);
               if (bytesDecoded < 0) throw new RuntimeException("bytesDecoded not working");
               offset += bytesDecoded;
               if (picture.isComplete()) {
                  comboWriter.encodeVideo(1, picture);

               }
            }
         }
         if (audioPacket.getStreamIndex() == audioId) {
            //audio packet

            IAudioSamples samples = IAudioSamples.make(512,
                    audioCoder.getChannels(),
                    IAudioSamples.Format.FMT_S16);
            int offset = 0;
            while (offset < audioPacket.getSize()) {
               int bytesDecodedaudio = audioCoder.decodeAudio(samples, audioPacket, offset);
               if (bytesDecodedaudio < 0)
                  throw new RuntimeException("could not detect audio");
               offset += bytesDecodedaudio;

               if (samples.isComplete()) {
                  comboWriter.encodeAudio(0, samples);

               }
            }

         }
      }
      comboWriter.close();
      audioPacket.delete();
      videoPacket.delete();
      videoCoder.close();
      audioCoder.close();
      videoContainer.close();
      audioContainer.close();

      return myFile;
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
   public class RecordAudio implements Runnable {
      private Thread t;
      private boolean stopAudio;
      public RecordAudio() {
         stopAudio = false;

      }
      public void run() {

         AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);
         Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
         int selectedMixerIndex = 0;
         Mixer mixer = AudioSystem.getMixer(mixerInfo[ selectedMixerIndex ]);
         System.out.println(mixer.getMixerInfo());
         DataLine.Info dataLineInfo = new DataLine.Info( TargetDataLine.class, audioFormat);
         TargetDataLine mic = null;

         try {
            mic = (TargetDataLine) mixer.getLine(dataLineInfo);
            mic.open(mic.getFormat(), mic.getBufferSize());

         } catch (LineUnavailableException e) {
            e.printStackTrace();
         }
         ByteArrayOutputStream out  = new ByteArrayOutputStream();
         byte[] audioBytes = new byte[ mic.getBufferSize() / 2 ];
         int numBytesRead = 0;
         mic.start();
         while(true) {
            if(stopAudio) {
               break;
            }
            numBytesRead =  mic.read(audioBytes, 0, audioBytes.length);
            out.write(audioBytes, 0, numBytesRead);
         }
         try {
            out.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
         long unixTime = System.currentTimeMillis() / 1000L;
         Recorder.this.audioFileName = "recordings/" + unixTime + ".wav";
         File outputFile = new File(Recorder.this.audioFileName);
         try {
            boolean created = outputFile.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
         ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
         AudioInputStream outputAIS = new AudioInputStream(bais, audioFormat,
                 out.toByteArray().length / audioFormat.getFrameSize());
         try {
            AudioSystem.write(outputAIS, AudioFileFormat.Type.WAVE, outputFile);
            bais.close();
            outputAIS.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
         Recorder.this.safeCombine = true;

      }

      public void start() {
         if (t == null)
         {
            t = new Thread (this);
            t.start();
         }
      }
      public void stop() {
         stopAudio = true;
      }
   }
}
/*public class Recorder implements Runnable{//old code
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
      try {
         UploadManager.upload(file);
      } catch (IOException e) {
         e.printStackTrace();
      } catch (DbxException e) {
         e.printStackTrace();
      }

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
    
}*/

