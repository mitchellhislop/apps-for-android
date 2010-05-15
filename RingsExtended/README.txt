Rings Extended v1.0: A ringtone picker replacement.

This application provides a replacement for the standard ringtone picker
included in the Android system, allowing it to be used for selecting the
default ringtone and notification, specific per-contact ringtones, per-app
ringtones, and alarms.

There are two parts to the application:

(1) RingsExtended.java is the main application, which is run when the user
needs to pick a ringtone.  It provides information about the current ringtone
and basic options available for it (silent, use default, keep current), an
item to run the built-in ringtone picker, and one or more additional items
for every activity in the system that can return an audio file that can be
played.

(2) MusicPicker.java is a generic activity for selecting one of the music
tracks on the device.  This is needed because Android does not currently
come with a standard UI for this.  Note that RingsExtended has no built-in
knowledge about MusicPicker: it just asks who can return audio content, one
of which happens to be MusicPicker.
