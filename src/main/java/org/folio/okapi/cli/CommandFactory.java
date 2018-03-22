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
    commands.add(new CommandVersion());
  }

  Command create(String arg) {
    Command cmd = null;
    for (Command c : commands) {
      if (c.getName().equals(arg)) {
        return c;
      }
    }
    return null;
  }

  public void help() {
    for (Command c : commands) {
      System.out.println(c.getDescription());
    }
  }

}
