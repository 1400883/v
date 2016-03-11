package com.example.tuomas.ibanbookkeeper;

import java.io.Serializable;

// Based on the few sources I read, GuiManager's life expectancy
// as a singleton will be just as long as that of the mechanism
// built around the Bundle object. Therefore, state enums
// introduced in GuiManager wil hold their values across
// activity restart cycles just as fine.
//
// Other than state enums, there's EditText input field data
// (search bar and name & IBAN inputs). Restoring the fields
// is taken care by the system when Bundle is passed to base
// class constructor in onRestoreInstanceState() / onCreate().
// However, storing field inputs is a too useful not to be done -
// the process of restoring correct screen content after activity
// restart couldn't be any easier with these. Combined with the
// original account owner name (see the chapter below), all
// required data can be fully constructed and then directly
// passed to GuiManager tree traversal methods, which will take
// care of displaying correct screen content.
//
// Finally, there is still one value left to worry about:
// The original account owner name displayed in edit
// account screen when it appears on listview item click.
// This is used to
// a) target record deletion to the original entry (and display it
// in the replace confirmation screen)
// b) skip replace confirmation screen edit account mode when the
// edited entry name has not changed (fails the uniqueness test)
//
// The user may well have replaced the name by the time the app
// has its activity paused/stopped/destroyed/molested/whatever
// prior to activity restart, leaving the original account owner
// data exposed to permanent loss condition. This original name is
// required other times during execution and will be stored here
// frequently, so no need to do that during onSaveInstanceState()
public class SessionStorage implements Serializable {
  private String currentName;
  private String currentIban;
  private String originalName;

  public String getCurrentName() { return currentName != null ? currentName: ""; }
  public String getCurrentIban() { return currentIban != null ? currentIban: ""; }
  public String getOriginalName() { return originalName != null ? originalName : ""; }

  public void setCurrentName(String name) { currentName = name; }
  public void setCurrentIban(String iban) { currentIban = iban; }
  public void setOriginalName(String name) { originalName = name; }
}
