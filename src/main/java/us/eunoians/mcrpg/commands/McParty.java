package us.eunoians.mcrpg.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.eunoians.mcrpg.McRPG;
import us.eunoians.mcrpg.api.exceptions.McRPGPlayerNotFoundException;
import us.eunoians.mcrpg.api.util.FileManager;
import us.eunoians.mcrpg.api.util.Methods;
import us.eunoians.mcrpg.gui.PartyMainGUI;
import us.eunoians.mcrpg.party.Party;
import us.eunoians.mcrpg.party.PartyInvite;
import us.eunoians.mcrpg.party.PartyMember;
import us.eunoians.mcrpg.players.McRPGPlayer;
import us.eunoians.mcrpg.players.PlayerManager;
import us.eunoians.mcrpg.types.PartyPermissions;
import us.eunoians.mcrpg.types.PartyRoles;
import us.eunoians.mcrpg.types.PartyUpgrades;

import java.util.UUID;

public class McParty implements CommandExecutor{
  
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
    String pluginPrefix = McRPG.getInstance().getPluginPrefix();
    if(sender instanceof Player){
      Player p = (Player) sender;
      if(PlayerManager.isPlayerFrozen(p.getUniqueId())){
        return true;
      }
      //Disabled Worlds
      String world = p.getWorld().getName();
      if(McRPG.getInstance().getConfig().contains("Configuration.DisabledWorlds") &&
           McRPG.getInstance().getConfig().getStringList("Configuration.DisabledWorlds").contains(world)){
        return true;
      }
      McRPGPlayer mp;
      try{
        mp = PlayerManager.getPlayer(p.getUniqueId());
      }catch(McRPGPlayerNotFoundException e){
        e.printStackTrace();
        return true;
      }
      if(args.length == 0){
        if(mp.getPartyID() == null){
          if(mp.getPartyInvites().isEmpty()){
            p.sendMessage(Methods.color(p,pluginPrefix + "&cYou have no pending party invites and are not in a party."));
            return true;
          }
          else{
            p.sendMessage(Methods.color(p,pluginPrefix + "&cYou have party invites waiting for you to accept using /mcparty invites"));
            return true;
          }
        }
        Party party = McRPG.getInstance().getPartyManager().getParty(mp.getPartyID());
        //TODO open GUI
        PartyMainGUI partyMainGUI = new PartyMainGUI(mp);
        p.openInventory(partyMainGUI.getGui().getInv());
        return true;
      }
      else{
        if(args[0].equalsIgnoreCase("invites")){
          if(args.length == 1){
            if(mp.getPartyInvites().isEmpty()){
              p.sendMessage(Methods.color(p,pluginPrefix + "&cYou do not have any pending invites."));
              return true;
            }
            else{
              PartyInvite partyInvite = mp.getPartyInvites().elements().nextElement();
              Party party = McRPG.getInstance().getPartyManager().getParty(partyInvite.getPartyID());
              if(party == null){
                try{
                  mp.getPartyInvites().dequeue();
                  p.sendMessage(Methods.color(p,pluginPrefix + "&cThe party you were invited to no longer exists and the invite has been removed."));
                }catch(InterruptedException e){
                  e.printStackTrace();
                }
                return true;
              }
              p.sendMessage(Methods.color(p,pluginPrefix + "&aYou have a pending invite from &e" + party.getName() +
                                            "&a. To accept, do /mcparty invites accept. To deny it and view the next invitation, do /mcparty invites deny"));
              return true;
            }
          }
          else{
            if(args[1].equalsIgnoreCase("accept")){
              if(mp.getPartyID() != null){
                p.sendMessage(Methods.color(p, Methods.color("&cYou are already in a party and can not accept any invites.")));
                return true;
              }
              if(mp.getPartyInvites().isEmpty()){
                p.sendMessage(Methods.color(p,pluginPrefix + "&cYou do not have any pending invites."));
                return true;
              }
              else{
                PartyInvite partyInvite = mp.getPartyInvites().elements().nextElement();
                Party party = McRPG.getInstance().getPartyManager().getParty(partyInvite.getPartyID());
                if(party == null){
                  try{
                    mp.getPartyInvites().dequeue();
                    p.sendMessage(Methods.color(p, pluginPrefix + "&cThe party you were invited to no longer exists and the invite has been removed."));
                  }catch(InterruptedException e){
                    e.printStackTrace();
                  }
                  return true;
                }
                else{
                  if(party.getPartyMembers().size() >= PartyUpgrades.getMemberCountAtTier(party.getPartyUpgrades().get(PartyUpgrades.MEMBER_COUNT))){
                    try{
                      mp.getPartyInvites().dequeue();
                      p.sendMessage(Methods.color(p, pluginPrefix + "&cThe party you were invited is full."));
                    }catch(InterruptedException e){
                      e.printStackTrace();
                    }
                    return true;
                  }
                  else{
                    party.addPlayer(p.getUniqueId());
                    party.saveParty();
                    try{
                      mp.getPartyInvites().dequeue();
                    }catch(InterruptedException e){
                      e.printStackTrace();
                    }
                    mp.setPartyID(party.getPartyID());
                    p.sendMessage(Methods.color(p, pluginPrefix + "&aYou joined " + party.getName()));
                    for(UUID uuid : party.getPartyMembers().keySet()){
                      if(uuid.equals(p.getUniqueId())){
                        continue;
                      }
                      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                      if(offlinePlayer.isOnline()){
                        ((Player) offlinePlayer).sendMessage(Methods.color(p, pluginPrefix + "&a" + p.getName() + " has joined your party!"));
                      }
                    }
                    return true;
                  }
                }
              }
            }
            else if(args[1].equalsIgnoreCase("decline")){
              if(mp.getPartyInvites().isEmpty()){
                p.sendMessage(Methods.color(p, pluginPrefix + "&cYou have no party invites."));
                return true;
              }
              try{
                Party party = McRPG.getInstance().getPartyManager().getParty(mp.getPartyInvites().dequeue().getPartyID());
                if(party == null){
                  p.sendMessage(Methods.color(p, pluginPrefix + "&cThat party no longer exists."));
                }
                else{
                  p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have declined the party invite from " + party.getName()));
                }
              }catch(InterruptedException e){
                e.printStackTrace();
              }
              return true;
            }
          }
        }
        // /mcparty invite player name
        else if(args[0].equalsIgnoreCase("invite")){
          if(args.length < 2){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cSee /mcparty help for more context."));
            return true;
          }
          if(mp.getPartyID() == null){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cYou are not in a party so you could not send an invite."));
            return true;
          }
          else{
            Party party = McRPG.getInstance().getPartyManager().getParty(mp.getPartyID());
            if(party == null){
              mp.setPartyID(null);
              p.sendMessage(Methods.color(p, pluginPrefix + "&cFor some reason your party does not exist so you were removed."));
              return true;
            }
            else{
              PartyMember partyPlayer = party.getPartyMembers().get(p.getUniqueId());
              if(partyPlayer.getPartyRole().getId() <= party.getPartyPermissions().get(PartyPermissions.INVITE_PLAYERS).getId()){
                if(party.getPartyMembers().size() >= PartyUpgrades.getMemberCountAtTier(party.getPartyUpgrades().get(PartyUpgrades.MEMBER_COUNT))){
                  p.sendMessage(Methods.color(p, pluginPrefix + "&cThere are too many players in your party to invite someone else."));
                  return true;
                }
                else{
                  if(Methods.hasPlayerLoggedInBefore(args[1])){
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                    if(offlinePlayer.isOnline()){
                      try{
                        PlayerManager.getPlayer(offlinePlayer.getUniqueId()).getPartyInvites().enqueue(new PartyInvite(party.getPartyID(), offlinePlayer.getUniqueId()));
                        p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have invited the player successfully!"));
                        ((Player) offlinePlayer).sendMessage(Methods.color(p, pluginPrefix + "&aYou have been invited by &e" + p.getDisplayName() + " &ato their party! To join do &e/mcparty invites accept"));
                      }catch(McRPGPlayerNotFoundException e){
                        p.sendMessage(Methods.color(p, pluginPrefix + "&cThere was an issue inviting that player. Please wait a few seconds and try again."));
                      }
                      return true;
                    }
                    else{
                      p.sendMessage(Methods.color(p, pluginPrefix + "&cThat player is not currently online and as such could not be invited"));
                      return true;
                    }
                  }
                  else{
                    p.sendMessage(Methods.color(p, pluginPrefix + "&cThat player has not logged in before."));
                    return true;
                  }
                }
              }
              else{
                p.sendMessage(Methods.color(p, pluginPrefix + "&cYou do not have permission to invite people to your party"));
                return true;
              }
            }
          }
        }
        else if(args[0].equalsIgnoreCase("create")){
          if(mp.getPartyID() != null){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cYou are currently in a party and as such could not make a new one."));
            return true;
          }
          else{
            if(args.length < 2){
              p.sendMessage(Methods.color(p, pluginPrefix + "&cSee the help command"));
              return true;
            }
            if(McRPG.getInstance().getPartyManager().isPartyNameUsed(args[1])){
              p.sendMessage(Methods.color(p, pluginPrefix + "&cThat name is already in use, please use another."));
              return true;
            }
            String lowerCaseName = args[1].toLowerCase();
            for(String s : McRPG.getInstance().getFileManager().getFile(FileManager.Files.FILTER).getStringList("BannedPartyStrings")){
              if(lowerCaseName.contains(s.toLowerCase())){
                p.sendMessage(Methods.color(p, pluginPrefix + "&cYour party name contains an invalid string and could not be accepted."));
                return true;
              }
            }
            Party party = McRPG.getInstance().getPartyManager().addParty(args[1], p.getUniqueId());
            mp.setPartyID(party.getPartyID());
            mp.saveData();
            p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have successfully created the " + party.getName() + " party."));
            return true;
          }
        }
        else if(args[0].equalsIgnoreCase("kick")){
          if(args.length < 2){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cSee /mcparty help for more context."));
            return true;
          }
          if(mp.getPartyID() == null){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cYou are not in a party."));
            return true;
          }
          Party party = McRPG.getInstance().getPartyManager().getParty(mp.getPartyID());
          if(party == null){
            mp.setPartyID(null);
            p.sendMessage(Methods.color(p, pluginPrefix + "&cFor some reason your party does not exist so you were removed."));
            return true;
          }
          else{
            PartyMember partyPlayer = party.getPartyMembers().get(p.getUniqueId());
            if(partyPlayer.getPartyRole().getId() <= party.getPartyPermissions().get(PartyPermissions.KICK_PLAYERS).getId()){
              if(Methods.hasPlayerLoggedInBefore(args[1])){
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                if(offlinePlayer.getUniqueId().equals(p.getUniqueId())){
                  p.sendMessage(Methods.color(p, pluginPrefix + "&cYou can not kick yourself from the party"));
                  return true;
                }
                if(!party.getPartyMembers().containsKey(offlinePlayer.getUniqueId())){
                  p.sendMessage(Methods.color(p, pluginPrefix + "&cThat player is not in your party."));
                  return true;
                }
                PartyMember playerToKick = party.getPartyMembers().get(offlinePlayer.getUniqueId());
                //This lets members kick other members if for some reason people decide to let members have kicking power. I don't control the people
                if(playerToKick.getPartyRole().getId() <= partyPlayer.getPartyRole().getId() && party.getPartyPermissions().get(PartyPermissions.KICK_PLAYERS) != PartyRoles.MEMBER){
                  p.sendMessage(Methods.color(p, pluginPrefix + "&cYou can not kick people at the same rank or higher than you"));
                  return true;
                }
                party.kickPlayer(offlinePlayer.getUniqueId());
                party.saveParty();
                p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have kicked " + offlinePlayer.getName() + " from your party."));
                if(offlinePlayer.isOnline()){
                  try{
                    McRPGPlayer target = PlayerManager.getPlayer(offlinePlayer.getUniqueId());
                    target.setPartyID(null);
                    target.saveData();
                    ((Player) offlinePlayer).sendMessage(Methods.color(p, pluginPrefix + "&aYou have been kicked from " + party.getName() + "."));
                  }catch(McRPGPlayerNotFoundException e){
                    McRPGPlayer target = new McRPGPlayer(offlinePlayer.getUniqueId());
                    target.setPartyID(null);
                    target.saveData();
                  }
                  return true;
                }
                else{
                  p.sendMessage(Methods.color(p, pluginPrefix + "&cThat player is not currently online and as such could not be invited"));
                  return true;
                }
              }
              else{
                p.sendMessage(Methods.color(p, pluginPrefix + "&cThat player has not logged in before."));
                return true;
              }
            }
            else{
              p.sendMessage(Methods.color(p, pluginPrefix + "&cYou do not have permission to kick people from your party"));
              return true;
            }
          }
        }
        else if(args[0].equalsIgnoreCase("disband")){
          if(mp.getPartyID() == null){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cYou are not in a party so you could not send an invite."));
            return true;
          }
          else{
            Party party = McRPG.getInstance().getPartyManager().getParty(mp.getPartyID());
            if(party == null){
              mp.setPartyID(null);
              p.sendMessage(Methods.color(p, pluginPrefix + "&cFor some reason your party does not exist so you were removed."));
              return true;
            }
            else{
              PartyMember partyMember = party.getPartyMembers().get(p.getUniqueId());
              if(partyMember.getPartyRole() != PartyRoles.OWNER){
                p.sendMessage(Methods.color(p, pluginPrefix + "&cOnly party owners can disband the party."));
                return true;
              }
              else{
                McRPG.getInstance().getPartyManager().removeParty(party.getPartyID());
                for(UUID uuid : party.getPartyMembers().keySet()){
                  if(!uuid.equals(p.getUniqueId())){
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if(offlinePlayer.isOnline()){
                      ((Player) offlinePlayer).sendMessage(Methods.color(pluginPrefix + "&cYour party was disbanded."));
                    }
                  }
                }
                p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have disbanded your party."));
                return true;
              }
            }
          }
        }
        else if(args[0].equalsIgnoreCase("leave")){
          if(mp.getPartyID() == null){
            p.sendMessage(Methods.color(p, pluginPrefix + "&cYou are not in a party."));
            return true;
          }
          else{
            Party party = McRPG.getInstance().getPartyManager().getParty(mp.getPartyID());
            if(party == null){
              mp.setPartyID(null);
              p.sendMessage(Methods.color(p, pluginPrefix + "&cFor some reason your party does not exist so you were removed."));
              return true;
            }
            else{
              if(party.getPartyMembers().size() == 1){
                p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have disbanded your party."));
              }
              else{
                p.sendMessage(Methods.color(p, pluginPrefix + "&aYou have left your party."));
              }
              mp.setPartyID(null);
              party.kickPlayer(p.getUniqueId());
              for(UUID uuid : party.getPartyMembers().keySet()){
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if(offlinePlayer.isOnline()){
                  ((Player) offlinePlayer).sendMessage(Methods.color(p, pluginPrefix + "&c" + p.getName() + " has left your party."));
                }
              }
            }
            return true;
          }
        }
      }
      return true;
    }
    else{
      sender.sendMessage(Methods.color("&cYou can not send a party command via console"));
      return true;
    }
  }
}
