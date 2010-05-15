/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.clickin2dabeat;


import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A rhythm/music game for Android that can use any video as the background.
 */
public class C2B extends Activity {
  public static final int GAME_MODE = 0;
  public static final int TWOPASS_MODE = 1;
  public static final int ONEPASS_MODE = 2;
  public int mode;
  public boolean wasEditMode;
  private boolean forceEditMode;

  private VideoView background;
  private GameView foreground;
  private FrameLayout layout;

  private String c2bFileName;
  private String[] filenames;
  
  private String marketId;

  // These are parsed in from the C2B file
  private String title;
  private String author;
  private String level;
  private String media;

  private Uri videoUri;

  public ArrayList<Target> targets;

  public ArrayList<Integer> beatTimes;
  private BeatTimingsAdjuster timingAdjuster;

  private boolean busyProcessing;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    UpdateChecker checker = new UpdateChecker();
    int latestVersion = checker.getLatestVersionCode();
    String packageName = C2B.class.getPackage().getName();
    int currentVersion = 0;
    try {
      currentVersion = getPackageManager().getPackageInfo(packageName, 0).versionCode;
    } catch (NameNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if (latestVersion > currentVersion){
      marketId = checker.marketId;
      displayUpdateMessage();
    } else {
      resetGame();
    }
  }

  private void resetGame() {
    targets = new ArrayList<Target>();

    mode = GAME_MODE;
    forceEditMode = false;
    wasEditMode = false;
    busyProcessing = false;

    c2bFileName = "";
    title = "";
    author = "";
    level = "";
    media = "";

    background = null;
    foreground = null;
    layout = null;

    background = new VideoView(this);

    foreground = new GameView(this);
    layout = new FrameLayout(this);
    layout.addView(background);
    layout.setPadding(30, 0, 0, 0); // Is there a better way to do layout?

    beatTimes = null;
    timingAdjuster = null;

    background.setOnPreparedListener(new OnPreparedListener() {
      public void onPrepared(MediaPlayer mp) {
        background.start();
      }
    });

    background.setOnErrorListener(new OnErrorListener() {
      public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
        background.setVideoURI(videoUri);
        return true;
      }
    });

    background.setOnCompletionListener(new OnCompletionListener() {
      public void onCompletion(MediaPlayer mp) {
        if (mode == ONEPASS_MODE) {
          Toast waitMessage = Toast.makeText(C2B.this, getString(R.string.PROCESSING), 5000);
          waitMessage.show();
          (new Thread(new BeatsWriter())).start();
        } else if (mode == TWOPASS_MODE) {
          mode = ONEPASS_MODE;
          (new Thread(new BeatsTimingAdjuster())).start();
          displayCreateLevelInfo();
          return;
        }
        displayStats();
      }
    });

    layout.addView(foreground);
    setContentView(layout);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    displayStartupMessage();
  }

  private void writeC2BFile(String filename) {
    String contents =
        "<c2b title='" + title + "' level='" + level + "' author='" + author + "' media='" + media
            + "'>";
    ArrayList<Target> targets = foreground.recordedTargets;
    if (timingAdjuster != null) {
      targets = timingAdjuster.adjustBeatTargets(foreground.recordedTargets);
    }
    for (int i = 0; i < targets.size(); i++) {
      Target t = targets.get(i);
      contents = contents + "<beat time='" + Double.toString(t.time) + "' ";
      contents = contents + "x='" + Integer.toString(t.x) + "' ";
      contents = contents + "y='" + Integer.toString(t.y) + "' ";
      contents = contents + "color='" + Integer.toHexString(t.color) + "'/>";
    }
    contents = contents + "</c2b>";
    try {
      FileWriter writer = new FileWriter(filename);
      writer.write(contents);
      writer.close();
    } catch (IOException e) {
      // TODO(clchen): Do better error handling here
      e.printStackTrace();
    }
  }


  private void loadC2B(String fileUriString) {
    try {
      FileInputStream fis = new FileInputStream(fileUriString);
      DocumentBuilder docBuild;
      docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document c2b = docBuild.parse(fis);
      runC2B(c2b);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FactoryConfigurationError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  private void runC2B(Document c2b) {
    Node root = c2b.getElementsByTagName("c2b").item(0);

    title = root.getAttributes().getNamedItem("title").getNodeValue();
    author = root.getAttributes().getNamedItem("author").getNodeValue();
    level = root.getAttributes().getNamedItem("level").getNodeValue();
    media = root.getAttributes().getNamedItem("media").getNodeValue();

    NodeList beats = c2b.getElementsByTagName("beat");
    targets = new ArrayList<Target>();
    for (int i = 0; i < beats.getLength(); i++) {
      NamedNodeMap attribs = beats.item(i).getAttributes();
      double time = Double.parseDouble(attribs.getNamedItem("time").getNodeValue());
      int x = Integer.parseInt(attribs.getNamedItem("x").getNodeValue());
      int y = Integer.parseInt(attribs.getNamedItem("y").getNodeValue());
      String colorStr = attribs.getNamedItem("color").getNodeValue();
      targets.add(new Target(time, x, y, colorStr));
    }
    if ((beats.getLength() == 0) || forceEditMode) {
      displayCreateLevelAlert();
    } else {
      videoUri = Uri.parse(media);
      background.setVideoURI(videoUri);
    }
  }

  private void displayCreateLevelAlert() {
    mode = ONEPASS_MODE;

    Builder createLevelAlert = new Builder(this);

    String titleText = getString(R.string.NO_BEATS) + " \"" + title + "\"";
    createLevelAlert.setTitle(titleText);

    String[] choices = new String[2];
    choices[0] = getString(R.string.ONE_PASS);
    choices[1] = getString(R.string.TWO_PASS);
    createLevelAlert.setSingleChoiceItems(choices, 0, new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if (which == 0) {
          mode = ONEPASS_MODE;
        } else {
          mode = TWOPASS_MODE;
        }
      }
    });

    createLevelAlert.setPositiveButton("Ok", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if (busyProcessing) {
          Toast.makeText(C2B.this, R.string.STILL_BUSY, 5000).show();
          displayCreateLevelAlert();
          return;
        }
        wasEditMode = true;
        if (mode == TWOPASS_MODE) {
          beatTimes = new ArrayList<Integer>();
          timingAdjuster = new BeatTimingsAdjuster();
        }
        displayCreateLevelInfo();
      }
    });

    createLevelAlert.setNegativeButton("Cancel", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if (busyProcessing) {
          Toast.makeText(C2B.this, R.string.STILL_BUSY, 5000).show();
          displayCreateLevelAlert();
          return;
        }
        displayC2BFiles();
      }
    });

    createLevelAlert.setCancelable(false);

    createLevelAlert.show();
  }


  private void displayC2BFiles() {
    Builder c2bFilesAlert = new Builder(this);

    String titleText = getString(R.string.CHOOSE_STAGE);
    c2bFilesAlert.setTitle(titleText);

    File c2bDir = new File("/sdcard/c2b/");
    filenames = c2bDir.list(new FilenameFilter() {
      public boolean accept(File dir, String filename) {
        return filename.endsWith(".c2b");
      }
    });

    if (filenames == null) {
      displayNoFilesMessage();
      return;
    }
    if (filenames.length == 0) {
      displayNoFilesMessage();
      return;
    }

    c2bFilesAlert.setSingleChoiceItems(filenames, -1, new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        c2bFileName = "/sdcard/c2b/" + filenames[which];
      }
    });

    c2bFilesAlert.setPositiveButton("Go!", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        loadC2B(c2bFileName);
        dialog.dismiss();
      }
    });
/*
    c2bFilesAlert.setNeutralButton("Set new beats", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        displaySetNewBeatsConfirmation();
      }
    });
*/    
    final Activity self = this;
    c2bFilesAlert.setNeutralButton(getString(R.string.MOAR_STAGES), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Intent i = new Intent();
        ComponentName comp =
            new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
        i.setComponent(comp);
        i.setAction("android.intent.action.VIEW");
        i.addCategory("android.intent.category.BROWSABLE");
        Uri uri = Uri.parse("http://groups.google.com/group/clickin-2-da-beat/files");
        i.setData(uri);
        self.startActivity(i);
        finish();
      }
    });

    c2bFilesAlert.setNegativeButton(getString(R.string.QUIT), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });

    c2bFilesAlert.setCancelable(false);
    c2bFilesAlert.show();
  }

/*
  private void displaySetNewBeatsConfirmation() {
    Builder setNewBeatsConfirmation = new Builder(this);

    String titleText = getString(R.string.EDIT_CONFIRMATION);
    setNewBeatsConfirmation.setTitle(titleText);

    String message = getString(R.string.EDIT_WARNING);
    setNewBeatsConfirmation.setMessage(message);

    setNewBeatsConfirmation.setPositiveButton("Continue", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        forceEditMode = true;
        loadC2B(c2bFileName);
        dialog.dismiss();
      }
    });

    setNewBeatsConfirmation.setNegativeButton("Cancel", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        displayC2BFiles();
        dialog.dismiss();
      }
    });

    setNewBeatsConfirmation.setCancelable(false);
    setNewBeatsConfirmation.show();
  }
*/
  private void displayCreateLevelInfo() {
    Builder createLevelInfoAlert = new Builder(this);

    String titleText = getString(R.string.BEAT_SETTING_INFO);
    createLevelInfoAlert.setTitle(titleText);

    String message = "";
    if (mode == TWOPASS_MODE) {
      message = getString(R.string.TWO_PASS_INFO);
    } else {
      message = getString(R.string.ONE_PASS_INFO);
    }

    createLevelInfoAlert.setMessage(message);

    createLevelInfoAlert.setPositiveButton("Start", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        videoUri = Uri.parse(media);
        background.setVideoURI(videoUri);
      }
    });

    createLevelInfoAlert.setCancelable(false);
    createLevelInfoAlert.show();
  }

  private void displayStats() {
    Builder statsAlert = new Builder(this);

    String titleText = "";
    if (!wasEditMode) {
      titleText = "Game Over";
    } else {
      titleText = "Stage created!";
    }

    statsAlert.setTitle(titleText);

    int longestCombo = foreground.longestCombo;
    if (foreground.comboCount > longestCombo) {
      longestCombo = foreground.comboCount;
    }
    String message = "";

    if (!wasEditMode) {
      message = "Longest combo: " + Integer.toString(longestCombo);
    } else {
      message = "Beats recorded!";
    }

    statsAlert.setMessage(message);

    statsAlert.setPositiveButton("Play", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if (busyProcessing) {
          Toast.makeText(C2B.this, R.string.STILL_BUSY, 5000).show();
          displayStats();
          return;
        }
        resetGame();
      }
    });

    statsAlert.setNegativeButton("Quit", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        if (busyProcessing) {
          Toast.makeText(C2B.this, R.string.STILL_BUSY, 5000).show();
          displayStats();
          return;
        }
        finish();
      }
    });

    statsAlert.setCancelable(false);
    statsAlert.show();
  }

  private void displayNoFilesMessage() {
    Builder noFilesMessage = new Builder(this);

    String titleText = getString(R.string.NO_STAGES_FOUND);
    noFilesMessage.setTitle(titleText);

    String message = getString(R.string.NO_STAGES_INFO);
    noFilesMessage.setMessage(message);

    noFilesMessage.setPositiveButton(getString(R.string.SHUT_UP), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        loadHardCodedRickroll();
      }
    });

    final Activity self = this;
    noFilesMessage.setNeutralButton(getString(R.string.ILL_GET_STAGES), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Intent i = new Intent();
        ComponentName comp =
            new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
        i.setComponent(comp);
        i.setAction("android.intent.action.VIEW");
        i.addCategory("android.intent.category.BROWSABLE");
        Uri uri = Uri.parse("http://groups.google.com/group/clickin-2-da-beat/files");
        i.setData(uri);
        self.startActivity(i);
        finish();
      }
    });
    
    noFilesMessage.setNegativeButton(getString(R.string.QUIT),
        new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });

    noFilesMessage.setCancelable(false);
    noFilesMessage.show();
  }

  private void loadHardCodedRickroll() {
    try {
      Resources res = getResources();
      InputStream fis = res.openRawResource(R.raw.rickroll);
      DocumentBuilder docBuild;
      docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document c2b = docBuild.parse(fis);
      runC2B(c2b);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FactoryConfigurationError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  private void displayStartupMessage() {
    Builder startupMessage = new Builder(this);

    String titleText = getString(R.string.WELCOME);
    startupMessage.setTitle(titleText);

    String message = getString(R.string.BETA_MESSAGE);
    startupMessage.setMessage(message);

    startupMessage.setPositiveButton(getString(R.string.START_GAME), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        displayC2BFiles();
      }
    });
    
    final Activity self = this;
    
    startupMessage.setNeutralButton(getString(R.string.VISIT_WEBSITE), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Intent i = new Intent();
        ComponentName comp =
            new ComponentName("com.android.browser", "com.android.browser.BrowserActivity");
        i.setComponent(comp);
        i.setAction("android.intent.action.VIEW");
        i.addCategory("android.intent.category.BROWSABLE");
        Uri uri = Uri.parse("http://groups.google.com/group/clickin-2-da-beat");
        i.setData(uri);
        self.startActivity(i);
        finish();
      }
    });

    startupMessage.setNegativeButton(getString(R.string.QUIT), new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        finish();
      }
    });

    startupMessage.setCancelable(false);
    startupMessage.show();
  }


  private void displayUpdateMessage() {
    Builder updateMessage = new Builder(this);

    String titleText = getString(R.string.UPDATE_AVAILABLE);
    updateMessage.setTitle(titleText);

    String message = getString(R.string.UPDATE_MESSAGE);
    updateMessage.setMessage(message);

    updateMessage.setPositiveButton("Yes", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Uri marketUri = Uri.parse("market://details?id=" + marketId);
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        startActivity(marketIntent);
        finish();
      }
    });
    
    final Activity self = this;
    

    updateMessage.setNegativeButton("No", new OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        resetGame();
      }
    });

    updateMessage.setCancelable(false);
    updateMessage.show();
  }
  
  public int getCurrentTime() {
    try {
      return background.getCurrentPosition();
    } catch (IllegalStateException e) {
      // This will be thrown if the player is exiting mid-game and the video
      // view is going away at the same time as the foreground is trying to get
      // the position. This error can be safely ignored without doing anything.
      e.printStackTrace();
      return 0;
    }
  }


  // Do beats processing in another thread to avoid hogging the UI thread and
  // generating a "not responding" error
  public class BeatsWriter implements Runnable {
    public void run() {
      writeC2BFile(c2bFileName);
      busyProcessing = false;
    }
  }

  public class BeatsTimingAdjuster implements Runnable {
    public void run() {
      timingAdjuster.setRawBeatTimes(beatTimes);
      busyProcessing = false;
    }
  }

}
