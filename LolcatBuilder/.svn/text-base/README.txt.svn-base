Simple Lolcat Builder application, version 2.0.

Instructions:
  (1) Take photo of cat using Camera
  (2) Run Lolcat Builder
  (3) Pick photo
  (4) Add caption(s). Touch and drag to reposition.
  (5) Save and share.

A Lolcat, by the way, is an image of a cat (or some other animal),
captioned in a particular style.  See http://en.wikipedia.org/wiki/Lolcat
or http://images.google.com/images?q=lolcat for examples.

This is an open source sample application for the Android platform.
It's designed to demonstrate:

  - A simple layout with ImageView + Buttons
  - Using different resources for portrait and landscape modes
  - Picking a photo using the ACTION_GET_CONTENT Intent
  - Displaying a Bitmap in an ImageView, handling touch events, converting
    between Bitmap and View coordinate systems
  - Bitmap manipulation: rescaling, rendering text
  - Creating Dialogs using the onCreateDialog() API; using Dialogs that
    contain custom views
  - Preserving the state of the UI across orientation changes
  - Writing a bitmap to the SD card
  - Using the MediaScanner to scan files on the SD card
  - Viewing and Sharing an image on the SD card

Author: David Brown

TODO / known bugs:

  - Provide UI to add more than 2 captions.  (Maybe leave the initial
    dialog as-is, but provide a menu item to add extra captions, one at a
    time?)

  - Reword all onscreen UI to use LOLspeak? (or maybe not)

  - Reword all comments and variable names in this file to use LOLspeak?
    (or maybe not)

  - Use the actual Impact font, if there's a free version out there.
    (Individual applications can have their own fonts; just use
    Typeface.createFromAsset() to load the font from a file.)

    And if possible use a real "white fill, black stroke" style, rather
    than simulating it with the Paint.setShadowLayer() feature.

  - Bug: we sometimes hit an OutOfMemoryError ("bitmap size exceeds VM
    budget") when loading 2 or 3 photos in a row.  (This can happen even
    though we release the previous bitmap *and* force a GC each time...)

  - Hitting ENTER in the bottom textfield of the caption dialog should
    always focus the OK button, not just move the focus "downward" (since
    the Cancel button can end up focused if you typed a long string.)

  - Provide UI to take a new picture (using the Camera) directly
    from this activity?

    (I originally assumed this wouldn't be very useful in practice, since
    it usually takes several attempts to get a good picture of a cat,
    especially if the cat isn't cooperating.  So it's usually easier to
    use the regular Camera app to take the picture...  But this is still
    an obvious enough feature, and you're not always taking a picture of a
    cat anyway, so I should add it.)

    Also, once we can directly launch the camera from the LolcatActivity,
    add a way for a Home screen shortcut to jump directly to
    camera-acquire mode; you'd click the shortcut, point camera at cat,
    click the shutter, enter a caption, and be done (thanks to Keir for
    the suggestion).

    And we could even consider registering for the CAMERA_BUTTON intent,
    so that longpressing the camera button could be a shortcut to the
    Lolcat builder.  (It would be too annoying to do this by default, but
    I could at least provide a menu item that would make the necessary
    registerReceiver() call...)

  - Provide UI to upload straight to some public Lolcat site?

  - Figure out some way to reposition captions using the trackball.
    (Currently it's touch-only.)

  - Provide UI to change font size (and style and color too?)

  - Bug: the "Save & share" button should *not* create a whole new image
    on the SD card if you haven't changed the photo or captions since the
    last time you saved!  (Instead, it should just go straight to the
    "save succeeded" dialog, using the same filename and URI as before.)
