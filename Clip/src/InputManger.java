

/**
 * This class is responsible for managing input from the user (keys).
 * This class should call a method startRecord() when the record button is first pressed,
 * and than call stopRecord() when the record button is pressed again. Just put comments in where these
 * would go for now.
 * <p>
 * We also need to consider how to display a recording notification on the screen, an "upload finished" notification
 * and consider displaying error messages.
 * <p>
 * A config file might be useful.
 */
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class InputManger implements NativeKeyListener {
	static ImageIcon icon = new ImageIcon("red_bl.gif");
	static JLabel label = new JLabel(icon);
	static JFrame frame = new JFrame();
	static Recorder myRecorder = new Recorder();
	
	static boolean myFlag;
	static String key = "";
	static String modifier = "";
	
    public void nativeKeyPressed(NativeKeyEvent e) {
        System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
             
        if (myFlag == true && NativeKeyEvent.getKeyText(e.getKeyCode()).equals(key)) {
        	redDot();
            myRecorder.start();
        	myFlag = false;
        	
    }
        
    	// when call stop() myFlag = false
    	if (NativeKeyEvent.getKeyText(e.getKeyCode()).equals("S")) {
    		myRecorder.stop();    		
    		frame.dispose();
    	}
    		
    		
    		
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            try {
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
    }
    

    public void nativeKeyReleased(NativeKeyEvent e) {

    	
        System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        if (NativeKeyEvent.getKeyText(e.getKeyCode()).equals("Left Alt")) {
        	myFlag = true;
        }
    }
    

    public void nativeKeyTyped(NativeKeyEvent e) {
        System.out.println("Key Typed: " + e.getKeyText(e.getKeyCode()));

    }

    public static void main(String[] args) throws FileNotFoundException {
    	Scanner settings = new Scanner(new File("Settings.config"));
    	settings.nextLine();
    	modifier =  settings.next() + " " + settings.next();
    	key = settings.next();
    	System.out.println(modifier);
    	System.out.println(key);
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }
        
        GlobalScreen.addNativeKeyListener(new InputManger());
        settings.close();
    }
    public static void redDot() {
    	
    	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX();
        int y = 0;
        frame.setLocation(x-38, y+20);
    	
        // Set's the window to be "always on top"
        frame.setAlwaysOnTop( true );       
        //frame.setLocationByPlatform( true );
        frame.add(label);
        frame.setUndecorated(true);
        frame.pack();
        frame.setVisible( true );
    }
}
