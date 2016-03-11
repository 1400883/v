package com.example.tuomas.myfirstapp;

import java.io.Serializable;

// Based on the few sources I read, GuiManager's life expectancy
// as a singleton will be just as long as that of the mechanism
// built around the Bundle object. Therefore, state enums
// introduced in GuiManager wil hold their values across
// activity restart cycles just as fine.
//
// Other than state enums, there's EditText input field data
// (search bar and name & IBAN inputs) that possibly needs to
// be restored. This is taken care by the system when
// Bundle is passed to base class constructor in
// onRestoreInstanceState() / onCreate(). However, restoring
// correct screen content after activity restart benefits a
// a great deal from storing these, as access to values in
// the Bundle object may be tricky, I'm not sure.
//
// Finally, there is still one value left to worry about:
// The original account owner name displayed in account
// edit screen when it appears on listview item click. This
// is used to
// a) target record deletion to the original entry in edit screen
// b) skip replace confirmation dialog in edit screen when the
// edited name is not found unique among the records but not changed
// either.
// The user may well have replaced the name by the time the app
// has its activity paused/stopped/destroyed/molested/etc prior
// to activity restart, leaving the original account owner data
// exposed to permanent loss condition. This original name is
// required other times during execution and will be stored here
// frequently, so no need to do that during onSaveInstanceState()
public class SessionStorage implements Serializable {
  private String currentName;
  private String currentIban;
  private String originalName;

  public String getCurrentName() { return currentName; }
  public String getCurrentIban() { return currentIban; }
  public String getOriginalName() { return originalName; }

  public void setCurrentName(String name) { currentName = name; }
  public void setCurrentIban(String iban) { currentIban = iban; }
  public void setOriginalName(String name) { originalName = name; }
}
