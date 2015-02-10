package com.spinalcraft.bigbrother;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spinalcraft.spinalpack.Co;
import com.spinalcraft.spinalpack.Spinalpack;

class PlayerID{
	String uuid;
	String name;
}

class OldUsername{
	String name;
	long changedToAt;
}

public class BigBrother extends JavaPlugin implements Listener{
	ConsoleCommandSender console;
	
	@Override
	public void onEnable(){
		console = Bukkit.getConsoleSender();
		
		console.sendMessage(Spinalpack.code(Co.BLUE) + "BigBrother online!");
		
		createUsernameTable();
		
		getServer().getPluginManager().registerEvents((Listener)this,  this);
	}
	
	@Override
	public void onDisable(){
		
	}
	
	private void createUsernameTable(){
		
		String query = "CREATE TABLE IF NOT EXISTS Usernames (uuid VARCHAR(36), username VARCHAR(31), PRIMARY KEY (uuid, username))";
		Spinalpack.update(query);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("original")) {
			String uuid, username;
			if(args.length == 0)
				return false;
			username = args[0];
			Player player = Bukkit.getPlayer(username);
			if(player == null)
				try {
					uuid = UUIDFetcher.getUUIDOf(username).toString();
				} catch (Exception e) {
					sender.sendMessage("");
					sender.sendMessage(Spinalpack.code(Co.RED) + "Player could not be found!");
					return true;
				}
			else
				uuid = player.getUniqueId().toString();
			ArrayList<OldUsername> names = usernames(uuid, username);
			sender.sendMessage("");
			if(names.size() == 1)
				sender.sendMessage(Spinalpack.code(Co.GREEN) + username + Spinalpack.code(Co.GOLD) + " has never changed their name.");
			else{
				sender.sendMessage(Spinalpack.code(Co.GOLD) + "Originally " + Spinalpack.code(Co.GREEN) + names.get(0).name);
				sender.sendMessage("");
				
				for(int i = 1; i < names.size(); i++){
					DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
					Date date = new Date(names.get(i).changedToAt);
					String formattedDate = df.format(date);
					sender.sendMessage(Spinalpack.code(Co.BLUE) + formattedDate + Spinalpack.code(Co.GOLD) + " - changed to " + Spinalpack.code(Co.GREEN) +  names.get(i).name);
				}
			}
			return true;
		}
		return false;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		
		PreparedStatement stmt;
		String query = "INSERT IGNORE INTO Usernames (uuid, username) VALUES (?, ?)";
		try {
			stmt = Spinalpack.prepareStatement(query);
			stmt.setString(1, player.getUniqueId().toString());
			stmt.setString(2, player.getName());
			
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<OldUsername> usernames(String uuid, String current){
		ArrayList<OldUsername> nameList = new ArrayList<OldUsername>();
		Gson gson = new GsonBuilder().create();
		String compactUuid = uuid.replace("-", "");
		try {
			URL url = new URL("https://api.mojang.com/user/profiles/" + compactUuid + "/names");
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			OldUsername[] oldNames = gson.fromJson(reader, OldUsername[].class);
			
			//HACK: I don't trust Mojang to always give the original name first, so I re-order it myself
			for(int i = 0; i < oldNames.length; i++){
				if(oldNames[i].changedToAt == 0)
					nameList.add(oldNames[i]);
			}
			for(int i = 0; i < oldNames.length; i++){
				if(oldNames[i].changedToAt != 0)
					nameList.add(oldNames[i]);
			}
			reader.close();
			conn.disconnect();
			return nameList;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
