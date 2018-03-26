package org.folio.okapi.cli;

import java.util.LinkedList;
import java.util.List;

public class CommandFactory {

  List<Command> commands = new LinkedList<>();

  CommandFactory() {
    commands.add(new CommandDelete());
    commands.add(new CommandEnv());
    commands.add(new CommandGet());
    commands.add(new CommandInstall());
    commands.add(new CommandLogin());
    commands.add(new CommandLogout());
    commands.add(new CommandPost());
    commands.add(new CommandPull());
    commands.add(new CommandPut());
    commands.add(new CommandTenant());
    commands.add(new CommandUpgrade());
    commands.add(new CommandVersion());
    commands.add(new OptionDeploy());
    commands.add(new OptionDisable());
    commands.add(new OptionEnable());
    commands.add(new OptionNoTenant());
    commands.add(new OptionOkapiUrl());
    commands.add(new OptionPullUrl());
    commands.add(new OptionTenant());
  }

  Command create(String arg) {
    for (Command c : commands) {
      String des = c.getDescription();
      if (des.startsWith("-")) {
        int i = des.indexOf('=');
        if (i != -1) {
          i++; // match includes =
        } else {
          i = des.indexOf(':');
        }
        if (arg.startsWith(des.substring(0, i))) {
          return c;
        }
      } else {
        int i;
        for (i = 0; i < des.length(); i++) {
          char ch = des.charAt(i);
          if (ch == ' ' || ch == ':') {
            break;
          }
        }
        String cmdName = des.substring(0, i);
        if (cmdName.equals(arg)) {
          return c;
        }
      }
    }
    return null;
  }

  public int noArgs(Command command) {
    final String des = command.getDescription();
    int no = 0;
    if (!des.startsWith("-")) {
      for (int i = 0; i < des.length(); i++) {
        char c = des.charAt(i);
        if (c == ':') {
          break;
        }
        if (c == ' ') {
          no++;
        }
      }
    }
    return no;
  }

  public void help() {
    for (Command c : commands) {
      System.out.println(c.getDescription());
    }
  }


}
