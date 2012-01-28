/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.command;

import java.io.IOException;

import simpleserver.Server;
import simpleserver.bot.BotController.ConnectException;
import simpleserver.bot.EnchantmentDetector;
import simpleserver.stream.StreamTunnel;

public class FindEnchantableItemsCommand extends AbstractCommand implements ServerCommand {
  private Server server;
  private CommandFeedback feedback;

  public FindEnchantableItemsCommand() {
    // temporary name for faster development ;)
    super("fei", "find enchantable items");
  }

  public synchronized void execute(Server server, String message, CommandFeedback feedback) {
    String[] parts = message.trim().split(" ");
    if (parts.length == 1) {
      feedback.send("This command scanns items up to a given ID for enchantability");
      feedback.send("Usage:");
      feedback.send(" fei <max id>: Start scanning (this might take a while)");
      feedback.send(" fei reset: Revert to the default list");
    } else if (parts[1].toLowerCase().equals("reset") || parts[1].toLowerCase().equals("default")) {
      server.data.enchantable.clear();
      server.data.enchantable.loadDefaults();
      server.data.save();
    } else {
      short end;
      try {
        end = Short.valueOf(parts[1]);
      } catch (NumberFormatException e) {
        feedback.send("Error: The provided ID was invalid");
        return;
      }
      this.server = server;
      this.feedback = feedback;
      server.data.enchantable.clear();
      int n = (int) Math.ceil((end - 256) / 35F);
      float time = n * 5 / 60F;
      new BotStarter(256, n).start();
      feedback.send("Scanning process started");
      if (!Server.addressFactory.canCycle() && !server.config.properties.getBoolean("fastScanning")) {
        feedback.send("This process will take about %.1f minutes", time);
        feedback.send("See https://github.com/SimpleServer/SimpleServer/wiki/Item_Scanner for details");
      }
    }
  }

  private void addBot(int start) {
    EnchantmentDetector bot;
    try {
      bot = new EnchantmentDetector(server, start);
    } catch (IOException e1) {
      e1.printStackTrace();
      return;
    }
    try {
      server.bots.connect(bot);
    } catch (ConnectException e) {
      e.printStackTrace();
    }
  }

  private class BotStarter extends Thread {
    private int start;
    private int n;

    BotStarter(int start, int n) {
      this.start = start;
      this.n = n;
    }

    @Override
    public void run() {
      int started = 0;
      int delay = Server.addressFactory.canCycle() || server.config.properties.getBoolean("fastScanning") ? 100 : 5001;
      while (started < n) {
        if (server.bots.size() + server.playerList.size() < server.config.properties.getInt("maxPlayers")) {
          addBot(start + started * 35);
          started += 1;
          if (delay > 100) {
            try {
              sleep(delay);
            } catch (InterruptedException e) {
            }
          }
        } else {
          try {
            sleep(delay);
          } catch (InterruptedException e) {
          }
        }
      }
      while (server.bots.size() > 0) {
        try {
          sleep(100);
        } catch (InterruptedException e) {
        }
      }
      server.data.enchantable.update();
      server.data.save();
      feedback.send("Found %s enchantable items", StreamTunnel.ENCHANTABLE.size());
    }
  }
}