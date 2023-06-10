package com.cavetale.battleboats;

import java.util.*;

public final class Game {
    boolean running;
    Position spawnA;
    Position spawnB;
    Set<UUID> teamA = new HashSet<>();
    Set<UUID> teamB = new HashSet<>();
    Set<UUID> dead = new HashSet<>();
    List<String> items = new ArrayList<>();
    List<String> drops = new ArrayList<>();
    List<String> itemsA = new ArrayList<>();
    List<String> itemsB = new ArrayList<>();
    int bombCooldown;
    String bombTeam = "";
}
