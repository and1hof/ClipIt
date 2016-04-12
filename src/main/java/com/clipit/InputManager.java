package com.clipit;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class InputManager implements NativeKeyListener {
    static ImageIcon icon = new ImageIcon("red_bl.gif");
	static JLabel label = new JLabel(icon);
	static JFrame frame = new JFrame();
	static Recorder myRecorder = null;
	static long startTime;
	static long stopTime;
	static boolean first_key_pressed;
	static String key = "";
	static String modifier = "";
	static boolean dot_showing = false;
    public static void manageInput() throws FileNotFoundException {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

// Change the level for all handlers attached to the default logger.
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].setLevel(Level.OFF);
        }

        Scanner settings = new Scanner(new File("settings.conf"));
        settings.nextLine();
        modifier = settings.next() + " " + settings.next();
        key = settings.next();
        //System.out.println(modifier);
        //System.out.println(key);
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(new InputManager());
        settings.close();
    }

    public static void redDot() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX();
        int y = 0;
        frame.setLocation(x - 38, y + 20);

        // Set's the window to be "always on top"
        frame.setAlwaysOnTop(true);
        //frame.setLocationByPlatform( true );
        frame.add(label);
        frame.setUndecorated(true);
        frame.pack();
        frame.setVisible(true);
        dot_showing = true;
    }
    
    public void nativeKeyPressed(NativeKeyEvent e) {
    	
        //System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
    	
    	if (NativeKeyEvent.getKeyText(e.getKeyCode()).equals("Left Alt")) {
    		first_key_pressed = true;
        }
    	
        if (NativeKeyEvent.getKeyText(e.getKeyCode()).equals(key) && first_key_pressed) {
        	myRecorder = new Recorder();
        	if(!dot_showing) {
        		redDot();
        	}
            myRecorder.start();
        	first_key_pressed = false;

    }
       
    	// when call stop() myFlag = falssse
    	if (NativeKeyEvent.getKeyText(e.getKeyCode()).equals("S")) {
            if(myRecorder != null) {
	    		myRecorder.stop();
	            frame.dispose();
	            dot_showing = false;
            }
            myRecorder = null;
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
    	first_key_pressed = false;

        //System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
        
    }

    public void nativeKeyTyped(NativeKeyEvent e) {
        //System.out.println("Key Typed: " + e.getKeyText(e.getKeyCode()));

    }
}
